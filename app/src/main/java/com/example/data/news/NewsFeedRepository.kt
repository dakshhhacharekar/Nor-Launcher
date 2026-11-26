package com.example.data.news

import android.content.Context
import com.example.data.model.NewsArticle
import com.example.data.model.NewsFeedCache
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsTopic
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Process-wide live state holder so background WorkManager refreshes immediately notify
 * any active ViewModel / Compose UI without polling.
 */
object NewsFeedStore {
  private val _cache = MutableStateFlow(NewsFeedCache())
  val cache: StateFlow<NewsFeedCache> = _cache.asStateFlow()

  fun update(cache: NewsFeedCache) {
    _cache.value = cache
  }
}

data class NewsRefreshOutcome(
  val success: Boolean,
  val fetchedArticleCount: Int,
  val cache: NewsFeedCache,
)

class NewsFeedRepository(
  private val appContext: Context,
) {
  private val httpClient: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(10, TimeUnit.SECONDS)
      .readTimeout(12, TimeUnit.SECONDS)
      .callTimeout(18, TimeUnit.SECONDS)
      .followRedirects(true)
      .followSslRedirects(true)
      .build()

  private val cacheFile: File
    get() = File(appContext.filesDir, CACHE_FILE_NAME)

  val cacheFlow: StateFlow<NewsFeedCache> = NewsFeedStore.cache

  suspend fun loadCachedFeed(): NewsFeedCache =
    withContext(Dispatchers.IO) {
      val loaded = readCacheFromDisk()
      NewsFeedStore.update(loaded)
      loaded
    }

  suspend fun refreshFeeds(sources: List<NewsFeedSource>): NewsRefreshOutcome =
    withContext(Dispatchers.IO) {
      val enabledSources = sources.filter { it.isEnabled }
      if (enabledSources.isEmpty()) {
        val emptyCache =
          NewsFeedCache(
            articles = emptyList(),
            lastUpdatedEpochMs = System.currentTimeMillis(),
            lastErrorMessage = null,
          )
        writeCacheToDisk(emptyCache)
        NewsFeedStore.update(emptyCache)
        return@withContext NewsRefreshOutcome(
          success = true,
          fetchedArticleCount = 0,
          cache = emptyCache,
        )
      }

      val nowMs = System.currentTimeMillis()
      val perSourceResults: List<List<NewsArticle>?> =
        coroutineScope {
          enabledSources
            .map { source ->
              async { fetchSingleSource(source = source, fallbackNowMs = nowMs) }
            }
            .awaitAll()
        }

      val succeededCount = perSourceResults.count { it != null }
      if (succeededCount == 0) {
        val existing = readCacheFromDisk()
        val updated = existing.copy(lastErrorMessage = "Network unavailable")
        NewsFeedStore.update(updated)
        return@withContext NewsRefreshOutcome(
          success = false,
          fetchedArticleCount = 0,
          cache = updated,
        )
      }

      val seenLinks = HashSet<String>()
      val merged =
        perSourceResults
          .filterNotNull()
          .flatten()
          .sortedByDescending { it.publishedAtEpochMs }
          .filter { article ->
            val key = article.link.substringBefore('#').lowercase()
            seenLinks.add(key)
          }
          .take(MAX_CACHED_ARTICLES)

      val newCache =
        NewsFeedCache(
          articles = merged,
          lastUpdatedEpochMs = nowMs,
          lastErrorMessage = null,
        )
      writeCacheToDisk(newCache)
      NewsFeedStore.update(newCache)
      NewsRefreshOutcome(
        success = true,
        fetchedArticleCount = merged.size,
        cache = newCache,
      )
    }

  private fun fetchSingleSource(
    source: NewsFeedSource,
    fallbackNowMs: Long,
  ): List<NewsArticle>? {
    return runCatching {
      val request =
        Request.Builder()
          .url(source.feedUrl)
          .header("User-Agent", USER_AGENT)
          .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
          .build()

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val body = response.body ?: return null
        val bytes = body.source().let { src ->
          src.request(MAX_FEED_BYTES)
          val buffer = src.buffer.clone()
          val readLen = minOf(buffer.size, MAX_FEED_BYTES)
          buffer.readUtf8(readLen)
        }
        RssFeedParser.parse(
          xmlContent = bytes,
          source = source,
          fallbackNowMs = fallbackNowMs,
        )
      }
    }.getOrNull()
  }

  private fun readCacheFromDisk(): NewsFeedCache {
    val file = cacheFile
    if (!file.exists()) return NewsFeedCache()
    return runCatching {
      val root = JSONObject(file.readText())
      val updatedAt = root.optLong("updatedAt", 0L)
      val array = root.optJSONArray("articles") ?: JSONArray()
      val articles =
        buildList(array.length()) {
          for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val link = obj.optString("link").takeIf { RssFeedParser.isValidHttpUrl(it) } ?: continue
            val title = obj.optString("title").takeIf { it.isNotBlank() } ?: continue
            add(
              NewsArticle(
                id = obj.optString("id").ifBlank { link.hashCode().toString() },
                title = title,
                summary = obj.optString("summary"),
                link = link,
                sourceId = obj.optString("sourceId"),
                sourceName = obj.optString("sourceName"),
                topic = NewsTopic.fromId(obj.optString("topic")),
                publishedAtEpochMs = obj.optLong("publishedAt", updatedAt),
                imageUrl = obj.optString("imageUrl").takeIf { RssFeedParser.isValidHttpUrl(it) },
              )
            )
          }
        }
      NewsFeedCache(
        articles = articles,
        lastUpdatedEpochMs = updatedAt,
        lastErrorMessage = null,
      )
    }.getOrDefault(NewsFeedCache())
  }

  private fun writeCacheToDisk(cache: NewsFeedCache) {
    runCatching {
      val array = JSONArray()
      cache.articles.forEach { article ->
        array.put(
          JSONObject().apply {
            put("id", article.id)
            put("title", article.title)
            put("summary", article.summary)
            put("link", article.link)
            put("sourceId", article.sourceId)
            put("sourceName", article.sourceName)
            put("topic", article.topic.id)
            put("publishedAt", article.publishedAtEpochMs)
            if (article.imageUrl != null) {
              put("imageUrl", article.imageUrl)
            }
          }
        )
      }
      val root =
        JSONObject().apply {
          put("updatedAt", cache.lastUpdatedEpochMs)
          put("articles", array)
        }
      val tmpFile = File(appContext.filesDir, "$CACHE_FILE_NAME.tmp")
      tmpFile.writeText(root.toString())
      if (!tmpFile.renameTo(cacheFile)) {
        cacheFile.writeText(root.toString())
        tmpFile.delete()
      }
    }
  }

  companion object {
    private const val CACHE_FILE_NAME = "nor_news_cache_v1.json"
    private const val MAX_CACHED_ARTICLES = 60
    private const val MAX_FEED_BYTES = 1_500_000L
    private const val USER_AGENT = "NorLauncher/1.0 (Android RSS Reader)"
  }
}
