package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PermMedia
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.BuiltInCategory
import com.example.data.model.CustomCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
  app: AppItem,
  onDismiss: () -> Unit,
  onPinToDock: () -> Unit,
  onMoveToCategory: () -> Unit,
  onHideApp: () -> Unit,
  onOpenAppInfo: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("app_action_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      ) {
        AppIconBadge(app = app, sizeDp = 48)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = app.effectiveCategoryName,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            Surface(
              shape = RoundedCornerShape(6.dp),
              color =
                if (app.isCategoryOverridden) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceContainerHigh
                },
            ) {
              Text(
                text =
                  if (app.isCategoryOverridden) {
                    stringResource(R.string.category_badge_custom)
                  } else {
                    stringResource(R.string.category_badge_auto)
                  },
                style = MaterialTheme.typography.labelSmall,
                color =
                  if (app.isCategoryOverridden) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Spacer(modifier = Modifier.height(4.dp))

      ActionSheetItem(
        icon = if (app.isPinnedToDock) Icons.Outlined.PushPin else Icons.Rounded.PushPin,
        label =
          if (app.isPinnedToDock) {
            stringResource(R.string.action_unpin_from_dock)
          } else {
            stringResource(R.string.action_pin_to_dock)
          },
        onClick = onPinToDock,
        testTag = "menu_pin_to_dock",
      )

      ActionSheetItem(
        icon = Icons.Rounded.DriveFileMove,
        label = stringResource(R.string.action_move_to_category),
        onClick = onMoveToCategory,
        testTag = "menu_move_to_category",
      )

      ActionSheetItem(
        icon = Icons.Rounded.VisibilityOff,
        label = stringResource(R.string.action_hide_app),
        onClick = onHideApp,
        testTag = "menu_hide_app",
      )

      ActionSheetItem(
        icon = Icons.Rounded.Info,
        label = stringResource(R.string.action_app_info),
        onClick = onOpenAppInfo,
        testTag = "menu_app_info",
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToCategorySheet(
  app: AppItem,
  autoCategoryLabel: String,
  customCategories: List<CustomCategory>,
  builtInLabels: Map<BuiltInCategory, String>,
  onDismiss: () -> Unit,
  onSelectCategory: (categoryId: String, categoryName: String) -> Unit,
  onResetToAuto: () -> Unit,
  onCreateNewCategory: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("move_category_sheet"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
          text = stringResource(R.string.move_category_title, app.label),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.move_category_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
      ) {
        if (app.isCategoryOverridden) {
          item(key = "reset_auto") {
            ActionSheetItem(
              icon = Icons.Rounded.RestartAlt,
              label = stringResource(R.string.move_category_reset_auto, autoCategoryLabel),
              tint = MaterialTheme.colorScheme.secondary,
              onClick = onResetToAuto,
              testTag = "move_category_reset_auto",
            )
            HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
              modifier = Modifier.padding(horizontal = 16.dp),
            )
          }
        }

        if (customCategories.isNotEmpty()) {
          items(items = customCategories, key = { it.id }) { custom ->
            CategoryOptionRow(
              icon = Icons.Rounded.Bookmark,
              name = custom.name,
              isSelected = app.effectiveCategoryId == custom.id,
              isCustom = true,
              onClick = { onSelectCategory(custom.id, custom.name) },
              testTag = "move_option_${custom.id}",
            )
          }
          item(key = "custom_divider") {
            HorizontalDivider(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
              modifier = Modifier.padding(horizontal = 16.dp),
            )
          }
        }

        items(items = BuiltInCategory.assignableCategories, key = { it.id }) { builtIn ->
          val label = builtInLabels[builtIn] ?: builtIn.name
          CategoryOptionRow(
            icon = builtInCategoryIcon(builtIn),
            name = label,
            isSelected = app.effectiveCategoryId == builtIn.id,
            isCustom = false,
            onClick = { onSelectCategory(builtIn.id, label) },
            testTag = "move_option_${builtIn.id}",
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      FilledTonalButton(
        onClick = onCreateNewCategory,
        shape = RoundedCornerShape(14.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp)
            .minimumInteractiveComponentSize()
            .testTag("move_sheet_create_category_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.move_category_create_new))
      }
    }
  }
}

@Composable
private fun CategoryOptionRow(
  icon: ImageVector,
  name: String,
  isSelected: Boolean,
  isCustom: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = name,
      style = MaterialTheme.typography.bodyLarge,
      color =
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f),
    )
    if (isCustom) {
      Text(
        text = stringResource(R.string.category_badge_custom),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
          Modifier.clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 6.dp, vertical = 2.dp),
      )
      Spacer(modifier = Modifier.width(8.dp))
    }
    if (isSelected) {
      Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

@Composable
fun ActionSheetItem(
  icon: ImageVector,
  label: String,
  onClick: () -> Unit,
  testTag: String,
  tint: Color = MaterialTheme.colorScheme.onSurface,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      Modifier.fillMaxWidth()
        .minimumInteractiveComponentSize()
        .clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 14.dp)
        .testTag(testTag),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = tint,
      modifier = Modifier.size(22.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = tint,
    )
  }
}

fun builtInCategoryIcon(category: BuiltInCategory): ImageVector =
  when (category) {
    BuiltInCategory.COMMUNICATION -> Icons.Rounded.ChatBubbleOutline
    BuiltInCategory.INTERNET -> Icons.Rounded.Public
    BuiltInCategory.GAMES -> Icons.Rounded.SportsEsports
    BuiltInCategory.MEDIA -> Icons.Rounded.PermMedia
    BuiltInCategory.PRODUCTIVITY -> Icons.Rounded.WorkOutline
    BuiltInCategory.UTILITIES -> Icons.Rounded.Category
    BuiltInCategory.OTHER -> Icons.Rounded.Settings
  }
