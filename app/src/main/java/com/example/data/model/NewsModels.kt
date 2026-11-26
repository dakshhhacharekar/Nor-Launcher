package com.example.data.model

enum class NewsTopic(val id: String) {
  ALL("all"),
  INDIA("india"),
  WORLD("world"),
  TECH("tech"),
  BUSINESS("business"),
  SCIENCE("science"),
  SPORTS("sports"),
  ENTERTAINMENT("entertainment"),
  CUSTOM("custom");

  companion object {
    /** Selectable news contexts for user personalization. */
    val selectableContexts: List<NewsTopic> =
      listOf(INDIA, WORLD, TECH, BUSINESS, SCIENCE, SPORTS, ENTERTAINMENT)

    fun fromId(id: String?): NewsTopic = entries.firstOrNull { it.id == id } ?: INDIA
  }
}

data class NewsPersonalization(
  val isCompleted: Boolean = false,
  val selectedContexts: Set<NewsTopic> = emptySet(),
  val focusKeywords: List<String> = emptyList(),
  val strictKeywordFilter: Boolean = false,
)

data class NewsFeedSource(
  val id: String,
  val name: String,
  val feedUrl: String,
  val topic: NewsTopic,
  val isEnabled: Boolean = false,
  val isCustom: Boolean = false,
)

data class NewsArticle(
  val id: String,
  val title: String,
  val summary: String,
  val link: String,
  val sourceId: String,
  val sourceName: String,
  val topic: NewsTopic,
  val publishedAtEpochMs: Long,
  val imageUrl: String? = null,
  val matchedKeyword: String? = null,
)

data class NewsFeedCache(
  val articles: List<NewsArticle> = emptyList(),
  val lastUpdatedEpochMs: Long = 0L,
  val lastErrorMessage: String? = null,
)

sealed interface NewsSourceMutationResult {
  data class Success(val source: NewsFeedSource) : NewsSourceMutationResult
  data object EmptyName : NewsSourceMutationResult
  data object InvalidUrl : NewsSourceMutationResult
  data object DuplicateUrl : NewsSourceMutationResult
}

object DefaultNewsSources {
  /** One-tap trending focus keywords for fast Gen Z news curation. */
  val trendingFocusKeywords: List<String> =
    listOf("IPL", "Cricket", "AI", "Startups", "Gaming", "Anime", "Bollywood", "Tech", "Music", "Space")

  val builtIn: List<NewsFeedSource> =
    listOf(
      // India (National)
      NewsFeedSource(
        id = "ndtv_india",
        name = "NDTV India",
        feedUrl = "https://feeds.feedburner.com/ndtvnews-india-news",
        topic = NewsTopic.INDIA,
      ),
      NewsFeedSource(
        id = "the_hindu",
        name = "The Hindu",
        feedUrl = "https://www.thehindu.com/news/national/feeder/default.rss",
        topic = NewsTopic.INDIA,
      ),
      NewsFeedSource(
        id = "times_of_india",
        name = "Times of India",
        feedUrl = "https://timesofindia.indiatimes.com/rssfeeds/-2128936835.cms",
        topic = NewsTopic.INDIA,
      ),
      NewsFeedSource(
        id = "indian_express",
        name = "The Indian Express",
        feedUrl = "https://indianexpress.com/section/india/feed/",
        topic = NewsTopic.INDIA,
      ),
      NewsFeedSource(
        id = "hindustan_times",
        name = "Hindustan Times",
        feedUrl = "https://www.hindustantimes.com/feeds/rss/india-news/rssfeed.xml",
        topic = NewsTopic.INDIA,
      ),
      NewsFeedSource(
        id = "india_today",
        name = "India Today",
        feedUrl = "https://www.indiatoday.in/rss/1206514",
        topic = NewsTopic.INDIA,
      ),
      // World
      NewsFeedSource(
        id = "bbc_world",
        name = "BBC World",
        feedUrl = "https://feeds.bbci.co.uk/news/world/rss.xml",
        topic = NewsTopic.WORLD,
      ),
      NewsFeedSource(
        id = "npr_world",
        name = "NPR News",
        feedUrl = "https://feeds.npr.org/1001/rss.xml",
        topic = NewsTopic.WORLD,
      ),
      NewsFeedSource(
        id = "al_jazeera",
        name = "Al Jazeera",
        feedUrl = "https://www.aljazeera.com/xml/rss/all.xml",
        topic = NewsTopic.WORLD,
      ),
      // Tech (Indian + Global)
      NewsFeedSource(
        id = "gadgets_360",
        name = "Gadgets 360",
        feedUrl = "https://feeds.feedburner.com/gadgets360-latest",
        topic = NewsTopic.TECH,
      ),
      NewsFeedSource(
        id = "the_hindu_tech",
        name = "The Hindu Tech",
        feedUrl = "https://www.thehindu.com/sci-tech/technology/feeder/default.rss",
        topic = NewsTopic.TECH,
      ),
      NewsFeedSource(
        id = "the_verge",
        name = "The Verge",
        feedUrl = "https://www.theverge.com/rss/index.xml",
        topic = NewsTopic.TECH,
      ),
      NewsFeedSource(
        id = "ars_technica",
        name = "Ars Technica",
        feedUrl = "https://feeds.arstechnica.com/arstechnica/index",
        topic = NewsTopic.TECH,
      ),
      NewsFeedSource(
        id = "hacker_news",
        name = "Hacker News",
        feedUrl = "https://hnrss.org/frontpage",
        topic = NewsTopic.TECH,
      ),
      // Business (Indian + Global)
      NewsFeedSource(
        id = "economic_times",
        name = "Economic Times",
        feedUrl = "https://economictimes.indiatimes.com/rssfeedstopstories.cms",
        topic = NewsTopic.BUSINESS,
      ),
      NewsFeedSource(
        id = "livemint",
        name = "Livemint",
        feedUrl = "https://www.livemint.com/rss/news",
        topic = NewsTopic.BUSINESS,
      ),
      NewsFeedSource(
        id = "cnbc_top",
        name = "CNBC Business",
        feedUrl = "https://search.cnbc.com/rs/search/combinedcms/view.xml?partnerId=wrss01&id=100003114",
        topic = NewsTopic.BUSINESS,
      ),
      NewsFeedSource(
        id = "bbc_business",
        name = "BBC Business",
        feedUrl = "https://feeds.bbci.co.uk/news/business/rss.xml",
        topic = NewsTopic.BUSINESS,
      ),
      // Science
      NewsFeedSource(
        id = "nasa_breaking",
        name = "NASA",
        feedUrl = "https://www.nasa.gov/rss/dyn/breaking_news.rss",
        topic = NewsTopic.SCIENCE,
      ),
      NewsFeedSource(
        id = "science_daily",
        name = "ScienceDaily",
        feedUrl = "https://www.sciencedaily.com/rss/top/science.xml",
        topic = NewsTopic.SCIENCE,
      ),
      // Sports & Cricket (Indian + Global)
      NewsFeedSource(
        id = "ndtv_sports",
        name = "NDTV Sports",
        feedUrl = "https://feeds.feedburner.com/ndtvsports-latest",
        topic = NewsTopic.SPORTS,
      ),
      NewsFeedSource(
        id = "toi_cricket",
        name = "TOI Cricket",
        feedUrl = "https://timesofindia.indiatimes.com/rssfeeds/54829575.cms",
        topic = NewsTopic.SPORTS,
      ),
      NewsFeedSource(
        id = "espn_news",
        name = "ESPN",
        feedUrl = "https://www.espn.com/espn/rss/news",
        topic = NewsTopic.SPORTS,
      ),
      NewsFeedSource(
        id = "bbc_sport",
        name = "BBC Sport",
        feedUrl = "https://feeds.bbci.co.uk/sport/rss.xml",
        topic = NewsTopic.SPORTS,
      ),
      // Entertainment (Indian + Global)
      NewsFeedSource(
        id = "ndtv_movies",
        name = "NDTV Movies",
        feedUrl = "https://feeds.feedburner.com/ndtvmovies-latest",
        topic = NewsTopic.ENTERTAINMENT,
      ),
      NewsFeedSource(
        id = "toi_entertainment",
        name = "TOI Entertainment",
        feedUrl = "https://timesofindia.indiatimes.com/rssfeeds/1081479906.cms",
        topic = NewsTopic.ENTERTAINMENT,
      ),
      NewsFeedSource(
        id = "bbc_entertainment",
        name = "BBC Entertainment",
        feedUrl = "https://feeds.bbci.co.uk/news/entertainment_and_arts/rss.xml",
        topic = NewsTopic.ENTERTAINMENT,
      ),
    )

  val defaultEnabledIds: Set<String> = emptySet()
}
