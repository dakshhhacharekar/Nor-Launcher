package com.example.ui.components

import android.Manifest
import android.os.BatteryManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.CalendarEventItem
import com.example.data.model.HomeWidgetStyle
import com.example.data.model.MediaPlaybackState
import com.example.data.model.QuickNoteData
import com.example.data.model.SmartWidgetType
import com.example.data.model.TemperatureUnit
import com.example.data.model.WeatherSnapshot
import com.example.data.weather.WeatherRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class BatteryTelemetry(
  val levelPercent: Int = 100,
  val isCharging: Boolean = false,
  val isFull: Boolean = false,
  val pluggedType: Int = 0,
  val temperatureC: Float = 0f,
  val health: Int = BatteryManager.BATTERY_HEALTH_GOOD,
)

/**
 * Customizable Smart Widget Stack using Jetpack Compose [VerticalPager]:
 * Stacks any user-selected combination of Clock, Weather, Music, Calendar, Battery,
 * Quick Note, and Icon Group into one compact vertically-scrollable hero card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWidgetStack(
  stackWidgets: List<SmartWidgetType>,
  clockStyle: HomeWidgetStyle,
  nowMillis: Long,
  is24Hour: Boolean,
  weather: WeatherSnapshot?,
  temperatureUnit: TemperatureUnit,
  isWeatherLoading: Boolean,
  hasLocationPermission: Boolean,
  battery: BatteryTelemetry,
  mediaState: MediaPlaybackState,
  isNotificationAccessGranted: Boolean,
  calendarEvents: List<CalendarEventItem>,
  hasCalendarPermission: Boolean,
  isCalendarLoading: Boolean,
  quickNote: QuickNoteData,
  iconGroupApps: List<AppItem>,
  badgeBorderColor: Color,
  useDarkTextContrast: Boolean,
  onLongPressWidget: () -> Unit,
  onRefreshWeather: () -> Unit,
  onRequestLocationWeather: () -> Unit,
  onLocationPermissionResult: (Boolean) -> Unit,
  onOpenCityDialog: () -> Unit,
  onToggleTemperatureUnit: () -> Unit,
  onMusicPlayPause: () -> Unit,
  onMusicPrevious: () -> Unit,
  onMusicNext: () -> Unit,
  onOpenMusicPlayer: () -> Unit,
  onEnableLiveMusicInfo: () -> Unit,
  onCalendarPermissionResult: (Boolean) -> Unit,
  onRefreshCalendar: () -> Unit,
  onOpenCalendarEvent: (CalendarEventItem) -> Unit,
  onOpenCalendarApp: () -> Unit,
  onEditNote: () -> Unit,
  onEditIconGroup: () -> Unit,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val safePages = remember(stackWidgets) { stackWidgets.ifEmpty { listOf(SmartWidgetType.CLOCK) } }
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { safePages.size })
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val noRippleInteraction = remember { MutableInteractionSource() }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier =
      modifier
        .fillMaxWidth()
        .widthIn(max = 440.dp)
        .height(156.dp)
        .testTag("home_widget_stack"),
  ) {
    VerticalPager(
      state = pagerState,
      beyondViewportPageCount = 1,
      pageSpacing = 12.dp,
      modifier =
        Modifier.weight(1f)
          .fillMaxHeight()
          .clip(RoundedCornerShape(26.dp))
          .combinedClickable(
            interactionSource = noRippleInteraction,
            indication = null,
            onClick = {},
            onLongClick = {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              onLongPressWidget()
            },
          )
          .testTag("home_widget_pager"),
    ) { page ->
      val widgetType = safePages.getOrElse(page) { SmartWidgetType.CLOCK }
      SmartWidgetCardContent(
        type = widgetType,
        clockStyle = clockStyle,
        nowMillis = nowMillis,
        is24Hour = is24Hour,
        weather = weather,
        temperatureUnit = temperatureUnit,
        isWeatherLoading = isWeatherLoading,
        hasLocationPermission = hasLocationPermission,
        battery = battery,
        mediaState = mediaState,
        isNotificationAccessGranted = isNotificationAccessGranted,
        calendarEvents = calendarEvents,
        hasCalendarPermission = hasCalendarPermission,
        isCalendarLoading = isCalendarLoading,
        quickNote = quickNote,
        iconGroupApps = iconGroupApps,
        badgeBorderColor = badgeBorderColor,
        useDarkTextContrast = useDarkTextContrast,
        onRefreshWeather = onRefreshWeather,
        onRequestLocationWeather = onRequestLocationWeather,
        onLocationPermissionResult = onLocationPermissionResult,
        onOpenCityDialog = onOpenCityDialog,
        onToggleTemperatureUnit = onToggleTemperatureUnit,
        onMusicPlayPause = onMusicPlayPause,
        onMusicPrevious = onMusicPrevious,
        onMusicNext = onMusicNext,
        onOpenMusicPlayer = onOpenMusicPlayer,
        onEnableLiveMusicInfo = onEnableLiveMusicInfo,
        onCalendarPermissionResult = onCalendarPermissionResult,
        onRefreshCalendar = onRefreshCalendar,
        onOpenCalendarEvent = onOpenCalendarEvent,
        onOpenCalendarApp = onOpenCalendarApp,
        onEditNote = onEditNote,
        onEditIconGroup = onEditIconGroup,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
      )
    }

    if (safePages.size > 1) {
      Spacer(modifier = Modifier.width(8.dp))

      val primaryTextColor = if (useDarkTextContrast) Color(0xFF0F172A) else Color(0xFFF8FAFC)
      val compactDots = safePages.size > 4
      val dotSpacing = if (compactDots) 2.dp else 5.dp
      val dotSlotHeight = if (compactDots) 18.dp else 24.dp
      val selectedDotHeight = if (compactDots) 15.dp else 20.dp
      val unselectedDotHeight = if (compactDots) 5.dp else 7.dp

      Column(
        verticalArrangement = Arrangement.spacedBy(dotSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 4.dp).testTag("widget_pager_dots"),
      ) {
        safePages.forEachIndexed { index, type ->
          val selected = pagerState.currentPage == index
          val pageLabel = smartWidgetDisplayName(type)
          val dotHeight by
            animateDpAsState(
              targetValue = if (selected) selectedDotHeight else unselectedDotHeight,
              label = "widget_dot_height_${type.id}",
            )
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier.size(width = 20.dp, height = dotSlotHeight)
                .clip(CircleShape)
                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                .semantics { contentDescription = pageLabel }
                .testTag("widget_dot_$index"),
          ) {
            Box(
              modifier =
                Modifier.width(5.dp)
                  .height(dotHeight)
                  .clip(CircleShape)
                  .background(
                    if (selected) MaterialTheme.colorScheme.primary
                    else primaryTextColor.copy(alpha = 0.34f)
                  )
            )
          }
        }
      }
    }
  }
}

/**
 * Shared renderer for any [SmartWidgetType], used both inside [HomeWidgetStack]
 * and for standalone pinned smart widget cards on the Home Screen canvas.
 */
@Composable
fun SmartWidgetCardContent(
  type: SmartWidgetType,
  clockStyle: HomeWidgetStyle,
  nowMillis: Long,
  is24Hour: Boolean,
  weather: WeatherSnapshot?,
  temperatureUnit: TemperatureUnit,
  isWeatherLoading: Boolean,
  hasLocationPermission: Boolean,
  battery: BatteryTelemetry,
  mediaState: MediaPlaybackState,
  isNotificationAccessGranted: Boolean,
  calendarEvents: List<CalendarEventItem>,
  hasCalendarPermission: Boolean,
  isCalendarLoading: Boolean,
  quickNote: QuickNoteData,
  iconGroupApps: List<AppItem>,
  badgeBorderColor: Color,
  useDarkTextContrast: Boolean,
  onRefreshWeather: () -> Unit,
  onRequestLocationWeather: () -> Unit,
  onLocationPermissionResult: (Boolean) -> Unit,
  onOpenCityDialog: () -> Unit,
  onToggleTemperatureUnit: () -> Unit,
  onMusicPlayPause: () -> Unit,
  onMusicPrevious: () -> Unit,
  onMusicNext: () -> Unit,
  onOpenMusicPlayer: () -> Unit,
  onEnableLiveMusicInfo: () -> Unit,
  onCalendarPermissionResult: (Boolean) -> Unit,
  onRefreshCalendar: () -> Unit,
  onOpenCalendarEvent: (CalendarEventItem) -> Unit,
  onOpenCalendarApp: () -> Unit,
  onEditNote: () -> Unit,
  onEditIconGroup: () -> Unit,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
) {
  val primaryTextColor = if (useDarkTextContrast) Color(0xFF0F172A) else Color(0xFFF8FAFC)
  val secondaryTextColor = if (useDarkTextContrast) Color(0xFF334155) else Color(0xFFCBD5E1)
  val glassSurface =
    if (useDarkTextContrast) Color.White.copy(alpha = 0.62f) else Color(0xFF0D1426).copy(alpha = 0.44f)
  val glassBorder =
    if (useDarkTextContrast) Color.White.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.22f)

  when (type) {
    SmartWidgetType.CLOCK ->
      ClockWidgetPage(
        style = clockStyle,
        nowMillis = nowMillis,
        is24Hour = is24Hour,
        weather = weather,
        temperatureUnit = temperatureUnit,
        battery = battery,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
      )
    SmartWidgetType.WEATHER ->
      WeatherWidgetPage(
        weather = weather,
        unit = temperatureUnit,
        isLoading = isWeatherLoading,
        hasLocationPermission = hasLocationPermission,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
        onRefresh = onRefreshWeather,
        onRequestLocationWeather = onRequestLocationWeather,
        onLocationPermissionResult = onLocationPermissionResult,
        onSetCityClick = onOpenCityDialog,
        onToggleUnit = onToggleTemperatureUnit,
      )
    SmartWidgetType.MUSIC ->
      MusicWidgetPage(
        mediaState = mediaState,
        isNotificationAccessGranted = isNotificationAccessGranted,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
        onPlayPause = onMusicPlayPause,
        onPrevious = onMusicPrevious,
        onNext = onMusicNext,
        onOpenPlayer = onOpenMusicPlayer,
        onEnableLiveInfo = onEnableLiveMusicInfo,
      )
    SmartWidgetType.CALENDAR ->
      CalendarWidgetPage(
        nowMillis = nowMillis,
        is24Hour = is24Hour,
        events = calendarEvents,
        hasCalendarPermission = hasCalendarPermission,
        isLoading = isCalendarLoading,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
        onCalendarPermissionResult = onCalendarPermissionResult,
        onRefresh = onRefreshCalendar,
        onOpenEvent = onOpenCalendarEvent,
        onOpenCalendarApp = onOpenCalendarApp,
      )
    SmartWidgetType.BATTERY ->
      BatteryWidgetPage(
        battery = battery,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
      )
    SmartWidgetType.NOTE ->
      QuickNoteWidgetPage(
        note = quickNote,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
        onEditNote = onEditNote,
      )
    SmartWidgetType.ICON_GROUP ->
      IconGroupWidgetPage(
        apps = iconGroupApps,
        primaryTextColor = primaryTextColor,
        secondaryTextColor = secondaryTextColor,
        glassSurface = glassSurface,
        glassBorder = glassBorder,
        badgeBorderColor = badgeBorderColor,
        onAppClick = onAppClick,
        onAppLongClick = onAppLongClick,
        onEditGroup = onEditIconGroup,
      )
  }
}

@Composable
private fun ClockWidgetPage(
  style: HomeWidgetStyle,
  nowMillis: Long,
  is24Hour: Boolean,
  weather: WeatherSnapshot?,
  temperatureUnit: TemperatureUnit,
  battery: BatteryTelemetry,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
) {
  val date = remember(nowMillis) { Date(nowMillis) }
  val timePattern = if (is24Hour) "HH:mm" else "h:mm"
  val formattedTime =
    remember(nowMillis, is24Hour) {
      SimpleDateFormat(timePattern, Locale.getDefault()).format(date)
    }
  val formattedDate =
    remember(nowMillis) {
      SimpleDateFormat("EEE, d MMMM", Locale.getDefault()).format(date)
    }
  val weatherSummary =
    weather?.let { " · ${formatTemp(it.temperatureC, temperatureUnit)}" }.orEmpty()

  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.fillMaxSize().testTag("widget_page_clock"),
  ) {
    when (style) {
      HomeWidgetStyle.MINIMAL_CLOCK -> {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = formattedTime,
            style =
              MaterialTheme.typography.displayLarge.copy(
                fontSize = 60.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1.5).sp,
              ),
            color = primaryTextColor,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formattedDate,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = primaryTextColor.copy(alpha = 0.90f),
            )
            if (weatherSummary.isNotEmpty()) {
              Spacer(modifier = Modifier.width(4.dp))
              Icon(
                imageVector = weatherIconForCode(weather?.weatherCode ?: 0, weather?.isDay ?: true),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = weatherSummary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = primaryTextColor.copy(alpha = 0.90f),
              )
            }
          }
        }
      }
      HomeWidgetStyle.GLASS_CLOCK_BATTERY -> {
        Surface(
          color = glassSurface,
          shape = RoundedCornerShape(26.dp),
          border = BorderStroke(1.dp, glassBorder),
          modifier = Modifier.fillMaxSize(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
          ) {
            Column {
              Text(
                text = formattedTime,
                style =
                  MaterialTheme.typography.displayLarge.copy(
                    fontSize = 44.sp,
                    lineHeight = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.0).sp,
                  ),
                color = primaryTextColor,
              )
              Text(
                text = "$formattedDate$weatherSummary",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor,
              )
            }
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.90f),
              shape = RoundedCornerShape(16.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              ) {
                Icon(
                  imageVector =
                    if (battery.isCharging) Icons.Rounded.BatteryChargingFull
                    else Icons.Rounded.BatteryStd,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = "${battery.levelPercent}%",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
              }
            }
          }
        }
      }
      HomeWidgetStyle.ANALOG_CLOCK -> {
        AnalogClockWidgetFace(
          nowMillis = nowMillis,
          formattedDate = formattedDate,
          weatherSummary = weatherSummary,
          primaryTextColor = primaryTextColor,
          secondaryTextColor = secondaryTextColor,
          glassSurface = glassSurface,
          glassBorder = glassBorder,
        )
      }
      HomeWidgetStyle.COMPACT_BATTERY_DATE -> {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxSize(),
        ) {
          Text(
            text = formattedTime,
            style =
              MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                lineHeight = 52.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = primaryTextColor,
          )
          Spacer(modifier = Modifier.height(8.dp))
          Surface(
            color = glassSurface,
            shape = CircleShape,
            border = BorderStroke(1.dp, glassBorder),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
              Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelLarge,
                color = primaryTextColor,
              )
              Text(
                text = "  ·  ${battery.levelPercent}%$weatherSummary",
                style = MaterialTheme.typography.labelLarge,
                color = secondaryTextColor,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeatherWidgetPage(
  weather: WeatherSnapshot?,
  unit: TemperatureUnit,
  isLoading: Boolean,
  hasLocationPermission: Boolean,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
  onRefresh: () -> Unit,
  onRequestLocationWeather: () -> Unit,
  onLocationPermissionResult: (Boolean) -> Unit,
  onSetCityClick: () -> Unit,
  onToggleUnit: () -> Unit,
) {
  val locationPermissionLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.RequestPermission(),
      onResult = onLocationPermissionResult,
    )
  val onUseLocationClick = {
    if (hasLocationPermission) {
      onRequestLocationWeather()
    } else {
      locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
  }

  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier = Modifier.fillMaxSize().testTag("widget_page_weather"),
  ) {
    if (weather == null) {
      // First-run setup card: Use coarse location or enter city manually
      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Rounded.WbSunny,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = stringResource(R.string.weather_set_location_title),
              style = MaterialTheme.typography.titleMedium,
              color = primaryTextColor,
            )
            Text(
              text =
                if (isLoading) stringResource(R.string.weather_loading)
                else stringResource(R.string.weather_set_location_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = secondaryTextColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          if (isLoading) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          FilledTonalButton(
            onClick = onUseLocationClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("weather_use_location_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.weather_use_location),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          OutlinedButton(
            onClick = onSetCityClick,
            enabled = !isLoading,
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier =
              Modifier.weight(1f)
                .minimumInteractiveComponentSize()
                .testTag("weather_set_city_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.EditLocationAlt,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(R.string.weather_set_city),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    } else {
      val conditionText = WeatherRepository.describeWmoCode(weather.weatherCode)
      val toggleCd = stringResource(R.string.cd_toggle_temp_unit)

      Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp),
      ) {
        // Top row: City + Edit city + Refresh
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onSetCityClick)
                .padding(vertical = 2.dp, horizontal = 2.dp)
                .testTag("weather_city_label"),
          ) {
            Icon(
              imageVector = Icons.Rounded.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = weather.cityName,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
              color = primaryTextColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          if (isLoading) {
            CircularProgressIndicator(
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
              modifier = Modifier.padding(end = 8.dp).size(16.dp),
            )
          } else {
            IconButton(
              onClick = onRefresh,
              modifier = Modifier.size(36.dp).testTag("weather_refresh_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = stringResource(R.string.cd_refresh_weather),
                tint = secondaryTextColor,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Middle row: Large temp (tap to toggle °C / °F) + Icon & condition
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
              Modifier.clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggleUnit)
                .semantics { contentDescription = toggleCd }
                .padding(end = 8.dp)
                .testTag("weather_temp_toggle"),
          ) {
            Text(
              text = formatTemp(weather.temperatureC, unit),
              style =
                MaterialTheme.typography.displayLarge.copy(
                  fontSize = 40.sp,
                  lineHeight = 44.sp,
                  fontWeight = FontWeight.Bold,
                ),
              color = primaryTextColor,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = conditionText,
                style = MaterialTheme.typography.titleSmall,
                color = primaryTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text =
                  stringResource(
                    R.string.weather_high_low,
                    formatTemp(weather.highC, unit),
                    formatTemp(weather.lowC, unit),
                  ),
                style = MaterialTheme.typography.labelMedium,
                color = secondaryTextColor,
              )
            }
          }

          Icon(
            imageVector = weatherIconForCode(weather.weatherCode, weather.isDay),
            contentDescription = conditionText,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(38.dp),
          )
        }

        // Bottom metrics row
        Row(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(R.string.weather_feels_like, formatTemp(weather.feelsLikeC, unit)),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryTextColor,
          )
          Text(
            text = stringResource(R.string.weather_humidity, weather.humidityPercent),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryTextColor,
          )
          Text(
            text = stringResource(R.string.weather_wind, weather.windKmh),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryTextColor,
          )
        }
      }
    }
  }
}

@Composable
private fun BatteryWidgetPage(
  battery: BatteryTelemetry,
  primaryTextColor: Color,
  secondaryTextColor: Color,
  glassSurface: Color,
  glassBorder: Color,
) {
  val fillFraction by
    animateFloatAsState(
      targetValue = (battery.levelPercent / 100f).coerceIn(0f, 1f),
      label = "battery_fill_fraction",
    )

  val accentColor =
    when {
      battery.isCharging || battery.isFull -> Color(0xFF22C55E)
      battery.levelPercent <= 15 -> Color(0xFFEF4444)
      battery.levelPercent <= 28 -> Color(0xFFF59E0B)
      else -> MaterialTheme.colorScheme.primary
    }

  val statusLabel =
    when {
      battery.isFull -> stringResource(R.string.battery_status_full)
      battery.isCharging ->
        when (battery.pluggedType) {
          BatteryManager.BATTERY_PLUGGED_AC -> stringResource(R.string.battery_status_charging_ac)
          BatteryManager.BATTERY_PLUGGED_USB -> stringResource(R.string.battery_status_charging_usb)
          BatteryManager.BATTERY_PLUGGED_WIRELESS ->
            stringResource(R.string.battery_status_charging_wireless)
          else -> stringResource(R.string.battery_status_charging)
        }
      else -> stringResource(R.string.battery_status_discharging)
    }

  val healthLabel =
    when (battery.health) {
      BatteryManager.BATTERY_HEALTH_OVERHEAT -> stringResource(R.string.battery_health_overheat)
      BatteryManager.BATTERY_HEALTH_COLD -> stringResource(R.string.battery_health_cold)
      else -> stringResource(R.string.battery_health_good)
    }

  Surface(
    color = glassSurface,
    shape = RoundedCornerShape(26.dp),
    border = BorderStroke(1.dp, glassBorder),
    modifier = Modifier.fillMaxSize().testTag("widget_page_battery"),
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector =
              if (battery.isCharging) Icons.Rounded.BatteryChargingFull
              else Icons.Rounded.BatteryStd,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(28.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${battery.levelPercent}%",
            style =
              MaterialTheme.typography.displayLarge.copy(
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
              ),
            color = primaryTextColor,
          )
        }

        Column(horizontalAlignment = Alignment.End) {
          Text(
            text = statusLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = accentColor,
          )
          val meta =
            if (battery.temperatureC > 0f) {
              "${stringResource(R.string.battery_temp_format, battery.temperatureC)} · $healthLabel"
            } else {
              healthLabel
            }
          Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = secondaryTextColor,
          )
        }
      }

      // Sleek animated charge progress bar
      Box(
        modifier =
          Modifier.fillMaxWidth()
            .height(14.dp)
            .clip(CircleShape)
            .background(primaryTextColor.copy(alpha = 0.14f)),
      ) {
        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(fillFraction)
              .clip(CircleShape)
              .background(
                Brush.horizontalGradient(
                  colors = listOf(accentColor.copy(alpha = 0.80f), accentColor)
                )
              )
        )
      }
    }
  }
}

@Composable
fun WeatherCityDialog(
  initialCity: String,
  onDismiss: () -> Unit,
  onConfirm: (String) -> Unit,
) {
  var cityText by remember(initialCity) { mutableStateOf(initialCity) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = stringResource(R.string.weather_city_dialog_title)) },
    text = {
      OutlinedTextField(
        value = cityText,
        onValueChange = { cityText = it },
        singleLine = true,
        label = { Text(stringResource(R.string.weather_city_dialog_label)) },
        placeholder = { Text(stringResource(R.string.weather_city_dialog_placeholder)) },
        modifier = Modifier.fillMaxWidth().testTag("weather_city_input"),
      )
    },
    confirmButton = {
      TextButton(
        onClick = { onConfirm(cityText) },
        enabled = cityText.trim().length >= 2,
        modifier = Modifier.testTag("weather_city_confirm"),
      ) {
        Text(stringResource(R.string.weather_city_dialog_save))
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.action_cancel))
      }
    },
  )
}

private fun formatTemp(tempC: Double, unit: TemperatureUnit): String =
  when (unit) {
    TemperatureUnit.CELSIUS -> "${tempC.roundToInt()}°C"
    TemperatureUnit.FAHRENHEIT -> "${((tempC * 9.0 / 5.0) + 32.0).roundToInt()}°F"
  }

private fun weatherIconForCode(code: Int, isDay: Boolean): ImageVector =
  when (code) {
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2 -> Icons.Rounded.WbCloudy
    3, 45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.WbCloudy
  }
