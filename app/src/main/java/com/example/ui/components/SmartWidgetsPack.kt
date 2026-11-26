package com.example.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.CalendarEventItem
import com.example.data.model.MediaPlaybackState
import com.example.data.model.QuickNoteData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Live Canvas-drawn circular Analog Clock face paired with date & optional weather summary.
 */
@Composable
fun AnalogClockWidgetFace(
  nowMillis: Long,
  formattedDate: String,
  weatherSummary: String,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
) {
  val accentColor = MaterialTheme.colorScheme.primary
  val calendar =
    remember(nowMillis) {
      Calendar.getInstance().apply { timeInMillis = nowMillis }
    }
  val hours = calendar.get(Calendar.HOUR)
  val minutes = calendar.get(Calendar.MINUTE)
  val seconds = calendar.get(Calendar.SECOND)

  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier = Modifier.fillMaxSize().testTag("widget_analog_clock"),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(18.dp),
      modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
      Canvas(modifier = Modifier.size(112.dp)) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Subtle glass dial ring
        drawCircle(
          color = primaryTextColor.copy(alpha = 0.08f),
          radius = radius,
          center = center,
        )
        drawCircle(
          color = primaryTextColor.copy(alpha = 0.24f),
          radius = radius - 1.dp.toPx(),
          center = center,
          style = Stroke(width = 1.5.dp.toPx()),
        )

        // 12 hour markers
        for (i in 0 until 12) {
          val angleRad = (i * 30.0 - 90.0) * PI / 180.0
          val isCardinal = i % 3 == 0
          val outerR = radius - 6.dp.toPx()
          val innerR = radius - (if (isCardinal) 14.dp.toPx() else 10.dp.toPx())
          drawLine(
            color =
              if (isCardinal) primaryTextColor.copy(alpha = 0.85f)
              else primaryTextColor.copy(alpha = 0.42f),
            start =
              Offset(
                x = center.x + (innerR * cos(angleRad)).toFloat(),
                y = center.y + (innerR * sin(angleRad)).toFloat(),
              ),
            end =
              Offset(
                x = center.x + (outerR * cos(angleRad)).toFloat(),
                y = center.y + (outerR * sin(angleRad)).toFloat(),
              ),
            strokeWidth = if (isCardinal) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round,
          )
        }

        // Hour hand
        val hourAngle = ((hours + minutes / 60.0) * 30.0 - 90.0) * PI / 180.0
        val hourLen = radius * 0.48f
        drawLine(
          color = primaryTextColor,
          start = center,
          end =
            Offset(
              x = center.x + (hourLen * cos(hourAngle)).toFloat(),
              y = center.y + (hourLen * sin(hourAngle)).toFloat(),
            ),
          strokeWidth = 3.8.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Minute hand
        val minuteAngle = ((minutes + seconds / 60.0) * 6.0 - 90.0) * PI / 180.0
        val minuteLen = radius * 0.70f
        drawLine(
          color = primaryTextColor.copy(alpha = 0.92f),
          start = center,
          end =
            Offset(
              x = center.x + (minuteLen * cos(minuteAngle)).toFloat(),
              y = center.y + (minuteLen * sin(minuteAngle)).toFloat(),
            ),
          strokeWidth = 2.6.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Second hand in primary accent color
        val secondAngle = (seconds * 6.0 - 90.0) * PI / 180.0
        val secondLen = radius * 0.78f
        drawLine(
          color = accentColor,
          start = center,
          end =
            Offset(
              x = center.x + (secondLen * cos(secondAngle)).toFloat(),
              y = center.y + (secondLen * sin(secondAngle)).toFloat(),
            ),
          strokeWidth = 1.6.dp.toPx(),
          cap = StrokeCap.Round,
        )

        // Center pivot
        drawCircle(color = accentColor, radius = 4.dp.toPx(), center = center)
        drawCircle(color = primaryTextColor, radius = 1.8.dp.toPx(), center = center)
      }

      Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.weight(1f),
      ) {
        val dayName =
          remember(nowMillis) {
            SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(nowMillis))
          }
        Text(
          text = dayName,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = primaryTextColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "$formattedDate$weatherSummary",
          style = MaterialTheme.typography.bodyMedium,
          color = secondaryTextColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

/**
 * Functional Smart Music Player widget wired to real Android MediaSessionManager + AudioManager.
 */
@Composable
fun MusicWidgetPage(
  mediaState: MediaPlaybackState,
  isNotificationAccessGranted: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
  onPlayPause: () -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
  onOpenPlayer: () -> Unit,
  onEnableLiveInfo: () -> Unit,
) {
  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier =
      Modifier.fillMaxSize()
        .clip(RoundedCornerShape(26.dp))
        .clickable(onClick = onOpenPlayer)
        .testTag("widget_page_music"),
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        // Album artwork or sleek icon tile
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.size(54.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)),
        ) {
          if (mediaState.artwork != null) {
            Image(
              bitmap = mediaState.artwork,
              contentDescription = mediaState.title.ifEmpty { null },
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.MusicNote,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(28.dp),
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          val resolvedTitle =
            when {
              mediaState.title.isNotBlank() -> mediaState.title
              mediaState.isPlaying -> stringResource(R.string.widget_music_active_fallback)
              else -> stringResource(R.string.widget_music_idle_title)
            }
          val resolvedSubtitle =
            when {
              mediaState.artist.isNotBlank() -> mediaState.artist
              !isNotificationAccessGranted -> stringResource(R.string.widget_music_enable_info)
              else -> stringResource(R.string.widget_music_idle_subtitle)
            }
          Text(
            text = resolvedTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = primaryTextColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = resolvedSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color =
              if (!isNotificationAccessGranted && mediaState.artist.isBlank()) {
                MaterialTheme.colorScheme.primary
              } else {
                secondaryTextColor
              },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier =
              if (!isNotificationAccessGranted && mediaState.artist.isBlank()) {
                Modifier.clip(RoundedCornerShape(8.dp))
                  .clickable(onClick = onEnableLiveInfo)
                  .padding(vertical = 2.dp)
                  .testTag("widget_music_enable_info")
              } else {
                Modifier
              },
          )
        }
      }

      // Transport controls row (Previous, Play/Pause, Next)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
      ) {
        IconButton(
          onClick = onPrevious,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("widget_music_prev"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.cd_music_previous),
            tint = primaryTextColor,
            modifier = Modifier.size(26.dp),
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.primary,
          shape = CircleShape,
          modifier =
            Modifier.size(48.dp)
              .clip(CircleShape)
              .clickable(onClick = onPlayPause)
              .testTag("widget_music_play_pause"),
        ) {
          Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
              imageVector = if (mediaState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
              contentDescription =
                stringResource(
                  if (mediaState.isPlaying) R.string.cd_music_pause else R.string.cd_music_play
                ),
              tint = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.size(26.dp),
            )
          }
        }

        IconButton(
          onClick = onNext,
          modifier = Modifier.minimumInteractiveComponentSize().testTag("widget_music_next"),
        ) {
          Icon(
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.cd_music_next),
            tint = primaryTextColor,
            modifier = Modifier.size(26.dp),
          )
        }
      }
    }
  }
}

/**
 * Functional Smart Calendar Events widget backed by real Android CalendarContract.Instances.
 */
@Composable
fun CalendarWidgetPage(
  nowMillis: Long,
  is24Hour: Boolean,
  events: List<CalendarEventItem>,
  hasCalendarPermission: Boolean,
  isLoading: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
  onCalendarPermissionResult: (Boolean) -> Unit,
  onRefresh: () -> Unit,
  onOpenEvent: (CalendarEventItem) -> Unit,
  onOpenCalendarApp: () -> Unit,
) {
  val calendarPermLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = onCalendarPermissionResult,
    )

  val date = remember(nowMillis) { Date(nowMillis) }
  val dayOfWeekShort =
    remember(nowMillis) {
      SimpleDateFormat("EEE", Locale.getDefault()).format(date).uppercase(Locale.getDefault())
    }
  val dayNumber =
    remember(nowMillis) {
      SimpleDateFormat("d", Locale.getDefault()).format(date)
    }
  val timePattern = if (is24Hour) "HH:mm" else "h:mm a"

  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier = Modifier.fillMaxSize().testTag("widget_page_calendar"),
  ) {
    if (!hasCalendarPermission) {
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.widget_calendar_title),
              style = MaterialTheme.typography.titleMedium,
              color = primaryTextColor,
            )
            Text(
              text = stringResource(R.string.widget_calendar_permission_body),
              style = MaterialTheme.typography.bodySmall,
              color = secondaryTextColor,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          FilledTonalButton(
            onClick = { calendarPermLauncher.launch(Manifest.permission.READ_CALENDAR) },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("widget_calendar_allow_button"),
          ) {
            Text(
              text = stringResource(R.string.widget_calendar_allow_button),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          OutlinedButton(
            onClick = onOpenCalendarApp,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("widget_calendar_open_button"),
          ) {
            Text(
              text = stringResource(R.string.widget_calendar_open_button),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    } else {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
      ) {
        // Left date badge column (Smart Launcher style)
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
          shape = RoundedCornerShape(18.dp),
          modifier =
            Modifier.width(62.dp)
              .fillMaxHeight()
              .clip(RoundedCornerShape(18.dp))
              .clickable(onClick = onOpenCalendarApp)
              .testTag("widget_calendar_date_badge"),
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(6.dp),
          ) {
            Text(
              text = dayOfWeekShort,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.80f),
            )
            Text(
              text = dayNumber,
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right upcoming events preview
        Column(
          verticalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.weight(1f).fillMaxHeight(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            val countText =
              when (events.size) {
                0 -> stringResource(R.string.widget_calendar_title)
                1 -> stringResource(R.string.widget_calendar_events_count_one)
                else -> stringResource(R.string.widget_calendar_events_count_many, events.size)
              }
            Text(
              text = countText,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
            if (isLoading) {
              CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
            } else {
              IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(32.dp).testTag("widget_calendar_refresh"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Refresh,
                  contentDescription = stringResource(R.string.cd_refresh_calendar),
                  tint = secondaryTextColor,
                  modifier = Modifier.size(16.dp),
                )
              }
            }
          }

          if (events.isEmpty()) {
            Column(
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(10.dp))
                  .clickable(onClick = onOpenCalendarApp)
                  .padding(vertical = 4.dp),
            ) {
              Text(
                text = stringResource(R.string.widget_calendar_empty_title),
                style = MaterialTheme.typography.titleSmall,
                color = primaryTextColor,
              )
              Text(
                text = stringResource(R.string.widget_calendar_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = secondaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              events.take(2).forEach { event ->
                val timeText =
                  if (event.allDay) {
                    stringResource(R.string.widget_calendar_all_day)
                  } else {
                    SimpleDateFormat(timePattern, Locale.getDefault())
                      .format(Date(event.startMillis))
                  }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier =
                    Modifier.fillMaxWidth()
                      .clip(RoundedCornerShape(10.dp))
                      .background(primaryTextColor.copy(alpha = 0.08f))
                      .clickable { onOpenEvent(event) }
                      .padding(horizontal = 8.dp, vertical = 5.dp)
                      .testTag("widget_calendar_event_${event.id}"),
                ) {
                  Box(
                    modifier =
                      Modifier.width(3.dp)
                        .height(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = event.title,
                      style = MaterialTheme.typography.labelLarge,
                      fontWeight = FontWeight.SemiBold,
                      color = primaryTextColor,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text = timeText,
                      style = MaterialTheme.typography.labelSmall,
                      color = secondaryTextColor,
                      maxLines = 1,
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

/**
 * Functional Quick Note widget backed by DataStore persistence.
 */
@Composable
fun QuickNoteWidgetPage(
  note: QuickNoteData,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
  onEditNote: () -> Unit,
) {
  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier =
      Modifier.fillMaxSize()
        .clip(RoundedCornerShape(26.dp))
        .clickable(onClick = onEditNote)
        .testTag("widget_page_note"),
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = note.title.ifBlank { stringResource(R.string.widget_note_default_title) },
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = primaryTextColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Rounded.EditNote,
          contentDescription = stringResource(R.string.widget_note_dialog_title),
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(22.dp),
        )
      }

      Text(
        text = note.body.ifBlank { stringResource(R.string.widget_note_empty_body) },
        style = MaterialTheme.typography.bodyMedium,
        color = if (note.body.isBlank()) secondaryTextColor.copy(alpha = 0.80f) else primaryTextColor.copy(alpha = 0.90f),
        maxLines = 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 6.dp),
      )
    }
  }
}

/**
 * Functional Icon Group widget displaying up to 6 customizable quick-launch apps with notification dots.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IconGroupWidgetPage(
  apps: List<AppItem>,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
  badgeBorderColor: Color,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  onEditGroup: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current

  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier = Modifier.fillMaxSize().testTag("widget_page_icon_group"),
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = stringResource(R.string.widget_icon_group_title),
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = primaryTextColor,
          modifier = Modifier.padding(start = 4.dp),
        )
        IconButton(
          onClick = onEditGroup,
          modifier = Modifier.size(34.dp).testTag("widget_icon_group_edit"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = stringResource(R.string.widget_icon_group_edit_cd),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      if (apps.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier =
            Modifier.fillMaxWidth()
              .weight(1f)
              .clip(RoundedCornerShape(14.dp))
              .clickable(onClick = onEditGroup)
              .padding(horizontal = 12.dp),
        ) {
          Text(
            text = stringResource(R.string.widget_icon_group_empty),
            style = MaterialTheme.typography.bodySmall,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        Row(
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
          apps.take(6).forEach { app ->
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .combinedClickable(
                    onClick = { onAppClick(app) },
                    onLongClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                      onAppLongClick(app)
                    },
                  )
                  .padding(vertical = 4.dp, horizontal = 2.dp)
                  .testTag("widget_icon_group_app_${app.packageName}"),
            ) {
              AppIconBadge(
                app = app,
                sizeDp = 44,
                showNotificationDot = true,
                badgeBorderColor = badgeBorderColor,
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = app.label,
                style = MaterialTheme.typography.labelSmall,
                color = primaryTextColor.copy(alpha = 0.90f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun NoteEditorDialog(
  initialNote: QuickNoteData,
  onDismiss: () -> Unit,
  onSave: (title: String, body: String) -> Unit,
) {
  var title by remember(initialNote) { mutableStateOf(initialNote.title) }
  var body by remember(initialNote) { mutableStateOf(initialNote.body) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.widget_note_dialog_title)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          singleLine = true,
          label = { Text(stringResource(R.string.widget_note_title_label)) },
          modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
        )
        OutlinedTextField(
          value = body,
          onValueChange = { body = it },
          minLines = 3,
          maxLines = 6,
          label = { Text(stringResource(R.string.widget_note_body_label)) },
          modifier = Modifier.fillMaxWidth().testTag("note_body_input"),
        )
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(title, body) },
        modifier = Modifier.testTag("note_save_button"),
      ) {
        Text(text = stringResource(R.string.widget_note_save))
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        if (title.isNotBlank() || body.isNotBlank()) {
          TextButton(
            onClick = {
              title = ""
              body = ""
            },
            modifier = Modifier.testTag("note_clear_button"),
          ) {
            Text(text = stringResource(R.string.widget_note_clear))
          }
        }
        TextButton(onClick = onDismiss) {
          Text(text = stringResource(R.string.action_cancel))
        }
      }
    },
  )
}

@Composable
fun IconGroupPickerDialog(
  allApps: List<AppItem>,
  selectedPackages: List<String>,
  maxApps: Int,
  onDismiss: () -> Unit,
  onSave: (List<String>) -> Unit,
) {
  val chosen = remember(selectedPackages) { mutableStateListOf(*selectedPackages.toTypedArray()) }
  var filter by remember { mutableStateOf("") }

  val filteredApps =
    remember(allApps, filter) {
      val q = filter.trim()
      if (q.isEmpty()) allApps
      else allApps.filter { it.label.contains(q, ignoreCase = true) }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(text = stringResource(R.string.widget_icon_group_picker_title))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = stringResource(R.string.widget_icon_group_picker_subtitle, maxApps, chosen.size),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = filter,
          onValueChange = { filter = it },
          singleLine = true,
          placeholder = { Text(stringResource(R.string.search_placeholder)) },
          modifier = Modifier.fillMaxWidth().testTag("icon_group_search_input"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(4.dp),
          modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
        ) {
          items(items = filteredApps, key = { it.packageName }) { app ->
            val isSelected = app.packageName in chosen
            val canAdd = chosen.size < maxApps
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier =
                Modifier.fillMaxWidth()
                  .minimumInteractiveComponentSize()
                  .clip(RoundedCornerShape(12.dp))
                  .clickable(enabled = isSelected || canAdd) {
                    if (isSelected) {
                      chosen.remove(app.packageName)
                    } else if (canAdd) {
                      chosen.add(app.packageName)
                    }
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("icon_group_pick_${app.packageName}"),
            ) {
              AppIconBadge(
                app = app,
                sizeDp = 36,
                showNotificationDot = false,
                badgeBorderColor = MaterialTheme.colorScheme.surface,
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(
                text = app.label,
                style = MaterialTheme.typography.bodyMedium,
                color =
                  if (isSelected || canAdd) MaterialTheme.colorScheme.onSurface
                  else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
              Icon(
                imageVector =
                  if (isSelected) Icons.Rounded.CheckCircle
                  else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint =
                  if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = { onSave(chosen.toList()) },
        modifier = Modifier.testTag("icon_group_save_button"),
      ) {
        Text(text = stringResource(R.string.widget_icon_group_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(text = stringResource(R.string.action_cancel))
      }
    },
  )
}
