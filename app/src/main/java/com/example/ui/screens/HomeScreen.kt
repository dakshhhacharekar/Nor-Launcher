package com.example.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.CalendarEventItem
import com.example.data.model.HomeWidgetStyle
import com.example.data.model.HostedWidgetRecord
import com.example.data.model.MediaPlaybackState
import com.example.data.model.SmartWidgetsConfig
import com.example.data.model.TemperatureUnit
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPreset
import com.example.data.model.WeatherSnapshot
import com.example.data.widgets.LauncherWidgetHostManager
import com.example.ui.components.AppIconBadge
import com.example.ui.components.BatteryTelemetry
import com.example.ui.components.HomeWidgetStack
import com.example.ui.components.HostedWidgetSection
import com.example.ui.components.SmartWidgetCardContent
import com.example.ui.theme.LocalIconShapeOption
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.shapeForIcon
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreenContent(
  homeApps: List<AppItem>,
  widgetStyle: HomeWidgetStyle,
  smartWidgetsConfig: SmartWidgetsConfig,
  iconGroupApps: List<AppItem>,
  calendarEvents: List<CalendarEventItem>,
  hasCalendarPermission: Boolean,
  isCalendarLoading: Boolean,
  mediaPlaybackState: MediaPlaybackState,
  isNotificationAccessGranted: Boolean,
  wallpaperConfig: WallpaperConfig,
  hasCustomWallpaperBitmap: Boolean,
  weather: WeatherSnapshot?,
  temperatureUnit: TemperatureUnit,
  isWeatherLoading: Boolean,
  hasLocationPermission: Boolean,
  hostedWidgets: List<HostedWidgetRecord>,
  editingWidgetId: Int?,
  widgetHostManager: LauncherWidgetHostManager,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  onWidgetLongPress: () -> Unit,
  onStartEditWidget: (Int) -> Unit,
  onStopEditWidget: () -> Unit,
  onResizeWidget: (Int, Int, Int) -> Unit,
  onMoveWidget: (Int, Int) -> Unit,
  onReconfigureWidget: (Int) -> Unit,
  onRemoveWidget: (Int) -> Unit,
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
  onSearchBarClick: () -> Unit,
  onSwipeUpHintClick: () -> Unit,
  onOpenNewsPage: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val darkTheme = LocalIsDarkTheme.current
  val haptics = LocalHapticFeedback.current
  val is24Hour = DateFormat.is24HourFormat(context)
  val nowMillis by
    rememberCurrentTimeMillis(
      context = context,
      tickSeconds = widgetStyle == HomeWidgetStyle.ANALOG_CLOCK,
    )
  val batteryTelemetry = rememberBatteryState(context)

  val effectiveIsDarkBackdrop =
    darkTheme ||
      (wallpaperConfig.preset.isAlwaysDark &&
        (wallpaperConfig.preset != WallpaperPreset.CUSTOM_PHOTO || hasCustomWallpaperBitmap)) ||
      wallpaperConfig.dimPercent >= 35
  val useDarkTextContrast = !effectiveIsDarkBackdrop

  val homeSecondaryColor = if (useDarkTextContrast) Color(0xFF334155) else Color(0xFFCBD5E1)
  val badgeBorderColor = if (useDarkTextContrast) Color(0xFFD2D5DA) else Color(0xFF141B2B)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp)
        .testTag("home_screen"),
  ) {
    Spacer(modifier = Modifier.height(22.dp))

    // Customizable Smart Widget Stack (Clock, Weather, Music, Calendar, Battery, Note, Icon Group)
    HomeWidgetStack(
      stackWidgets = smartWidgetsConfig.stackWidgets,
      clockStyle = widgetStyle,
      nowMillis = nowMillis,
      is24Hour = is24Hour,
      weather = weather,
      temperatureUnit = temperatureUnit,
      isWeatherLoading = isWeatherLoading,
      hasLocationPermission = hasLocationPermission,
      battery = batteryTelemetry,
      mediaState = mediaPlaybackState,
      isNotificationAccessGranted = isNotificationAccessGranted,
      calendarEvents = calendarEvents,
      hasCalendarPermission = hasCalendarPermission,
      isCalendarLoading = isCalendarLoading,
      quickNote = smartWidgetsConfig.quickNote,
      iconGroupApps = iconGroupApps,
      badgeBorderColor = badgeBorderColor,
      useDarkTextContrast = useDarkTextContrast,
      onLongPressWidget = onWidgetLongPress,
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

    // Middle Home Canvas: Pinned Standalone Smart Widgets + Real Android AppWidgets
    val pinnedSmartWidgets = smartWidgetsConfig.pinnedWidgets
    if (pinnedSmartWidgets.isEmpty() && hostedWidgets.isEmpty()) {
      Spacer(modifier = Modifier.weight(1f))
    } else {
      Box(
        modifier =
          Modifier.weight(1f)
            .fillMaxWidth()
            .widthIn(max = 460.dp)
            .padding(vertical = 12.dp),
      ) {
        Column(
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier =
            Modifier.fillMaxWidth()
              .verticalScroll(rememberScrollState()),
        ) {
          pinnedSmartWidgets.forEach { widgetType ->
            val noRipple = remember(widgetType) { MutableInteractionSource() }
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .height(156.dp)
                  .clip(RoundedCornerShape(26.dp))
                  .combinedClickable(
                    interactionSource = noRipple,
                    indication = null,
                    onClick = {},
                    onLongClick = {
                      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                      onWidgetLongPress()
                    },
                  )
                  .testTag("pinned_smart_widget_${widgetType.id}"),
            ) {
              SmartWidgetCardContent(
                type = widgetType,
                clockStyle = widgetStyle,
                nowMillis = nowMillis,
                is24Hour = is24Hour,
                weather = weather,
                temperatureUnit = temperatureUnit,
                isWeatherLoading = isWeatherLoading,
                hasLocationPermission = hasLocationPermission,
                battery = batteryTelemetry,
                mediaState = mediaPlaybackState,
                isNotificationAccessGranted = isNotificationAccessGranted,
                calendarEvents = calendarEvents,
                hasCalendarPermission = hasCalendarPermission,
                isCalendarLoading = isCalendarLoading,
                quickNote = smartWidgetsConfig.quickNote,
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
          }

          if (hostedWidgets.isNotEmpty()) {
            HostedWidgetSection(
              widgets = hostedWidgets,
              editingWidgetId = editingWidgetId,
              widgetHostManager = widgetHostManager,
              useDarkTextContrast = useDarkTextContrast,
              onStartEditWidget = onStartEditWidget,
              onStopEditWidget = onStopEditWidget,
              onResizeWidget = onResizeWidget,
              onMoveWidget = onMoveWidget,
              onReconfigureWidget = onReconfigureWidget,
              onRemoveWidget = onRemoveWidget,
            )
          }
        }
      }
    }

    // Home quick-access app grid (Smart Launcher style: clean rounded icons with notification dots)
    HomeAppsCluster(
      apps = homeApps,
      badgeBorderColor = badgeBorderColor,
      emptyHintColor = homeSecondaryColor,
      onAppClick = onAppClick,
      onAppLongClick = onAppLongClick,
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    )

    Spacer(modifier = Modifier.height(22.dp))

    // Bottom frosted "Nor search" pill bar
    HomeSearchPill(
      darkTheme = effectiveIsDarkBackdrop,
      onClick = onSearchBarClick,
      modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp),
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Subtle 2-page indicator (News feed left, Home active) + swipe-up drawer affordance
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.padding(horizontal = 8.dp),
    ) {
      val newsIndicatorCd = stringResource(R.string.news_home_indicator_cd)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier =
          Modifier.clip(CircleShape)
            .clickable(onClick = onOpenNewsPage)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = newsIndicatorCd }
            .testTag("home_news_page_indicator"),
      ) {
        Surface(
          shape = CircleShape,
          color = homeSecondaryColor.copy(alpha = 0.48f),
          modifier = Modifier.size(6.dp),
        ) {}
        Surface(
          shape = CircleShape,
          color = homeSecondaryColor.copy(alpha = 0.92f),
          modifier = Modifier.width(16.dp).height(6.dp),
        ) {}
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier =
          Modifier.clip(CircleShape)
            .clickable(onClick = onSwipeUpHintClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("home_swipe_up_hint"),
      ) {
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowUp,
          contentDescription = null,
          tint = homeSecondaryColor.copy(alpha = 0.78f),
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = stringResource(R.string.home_swipe_hint),
          style = MaterialTheme.typography.labelSmall,
          color = homeSecondaryColor.copy(alpha = 0.80f),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeAppsCluster(
  apps: List<AppItem>,
  badgeBorderColor: Color,
  emptyHintColor: Color,
  onAppClick: (AppItem) -> Unit,
  onAppLongClick: (AppItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (apps.isEmpty()) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = modifier.height(92.dp).padding(horizontal = 24.dp),
    ) {
      Text(
        text = stringResource(R.string.home_empty_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = emptyHintColor,
        textAlign = TextAlign.Center,
      )
    }
    return
  }

  val haptics = LocalHapticFeedback.current

  // If 9-10 apps are pinned, mirror the Smart Launcher mountain-flank layout:
  // 2 apps flanking left & right on top + two neat rows of 4 below.
  val hasFlankRow = apps.size > 8
  val flankApps = if (hasFlankRow) apps.take(apps.size - 8) else emptyList()
  val gridApps = if (hasFlankRow) apps.drop(flankApps.size) else apps
  val gridRows = gridApps.chunked(4)

  Column(
    verticalArrangement = Arrangement.spacedBy(20.dp),
    modifier = modifier.testTag("home_apps_cluster"),
  ) {
    if (flankApps.isNotEmpty()) {
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      ) {
        HomeIconCell(
          app = flankApps.first(),
          badgeBorderColor = badgeBorderColor,
          onClick = { onAppClick(flankApps.first()) },
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onAppLongClick(flankApps.first())
          },
        )
        if (flankApps.size >= 2) {
          HomeIconCell(
            app = flankApps[1],
            badgeBorderColor = badgeBorderColor,
            onClick = { onAppClick(flankApps[1]) },
            onLongClick = {
              haptics.performHapticFeedback(HapticFeedbackType.LongPress)
              onAppLongClick(flankApps[1])
            },
          )
        }
      }
    }

    gridRows.forEach { rowApps ->
      Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
      ) {
        for (i in 0 until 4) {
          val app = rowApps.getOrNull(i)
          if (app != null) {
            HomeIconCell(
              app = app,
              badgeBorderColor = badgeBorderColor,
              onClick = { onAppClick(app) },
              onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onAppLongClick(app)
              },
            )
          } else {
            Spacer(modifier = Modifier.size(62.dp))
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeIconCell(
  app: AppItem,
  badgeBorderColor: Color,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val cellShape = shapeForIcon(LocalIconShapeOption.current)
  Box(
    contentAlignment = Alignment.Center,
    modifier =
      Modifier.size(64.dp)
        .minimumInteractiveComponentSize()
        .clip(cellShape)
        .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        .testTag("home_app_${app.packageName}"),
  ) {
    AppIconBadge(
      app = app,
      sizeDp = 58,
      showNotificationDot = true,
      elevated = true,
      badgeBorderColor = badgeBorderColor,
    )
  }
}

@Composable
private fun HomeSearchPill(
  darkTheme: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val pillColor =
    if (darkTheme) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.84f)
  val borderColor =
    if (darkTheme) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.95f)
  val textColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF1E293B)
  val searchCd = stringResource(R.string.cd_home_search_bar)

  val labelText =
    buildAnnotatedString {
      withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
        append(stringResource(R.string.home_search_prefix))
      }
      append(' ')
      withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
        append(stringResource(R.string.home_search_suffix))
      }
    }

  Surface(
    color = pillColor,
    shape = CircleShape,
    border = BorderStroke(1.dp, borderColor),
    shadowElevation = if (darkTheme) 0.dp else 2.dp,
    modifier =
      modifier
        .height(54.dp)
        .clip(CircleShape)
        .clickable(onClick = onClick)
        .semantics { contentDescription = searchCd }
        .testTag("home_search_pill"),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(
        text = labelText,
        style = MaterialTheme.typography.titleMedium,
        color = textColor,
      )
    }
  }
}

@Composable
private fun rememberCurrentTimeMillis(
  context: Context,
  tickSeconds: Boolean,
) =
  remember { mutableLongStateOf(System.currentTimeMillis()) }.also { state ->
    DisposableEffect(context) {
      state.longValue = System.currentTimeMillis()
      val receiver =
        object : BroadcastReceiver() {
          override fun onReceive(ctx: Context?, intent: Intent?) {
            state.longValue = System.currentTimeMillis()
          }
        }
      val filter =
        IntentFilter().apply {
          addAction(Intent.ACTION_TIME_TICK)
          addAction(Intent.ACTION_TIME_CHANGED)
          addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
      ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
      onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    LaunchedEffect(tickSeconds) {
      if (tickSeconds) {
        while (true) {
          state.longValue = System.currentTimeMillis()
          val millisIntoSecond = state.longValue % 1_000L
          delay((1_000L - millisIntoSecond).coerceAtLeast(100L))
        }
      }
    }
  }

@Composable
private fun rememberBatteryState(context: Context): BatteryTelemetry {
  var telemetry by remember { mutableStateOf(BatteryTelemetry()) }

  DisposableEffect(context) {
    fun updateFromIntent(intent: Intent?) {
      if (intent == null) return
      val rawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
      val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
      val computedLevel =
        if (rawLevel >= 0 && scale > 0) {
          ((rawLevel * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
          telemetry.levelPercent
        }
      val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
      val isFull = status == BatteryManager.BATTERY_STATUS_FULL
      val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || isFull
      val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
      val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
      val tempC = if (tempTenths > 0) tempTenths / 10f else 0f
      val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_GOOD)

      telemetry =
        BatteryTelemetry(
          levelPercent = computedLevel,
          isCharging = isCharging,
          isFull = isFull,
          pluggedType = plugged,
          temperatureC = tempC,
          health = health,
        )
    }

    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    updateFromIntent(sticky)

    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
          updateFromIntent(intent)
        }
      }
    ContextCompat.registerReceiver(
      context,
      receiver,
      IntentFilter(Intent.ACTION_BATTERY_CHANGED),
      ContextCompat.RECEIVER_EXPORTED,
    )
    onDispose { runCatching { context.unregisterReceiver(receiver) } }
  }

  return telemetry
}
