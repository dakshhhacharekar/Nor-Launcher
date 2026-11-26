package com.example.data.search

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

/**
 * Fetches live web autocomplete suggestions from DuckDuckGo's public suggestion endpoint
 * (`https://duckduckgo.com/ac/?q=...&type=list`) using OkHttp with short timeouts and clean
 * offline degradation.
 */
class WebSuggestionRepository(
  private val context: Context,
) {

  private val client: OkHttpClient =
    OkHttpClient.Builder()
      .connectTimeout(3_500L, TimeUnit.MILLISECONDS)
      .readTimeout(3_500L, TimeUnit.MILLISECONDS)
      .callTimeout(4_500L, TimeUnit.MILLISECONDS)
      .build()

  suspend fun fetchSuggestions(rawQuery: String, limit: Int = 5): List<String> =
    withContext(Dispatchers.IO) {
      val query = rawQuery.trim()
      if (query.length < 2 || !isNetworkAvailable()) {
        return@withContext emptyList()
      }

      val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
      val url = "https://duckduckgo.com/ac/?q=$encoded&type=list"
      val request =
        Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .header("User-Agent", "NorLauncher/1.0 (Android)")
          .build()

      runCatching {
        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) return@use emptyList()
          val body = response.body?.string()?.trim().orEmpty()
          if (body.isEmpty()) return@use emptyList()
          parseSuggestions(body, query, limit)
        }
      }.getOrElse { emptyList() }
    }

  private fun parseSuggestions(jsonBody: String, query: String, limit: Int): List<String> {
    val root = JSONArray(jsonBody)
    val results = mutableListOf<String>()

    // Format 1 (`type=list`): ["query", ["suggestion 1", "suggestion 2", ...]]
    if (root.length() >= 2 && root.optJSONArray(1) != null) {
      val list = root.getJSONArray(1)
      for (i in 0 until list.length()) {
        val phrase = list.optString(i).trim()
        if (phrase.isNotEmpty() && !phrase.equals(query, ignoreCase = true)) {
          results += phrase
        }
        if (results.size >= limit) break
      }
      return results
    }

    // Format 2 fallback: [{"phrase": "suggestion 1"}, ...]
    for (i in 0 until root.length()) {
      val obj = root.optJSONObject(i) ?: continue
      val phrase = obj.optString("phrase").trim()
      if (phrase.isNotEmpty() && !phrase.equals(query, ignoreCase = true)) {
        results += phrase
      }
      if (results.size >= limit) break
    }
    return results
  }

  private fun isNetworkAvailable(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
      ?: return true
    val activeNetwork = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
