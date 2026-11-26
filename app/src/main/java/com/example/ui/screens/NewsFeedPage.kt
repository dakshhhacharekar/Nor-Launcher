package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.model.NewsArticle
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsPersonalization
import com.example.data.model.NewsTopic
import com.example.ui.components.NewsPersonalizationSetup
import com.example.ui.components.newsTopicDisplayName
import com.example.ui.theme.LocalIsDarkTheme

/**
 * Editorial glassmorphic RSS News Feed page displayed to the left of the Home screen.
 * First-time visitors personalize their contexts, optional focus keywords, and channels inline;
 * once personalized, displays tailored stories with pull-to-refresh, topic filters, and sharing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedPage(
  articles: List<NewsArticle>,
  sources: List<NewsFeedSource>,
  personalization: NewsPersonalization,
  selectedTopic: NewsTopic,
  lastUpdatedEpochMs: Long,
  isRefreshing: Boolean,
  onCompletePersonalization: (Set<NewsTopic>, Set<String>, List<String>) -> Unit,
  onSelectTopic: (NewsTopic) -> Unit,
  onClearStrictKeywordFilter: () -> Unit,
  onRefresh: () -> Unit,
  onOpenSourcesSheet: () -> Unit,
  onArticleClick: (NewsArticle) -> Unit,
  onArticleShare: (NewsArticle) -> Unit,
  onReturnHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val isDark = LocalIsDarkTheme.current
  val scrimColor =
    if (isDark) {
      MaterialTheme.colorScheme.surface.copy(alpha = 0.74f)
    } else {
      MaterialTheme.colorScheme.surface.copy(alpha = 0.80f)
    }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(scrimColor)
        .statusBarsPadding()
        .navigationBarsPadding()
        .testTag("news_feed_page"),
  ) {
    if (!personalization.isCompleted) {
      NewsPersonalizationSetup(
        availableSources = sources,
        onComplete = onCompletePersonalization,
        onReturnHome = onReturnHome,
      )
      return@Box
    }

    val hasCustomSources = sources.any { it.isCustom && it.isEnabled }
    val hasEnabledSources = sources.any { it.isEnabled }

    val availableTopics =
      remember(personalization.selectedContexts, hasCustomSources) {
        buildList {
          add(NewsTopic.ALL)
          NewsTopic.selectableContexts.forEach { topic ->
            if (topic in personalization.selectedContexts) {
              add(topic)
            }
          }
          if (hasCustomSources) add(NewsTopic.CUSTOM)
        }
      }

    val subtitleText =
      when {
        isRefreshing -> stringResource(R.string.news_page_subtitle_syncing)
        lastUpdatedEpochMs <= 0L -> stringResource(R.string.news_page_subtitle_never)
        else -> formatLastUpdatedSubtitle(lastUpdatedEpochMs)
      }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp).padding(horizontal = 16.dp),
      ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Editorial header bar
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.news_page_title),
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = subtitleText,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          IconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("news_refresh_button"),
          ) {
            if (isRefreshing) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(20.dp),
              )
            } else {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.news_action_refresh),
                tint = MaterialTheme.colorScheme.onSurface,
              )
            }
          }

          IconButton(
            onClick = onOpenSourcesSheet,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("news_sources_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Tune,
              contentDescription = stringResource(R.string.news_action_sources),
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(onClick = onReturnHome)
                .minimumInteractiveComponentSize()
                .testTag("news_return_home_button"),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
              Text(
                text = stringResource(R.string.news_action_return_home),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(16.dp),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Personalized context filter strip
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 2.dp),
          modifier = Modifier.fillMaxWidth().testTag("news_topic_filter_row"),
        ) {
          items(availableTopics, key = { it.id }) { topic ->
            val selected = topic == selectedTopic
            FilterChip(
              selected = selected,
              onClick = { onSelectTopic(topic) },
              label = {
                Text(
                  text = newsTopicDisplayName(topic),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
              },
              shape = CircleShape,
              colors =
                FilterChipDefaults.filterChipColors(
                  containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
              modifier = Modifier.testTag("news_topic_chip_${topic.id}"),
            )
          }
        }

        // Active focus keywords strip (tapping opens personalization sheet)
        if (personalization.focusKeywords.isNotEmpty()) {
          Spacer(modifier = Modifier.height(4.dp))
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            items(personalization.focusKeywords, key = { it }) { kw ->
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                modifier = Modifier.clip(CircleShape).clickable(onClick = onOpenSourcesSheet),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(12.dp),
                  )
                  Text(
                    text = kw,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Pull-to-refresh article stream
      PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 640.dp),
      ) {
        when {
          articles.isEmpty() && isRefreshing -> {
            NewsLoadingPlaceholder()
          }
          articles.isEmpty() -> {
            NewsEmptyState(
              hasEnabledSources = hasEnabledSources,
              isFilteredTopic = selectedTopic != NewsTopic.ALL,
              isStrictKeywordsActive =
                personalization.strictKeywordFilter && personalization.focusKeywords.isNotEmpty(),
              onRefresh = onRefresh,
              onOpenSources = onOpenSourcesSheet,
              onShowAllTopics = { onSelectTopic(NewsTopic.ALL) },
              onClearStrictKeywords = onClearStrictKeywordFilter,
            )
          }
          else -> {
            LazyColumn(
              contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
              modifier = Modifier.fillMaxSize().testTag("news_article_list"),
            ) {
              itemsIndexed(
                items = articles,
                key = { _, item -> item.id },
              ) { index, article ->
                if (index == 0 && !article.imageUrl.isNullOrBlank()) {
                  NewsHeroCard(
                    article = article,
                    onClick = { onArticleClick(article) },
                    onShare = { onArticleShare(article) },
                  )
                } else {
                  NewsCompactCard(
                    article = article,
                    onClick = { onArticleClick(article) },
                    onShare = { onArticleShare(article) },
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NewsHeroCard(
  article: NewsArticle,
  onClick: () -> Unit,
  onShare: () -> Unit,
) {
  val context = LocalContext.current
  val cardShape = RoundedCornerShape(24.dp)

  Surface(
    shape = cardShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    tonalElevation = 2.dp,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .clickable(onClick = onClick)
        .testTag("news_hero_card"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      ) {
        AsyncImage(
          model =
            ImageRequest.Builder(context)
              .data(article.imageUrl)
              .crossfade(true)
              .build(),
          contentDescription = article.title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )

        Box(
          modifier =
            Modifier
              .fillMaxSize()
              .background(
                Brush.verticalGradient(
                  colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)),
                  startY = 120f,
                )
              )
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
          ) {
            Text(
              text = article.sourceName,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
          Text(
            text = formatRelativeArticleTime(article.publishedAtEpochMs),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f),
          )
        }
      }

      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
        article.matchedKeyword?.let { kw ->
          KeywordMatchBadge(keyword = kw)
          Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
          text = article.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )

        if (article.summary.isNotBlank()) {
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = article.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.End,
          modifier = Modifier.fillMaxWidth(),
        ) {
          IconButton(
            onClick = onShare,
            modifier = Modifier.minimumInteractiveComponentSize().testTag("news_share_${article.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Share,
              contentDescription = stringResource(R.string.news_action_share),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NewsCompactCard(
  article: NewsArticle,
  onClick: () -> Unit,
  onShare: () -> Unit,
) {
  val context = LocalContext.current
  val cardShape = RoundedCornerShape(20.dp)

  Surface(
    shape = cardShape,
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.84f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.38f)),
    tonalElevation = 1.dp,
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(cardShape)
        .clickable(onClick = onClick)
        .testTag("news_card_${article.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.Top,
      modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 12.dp, bottom = 10.dp, end = 12.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = article.sourceName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = formatRelativeArticleTime(article.publishedAtEpochMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        article.matchedKeyword?.let { kw ->
          Spacer(modifier = Modifier.height(4.dp))
          KeywordMatchBadge(keyword = kw)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = article.title,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
        )

        if (article.summary.isNotBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = article.summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        if (!article.imageUrl.isNullOrBlank()) {
          AsyncImage(
            model =
              ImageRequest.Builder(context)
                .data(article.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = article.title,
            contentScale = ContentScale.Crop,
            modifier =
              Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
          )
        } else {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(56.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
              )
            }
          }
        }

        IconButton(
          onClick = onShare,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("news_share_${article.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = stringResource(R.string.news_action_share),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun KeywordMatchBadge(keyword: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
      Icon(
        imageVector = Icons.Rounded.AutoAwesome,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.size(11.dp),
      )
      Text(
        text = stringResource(R.string.news_for_you_badge, keyword),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
      )
    }
  }
}

@Composable
private fun NewsLoadingPlaceholder() {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
  ) {
    repeat(4) { index ->
      Surface(
        shape = RoundedCornerShape(if (index == 0) 24.dp else 20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth().height(if (index == 0) 200.dp else 108.dp),
      ) {}
    }
  }
}

@Composable
private fun NewsEmptyState(
  hasEnabledSources: Boolean,
  isFilteredTopic: Boolean,
  isStrictKeywordsActive: Boolean,
  onRefresh: () -> Unit,
  onOpenSources: () -> Unit,
  onShowAllTopics: () -> Unit,
  onClearStrictKeywords: () -> Unit,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth().testTag("news_empty_state"),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
      ) {
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(56.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.RssFeed,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text =
            stringResource(
              if (!hasEnabledSources) R.string.news_empty_no_sources_title
              else R.string.news_empty_title
            ),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text =
            stringResource(
              when {
                !hasEnabledSources -> R.string.news_empty_no_sources_body
                isStrictKeywordsActive -> R.string.news_empty_keywords_body
                isFilteredTopic -> R.string.news_empty_topic_body
                else -> R.string.news_empty_body
              }
            ),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          when {
            !hasEnabledSources -> {
              FilledTonalButton(
                onClick = onOpenSources,
                modifier = Modifier.testTag("news_empty_manage_sources_btn"),
              ) {
                Text(stringResource(R.string.news_btn_manage_sources))
              }
            }
            isStrictKeywordsActive -> {
              FilledTonalButton(
                onClick = onClearStrictKeywords,
                modifier = Modifier.testTag("news_empty_clear_strict_btn"),
              ) {
                Text(stringResource(R.string.news_btn_clear_strict))
              }
              OutlinedButton(onClick = onOpenSources) {
                Text(stringResource(R.string.news_btn_manage_sources))
              }
            }
            isFilteredTopic -> {
              FilledTonalButton(
                onClick = onShowAllTopics,
                modifier = Modifier.testTag("news_empty_show_all_btn"),
              ) {
                Text(stringResource(R.string.news_topic_all))
              }
              OutlinedButton(onClick = onRefresh) {
                Text(stringResource(R.string.news_btn_retry))
              }
            }
            else -> {
              FilledTonalButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("news_empty_retry_btn"),
              ) {
                Text(stringResource(R.string.news_btn_retry))
              }
              OutlinedButton(
                onClick = onOpenSources,
                modifier = Modifier.testTag("news_empty_sources_btn"),
              ) {
                Text(stringResource(R.string.news_btn_manage_sources))
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun formatLastUpdatedSubtitle(updatedAtEpochMs: Long): String {
  val diffMins = ((System.currentTimeMillis() - updatedAtEpochMs).coerceAtLeast(0L)) / 60_000L
  return when {
    diffMins < 1L -> stringResource(R.string.news_page_subtitle_just_now)
    diffMins < 60L -> stringResource(R.string.news_page_subtitle_mins_ago, diffMins.toInt())
    else -> stringResource(R.string.news_page_subtitle_hours_ago, (diffMins / 60L).toInt().coerceAtLeast(1))
  }
}

@Composable
private fun formatRelativeArticleTime(publishedAtEpochMs: Long): String {
  val diffMins = ((System.currentTimeMillis() - publishedAtEpochMs).coerceAtLeast(0L)) / 60_000L
  return when {
    diffMins < 2L -> stringResource(R.string.news_time_just_now)
    diffMins < 60L -> stringResource(R.string.news_time_minutes, diffMins.toInt())
    diffMins < 24L * 60L -> stringResource(R.string.news_time_hours, (diffMins / 60L).toInt())
    else -> stringResource(R.string.news_time_days, (diffMins / (24L * 60L)).toInt().coerceAtLeast(1))
  }
}
