package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.CalculatorResult
import com.example.data.model.ContactMatch
import com.example.data.model.QuickActionItem
import com.example.data.model.QuickActionType
import com.example.data.model.SmartSearchState
import com.example.ui.components.AppIconBadge

/**
 * Step 6 One-Handed Glassmorphic Smart Search Overlay:
 * - Search input bar is anchored at the bottom right above the keyboard (`imePadding`).
 * - Results (Apps, Calculator, Quick Actions, Contacts, DuckDuckGo Web Suggestions) appear
 *   immediately above the input bar over the blurred Home screen.
 * - Requests `READ_CONTACTS` only on demand via ActivityResultContracts.RequestPermission().
 */
@Composable
fun SmartSearchOverlay(
  state: SmartSearchState,
  onQueryChange: (String) -> Unit,
  onClearQuery: () -> Unit,
  onClose: () -> Unit,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  onCopyCalculatorResult: (CalculatorResult) -> Unit,
  onQuickActionClick: (QuickActionItem) -> Unit,
  onCallContact: (ContactMatch) -> Unit,
  onMessageContact: (ContactMatch) -> Unit,
  onContactsPermissionResult: (Boolean) -> Unit,
  onOpenAppSettingsForContacts: () -> Unit,
  onWebSearchClick: (String) -> Unit,
  onPickSuggestion: (String) -> Unit,
  onSelectRecentQuery: (String) -> Unit,
  onRemoveRecentQuery: (String) -> Unit,
  onClearRecentQueries: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler(enabled = true) { onClose() }

  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current

  val contactsPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = onContactsPermissionResult,
    )

  LaunchedEffect(Unit) {
    runCatching { focusRequester.requestFocus() }
  }

  val overlayFill =
    Brush.verticalGradient(
      colors =
        listOf(
          Color(0xFF0A101E).copy(alpha = 0.54f),
          Color(0xFF0F172A).copy(alpha = 0.70f),
          Color(0xFF0B1120).copy(alpha = 0.82f),
        )
    )

  fun submitPrimaryAction() {
    val q = state.query.trim()
    if (q.isEmpty()) return
    when {
      state.calculatorResult != null -> onCopyCalculatorResult(state.calculatorResult)
      state.matchingApps.size == 1 -> {
        focusManager.clearFocus()
        onAppClick(state.matchingApps.first())
      }
      state.matchingApps.firstOrNull()?.label?.equals(q, ignoreCase = true) == true -> {
        focusManager.clearFocus()
        onAppClick(state.matchingApps.first())
      }
      state.quickActions.size == 1 -> {
        focusManager.clearFocus()
        onQuickActionClick(state.quickActions.first())
      }
      else -> {
        focusManager.clearFocus()
        onWebSearchClick(q)
      }
    }
  }

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(overlayFill)
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {
            focusManager.clearFocus()
            onClose()
          },
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .testTag("smart_search_overlay"),
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize(),
    ) {
      // Scrollable results list stacked cleanly above the bottom search bar
      LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        reverseLayout = false,
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .widthIn(max = 640.dp)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {},
            )
            .testTag("smart_search_results"),
      ) {
        val trimmedQuery = state.query.trim()

        if (trimmedQuery.isEmpty()) {
          if (state.recentQueries.isNotEmpty()) {
            item(key = "recent_searches_idle") {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
              ) {
                SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_recent))
                TextButton(
                  onClick = onClearRecentQueries,
                  colors =
                    ButtonDefaults.textButtonColors(
                      contentColor = MaterialTheme.colorScheme.primary,
                    ),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier =
                    Modifier.minimumInteractiveComponentSize()
                      .testTag("smart_search_clear_recent_button"),
                ) {
                  Text(
                    text = stringResource(R.string.smart_search_clear_recent),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
              Spacer(modifier = Modifier.height(4.dp))
              RecentSearchesCard(
                queries = state.recentQueries,
                onSelectQuery = onSelectRecentQuery,
                onRemoveQuery = onRemoveRecentQuery,
              )
            }
          } else {
            item(key = "empty_hint") {
              SmartSearchIntroCard()
            }
          }
          if (state.quickActions.isNotEmpty()) {
            item(key = "default_quick_actions") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_quick_actions))
              Spacer(modifier = Modifier.height(6.dp))
              QuickActionsChipRow(
                actions = state.quickActions,
                onActionClick = {
                  focusManager.clearFocus()
                  onQuickActionClick(it)
                },
              )
            }
          }
          if (!state.hasContactsPermission) {
            item(key = "contacts_permission_card_idle") {
              ContactsPermissionCard(
                deniedOnce = state.contactsPermissionDeniedOnce,
                onRequestPermission = {
                  contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                onOpenAppSettings = onOpenAppSettingsForContacts,
              )
            }
          }
        } else {
          // 1. Calculator result card
          state.calculatorResult?.let { calc ->
            item(key = "calculator_card") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_calculator))
              Spacer(modifier = Modifier.height(6.dp))
              CalculatorResultCard(
                result = calc,
                onCopy = { onCopyCalculatorResult(calc) },
              )
            }
          }

          // 2. Matching Apps row
          if (state.matchingApps.isNotEmpty()) {
            item(key = "matching_apps") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_apps))
              Spacer(modifier = Modifier.height(6.dp))
              MatchingAppsGlassCard(
                apps = state.matchingApps,
                onAppClick = {
                  focusManager.clearFocus()
                  onAppClick(it)
                },
                onAppLongClick = {
                  focusManager.clearFocus()
                  onAppLongClick(it)
                },
              )
            }
          }

          // 3. Quick Actions
          if (state.quickActions.isNotEmpty()) {
            item(key = "matched_quick_actions") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_quick_actions))
              Spacer(modifier = Modifier.height(6.dp))
              QuickActionsListCard(
                actions = state.quickActions,
                onActionClick = {
                  focusManager.clearFocus()
                  onQuickActionClick(it)
                },
              )
            }
          }

          // 4. Contacts (or on-demand READ_CONTACTS permission card)
          if (state.hasContactsPermission && state.contacts.isNotEmpty()) {
            item(key = "matched_contacts") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_contacts))
              Spacer(modifier = Modifier.height(6.dp))
              ContactsListCard(
                contacts = state.contacts,
                onCall = {
                  focusManager.clearFocus()
                  onCallContact(it)
                },
                onMessage = {
                  focusManager.clearFocus()
                  onMessageContact(it)
                },
              )
            }
          } else if (
            !state.hasContactsPermission &&
              trimmedQuery.length >= 2 &&
              state.calculatorResult == null
          ) {
            item(key = "contacts_permission_card_active") {
              ContactsPermissionCard(
                deniedOnce = state.contactsPermissionDeniedOnce,
                onRequestPermission = {
                  contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                },
                onOpenAppSettings = onOpenAppSettingsForContacts,
              )
            }
          }

          // 5. Matching Recent Searches (local history autocomplete while typing)
          val matchingRecent =
            state.recentQueries
              .filter {
                it.contains(trimmedQuery, ignoreCase = true) &&
                  !it.equals(trimmedQuery, ignoreCase = true)
              }
              .take(3)
          if (matchingRecent.isNotEmpty()) {
            item(key = "matching_recent_searches") {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_recent))
              Spacer(modifier = Modifier.height(6.dp))
              RecentSearchesCard(
                queries = matchingRecent,
                onSelectQuery = onSelectRecentQuery,
                onRemoveQuery = onRemoveRecentQuery,
              )
            }
          }

          // 6. Web Search & DuckDuckGo Suggestions
          item(key = "web_search_section") {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth(),
            ) {
              SmartSearchSectionLabel(text = stringResource(R.string.smart_search_section_web))
              if (state.isLoadingSuggestions) {
                CircularProgressIndicator(
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(14.dp),
                )
              }
            }
            Spacer(modifier = Modifier.height(6.dp))
            WebSuggestionsCard(
              query = trimmedQuery,
              suggestions = state.webSuggestions,
              onSearchWeb = { q ->
                focusManager.clearFocus()
                onWebSearchClick(q)
              },
              onFillQuery = onPickSuggestion,
            )
          }
        }
      }

      // Bottom-anchored one-handed search bar directly above keyboard
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .widthIn(max = 640.dp)
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null,
              onClick = {},
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        Surface(
          color = Color.White.copy(alpha = 0.17f),
          shape = CircleShape,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.34f)),
          modifier = Modifier.fillMaxWidth().height(54.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
          ) {
            IconButton(
              onClick = {
                focusManager.clearFocus()
                onClose()
              },
              modifier =
                Modifier.minimumInteractiveComponentSize().testTag("smart_search_close_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.cd_close_smart_search),
                tint = Color.White,
                modifier = Modifier.size(22.dp),
              )
            }

            Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
              if (state.query.isEmpty()) {
                Text(
                  text = stringResource(R.string.smart_search_placeholder),
                  style = MaterialTheme.typography.bodyLarge,
                  color = Color.White.copy(alpha = 0.70f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              BasicTextField(
                value = state.query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { submitPrimaryAction() }),
                modifier =
                  Modifier.fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag("smart_search_input"),
              )
            }

            if (state.query.isNotEmpty()) {
              IconButton(
                onClick = onClearQuery,
                modifier =
                  Modifier.minimumInteractiveComponentSize().testTag("smart_search_clear_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Close,
                  contentDescription = stringResource(R.string.search_clear_cd),
                  tint = Color.White.copy(alpha = 0.90f),
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SmartSearchSectionLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelLarge,
    fontWeight = FontWeight.SemiBold,
    color = Color.White.copy(alpha = 0.76f),
    modifier = Modifier.padding(horizontal = 4.dp),
  )
}

@Composable
private fun SmartSearchIntroCard() {
  GlassCardContainer {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
      Text(
        text = stringResource(R.string.smart_search_hint_title),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = stringResource(R.string.smart_search_hint_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.76f),
      )
    }
  }
}

@Composable
private fun RecentSearchesCard(
  queries: List<String>,
  onSelectQuery: (String) -> Unit,
  onRemoveQuery: (String) -> Unit,
) {
  GlassCardContainer(modifier = Modifier.testTag("smart_search_recent_card")) {
    Column(modifier = Modifier.fillMaxWidth()) {
      queries.forEachIndexed { index, query ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onSelectQuery(query) }
              .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
              .testTag("smart_search_recent_item_$index"),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.72f),
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.94f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = { onRemoveQuery(query) },
            modifier =
              Modifier.minimumInteractiveComponentSize()
                .testTag("smart_search_remove_recent_$index"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = stringResource(R.string.cd_remove_recent_search, query),
              tint = Color.White.copy(alpha = 0.62f),
              modifier = Modifier.size(18.dp),
            )
          }
        }
        if (index < queries.lastIndex) {
          HorizontalDivider(
            color = Color.White.copy(alpha = 0.10f),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatorResultCard(
  result: CalculatorResult,
  onCopy: () -> Unit,
) {
  GlassCardContainer(
    modifier =
      Modifier.clickable(onClick = onCopy).testTag("smart_search_calculator_card"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
      ) {
        Icon(
          imageVector = Icons.Rounded.Calculate,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "= ${result.formattedResult}",
          style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = result.expression,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.72f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = onCopy,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("smart_search_copy_calc"),
      ) {
        Icon(
          imageVector = Icons.Rounded.ContentCopy,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = stringResource(R.string.smart_search_copy_result))
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MatchingAppsGlassCard(
  apps: List<AppItem>,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  GlassCardContainer(modifier = Modifier.testTag("smart_search_apps_card")) {
    LazyRow(
      contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(items = apps, key = { it.packageName }) { app ->
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier =
            Modifier.width(74.dp)
              .minimumInteractiveComponentSize()
              .clip(RoundedCornerShape(16.dp))
              .combinedClickable(
                onClick = { onAppClick(app) },
                onLongClick = {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  onAppLongClick(app)
                },
              )
              .padding(horizontal = 4.dp, vertical = 6.dp)
              .testTag("smart_search_app_${app.packageName}"),
        ) {
          AppIconBadge(
            app = app,
            sizeDp = 50,
            showNotificationDot = true,
            badgeBorderColor = Color(0xFF141D35),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.94f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }
  }
}

@Composable
private fun QuickActionsChipRow(
  actions: List<QuickActionItem>,
  onActionClick: (QuickActionItem) -> Unit,
) {
  LazyRow(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    items(items = actions, key = { it.id }) { action ->
      Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)),
        modifier =
          Modifier.minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable { onActionClick(action) }
            .testTag("quick_action_chip_${action.type.name.lowercase()}"),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
          Icon(
            imageVector = quickActionIcon(action.type),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = action.title,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
          )
        }
      }
    }
  }
}

@Composable
private fun QuickActionsListCard(
  actions: List<QuickActionItem>,
  onActionClick: (QuickActionItem) -> Unit,
) {
  GlassCardContainer(modifier = Modifier.testTag("smart_search_quick_actions_card")) {
    Column(modifier = Modifier.fillMaxWidth()) {
      actions.forEachIndexed { index, action ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onActionClick(action) }
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("quick_action_row_${action.type.name.lowercase()}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
          ) {
            Icon(
              imageVector = quickActionIcon(action.type),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(20.dp),
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = action.title,
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = action.subtitle,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        if (index < actions.lastIndex) {
          HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ContactsListCard(
  contacts: List<ContactMatch>,
  onCall: (ContactMatch) -> Unit,
  onMessage: (ContactMatch) -> Unit,
) {
  GlassCardContainer(modifier = Modifier.testTag("smart_search_contacts_card")) {
    Column(modifier = Modifier.fillMaxWidth()) {
      contacts.forEachIndexed { index, contact ->
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .clickable { onCall(contact) }
              .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp)
              .testTag("smart_search_contact_${contact.id}"),
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.90f)),
          ) {
            Text(
              text = contact.displayName.take(1).uppercase(),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = contact.displayName,
              style = MaterialTheme.typography.titleSmall,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Text(
              text = contact.phoneNumber,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.72f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = { onMessage(contact) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Sms,
              contentDescription = stringResource(R.string.cd_message_contact, contact.displayName),
              tint = Color.White.copy(alpha = 0.88f),
              modifier = Modifier.size(20.dp),
            )
          }
          IconButton(
            onClick = { onCall(contact) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.Call,
              contentDescription = stringResource(R.string.cd_call_contact, contact.displayName),
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
          }
        }
        if (index < contacts.lastIndex) {
          HorizontalDivider(
            color = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.padding(horizontal = 16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun ContactsPermissionCard(
  deniedOnce: Boolean,
  onRequestPermission: () -> Unit,
  onOpenAppSettings: () -> Unit,
) {
  GlassCardContainer(modifier = Modifier.testTag("smart_search_contacts_permission_card")) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.14f)),
      ) {
        Icon(
          imageVector = Icons.Rounded.Contacts,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(20.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.smart_search_contacts_prompt_title),
          style = MaterialTheme.typography.titleSmall,
          color = Color.White,
        )
        Text(
          text =
            if (deniedOnce) {
              stringResource(R.string.smart_search_contacts_denied_body)
            } else {
              stringResource(R.string.smart_search_contacts_prompt_body)
            },
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.72f),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = if (deniedOnce) onOpenAppSettings else onRequestPermission,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier =
          Modifier.minimumInteractiveComponentSize().testTag("smart_search_allow_contacts_button"),
      ) {
        Text(
          text =
            if (deniedOnce) {
              stringResource(R.string.smart_search_contacts_open_settings)
            } else {
              stringResource(R.string.smart_search_contacts_allow)
            }
        )
      }
    }
  }
}

@Composable
private fun WebSuggestionsCard(
  query: String,
  suggestions: List<String>,
  onSearchWeb: (String) -> Unit,
  onFillQuery: (String) -> Unit,
) {
  GlassCardContainer(modifier = Modifier.testTag("smart_search_web_card")) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // Direct web search row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable { onSearchWeb(query) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("smart_search_web_direct_row"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Public,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
          text = stringResource(R.string.smart_search_web_direct, query),
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }

      suggestions.forEachIndexed { index, suggestion ->
        HorizontalDivider(
          color = Color.White.copy(alpha = 0.10f),
          modifier = Modifier.padding(horizontal = 16.dp),
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier.fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { onSearchWeb(suggestion) }
              .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
              .testTag("smart_search_suggestion_$index"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.70f),
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(14.dp))
          Text(
            text = suggestion,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = { onFillQuery(suggestion) },
            modifier = Modifier.minimumInteractiveComponentSize(),
          ) {
            Icon(
              imageVector = Icons.Rounded.NorthWest,
              contentDescription = null,
              tint = Color.White.copy(alpha = 0.62f),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GlassCardContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  val cardShape = RoundedCornerShape(22.dp)
  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(cardShape)
        .background(Color.White.copy(alpha = 0.12f))
        .border(width = 1.dp, color = Color.White.copy(alpha = 0.22f), shape = cardShape),
  ) {
    content()
  }
}

private fun quickActionIcon(type: QuickActionType): ImageVector =
  when (type) {
    QuickActionType.CALL -> Icons.Rounded.Call
    QuickActionType.ALARM -> Icons.Rounded.Alarm
    QuickActionType.WIFI -> Icons.Rounded.Wifi
    QuickActionType.BLUETOOTH -> Icons.Rounded.Bluetooth
    QuickActionType.SMS -> Icons.Rounded.Sms
    QuickActionType.SETTINGS -> Icons.Rounded.Settings
  }
