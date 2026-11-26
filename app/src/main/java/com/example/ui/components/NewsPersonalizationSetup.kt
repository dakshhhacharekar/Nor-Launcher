package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DefaultNewsSources
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsTopic

/**
 * First-visit personalization setup displayed directly on the News page so every user
 * explicitly picks their contexts, optional focus keywords, and trusted news channels.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsPersonalizationSetup(
  availableSources: List<NewsFeedSource>,
  onComplete: (selectedContexts: Set<NewsTopic>, enabledChannelIds: Set<String>, focusKeywords: List<String>) -> Unit,
  onReturnHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedContextIds by rememberSaveable { mutableStateOf(listOf<String>()) }
  var selectedChannelIds by rememberSaveable { mutableStateOf(listOf<String>()) }
  var focusKeywords by rememberSaveable { mutableStateOf(listOf<String>()) }
  var keywordDraft by rememberSaveable { mutableStateOf("") }
  var showAllChannels by rememberSaveable { mutableStateOf(false) }

  val selectedContexts =
    remember(selectedContextIds) { selectedContextIds.map { NewsTopic.fromId(it) }.toSet() }
  val selectedChannelsSet = remember(selectedChannelIds) { selectedChannelIds.toSet() }

  val visibleChannels =
    remember(availableSources, selectedContexts, showAllChannels) {
      if (showAllChannels || selectedContexts.isEmpty()) {
        availableSources
      } else {
        availableSources.filter { it.topic in selectedContexts || it.isCustom }
      }
    }

  fun addKeywordFromDraft() {
    val parts =
      keywordDraft
        .split(',', ';')
        .map { it.trim() }
        .filter { it.length >= 2 }
    if (parts.isEmpty()) return
    val updated = focusKeywords.toMutableList()
    parts.forEach { kw ->
      if (updated.none { it.equals(kw, ignoreCase = true) } && updated.size < 10) {
        updated.add(kw)
      }
    }
    focusKeywords = updated
    keywordDraft = ""
  }

  val canStart = selectedContexts.isNotEmpty() && selectedChannelsSet.isNotEmpty()

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxSize().testTag("news_personalization_setup"),
  ) {
    LazyColumn(
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      modifier = Modifier.weight(1f).fillMaxWidth().widthIn(max = 640.dp),
    ) {
      // Top header with Home return pill
      item(key = "setup_header") {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = stringResource(R.string.news_setup_badge),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }

          Spacer(modifier = Modifier.weight(1f))

          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
            modifier =
              Modifier
                .clip(CircleShape)
                .clickable(onClick = onReturnHome)
                .minimumInteractiveComponentSize()
                .testTag("news_setup_return_home"),
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

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = stringResource(R.string.news_setup_title),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.news_setup_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Step 1: Choose Contexts
      item(key = "step_contexts") {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = stringResource(R.string.news_setup_step_context),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              NewsTopic.selectableContexts.forEach { topic ->
                val isSelected = topic in selectedContexts
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    if (isSelected) {
                      selectedContextIds = selectedContextIds.filterNot { it == topic.id }
                    } else {
                      selectedContextIds = selectedContextIds + topic.id
                    }
                  },
                  label = {
                    Text(
                      text = newsTopicDisplayName(topic),
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    )
                  },
                  leadingIcon =
                    if (isSelected) {
                      {
                        Icon(
                          imageVector = Icons.Rounded.Check,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  shape = CircleShape,
                  colors =
                    FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                  modifier = Modifier.testTag("news_setup_context_${topic.id}"),
                )
              }
            }
          }
        }
      }

      // Step 2: Optional focus keywords (context refinement)
      item(key = "step_keywords") {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = stringResource(R.string.news_setup_step_keywords),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              OutlinedTextField(
                value = keywordDraft,
                onValueChange = { keywordDraft = it },
                placeholder = { Text(stringResource(R.string.news_setup_keywords_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { addKeywordFromDraft() }),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f).testTag("news_setup_keyword_input"),
              )
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier =
                  Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = { addKeywordFromDraft() })
                    .testTag("news_setup_keyword_add_btn"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.news_setup_keywords_add),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = stringResource(R.string.news_trending_keywords_hint),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              DefaultNewsSources.trendingFocusKeywords.forEach { preset ->
                val isSelected = focusKeywords.any { it.equals(preset, ignoreCase = true) }
                FilterChip(
                  selected = isSelected,
                  onClick = {
                    if (isSelected) {
                      focusKeywords = focusKeywords.filterNot { it.equals(preset, ignoreCase = true) }
                    } else if (focusKeywords.size < 10) {
                      focusKeywords = focusKeywords + preset
                    }
                  },
                  label = { Text(preset) },
                  leadingIcon =
                    if (isSelected) {
                      {
                        Icon(
                          imageVector = Icons.Rounded.Check,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                        )
                      }
                    } else null,
                  shape = CircleShape,
                  modifier = Modifier.testTag("news_setup_trending_${preset.lowercase()}"),
                )
              }
            }

            val customFocusKeywords =
              focusKeywords.filter { kw ->
                DefaultNewsSources.trendingFocusKeywords.none { it.equals(kw, ignoreCase = true) }
              }
            if (customFocusKeywords.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                customFocusKeywords.forEach { kw ->
                  InputChip(
                    selected = true,
                    onClick = { focusKeywords = focusKeywords.filterNot { it == kw } },
                    label = { Text(kw) },
                    trailingIcon = {
                      Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.news_keyword_remove_cd, kw),
                        modifier = Modifier.size(16.dp),
                      )
                    },
                    shape = CircleShape,
                  )
                }
              }
            }
          }
        }
      }

      // Step 3: Choose News Channels
      item(key = "step_channels") {
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = stringResource(R.string.news_setup_step_channels),
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = stringResource(R.string.news_setup_channels_hint, selectedChannelsSet.size),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (selectedContexts.isNotEmpty()) {
                TextButton(
                  onClick = { showAllChannels = !showAllChannels },
                  modifier = Modifier.testTag("news_setup_toggle_all_channels"),
                ) {
                  Text(
                    text =
                      if (showAllChannels) newsTopicDisplayName(selectedContexts.first())
                      else stringResource(R.string.news_setup_show_all_channels),
                    style = MaterialTheme.typography.labelMedium,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              visibleChannels.forEach { source ->
                val isSelected = source.id in selectedChannelsSet
                Surface(
                  shape = RoundedCornerShape(16.dp),
                  color =
                    if (isSelected) {
                      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                      MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                    },
                  border =
                    BorderStroke(
                      width = if (isSelected) 1.5.dp else 1.dp,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                    ),
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(16.dp))
                      .clickable {
                        if (isSelected) {
                          selectedChannelIds = selectedChannelIds.filterNot { it == source.id }
                        } else {
                          selectedChannelIds = selectedChannelIds + source.id
                          if (source.topic != NewsTopic.CUSTOM && source.topic !in selectedContexts) {
                            selectedContextIds = selectedContextIds + source.topic.id
                          }
                        }
                      }
                      .minimumInteractiveComponentSize()
                      .testTag("news_setup_channel_${source.id}"),
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                  ) {
                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = source.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Text(
                        text = newsTopicDisplayName(source.topic),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                      )
                    }
                    Surface(
                      shape = CircleShape,
                      color =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                      modifier = Modifier.size(26.dp),
                    ) {
                      if (isSelected) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp),
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
      }
    }

    // Bottom pinned confirmation bar
    Surface(
      color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
          Modifier
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        if (!canStart) {
          Text(
            text = stringResource(R.string.news_setup_validation_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Spacer(modifier = Modifier.height(6.dp))
        }
        Button(
          onClick = {
            if (keywordDraft.isNotBlank()) addKeywordFromDraft()
            onComplete(selectedContexts, selectedChannelsSet, focusKeywords)
          },
          enabled = canStart,
          shape = CircleShape,
          modifier =
            Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("news_setup_start_button"),
        ) {
          Text(
            text = stringResource(R.string.news_setup_start_button),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
fun newsTopicDisplayName(topic: NewsTopic): String =
  when (topic) {
    NewsTopic.ALL -> stringResource(R.string.news_topic_all)
    NewsTopic.INDIA -> stringResource(R.string.news_topic_india)
    NewsTopic.WORLD -> stringResource(R.string.news_topic_world)
    NewsTopic.TECH -> stringResource(R.string.news_topic_tech)
    NewsTopic.BUSINESS -> stringResource(R.string.news_topic_business)
    NewsTopic.SCIENCE -> stringResource(R.string.news_topic_science)
    NewsTopic.SPORTS -> stringResource(R.string.news_topic_sports)
    NewsTopic.ENTERTAINMENT -> stringResource(R.string.news_topic_entertainment)
    NewsTopic.CUSTOM -> stringResource(R.string.news_topic_custom)
  }
