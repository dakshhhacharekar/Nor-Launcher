package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AvailableWidgetEntry
import com.example.data.model.AvailableWidgetGroup

/**
 * One-handed ModalBottomSheet widget picker displaying installed Android AppWidgets
 * grouped by app with live previews, cell span badges, and a bottom-anchored search bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPickerSheet(
  widgetGroups: List<AvailableWidgetGroup>,
  isLoading: Boolean,
  onSelectWidget: (AvailableWidgetEntry) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var searchQuery by remember { mutableStateOf("") }
  val expandedPackages = remember { mutableStateMapOf<String, Boolean>() }

  val filteredGroups =
    remember(widgetGroups, searchQuery) {
      val q = searchQuery.trim()
      if (q.isEmpty()) {
        widgetGroups
      } else {
        widgetGroups.mapNotNull { group ->
          if (group.appLabel.contains(q, ignoreCase = true)) {
            group
          } else {
            val matchingWidgets =
              group.widgets.filter { w ->
                w.label.contains(q, ignoreCase = true) ||
                  (w.description?.contains(q, ignoreCase = true) == true)
              }
            if (matchingWidgets.isEmpty()) null else group.copy(widgets = matchingWidgets)
          }
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("widget_picker_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .fillMaxHeight(0.82f)
          .imePadding()
          .navigationBarsPadding(),
    ) {
      // Sheet header
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)) {
        Text(
          text = stringResource(R.string.widget_picker_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.widget_picker_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      // Widget groups list
      Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
        when {
          isLoading -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            ) {
              CircularProgressIndicator(modifier = Modifier.size(32.dp))
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = stringResource(R.string.widget_picker_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          filteredGroups.isEmpty() -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center,
              modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(24.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp),
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = stringResource(R.string.widget_picker_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          else -> {
            val forceExpand = searchQuery.isNotBlank()
            LazyColumn(
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth().testTag("widget_picker_list"),
            ) {
              items(items = filteredGroups, key = { it.packageName }) { group ->
                val isExpanded =
                  forceExpand ||
                    expandedPackages.getOrElse(group.packageName) {
                      filteredGroups.size <= 3 || filteredGroups.firstOrNull() == group
                    }
                WidgetAppGroupCard(
                  group = group,
                  expanded = isExpanded,
                  onToggleExpand = {
                    expandedPackages[group.packageName] = !isExpanded
                  },
                  onSelectWidget = onSelectWidget,
                )
              }
            }
          }
        }
      }

      // One-handed bottom-anchored search bar
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(50.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.weight(1f)) {
            if (searchQuery.isEmpty()) {
              Text(
                text = stringResource(R.string.widget_picker_search_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            BasicTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              singleLine = true,
              textStyle =
                MaterialTheme.typography.bodyLarge.copy(
                  color = MaterialTheme.colorScheme.onSurface
                ),
              cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
              modifier = Modifier.fillMaxWidth().testTag("widget_picker_search_input"),
            )
          }
          if (searchQuery.isNotEmpty()) {
            IconButton(
              onClick = { searchQuery = "" },
              modifier = Modifier.testTag("widget_picker_search_clear"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.search_clear_cd),
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetAppGroupCard(
  group: AvailableWidgetGroup,
  expanded: Boolean,
  onToggleExpand: () -> Unit,
  onSelectWidget: (AvailableWidgetEntry) -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    shape = RoundedCornerShape(20.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth().testTag("widget_group_${group.packageName}"),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier.fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 14.dp, vertical = 12.dp),
      ) {
        if (group.appIcon != null) {
          Image(
            bitmap = group.appIcon,
            contentDescription = null,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)),
          )
        } else {
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
          ) {
            Icon(
              imageVector = Icons.Rounded.Widgets,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = group.appLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text =
              if (group.widgets.size == 1) stringResource(R.string.widget_picker_count_one)
              else stringResource(R.string.widget_picker_count_many, group.widgets.size),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Icon(
          imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      AnimatedVisibility(
        visible = expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        ) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
          group.widgets.forEach { entry ->
            WidgetEntryCard(
              entry = entry,
              onClick = { onSelectWidget(entry) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun WidgetEntryCard(
  entry: AvailableWidgetEntry,
  onClick: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .clickable(onClick = onClick)
        .testTag("widget_entry_${entry.providerComponent}"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(width = 76.dp, height = 58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      ) {
        if (entry.previewImage != null) {
          Image(
            bitmap = entry.previewImage,
            contentDescription = entry.label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().padding(6.dp),
          )
        } else {
          Icon(
            imageVector = Icons.Rounded.Widgets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(26.dp),
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = entry.label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
          ) {
            Text(
              text = stringResource(R.string.widget_picker_size_cells, entry.cellWidth, entry.cellHeight),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
        }
        if (!entry.description.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = entry.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      Box(
        contentAlignment = Alignment.Center,
        modifier =
          Modifier.size(34.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = stringResource(R.string.widget_add_android_widget),
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(18.dp),
        )
      }
    }
  }
}
