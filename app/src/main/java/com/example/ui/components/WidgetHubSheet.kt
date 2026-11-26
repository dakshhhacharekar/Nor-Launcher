package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AddBox
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.HomeWidgetStyle
import com.example.data.model.SmartWidgetType
import com.example.data.model.SmartWidgetsConfig

@Composable
fun smartWidgetDisplayName(type: SmartWidgetType): String =
  when (type) {
    SmartWidgetType.CLOCK -> stringResource(R.string.widget_page_clock)
    SmartWidgetType.WEATHER -> stringResource(R.string.widget_page_weather)
    SmartWidgetType.MUSIC -> stringResource(R.string.widget_page_music)
    SmartWidgetType.CALENDAR -> stringResource(R.string.widget_page_calendar)
    SmartWidgetType.BATTERY -> stringResource(R.string.widget_page_battery)
    SmartWidgetType.NOTE -> stringResource(R.string.widget_page_note)
    SmartWidgetType.ICON_GROUP -> stringResource(R.string.widget_page_icon_group)
  }

fun smartWidgetIcon(type: SmartWidgetType): ImageVector =
  when (type) {
    SmartWidgetType.CLOCK -> Icons.Rounded.AccessTime
    SmartWidgetType.WEATHER -> Icons.Rounded.WbSunny
    SmartWidgetType.MUSIC -> Icons.Rounded.MusicNote
    SmartWidgetType.CALENDAR -> Icons.Rounded.CalendarMonth
    SmartWidgetType.BATTERY -> Icons.Rounded.BatteryChargingFull
    SmartWidgetType.NOTE -> Icons.Rounded.EditNote
    SmartWidgetType.ICON_GROUP -> Icons.Rounded.Apps
  }

/**
 * Unified Widgets Hub bottom sheet:
 * 1) Clock face style selector (Minimal, Glass Clock & Battery, Compact Pill, Analog Clock)
 * 2) Customizable Smart Widget Stack (toggle & reorder all 7 built-in functional widgets)
 * 3) Pin standalone Smart Widgets directly to the Home Screen canvas
 * 4) Launch Android AppWidget picker for third-party widgets
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WidgetHubSheet(
  selectedClockStyle: HomeWidgetStyle,
  smartWidgetsConfig: SmartWidgetsConfig,
  onSelectClockStyle: (HomeWidgetStyle) -> Unit,
  onToggleStackWidget: (SmartWidgetType, Boolean) -> Unit,
  onMoveStackWidget: (SmartWidgetType, Int) -> Unit,
  onTogglePinnedWidget: (SmartWidgetType, Boolean) -> Unit,
  onAddAndroidWidget: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val activeStack = smartWidgetsConfig.stackWidgets
  val pinnedSet = smartWidgetsConfig.pinnedWidgets.toSet()

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    modifier = Modifier.testTag("widget_customize_sheet"),
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .fillMaxHeight(0.86f)
          .navigationBarsPadding(),
    ) {
      // Header
      Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
        Text(
          text = stringResource(R.string.widget_customize_title),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.widget_customize_subtitle),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Column(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Section 1: Clock Face Style (including new Analog Clock)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = stringResource(R.string.widget_hub_section_clock),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
          )
          val clockOptions =
            listOf(
              Triple(
                HomeWidgetStyle.MINIMAL_CLOCK,
                R.string.widget_style_minimal_clock,
                R.string.widget_style_minimal_clock_desc,
              ),
              Triple(
                HomeWidgetStyle.ANALOG_CLOCK,
                R.string.widget_style_analog_clock,
                R.string.widget_style_analog_clock_desc,
              ),
              Triple(
                HomeWidgetStyle.GLASS_CLOCK_BATTERY,
                R.string.widget_style_glass_card,
                R.string.widget_style_glass_card_desc,
              ),
              Triple(
                HomeWidgetStyle.COMPACT_BATTERY_DATE,
                R.string.widget_style_compact_battery,
                R.string.widget_style_compact_battery_desc,
              ),
            )
          clockOptions.forEach { (style, titleRes, descRes) ->
            val isSelected = style == selectedClockStyle
            Surface(
              color =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.surfaceContainerHigh,
              shape = RoundedCornerShape(16.dp),
              border =
                BorderStroke(
                  width = 1.dp,
                  color =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                ),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(16.dp))
                  .clickable { onSelectClockStyle(style) }
                  .testTag("widget_option_${style.id}"),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color =
                      if (isSelected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = stringResource(descRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
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
          }
        }

        // Section 2: Smart Widget Stack (toggle + reorder all 7 built-in widgets)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Column {
            Text(
              text = stringResource(R.string.widget_hub_section_stack),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.widget_hub_section_stack_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Display active widgets in current order, followed by disabled widgets
          val orderedAll = activeStack + SmartWidgetType.entries.filterNot { it in activeStack }
          Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              orderedAll.forEachIndexed { idx, type ->
                val inStackIdx = activeStack.indexOf(type)
                val isEnabled = inStackIdx >= 0
                val canDisable = activeStack.size > 1

                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 6.dp)
                      .testTag("stack_widget_row_${type.id}"),
                ) {
                  Icon(
                    imageVector = smartWidgetIcon(type),
                    contentDescription = null,
                    tint =
                      if (isEnabled) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = smartWidgetDisplayName(type),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                  )
                  if (isEnabled && activeStack.size > 1) {
                    IconButton(
                      onClick = { onMoveStackWidget(type, -1) },
                      enabled = inStackIdx > 0,
                      modifier = Modifier.size(34.dp).testTag("stack_widget_up_${type.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.cd_widget_move_up),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                    IconButton(
                      onClick = { onMoveStackWidget(type, 1) },
                      enabled = inStackIdx < activeStack.lastIndex,
                      modifier = Modifier.size(34.dp).testTag("stack_widget_down_${type.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_widget_move_down),
                        modifier = Modifier.size(18.dp),
                      )
                    }
                  }
                  Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked -> onToggleStackWidget(type, checked) },
                    enabled = !isEnabled || canDisable,
                    modifier = Modifier.testTag("stack_widget_switch_${type.id}"),
                  )
                }
                if (idx < orderedAll.lastIndex) {
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(horizontal = 14.dp),
                  )
                }
              }
            }
          }
        }

        // Section 3: Pin standalone Smart Widgets directly onto the Home Screen
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Column {
            Text(
              text = stringResource(R.string.widget_hub_section_pinned),
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.widget_hub_section_pinned_hint),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            SmartWidgetType.entries.forEach { type ->
              val isPinned = type in pinnedSet
              FilterChip(
                selected = isPinned,
                onClick = { onTogglePinnedWidget(type, !isPinned) },
                leadingIcon = {
                  Icon(
                    imageVector = if (isPinned) Icons.Rounded.PushPin else smartWidgetIcon(type),
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                  )
                },
                label = { Text(text = smartWidgetDisplayName(type)) },
                modifier = Modifier.testTag("pin_smart_widget_${type.id}"),
              )
            }
          }
        }
      }

      // Sticky bottom CTA to add third-party Android AppWidgets
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      FilledTonalButton(
        onClick = onAddAndroidWidget,
        shape = RoundedCornerShape(16.dp),
        modifier =
          Modifier.fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .minimumInteractiveComponentSize()
            .testTag("widget_add_android_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.AddBox,
          contentDescription = null,
          modifier = Modifier.size(19.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.widget_add_android_widget))
      }
    }
  }
}
