package com.example.data.news

import android.util.Xml
import androidx.core.text.HtmlCompat
import com.example.data.model.NewsArticle
import com.example.data.model.NewsFeedSource
import java.io.StringReader
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.xmlpull.v1.XmlPullParser

/**
 * Parses both RSS 2.0 (`<item>`) and Atom 1.0 (`<entry>`) XML feeds using Android's [XmlPullParser].
 * Safely sanitizes HTML summaries, validates http(s) links, and extracts thumbnail image URLs.
 */
object RssFeedParser {

  private const val MAX_ITEMS_PER_SOURCE = 18
  private const val MAX_SUMMARY_CHARS = 220

  private val IMG_SRC_REGEX =
    Regex("""<img[^>]+src\s*=\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)
  private val WHITESPACE_REGEX = Regex("""\s+""")

  private val RFC_DATE_PATTERNS =
    listOf(
      "EEE, dd MMM yyyy HH:mm:ss Z",
      "EEE, d MMM yyyy HH:mm:ss Z",
      "EEE, dd MMM yyyy HH:mm:ss zzz",
      "EEE, d MMM yyyy HH:mm:ss zzz",
      "dd MMM yyyy HH:mm:ss Z",
      "yyyy-MM-dd'T'HH:mm:ssXXX",
      "yyyy-MM-dd'T'HH:mm:ss'Z'",
      "yyyy-MM-dd HH:mm:ss",
    )

  fun parse(
    xmlContent: String,
    source: NewsFeedSource,
    fallbackNowMs: Long = System.currentTimeMillis(),
  ): List<NewsArticle> {
    if (xmlContent.isBlank()) return emptyList()

    val articles = ArrayList<NewsArticle>(MAX_ITEMS_PER_SOURCE)
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
    parser.setInput(StringReader(xmlContent.trimStart('\uFEFF', ' ', '\n', '\r', '\t')))

    var insideItem = false
    var isAtomEntry = false
    var title = ""
    var summaryRaw = ""
    var link = ""
    var pubDateRaw = ""
    var imageUrl: String? = null
    var guid = ""

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT && articles.size < MAX_ITEMS_PER_SOURCE) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          val localName = parser.name?.lowercase(Locale.US).orEmpty()
          val namespace = parser.namespace.orEmpty().lowercase(Locale.US)

          if (!insideItem) {
            if (localName == "item" || localName == "entry") {
              insideItem = true
              isAtomEntry = localName == "entry"
              title = ""
              summaryRaw = ""
              link = ""
              pubDateRaw = ""
              imageUrl = null
              guid = ""
            }
          } else {
            when (localName) {
              "title" -> {
                if (title.isBlank()) {
                  title = readTextSafe(parser)
                }
              }
              "link" -> {
                val href = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                val rel = parser.getAttributeValue(null, "rel")?.trim().orEmpty()
                if (isAtomEntry && href.isNotEmpty()) {
                  if (link.isBlank() || rel.equals("alternate", ignoreCase = true)) {
                    link = href
                  }
                } else {
                  val textLink = readTextSafe(parser)
                  if (link.isBlank() && textLink.isNotBlank()) {
                    link = textLink
                  }
                }
              }
              "description", "summary" -> {
                val text = readTextSafe(parser)
                if (summaryRaw.isBlank() && text.isNotBlank()) {
                  summaryRaw = text
                }
              }
              "encoded", "content" -> {
                val urlAttr = parser.getAttributeValue(null, "url")?.trim()
                val mediumAttr = parser.getAttributeValue(null, "medium").orEmpty().lowercase(Locale.US)
                val typeAttr = parser.getAttributeValue(null, "type").orEmpty().lowercase(Locale.US)
                val isMediaTag =
                  namespace.contains("media") ||
                    parser.prefix.equals("media", ignoreCase = true) ||
                    urlAttr != null
                if (
                  imageUrl == null &&
                    urlAttr != null &&
                    isValidHttpUrl(urlAttr) &&
                    (mediumAttr == "image" || typeAttr.startsWith("image/") || isImageUrl(urlAttr))
                ) {
                  imageUrl = urlAttr
                }
                if (!isMediaTag || urlAttr == null) {
                  val text = readTextSafe(parser)
                  if (imageUrl == null) {
                    imageUrl = extractFirstImageFromHtml(text)
                  }
                  if (summaryRaw.isBlank() && text.isNotBlank()) {
                    summaryRaw = text
                  }
                }
              }
              "pubdate", "published", "updated", "date" -> {
                val text = readTextSafe(parser)
                if (pubDateRaw.isBlank() && text.isNotBlank()) {
                  pubDateRaw = text
                }
              }
              "guid", "id" -> {
                val text = readTextSafe(parser)
                if (guid.isBlank() && text.isNotBlank()) {
                  guid = text
                }
              }
              "enclosure" -> {
                val type = parser.getAttributeValue(null, "type").orEmpty().lowercase(Locale.US)
                val url = parser.getAttributeValue(null, "url")?.trim()
                if (
                  imageUrl == null &&
                    url != null &&
                    isValidHttpUrl(url) &&
                    (type.startsWith("image/") || isImageUrl(url))
                ) {
                  imageUrl = url
                }
              }
              "thumbnail" -> {
                val url = parser.getAttributeValue(null, "url")?.trim()
                if (imageUrl == null && isValidHttpUrl(url)) {
                  imageUrl = url
                }
              }
              "image" -> {
                val urlAttr = parser.getAttributeValue(null, "href")?.trim()
                  ?: parser.getAttributeValue(null, "url")?.trim()
                if (imageUrl == null && isValidHttpUrl(urlAttr)) {
                  imageUrl = urlAttr
                }
              }
            }
          }
        }
        XmlPullParser.END_TAG -> {
          val localName = parser.name?.lowercase(Locale.US).orEmpty()
          if (insideItem && ((isAtomEntry && localName == "entry") || (!isAtomEntry && localName == "item"))) {
            insideItem = false
            val cleanTitle = cleanPlainText(title)
            val validLink = link.trim().takeIf { isValidHttpUrl(it) }
            if (cleanTitle.isNotBlank() && validLink != null) {
              val resolvedImage = imageUrl ?: extractFirstImageFromHtml(summaryRaw)
              val cleanSummary =
                cleanPlainText(summaryRaw).let { text ->
                  if (text.length > MAX_SUMMARY_CHARS) {
                    text.take(MAX_SUMMARY_CHARS - 1).trimEnd() + "…"
                  } else {
                    text
                  }
                }
              val publishedAt = parseDateToEpochMs(pubDateRaw) ?: fallbackNowMs
              val stableId = (guid.ifBlank { validLink }).hashCode().toString()

              articles.add(
                NewsArticle(
                  id = "${source.id}_$stableId",
                  title = cleanTitle,
                  summary = cleanSummary,
                  link = validLink,
                  sourceId = source.id,
                  sourceName = source.name,
                  topic = source.topic,
                  publishedAtEpochMs = publishedAt,
                  imageUrl = resolvedImage,
                )
              )
            }
          }
        }
      }
      eventType = parser.next()
    }

    return articles
  }

  private fun readTextSafe(parser: XmlPullParser): String {
    var result = ""
    runCatching {
      if (parser.next() == XmlPullParser.TEXT) {
        result = parser.text.orEmpty()
        parser.nextTag()
      }
    }
    return result.trim()
  }

  private fun cleanPlainText(raw: String): String {
    if (raw.isBlank()) return ""
    val decoded =
      runCatching {
        HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
      }.getOrDefault(raw)
    return decoded
      .replace('\uFFFC', ' ') // Strip inline image replacement characters from HtmlCompat
      .replace(WHITESPACE_REGEX, " ")
      .trim()
  }

  private fun extractFirstImageFromHtml(html: String): String? {
    if (html.isBlank()) return null
    val match = IMG_SRC_REGEX.find(html) ?: return null
    val candidate = match.groupValues.getOrNull(1)?.trim() ?: return null
    return candidate.takeIf { isValidHttpUrl(it) && !it.contains("pixel", ignoreCase = true) }
  }

  private fun isImageUrl(url: String): Boolean {
    val lower = url.substringBefore('?').lowercase(Locale.US)
    return lower.endsWith(".jpg") ||
      lower.endsWith(".jpeg") ||
      lower.endsWith(".png") ||
      lower.endsWith(".webp") ||
      lower.endsWith(".gif")
  }

  internal fun isValidHttpUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    val trimmed = url.trim()
    return trimmed.startsWith("https://", ignoreCase = true) ||
      trimmed.startsWith("http://", ignoreCase = true)
  }

  internal fun parseDateToEpochMs(rawDate: String): Long? {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return null

    // Fast path for ISO-8601 (Atom / dc:date) & RFC-1123 (RSS 2.0) via java.time (minSdk 26)
    runCatching { return Instant.parse(trimmed).toEpochMilli() }
    runCatching { return OffsetDateTime.parse(trimmed).toInstant().toEpochMilli() }
    runCatching {
      return ZonedDateTime.parse(trimmed, DateTimeFormatter.RFC_1123_DATE_TIME)
        .toInstant()
        .toEpochMilli()
    }

    for (pattern in RFC_DATE_PATTERNS) {
      val parsed =
        runCatching {
          SimpleDateFormat(pattern, Locale.US).apply { isLenient = true }.parse(trimmed)?.time
        }.getOrNull()
      if (parsed != null && parsed > 0L) return parsed
    }
    return null
  }
}
