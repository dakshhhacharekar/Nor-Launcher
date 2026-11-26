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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RssFeed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DefaultNewsSources
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsPersonalization
import com.example.data.model.NewsTopic

/**
 * One-handed ModalBottomSheet to personalize news contexts, focus keywords,
 * built-in news channels, and custom RSS/Atom feeds with a chosen context.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewsSourcesSheet(
  sources: List<NewsFeedSource>,
  personalization: NewsPersonalization,
  addErrorMessage: String?,
  onToggleContext: (NewsTopic, Boolean) -> Unit,
  onAddFocusKeyword: (String) -> Unit,
  onRemoveFocusKeyword: (String) -> Unit,
  onSetStrictKeywordFilter: (Boolean) -> Unit,
  onToggleSource: (sourceId: String, enabled: Boolean) -> Unit,
  onAddCustomSource: (name: String, url: String, topic: NewsTopic) -> Unit,
  onRemoveCustomSource: (NewsFeedSource) -> Unit,
  onClearError: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var nameInput by rememberSaveable { mutableStateOf("") }
  var urlInput by rememberSaveable { mutableStateOf("") }
  var customTopicId by rememberSaveable { mutableStateOf(NewsTopic.CUSTOM.id) }
  var keywordInput by rememberSaveable { mutableStateOf("") }

  val customTopic = NewsTopic.fromId(customTopicId)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.testTag("news_sources_sheet"),
  ) {
    LazyColumn(
      contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
    ) {
      item(key = "sheet_header") {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.RssFeed,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.news_sources_sheet_title),
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = stringResource(R.string.news_sources_sheet_subtitle),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }

      // Contexts section
      item(key = "contexts_section") {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = stringResource(R.string.news_sources_section_contexts),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          NewsTopic.selectableContexts.forEach { topic ->
            val selected = topic in personalization.selectedContexts
            FilterChip(
              selected = selected,
              onClick = { onToggleContext(topic, !selected) },
              label = { Text(newsTopicDisplayName(topic)) },
              leadingIcon =
                if (selected) {
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
              modifier = Modifier.testTag("news_sheet_context_${topic.id}"),
            )
          }
        }
      }

      // Focus keywords section
      item(key = "keywords_section") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.news_sources_section_keywords),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = keywordInput,
            onValueChange = { keywordInput = it },
            placeholder = { Text(stringResource(R.string.news_setup_keywords_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
              KeyboardActions(
                onDone = {
                  if (keywordInput.trim().length >= 2) {
                    onAddFocusKeyword(keywordInput)
                    keywordInput = ""
                  }
                }
              ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("news_sheet_keyword_input"),
          )
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier =
              Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable {
                  if (keywordInput.trim().length >= 2) {
                    onAddFocusKeyword(keywordInput)
                    keywordInput = ""
                  }
                }
                .testTag("news_sheet_keyword_add"),
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

        Spacer(modifier = Modifier.height(8.dp))
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
            val isActive = personalization.focusKeywords.any { it.equals(preset, ignoreCase = true) }
            FilterChip(
              selected = isActive,
              onClick = {
                if (isActive) {
                  val existing =
                    personalization.focusKeywords.firstOrNull { it.equals(preset, ignoreCase = true) }
                  if (existing != null) onRemoveFocusKeyword(existing)
                } else {
                  onAddFocusKeyword(preset)
                }
              },
              label = { Text(preset) },
              leadingIcon =
                if (isActive) {
                  {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                  }
                } else null,
              shape = CircleShape,
              modifier = Modifier.testTag("news_sheet_trending_${preset.lowercase()}"),
            )
          }
        }

        val customKeywords =
          personalization.focusKeywords.filter { kw ->
            DefaultNewsSources.trendingFocusKeywords.none { it.equals(kw, ignoreCase = true) }
          }
        if (customKeywords.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            customKeywords.forEach { kw ->
              InputChip(
                selected = true,
                onClick = { onRemoveFocusKeyword(kw) },
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

        if (personalization.focusKeywords.isNotEmpty()) {

          Spacer(modifier = Modifier.height(6.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = stringResource(R.string.news_sources_strict_keywords_label),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            Switch(
              checked = personalization.strictKeywordFilter,
              onCheckedChange = onSetStrictKeywordFilter,
              modifier = Modifier.testTag("news_strict_keywords_switch"),
            )
          }
        }
      }

      // Channels list header
      item(key = "channels_header") {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text = stringResource(R.string.news_sources_section_channels),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      items(sources, key = { it.id }) { source ->
        NewsSourceRow(
          source = source,
          onToggle = { enabled -> onToggleSource(source.id, enabled) },
          onRemove = { onRemoveCustomSource(source) },
        )
      }

      // Add custom RSS/Atom channel
      item(key = "add_custom_section") {
        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = stringResource(R.string.news_sources_add_section),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = nameInput,
          onValueChange = {
            nameInput = it
            if (addErrorMessage != null) onClearError()
          },
          label = { Text(stringResource(R.string.news_sources_name_label)) },
          placeholder = { Text(stringResource(R.string.news_sources_name_placeholder)) },
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("news_custom_name_input"),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = urlInput,
          onValueChange = {
            urlInput = it
            if (addErrorMessage != null) onClearError()
          },
          label = { Text(stringResource(R.string.news_sources_url_label)) },
          placeholder = { Text(stringResource(R.string.news_sources_url_placeholder)) },
          singleLine = true,
          isError = addErrorMessage != null,
          supportingText = addErrorMessage?.let { msg -> { Text(msg) } },
          keyboardOptions =
            KeyboardOptions(
              keyboardType = KeyboardType.Uri,
              imeAction = ImeAction.Done,
            ),
          keyboardActions =
            KeyboardActions(
              onDone = { onAddCustomSource(nameInput, urlInput, customTopic) }
            ),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("news_custom_url_input"),
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stringResource(R.string.news_sources_context_label),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          (NewsTopic.selectableContexts + NewsTopic.CUSTOM).forEach { topic ->
            FilterChip(
              selected = topic == customTopic,
              onClick = { customTopicId = topic.id },
              label = { Text(newsTopicDisplayName(topic)) },
              shape = CircleShape,
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        FilledTonalButton(
          onClick = {
            val submittedName = nameInput
            val submittedUrl = urlInput
            onAddCustomSource(submittedName, submittedUrl, customTopic)
            if (submittedName.isNotBlank() && submittedUrl.contains('.')) {
              nameInput = ""
              urlInput = ""
            }
          },
          modifier = Modifier.fillMaxWidth().minimumInteractiveComponentSize().testTag("news_add_source_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.news_sources_add_button))
        }
      }
    }
  }
}

@Composable
private fun NewsSourceRow(
  source: NewsFeedSource,
  onToggle: (Boolean) -> Unit,
  onRemove: () -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth().testTag("news_source_row_${source.id}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = source.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
          ) {
            Text(
              text = newsTopicDisplayName(source.topic),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = source.feedUrl.removePrefix("https://").removePrefix("http://"),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (source.isCustom) {
        IconButton(
          onClick = onRemove,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("news_remove_source_${source.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.news_sources_remove_cd),
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }

      Switch(
        checked = source.isEnabled,
        onCheckedChange = onToggle,
        modifier = Modifier.testTag("news_toggle_source_${source.id}"),
      )
    }
  }
}
