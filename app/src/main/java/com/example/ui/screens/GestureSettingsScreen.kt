package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GestureAction
import com.example.data.model.GestureConfig
import com.example.data.model.HomeGesture

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureSettingsScreen(
  gestureConfig: GestureConfig,
  isAccessibilityEnabled: Boolean,
  onSelectGestureAction: (HomeGesture, GestureAction) -> Unit,
  onResetDefaults: () -> Unit,
  onOpenAccessibilitySettings: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var editingGesture by remember { mutableStateOf<HomeGesture?>(null) }

  BackHandler(enabled = true) { onBack() }

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize().testTag("gesture_settings_screen"),
  ) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
      // Top bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.testTag("gestures_back_button"),
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = stringResource(R.string.cd_back_button),
            tint = MaterialTheme.colorScheme.onBackground,
          )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
          Text(
            text = stringResource(R.string.gestures_screen_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = stringResource(R.string.gestures_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        item(key = "accessibility_card") {
          AccessibilityStatusCard(
            isEnabled = isAccessibilityEnabled,
            onOpenSettings = onOpenAccessibilitySettings,
          )
        }

        item(key = HomeGesture.SWIPE_UP.id) {
          GestureRowCard(
            icon = Icons.Rounded.KeyboardArrowUp,
            title = stringResource(R.string.gesture_swipe_up),
            subtitle = stringResource(R.string.gesture_swipe_up_desc),
            assignedAction = gestureConfig.swipeUp,
            onClick = { editingGesture = HomeGesture.SWIPE_UP },
            testTag = "gesture_row_swipe_up",
          )
        }

        item(key = HomeGesture.SWIPE_DOWN.id) {
          GestureRowCard(
            icon = Icons.Rounded.KeyboardArrowDown,
            title = stringResource(R.string.gesture_swipe_down),
            subtitle = stringResource(R.string.gesture_swipe_down_desc),
            assignedAction = gestureConfig.swipeDown,
            onClick = { editingGesture = HomeGesture.SWIPE_DOWN },
            testTag = "gesture_row_swipe_down",
          )
        }

        item(key = HomeGesture.DOUBLE_TAP.id) {
          GestureRowCard(
            icon = Icons.Rounded.TouchApp,
            title = stringResource(R.string.gesture_double_tap),
            subtitle = stringResource(R.string.gesture_double_tap_desc),
            assignedAction = gestureConfig.doubleTap,
            onClick = { editingGesture = HomeGesture.DOUBLE_TAP },
            testTag = "gesture_row_double_tap",
          )
        }

        item(key = "bottom_spacer") { Spacer(modifier = Modifier.height(8.dp)) }
      }

      // Bottom-anchored one-handed action bar
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border =
          BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          ),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier =
            Modifier.fillMaxWidth()
              .navigationBarsPadding()
              .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
          OutlinedButton(
            onClick = onResetDefaults,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("gesture_reset_defaults_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.gesture_reset_defaults))
          }

          FilledTonalButton(
            onClick = onBack,
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.weight(1f).minimumInteractiveComponentSize().testTag("gesture_done_button"),
          ) {
            Text(text = stringResource(R.string.gesture_done))
          }
        }
      }
    }
  }

  editingGesture?.let { gesture ->
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentAction = gestureConfig.actionFor(gesture)
    val gestureLabel = gestureTitle(gesture)

    ModalBottomSheet(
      onDismissRequest = { editingGesture = null },
      sheetState = sheetState,
      containerColor = MaterialTheme.colorScheme.surfaceContainer,
      modifier = Modifier.testTag("gesture_action_picker_sheet"),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
      ) {
        Text(
          text = stringResource(R.string.gesture_picker_title, gestureLabel),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(4.dp))

        GestureAction.entries.forEach { action ->
          val selected = action == currentAction
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clickable {
                  onSelectGestureAction(gesture, action)
                  editingGesture = null
                }
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("gesture_action_option_${action.id}"),
          ) {
            Icon(
              imageVector = gestureActionIcon(action),
              contentDescription = null,
              tint =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
              text = gestureActionLabel(action),
              style = MaterialTheme.typography.bodyLarge,
              color =
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.weight(1f),
            )
            if (action.requiresAccessibility) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
              ) {
                Text(
                  text = stringResource(R.string.gesture_badge_accessibility),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
            }
            if (selected) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AccessibilityStatusCard(
  isEnabled: Boolean,
  onOpenSettings: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("accessibility_status_card"),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                  if (isEnabled) MaterialTheme.colorScheme.secondaryContainer
                  else MaterialTheme.colorScheme.primaryContainer
                ),
          ) {
            Icon(
              imageVector = Icons.Rounded.AccessibilityNew,
              contentDescription = null,
              tint =
                if (isEnabled) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(22.dp),
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = stringResource(R.string.gesture_accessibility_card_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Surface(
          shape = RoundedCornerShape(8.dp),
          color =
            if (isEnabled) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text =
              if (isEnabled) stringResource(R.string.gesture_accessibility_status_on)
              else stringResource(R.string.gesture_accessibility_status_off),
            style = MaterialTheme.typography.labelSmall,
            color =
              if (isEnabled) MaterialTheme.colorScheme.onSecondaryContainer
              else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = stringResource(R.string.gesture_accessibility_card_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(modifier = Modifier.height(12.dp))

      FilledTonalButton(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(12.dp),
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .testTag("open_accessibility_settings_button"),
      ) {
        Text(text = stringResource(R.string.gesture_accessibility_open_settings))
      }
    }
  }
}

@Composable
private fun GestureRowCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  assignedAction: GestureAction,
  onClick: () -> Unit,
  testTag: String,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainer,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(20.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        ) {
          Text(
            text = gestureActionLabel(assignedAction),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilityEnableSheet(
  requestedAction: GestureAction,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("accessibility_enable_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .padding(bottom = 12.dp)
    ) {
      Text(
        text =
          stringResource(R.string.accessibility_prompt_title, gestureActionLabel(requestedAction)),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = stringResource(R.string.accessibility_prompt_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(18.dp))
      Button(
        onClick = onOpenSettings,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(50.dp)
            .minimumInteractiveComponentSize()
            .testTag("prompt_open_accessibility_button"),
      ) {
        Text(text = stringResource(R.string.gesture_accessibility_open_settings))
      }
      Spacer(modifier = Modifier.height(8.dp))
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .height(48.dp)
            .minimumInteractiveComponentSize()
            .testTag("prompt_dismiss_accessibility_button"),
      ) {
        Text(text = stringResource(R.string.action_cancel))
      }
    }
  }
}

@Composable
private fun gestureTitle(gesture: HomeGesture): String =
  when (gesture) {
    HomeGesture.SWIPE_UP -> stringResource(R.string.gesture_swipe_up)
    HomeGesture.SWIPE_DOWN -> stringResource(R.string.gesture_swipe_down)
    HomeGesture.DOUBLE_TAP -> stringResource(R.string.gesture_double_tap)
  }

@Composable
fun gestureActionLabel(action: GestureAction): String =
  when (action) {
    GestureAction.OPEN_DRAWER -> stringResource(R.string.gesture_action_open_drawer)
    GestureAction.LOCK_SCREEN -> stringResource(R.string.gesture_action_lock_screen)
    GestureAction.OPEN_NOTIFICATIONS -> stringResource(R.string.gesture_action_open_notifications)
    GestureAction.OPEN_QUICK_SETTINGS -> stringResource(R.string.gesture_action_quick_settings)
    GestureAction.OPEN_SEARCH -> stringResource(R.string.gesture_action_open_search)
    GestureAction.OPEN_HIDDEN_APPS -> stringResource(R.string.gesture_action_hidden_apps)
    GestureAction.NONE -> stringResource(R.string.gesture_action_none)
  }

private fun gestureActionIcon(action: GestureAction): ImageVector =
  when (action) {
    GestureAction.OPEN_DRAWER -> Icons.Rounded.Apps
    GestureAction.LOCK_SCREEN -> Icons.Rounded.Lock
    GestureAction.OPEN_NOTIFICATIONS -> Icons.Rounded.Notifications
    GestureAction.OPEN_QUICK_SETTINGS -> Icons.Rounded.Tune
    GestureAction.OPEN_SEARCH -> Icons.Rounded.Search
    GestureAction.OPEN_HIDDEN_APPS -> Icons.Rounded.VisibilityOff
    GestureAction.NONE -> Icons.Rounded.Block
  }
