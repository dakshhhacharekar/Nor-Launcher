package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.model.HostedWidgetRecord
import com.example.data.widgets.LauncherWidgetHostManager
import com.example.data.widgets.NorAppWidgetHostView
import kotlin.math.roundToInt

/**
 * Renders real Android AppWidgets hosted via [LauncherWidgetHostManager] inside Compose,
 * with long-press edit mode for live resizing (width span & height), reordering,
 * reconfiguring, and removing widgets.
 */
@Composable
fun HostedWidgetSection(
  widgets: List<HostedWidgetRecord>,
  editingWidgetId: Int?,
  widgetHostManager: LauncherWidgetHostManager,
  useDarkTextContrast: Boolean,
  onStartEditWidget: (Int) -> Unit,
  onStopEditWidget: () -> Unit,
  onResizeWidget: (appWidgetId: Int, widthSpan: Int, heightDp: Int) -> Unit,
  onMoveWidget: (appWidgetId: Int, delta: Int) -> Unit,
  onReconfigureWidget: (Int) -> Unit,
  onRemoveWidget: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier.fillMaxWidth().testTag("hosted_widgets_section"),
  ) {
    widgets.forEachIndexed { index, record ->
      key(record.appWidgetId) {
        HostedWidgetCard(
          record = record,
          isEditing = editingWidgetId == record.appWidgetId,
          canMoveUp = index > 0,
          canMoveDown = index < widgets.lastIndex,
          widgetHostManager = widgetHostManager,
          useDarkTextContrast = useDarkTextContrast,
          onLongPress = { onStartEditWidget(record.appWidgetId) },
          onDoneEditing = onStopEditWidget,
          onResize = { newSpan, newHeight ->
            onResizeWidget(record.appWidgetId, newSpan, newHeight)
          },
          onMoveUp = { onMoveWidget(record.appWidgetId, -1) },
          onMoveDown = { onMoveWidget(record.appWidgetId, 1) },
          onReconfigure = { onReconfigureWidget(record.appWidgetId) },
          onRemove = { onRemoveWidget(record.appWidgetId) },
        )
      }
    }
  }
}

@Composable
private fun HostedWidgetCard(
  record: HostedWidgetRecord,
  isEditing: Boolean,
  canMoveUp: Boolean,
  canMoveDown: Boolean,
  widgetHostManager: LauncherWidgetHostManager,
  useDarkTextContrast: Boolean,
  onLongPress: () -> Unit,
  onDoneEditing: () -> Unit,
  onResize: (widthSpan: Int, heightDp: Int) -> Unit,
  onMoveUp: () -> Unit,
  onMoveDown: () -> Unit,
  onReconfigure: () -> Unit,
  onRemove: () -> Unit,
) {
  val context = LocalContext.current
  val hostView =
    remember(record.appWidgetId) {
      widgetHostManager.createHostView(context, record.appWidgetId)
    }
  val isReconfigurable =
    remember(record.appWidgetId) { widgetHostManager.isReconfigurable(record.appWidgetId) }

  val animatedHeight by
    animateDpAsState(
      targetValue = record.heightDp.dp,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "hosted_widget_height_${record.appWidgetId}",
    )

  val widthFraction =
    when (record.widthSpan) {
      2 -> 0.54f
      3 -> 0.78f
      else -> 1f
    }

  val frameShape = RoundedCornerShape(22.dp)
  val frameBg =
    if (useDarkTextContrast) Color.White.copy(alpha = 0.42f)
    else Color(0xFF0E1526).copy(alpha = 0.34f)
  val frameBorderColor =
    if (isEditing) MaterialTheme.colorScheme.primary
    else if (useDarkTextContrast) Color.White.copy(alpha = 0.72f)
    else Color.White.copy(alpha = 0.18f)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.fillMaxWidth().testTag("hosted_widget_${record.appWidgetId}"),
  ) {
    BoxWithConstraints(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      val targetWidthDp = (maxWidth.value * widthFraction).roundToInt().coerceAtLeast(120)

      Box(
        modifier =
          Modifier.fillMaxWidth(widthFraction)
            .height(animatedHeight)
            .clip(frameShape)
            .background(frameBg)
            .border(
              width = if (isEditing) 2.dp else 1.dp,
              color = frameBorderColor,
              shape = frameShape,
            ),
      ) {
        if (hostView != null) {
          AndroidView(
            factory = {
              (hostView.parent as? ViewGroup)?.removeView(hostView)
              hostView.apply {
                if (this is NorAppWidgetHostView) {
                  onWidgetLongPress = onLongPress
                }
                widgetHostManager.updateWidgetSize(this, targetWidthDp, record.heightDp)
              }
            },
            update = { view ->
              if (view is NorAppWidgetHostView) {
                view.onWidgetLongPress = onLongPress
              }
              widgetHostManager.updateWidgetSize(view, targetWidthDp, record.heightDp)
            },
            modifier = Modifier.fillMaxSize().padding(4.dp),
          )
        } else {
          // Graceful fallback if widget provider was disabled or unavailable
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Rounded.Widgets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = record.label,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
                Text(
                  text = stringResource(R.string.widget_unavailable_label),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            TextButton(onClick = onRemove) {
              Text(text = stringResource(R.string.widget_action_remove))
            }
          }
        }
      }
    }

    // One-handed Resize & Remove Edit Controls directly below the active widget
    AnimatedVisibility(
      visible = isEditing,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
        tonalElevation = 6.dp,
        modifier =
          Modifier.fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("widget_edit_controls_${record.appWidgetId}"),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
          // Row 1: Widget title + Reorder + Done
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = record.label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f),
            )
            if (canMoveUp) {
              IconButton(
                onClick = onMoveUp,
                modifier = Modifier.size(38.dp).testTag("widget_move_up_${record.appWidgetId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowUp,
                  contentDescription = stringResource(R.string.cd_widget_move_up),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (canMoveDown) {
              IconButton(
                onClick = onMoveDown,
                modifier = Modifier.size(38.dp).testTag("widget_move_down_${record.appWidgetId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowDown,
                  contentDescription = stringResource(R.string.cd_widget_move_down),
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            if (isReconfigurable) {
              IconButton(
                onClick = onReconfigure,
                modifier = Modifier.size(38.dp).testTag("widget_configure_${record.appWidgetId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Tune,
                  contentDescription = stringResource(R.string.cd_widget_configure),
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
            IconButton(
              onClick = onRemove,
              modifier = Modifier.size(38.dp).testTag("widget_remove_${record.appWidgetId}"),
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.cd_widget_remove),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primary,
              shape = CircleShape,
              modifier =
                Modifier.padding(start = 4.dp)
                  .clip(CircleShape)
                  .clickable(onClick = onDoneEditing)
                  .testTag("widget_done_${record.appWidgetId}"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Check,
                  contentDescription = stringResource(R.string.cd_widget_edit_done),
                  tint = MaterialTheme.colorScheme.onPrimary,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = stringResource(R.string.widget_action_done),
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              }
            }
          }

          // Row 2: Width span selector (Half / Wide / Full) + Height (- / dp / +)
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            // Width span pills
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              val widthOptions =
                listOf(
                  2 to R.string.widget_width_half,
                  3 to R.string.widget_width_wide,
                  4 to R.string.widget_width_full,
                )
              widthOptions.forEach { (span, labelRes) ->
                val selected = record.widthSpan == span
                Surface(
                  color =
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainer,
                  shape = RoundedCornerShape(12.dp),
                  border =
                    BorderStroke(
                      width = 1.dp,
                      color =
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
                  modifier =
                    Modifier.clip(RoundedCornerShape(12.dp))
                      .clickable { onResize(span, record.heightDp) }
                      .testTag("widget_width_${span}_${record.appWidgetId}"),
                ) {
                  Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color =
                      if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                  )
                }
              }
            }

            // Height steppers
            Row(verticalAlignment = Alignment.CenterVertically) {
              val canShrink = record.heightDp > HostedWidgetRecord.MIN_HEIGHT_DP
              val canGrow = record.heightDp < HostedWidgetRecord.MAX_HEIGHT_DP
              IconButton(
                onClick = {
                  val nextH =
                    (record.heightDp - HostedWidgetRecord.HEIGHT_STEP_DP)
                      .coerceAtLeast(HostedWidgetRecord.MIN_HEIGHT_DP)
                  onResize(record.widthSpan, nextH)
                },
                enabled = canShrink,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .size(36.dp)
                    .testTag("widget_shorter_${record.appWidgetId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Remove,
                  contentDescription = stringResource(R.string.cd_widget_shorter),
                  modifier = Modifier.size(18.dp),
                )
              }
              Text(
                text = stringResource(R.string.widget_height_dp, record.heightDp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
              )
              IconButton(
                onClick = {
                  val nextH =
                    (record.heightDp + HostedWidgetRecord.HEIGHT_STEP_DP)
                      .coerceAtMost(HostedWidgetRecord.MAX_HEIGHT_DP)
                  onResize(record.widthSpan, nextH)
                },
                enabled = canGrow,
                modifier =
                  Modifier.minimumInteractiveComponentSize()
                    .size(36.dp)
                    .testTag("widget_taller_${record.appWidgetId}"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Add,
                  contentDescription = stringResource(R.string.cd_widget_taller),
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}
