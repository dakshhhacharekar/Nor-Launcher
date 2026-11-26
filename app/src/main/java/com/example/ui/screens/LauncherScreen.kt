package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppItem
import com.example.data.model.BuiltInCategory
import com.example.data.model.GestureAction
import com.example.data.model.HomeGesture
import com.example.data.model.NewsArticle
import com.example.data.news.RssFeedParser
import com.example.data.search.QuickActionMatcher
import com.example.data.security.BiometricAuthHelper
import com.example.data.security.BiometricAvailability
import com.example.data.store.LauncherPreferencesRepository
import com.example.data.widgets.CalendarRepository
import com.example.data.widgets.LauncherWidgetHostManager
import com.example.data.widgets.MediaControlManager
import com.example.service.AccessibilityAccessHelper
import com.example.service.NotificationAccessHelper
import com.example.ui.components.AppActionSheet
import com.example.ui.components.CategoryNameDialog
import com.example.ui.components.DeleteCategoryConfirmDialog
import com.example.ui.components.GlassyAppDrawer
import com.example.ui.components.HomeWallpaperBackdrop
import com.example.ui.components.IconGroupPickerDialog
import com.example.ui.components.IconPackPickerSheet
import com.example.ui.components.ManageCustomCategorySheet
import com.example.ui.components.MoveToCategorySheet
import com.example.ui.components.NewsSourcesSheet
import com.example.ui.components.NoteEditorDialog
import com.example.ui.components.WallpaperPickerSheet
import com.example.ui.components.WeatherCityDialog
import com.example.ui.components.WidgetHubSheet
import com.example.ui.components.WidgetPickerSheet
import com.example.ui.viewmodel.HiddenAppsFlowState
import com.example.ui.viewmodel.LauncherEvent
import com.example.ui.viewmodel.LauncherViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val PAGE_NEWS = 0
private const val PAGE_HOME = 1

@Composable
fun LauncherScreen(
  viewModel: LauncherViewModel,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val gridState = rememberLazyGridState()
  val desktopPagerState = rememberPagerState(initialPage = PAGE_HOME, pageCount = { 2 })
  val coroutineScope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current

  val undoLabel = stringResource(R.string.action_undo)
  val biometricTitle = stringResource(R.string.biometric_prompt_title)
  val biometricSubtitle = stringResource(R.string.biometric_prompt_subtitle)
  val biometricNegativePin = stringResource(R.string.biometric_prompt_negative_pin)

  val builtInLabels =
    remember(context) { BuiltInCategory.entries.associateWith { viewModel.builtInLabel(it) } }

  val widgetBindLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
      viewModel.onWidgetBindPermissionResult(granted = result.resultCode == Activity.RESULT_OK)
    }

  LaunchedEffect(desktopPagerState.currentPage) {
    if (desktopPagerState.currentPage == PAGE_NEWS) {
      viewModel.onNewsPageOpened()
    }
  }

  LaunchedEffect(viewModel) {
    var snackbarJob: Job? = null
    viewModel.events.collect { event ->
      when (event) {
        LauncherEvent.ScrollToTop -> {
          focusManager.clearFocus()
          if (gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0) {
            coroutineScope.launch { gridState.animateScrollToItem(0) }
          }
        }
        LauncherEvent.ReturnToHomePage -> {
          if (desktopPagerState.currentPage != PAGE_HOME) {
            coroutineScope.launch { desktopPagerState.animateScrollToPage(PAGE_HOME) }
          }
        }
        LauncherEvent.TriggerBiometricPrompt -> {
          focusManager.clearFocus()
          val activity = context.findFragmentActivity()
          if (activity != null) {
            BiometricAuthHelper.authenticate(
              activity = activity,
              title = biometricTitle,
              subtitle = biometricSubtitle,
              negativeButtonText = biometricNegativePin,
              onSuccess = viewModel::onBiometricAuthSuccess,
              onUsePin = viewModel::onBiometricUsePin,
              onLockout = viewModel::onBiometricLockout,
              onCanceled = viewModel::onBiometricCanceled,
            )
          } else {
            viewModel.onBiometricUsePin()
          }
        }
        is LauncherEvent.LaunchWidgetBindPermission -> {
          val intent =
            viewModel.widgetHostManager.buildBindPermissionIntent(
              appWidgetId = event.appWidgetId,
              providerInfo = event.providerInfo,
            )
          runCatching { widgetBindLauncher.launch(intent) }
            .onFailure { viewModel.onWidgetBindLaunchFailed() }
        }
        is LauncherEvent.LaunchWidgetConfigure -> {
          val activity = context.findFragmentActivity()
          val requestCode =
            if (event.isReconfigure) LauncherWidgetHostManager.REQUEST_RECONFIGURE_WIDGET
            else LauncherWidgetHostManager.REQUEST_CONFIGURE_WIDGET
          val launched =
            activity != null &&
              viewModel.widgetHostManager.startConfigureActivity(
                activity = activity,
                appWidgetId = event.appWidgetId,
                requestCode = requestCode,
              )
          if (!launched) {
            viewModel.onWidgetConfigureResult(resultOk = false, isReconfigure = event.isReconfigure)
          }
        }
        is LauncherEvent.ShowSnackbar -> {
          snackbarJob?.cancel()
          snackbarHostState.currentSnackbarData?.dismiss()
          snackbarJob =
            coroutineScope.launch {
              val result =
                snackbarHostState.showSnackbar(
                  message = event.message,
                  actionLabel = if (event.undoHiddenPackage != null) undoLabel else null,
                  duration =
                    if (event.undoHiddenPackage != null) SnackbarDuration.Long
                    else SnackbarDuration.Short,
                )
              if (result == SnackbarResult.ActionPerformed && event.undoHiddenPackage != null) {
                viewModel.unhideApp(event.undoHiddenPackage)
              }
            }
        }
      }
    }
  }

  // Step 3 first-run Notification Access screen
  if (uiState.showNotificationOnboarding) {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      },
      modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
      NotificationOnboardingScreen(
        onOpenSettings = {
          val opened = NotificationAccessHelper.openNotificationAccessSettings(context)
          if (!opened) {
            viewModel.notifyMessage(context.getString(R.string.notif_settings_failed))
          }
        },
        onSkip = viewModel::dismissNotificationOnboarding,
        modifier = Modifier.padding(innerPadding),
      )
    }
    return
  }

  // Step 2 Hidden Apps screen (protected by BiometricPrompt + hashed PIN)
  if (uiState.hiddenAppsFlow !is HiddenAppsFlowState.Closed) {
    val canUseBiometrics =
      remember(context, uiState.hiddenAppsFlow) {
        BiometricAuthHelper.checkAvailability(context) == BiometricAvailability.AVAILABLE
      }

    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      },
      modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
      HiddenAppsScreen(
        flowState = uiState.hiddenAppsFlow,
        hiddenApps = uiState.hiddenApps,
        canUseBiometrics = canUseBiometrics,
        onBackOrClose = viewModel::handleHiddenScreenBack,
        onSubmitSetupPin = viewModel::submitSetupPin,
        onSubmitVerifyPin = viewModel::submitVerifyPin,
        onSubmitChangePin = viewModel::submitChangePin,
        onRequestBiometrics = viewModel::requestBiometricsAgain,
        onOpenChangePin = viewModel::openChangePin,
        onLaunchHiddenApp = { app -> launchApp(context, app) { viewModel.notifyMessage(it) } },
        onOpenHiddenAppMenu = viewModel::openHiddenAppMenu,
        onDismissHiddenAppMenu = viewModel::dismissHiddenAppMenu,
        onUnhideApp = viewModel::unhideAppFromHiddenScreen,
        onOpenHiddenAppInfo = { app ->
          viewModel.dismissHiddenAppMenu()
          openAppInfo(context, app.packageName) { viewModel.notifyMessage(it) }
        },
        modifier = Modifier.padding(innerPadding),
      )
    }
    return
  }

  // Step 4 Gesture Settings screen
  if (uiState.isGestureSettingsOpen) {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      },
      modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
      GestureSettingsScreen(
        gestureConfig = uiState.gestureConfig,
        isAccessibilityEnabled = uiState.isAccessibilityEnabled,
        onSelectGestureAction = viewModel::setGestureAction,
        onResetDefaults = viewModel::resetGestureDefaults,
        onOpenAccessibilitySettings = {
          val opened = AccessibilityAccessHelper.openAccessibilitySettings(context)
          if (!opened) {
            viewModel.notifyMessage(context.getString(R.string.accessibility_settings_failed))
          }
        },
        onBack = viewModel::closeGestureSettings,
        modifier = Modifier.padding(innerPadding),
      )
    }
    return
  }

  // Step 5 Theming & Appearance screen
  if (uiState.isThemeSettingsOpen) {
    Scaffold(
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      snackbarHost = {
        SnackbarHost(
          hostState = snackbarHostState,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
      },
      modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
      ThemeSettingsScreen(
        themeConfig = uiState.themeConfig,
        activeIconPackLabel = uiState.activeIconPackLabel,
        onSelectColorSource = viewModel::setThemeColorSource,
        onSelectThemeMode = viewModel::setThemeMode,
        onSelectFontChoice = viewModel::setFontChoice,
        onSelectIconShape = viewModel::setIconShape,
        onToggleHideSystemBars = viewModel::setHideSystemBars,
        onOpenIconPackPicker = {
          viewModel.closeThemeSettings()
          viewModel.openIconPackPicker()
        },
        onOpenWallpaperPicker = {
          viewModel.closeThemeSettings()
          viewModel.openWallpaperSheet()
        },
        onResetDefaults = viewModel::resetThemeDefaults,
        onBack = viewModel::closeThemeSettings,
        modifier = Modifier.padding(innerPadding),
      )
    }
    return
  }

  // Standard home-screen back behavior: exit widget edit -> collapse search -> close drawer -> return from News to Home.
  BackHandler(enabled = true) {
    when {
      uiState.editingWidgetId != null -> {
        viewModel.stopEditingWidget()
      }
      uiState.isDrawerOpen && uiState.searchQuery.isNotEmpty() -> {
        focusManager.clearFocus()
        viewModel.clearSearchQuery()
      }
      uiState.isDrawerOpen && uiState.isSearchExpanded -> {
        focusManager.clearFocus()
        viewModel.toggleDrawerSearch()
      }
      uiState.isDrawerOpen -> {
        viewModel.closeDrawer()
      }
      desktopPagerState.currentPage != PAGE_HOME -> {
        coroutineScope.launch { desktopPagerState.animateScrollToPage(PAGE_HOME) }
      }
    }
  }

  // Live drag offset for interactive upward swipe glassmorphism transition
  var liveSwipeUpProgress by remember { mutableFloatStateOf(0f) }
  var verticalDragDelta by remember { mutableFloatStateOf(0f) }

  val targetDrawerProgress =
    if (uiState.isDrawerOpen) 1f else liveSwipeUpProgress.coerceIn(0f, 1f)

  val animatedDrawerProgress by
    animateFloatAsState(
      targetValue = targetDrawerProgress,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "glassmorphism_drawer_progress",
    )

  val backdropBlurProgress =
    if (uiState.isSmartSearchOpen) 1f else animatedDrawerProgress

  val swipeUpOpensDrawer =
    uiState.gestureConfig.swipeUp == GestureAction.OPEN_DRAWER ||
      uiState.gestureConfig.swipeUp == GestureAction.OPEN_SEARCH

  Scaffold(
    containerColor = Color.Transparent,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
      )
    },
    modifier = modifier.fillMaxSize(),
  ) { innerPadding ->
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      val density = LocalDensity.current
      val containerHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
      val gestureThresholdPx = with(density) { 72.dp.toPx() }
      val blurRadius = (26f * backdropBlurProgress).dp

      // Base Home Screen Box (applies real RenderEffect blur filter behind Drawer & Smart Search)
      val blurModifier =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.5.dp) {
          Modifier.blur(
            radiusX = blurRadius,
            radiusY = blurRadius,
            edgeTreatment = BlurredEdgeTreatment.Unbounded,
          )
        } else {
          Modifier
        }

      val isHomeInteractive = !uiState.isDrawerOpen && !uiState.isSmartSearchOpen

      Box(
        modifier =
          Modifier.fillMaxSize()
            .then(blurModifier)
      ) {
        HomeWallpaperBackdrop(
          wallpaperConfig = uiState.wallpaperConfig,
          customWallpaperBitmap = uiState.customWallpaperBitmap,
        )

        // Step 10: Horizontal desktop pager — Page 0 = News Feed (left of Home), Page 1 = Home Screen
        HorizontalPager(
          state = desktopPagerState,
          userScrollEnabled = isHomeInteractive && uiState.editingWidgetId == null,
          beyondViewportPageCount = 1,
          modifier = Modifier.fillMaxSize().testTag("launcher_desktop_pager"),
        ) { page ->
          when (page) {
            PAGE_NEWS -> {
              NewsFeedPage(
                articles = uiState.newsArticles,
                sources = uiState.newsSources,
                personalization = uiState.newsPersonalization,
                selectedTopic = uiState.selectedNewsTopic,
                lastUpdatedEpochMs = uiState.newsLastUpdatedEpochMs,
                isRefreshing = uiState.isNewsRefreshing,
                onCompletePersonalization = viewModel::completeNewsPersonalization,
                onSelectTopic = viewModel::selectNewsTopic,
                onClearStrictKeywordFilter = { viewModel.setStrictNewsKeywordFilter(false) },
                onRefresh = { viewModel.refreshNewsFeeds(showSnackbar = true) },
                onOpenSourcesSheet = viewModel::openNewsSourcesSheet,
                onArticleClick = { article ->
                  openArticleUrl(context, article.link) { viewModel.notifyMessage(it) }
                },
                onArticleShare = { article -> shareArticle(context, article) },
                onReturnHome = {
                  coroutineScope.launch { desktopPagerState.animateScrollToPage(PAGE_HOME) }
                },
              )
            }
            else -> {
              val isHomePageActive = isHomeInteractive && desktopPagerState.currentPage == PAGE_HOME
              Box(
                modifier =
                  Modifier.fillMaxSize()
                    .pointerInput(isHomePageActive) {
                      if (isHomePageActive) {
                        detectTapGestures(
                          onDoubleTap = {
                            val availability = BiometricAuthHelper.checkAvailability(context)
                            viewModel.executeHomeGesture(HomeGesture.DOUBLE_TAP, availability)
                          },
                          onLongPress = { viewModel.openWallpaperSheet() },
                        )
                      }
                    }
                    .pointerInput(isHomePageActive, swipeUpOpensDrawer) {
                      if (isHomePageActive) {
                        detectVerticalDragGestures(
                          onDragStart = {
                            verticalDragDelta = 0f
                            liveSwipeUpProgress = 0f
                          },
                          onVerticalDrag = { _, dragAmount ->
                            verticalDragDelta += dragAmount
                            if (swipeUpOpensDrawer && verticalDragDelta < 0f) {
                              liveSwipeUpProgress =
                                ((-verticalDragDelta) / (containerHeightPx * 0.45f)).coerceIn(0f, 1f)
                            } else {
                              liveSwipeUpProgress = 0f
                            }
                          },
                          onDragEnd = {
                            val delta = verticalDragDelta
                            verticalDragDelta = 0f
                            liveSwipeUpProgress = 0f
                            val availability = BiometricAuthHelper.checkAvailability(context)
                            when {
                              delta <= -gestureThresholdPx -> {
                                viewModel.executeHomeGesture(HomeGesture.SWIPE_UP, availability)
                              }
                              delta >= gestureThresholdPx -> {
                                viewModel.executeHomeGesture(HomeGesture.SWIPE_DOWN, availability)
                              }
                            }
                          },
                          onDragCancel = {
                            verticalDragDelta = 0f
                            liveSwipeUpProgress = 0f
                          },
                        )
                      }
                    }
              ) {
                HomeScreenContent(
                  homeApps = uiState.dockApps,
                  widgetStyle = uiState.homeWidgetStyle,
                  smartWidgetsConfig = uiState.smartWidgetsConfig,
                  iconGroupApps = uiState.iconGroupApps,
                  calendarEvents = uiState.calendarEvents,
                  hasCalendarPermission = uiState.hasCalendarPermission,
                  isCalendarLoading = uiState.isCalendarLoading,
                  mediaPlaybackState = uiState.mediaPlaybackState,
                  isNotificationAccessGranted = uiState.isNotificationAccessGranted,
                  wallpaperConfig = uiState.wallpaperConfig,
                  hasCustomWallpaperBitmap = uiState.customWallpaperBitmap != null,
                  weather = uiState.weatherSnapshot,
                  temperatureUnit = uiState.temperatureUnit,
                  isWeatherLoading = uiState.isWeatherLoading,
                  hasLocationPermission = uiState.hasLocationPermission,
                  hostedWidgets = uiState.hostedWidgets,
                  editingWidgetId = uiState.editingWidgetId,
                  widgetHostManager = viewModel.widgetHostManager,
                  onAppClick = { app -> launchApp(context, app) { viewModel.notifyMessage(it) } },
                  onAppLongClick = viewModel::openAppMenu,
                  onWidgetLongPress = viewModel::openWidgetCustomizeSheet,
                  onStartEditWidget = viewModel::startEditingWidget,
                  onStopEditWidget = viewModel::stopEditingWidget,
                  onResizeWidget = viewModel::resizeHostedWidget,
                  onMoveWidget = viewModel::moveHostedWidget,
                  onReconfigureWidget = viewModel::requestReconfigureWidget,
                  onRemoveWidget = viewModel::removeHostedWidget,
                  onRefreshWeather = viewModel::refreshWeather,
                  onRequestLocationWeather = viewModel::fetchWeatherFromLocation,
                  onLocationPermissionResult = viewModel::onLocationPermissionResult,
                  onOpenCityDialog = viewModel::openWeatherCityDialog,
                  onToggleTemperatureUnit = viewModel::toggleTemperatureUnit,
                  onMusicPlayPause = viewModel::musicTogglePlayPause,
                  onMusicPrevious = viewModel::musicSkipPrevious,
                  onMusicNext = viewModel::musicSkipNext,
                  onOpenMusicPlayer = {
                    val opened =
                      MediaControlManager.openMediaPlayerApp(
                        context = context,
                        preferredPackage = uiState.mediaPlaybackState.packageName,
                      )
                    if (!opened) {
                      viewModel.notifyMessage(context.getString(R.string.snackbar_music_app_unavailable))
                    }
                  },
                  onEnableLiveMusicInfo = {
                    val opened = NotificationAccessHelper.openNotificationAccessSettings(context)
                    if (!opened) {
                      viewModel.notifyMessage(context.getString(R.string.notif_settings_failed))
                    }
                  },
                  onCalendarPermissionResult = viewModel::onCalendarPermissionResult,
                  onRefreshCalendar = viewModel::refreshCalendarEvents,
                  onOpenCalendarEvent = { event ->
                    val opened = CalendarRepository.openCalendarEvent(context, event.id)
                    if (!opened) {
                      viewModel.notifyMessage(context.getString(R.string.snackbar_calendar_unavailable))
                    }
                  },
                  onOpenCalendarApp = {
                    val opened = CalendarRepository.openCalendarApp(context)
                    if (!opened) {
                      viewModel.notifyMessage(context.getString(R.string.snackbar_calendar_unavailable))
                    }
                  },
                  onEditNote = viewModel::openNoteEditor,
                  onEditIconGroup = viewModel::openIconGroupPicker,
                  onSearchBarClick = viewModel::openSmartSearch,
                  onSwipeUpHintClick = { viewModel.openDrawer(focusSearch = false) },
                  onOpenNewsPage = {
                    coroutineScope.launch { desktopPagerState.animateScrollToPage(PAGE_NEWS) }
                  },
                )
              }
            }
          }
        }
      }

      // Translucent Glassmorphism App Drawer Box (interactive slide & alpha driven by swipe-up)
      if (!uiState.isSmartSearchOpen && (uiState.isDrawerOpen || animatedDrawerProgress > 0.01f)) {
        Box(
          modifier =
            Modifier.fillMaxSize().graphicsLayer {
              translationY = containerHeightPx * (1f - animatedDrawerProgress) * 0.65f
              alpha = animatedDrawerProgress.coerceIn(0f, 1f)
            }
        ) {
          GlassyAppDrawer(
            categoryTitle = uiState.selectedCategoryTitle,
            apps = uiState.visibleApps,
            isLoading = uiState.isLoading,
            categories = uiState.categories,
            selectedCategoryId = uiState.selectedCategoryId,
            searchQuery = uiState.searchQuery,
            isSearchExpanded = uiState.isSearchExpanded,
            isNotificationAccessGranted = uiState.isNotificationAccessGranted,
            gridState = gridState,
            onToggleSearch = viewModel::openSmartSearch,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onClearSearch = {
              focusManager.clearFocus()
              viewModel.clearSearchQuery()
            },
            onOpenPlayStore = { openPlayStore(context) { viewModel.notifyMessage(it) } },
            onOpenTheming = {
              focusManager.clearFocus()
              viewModel.openThemeSettings()
            },
            onOpenIconPackPicker = {
              focusManager.clearFocus()
              viewModel.openIconPackPicker()
            },
            onOpenWallpaperPicker = {
              focusManager.clearFocus()
              viewModel.closeDrawer()
              viewModel.openWallpaperSheet()
            },
            onOpenGestures = {
              focusManager.clearFocus()
              viewModel.openGestureSettings()
            },
            onOpenHiddenApps = {
              focusManager.clearFocus()
              val availability = BiometricAuthHelper.checkAvailability(context)
              viewModel.openHiddenApps(availability)
            },
            onCreateCategoryClick = { viewModel.openCreateCategoryDialog(assignToApp = null) },
            onOpenNotificationSettings = {
              val opened = NotificationAccessHelper.openNotificationAccessSettings(context)
              if (!opened) {
                viewModel.notifyMessage(context.getString(R.string.notif_settings_failed))
              }
            },
            onOpenWidgetPicker = {
              focusManager.clearFocus()
              viewModel.openWidgetPicker()
            },
            onCustomizeWidgetClick = {
              viewModel.closeDrawer()
              viewModel.openWidgetCustomizeSheet()
            },
            onCategorySelect = viewModel::selectCategory,
            onCustomCategoryLongPress = viewModel::openManageCustomCategory,
            onAppClick = { app -> launchApp(context, app) { viewModel.notifyMessage(it) } },
            onAppLongClick = viewModel::openAppMenu,
            onDismissDrawer = {
              focusManager.clearFocus()
              viewModel.closeDrawer()
            },
          )
        }
      }

      // Step 6 One-Handed Glassmorphic Smart Search Overlay
      if (uiState.isSmartSearchOpen) {
        SmartSearchOverlay(
          state = uiState.smartSearch,
          onQueryChange = viewModel::updateSmartSearchQuery,
          onClearQuery = viewModel::clearSmartSearchQuery,
          onClose = viewModel::closeSmartSearch,
          onAppClick = { app ->
            viewModel.recordRecentSearchQuery(uiState.smartSearch.query)
            viewModel.closeSmartSearch()
            launchApp(context, app) { viewModel.notifyMessage(it) }
          },
          onAppLongClick = viewModel::openAppMenu,
          onCopyCalculatorResult = { calc ->
            viewModel.recordRecentSearchQuery(calc.expression)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("Calculator Result", calc.formattedResult))
            viewModel.notifyMessage(
              context.getString(R.string.smart_search_copied_result, calc.formattedResult)
            )
          },
          onQuickActionClick = { action ->
            val currentQuery = uiState.smartSearch.query
            val ok = QuickActionMatcher.execute(context, action)
            if (ok) {
              if (currentQuery.isNotBlank()) {
                viewModel.recordRecentSearchQuery(currentQuery)
              }
              viewModel.closeSmartSearch()
            } else {
              viewModel.notifyMessage(
                context.getString(R.string.snackbar_quick_action_failed, action.title)
              )
            }
          },
          onCallContact = { contact ->
            val currentQuery = uiState.smartSearch.query
            val ok = QuickActionMatcher.openDialer(context, contact.phoneNumber)
            if (ok) {
              viewModel.recordRecentSearchQuery(currentQuery)
              viewModel.closeSmartSearch()
            } else {
              viewModel.notifyMessage(
                context.getString(R.string.snackbar_quick_action_failed, contact.displayName)
              )
            }
          },
          onMessageContact = { contact ->
            val currentQuery = uiState.smartSearch.query
            val ok = QuickActionMatcher.openSms(context, contact.phoneNumber)
            if (ok) {
              viewModel.recordRecentSearchQuery(currentQuery)
              viewModel.closeSmartSearch()
            } else {
              viewModel.notifyMessage(
                context.getString(R.string.snackbar_quick_action_failed, contact.displayName)
              )
            }
          },
          onContactsPermissionResult = viewModel::onContactsPermissionResult,
          onOpenAppSettingsForContacts = {
            openAppInfo(context, context.packageName) { viewModel.notifyMessage(it) }
          },
          onWebSearchClick = { query ->
            viewModel.recordRecentSearchQuery(query)
            val ok = openDuckDuckGoSearch(context, query)
            if (ok) {
              viewModel.closeSmartSearch()
            } else {
              viewModel.notifyMessage(context.getString(R.string.snackbar_browser_unavailable))
            }
          },
          onPickSuggestion = viewModel::updateSmartSearchQuery,
          onSelectRecentQuery = viewModel::updateSmartSearchQuery,
          onRemoveRecentQuery = viewModel::removeRecentSearchQuery,
          onClearRecentQueries = viewModel::clearRecentSearchQueries,
        )
      }
    }
  }

  // Prompt to enable AccessibilityService when a user triggers a system gesture without it
  uiState.accessibilityPromptAction?.let { action ->
    AccessibilityEnableSheet(
      requestedAction = action,
      onOpenSettings = {
        viewModel.dismissAccessibilityPrompt()
        val opened = AccessibilityAccessHelper.openAccessibilitySettings(context)
        if (!opened) {
          viewModel.notifyMessage(context.getString(R.string.accessibility_settings_failed))
        }
      },
      onDismiss = viewModel::dismissAccessibilityPrompt,
    )
  }

  // Unified Smart Widgets Hub sheet (Clock styles, Stack reorder/toggle, Pinned widgets, AppWidgets)
  if (uiState.showWidgetCustomizeSheet) {
    WidgetHubSheet(
      selectedClockStyle = uiState.homeWidgetStyle,
      smartWidgetsConfig = uiState.smartWidgetsConfig,
      onSelectClockStyle = viewModel::selectHomeWidgetStyle,
      onToggleStackWidget = viewModel::toggleStackWidget,
      onMoveStackWidget = viewModel::moveStackWidget,
      onTogglePinnedWidget = viewModel::togglePinnedSmartWidget,
      onAddAndroidWidget = viewModel::openWidgetPicker,
      onDismiss = viewModel::dismissWidgetCustomizeSheet,
    )
  }

  // Quick Note Widget editor dialog
  if (uiState.showNoteEditorDialog) {
    NoteEditorDialog(
      initialNote = uiState.smartWidgetsConfig.quickNote,
      onDismiss = viewModel::dismissNoteEditor,
      onSave = viewModel::saveQuickNote,
    )
  }

  // Icon Group Widget app picker dialog
  if (uiState.showIconGroupPickerDialog) {
    IconGroupPickerDialog(
      allApps = uiState.allVisibleApps,
      selectedPackages = uiState.iconGroupApps.map { it.packageName },
      maxApps = LauncherPreferencesRepository.MAX_ICON_GROUP_APPS,
      onDismiss = viewModel::dismissIconGroupPicker,
      onSave = viewModel::saveIconGroupPackages,
    )
  }

  // Step 8 Real Android AppWidget Picker sheet
  if (uiState.showWidgetPickerSheet) {
    WidgetPickerSheet(
      widgetGroups = uiState.availableWidgetGroups,
      isLoading = uiState.isWidgetPickerLoading,
      onSelectWidget = viewModel::onPickAvailableWidget,
      onDismiss = viewModel::dismissWidgetPicker,
    )
  }

  // Wallpaper Changer sheet (presets + zero-permission Photo Picker + dim + system apply)
  if (uiState.showWallpaperSheet) {
    WallpaperPickerSheet(
      wallpaperConfig = uiState.wallpaperConfig,
      customBitmap = uiState.customWallpaperBitmap,
      onSelectPreset = viewModel::selectWallpaperPreset,
      onPhotoPicked = viewModel::importCustomWallpaper,
      onDimPercentChange = viewModel::setWallpaperDimPercent,
      onApplyToSystemWallpaper = viewModel::applyCustomWallpaperToSystem,
      onOpenSystemWallpaperPicker = viewModel::openSystemWallpaperPicker,
      onDismiss = viewModel::dismissWallpaperSheet,
    )
  }

  // Step 9 Third-Party Icon Pack Picker sheet
  if (uiState.showIconPackSheet) {
    IconPackPickerSheet(
      installedPacks = uiState.installedIconPacks,
      selectedPackPackage = uiState.selectedIconPackPackage,
      isLoading = uiState.isIconPacksLoading,
      onSelectPack = viewModel::selectIconPack,
      onRefresh = viewModel::refreshInstalledIconPacks,
      onBrowsePlayStore = {
        openPlayStoreIconPacks(context) { viewModel.notifyMessage(it) }
      },
      onDismiss = viewModel::dismissIconPackPicker,
    )
  }

  // Step 10 News Feed Personalize & Channels sheet
  if (uiState.showNewsSourcesSheet) {
    NewsSourcesSheet(
      sources = uiState.newsSources,
      personalization = uiState.newsPersonalization,
      addErrorMessage = uiState.newsAddSourceError,
      onToggleContext = viewModel::toggleNewsContext,
      onAddFocusKeyword = viewModel::addNewsFocusKeyword,
      onRemoveFocusKeyword = viewModel::removeNewsFocusKeyword,
      onSetStrictKeywordFilter = viewModel::setStrictNewsKeywordFilter,
      onToggleSource = viewModel::toggleNewsSource,
      onAddCustomSource = viewModel::addCustomNewsSource,
      onRemoveCustomSource = viewModel::removeCustomNewsSource,
      onClearError = viewModel::clearNewsAddSourceError,
      onDismiss = viewModel::dismissNewsSourcesSheet,
    )
  }

  // Manual Weather City chooser dialog
  if (uiState.showWeatherCityDialog) {
    WeatherCityDialog(
      initialCity = uiState.weatherLocationCity,
      onDismiss = viewModel::dismissWeatherCityDialog,
      onConfirm = viewModel::setWeatherCity,
    )
  }

  // Long-press app action menu
  uiState.menuApp?.let { app ->
    AppActionSheet(
      app = app,
      onDismiss = viewModel::dismissAppMenu,
      onPinToDock = { viewModel.toggleDockPin(app) },
      onMoveToCategory = { viewModel.openMoveToCategory(app) },
      onHideApp = { viewModel.hideApp(app) },
      onOpenAppInfo = {
        viewModel.dismissAppMenu()
        openAppInfo(context, app.packageName) { viewModel.notifyMessage(it) }
      },
    )
  }

  // Move to category picker sheet
  uiState.moveCategoryApp?.let { app ->
    MoveToCategorySheet(
      app = app,
      autoCategoryLabel = builtInLabels[app.autoCategory] ?: app.autoCategory.name,
      customCategories = uiState.customCategories,
      builtInLabels = builtInLabels,
      onDismiss = viewModel::dismissMoveToCategory,
      onSelectCategory = { catId, catName -> viewModel.assignCategory(app, catId, catName) },
      onResetToAuto = { viewModel.resetCategoryToAuto(app) },
      onCreateNewCategory = { viewModel.openCreateCategoryDialog(assignToApp = app) },
    )
  }

  // Create custom category dialog
  uiState.createCategoryState?.let { dialogState ->
    CategoryNameDialog(
      title = stringResource(R.string.create_category_title),
      confirmLabel = stringResource(R.string.create_category_confirm),
      initialName = "",
      errorMessage = dialogState.errorMessage,
      onDismiss = viewModel::dismissCreateCategoryDialog,
      onConfirm = viewModel::createCustomCategory,
    )
  }

  // Long-press custom category management sheet
  uiState.manageCustomCategory?.let { category ->
    ManageCustomCategorySheet(
      category = category,
      onDismiss = viewModel::dismissManageCustomCategory,
      onRename = { viewModel.openRenameCustomCategory(category) },
      onDelete = { viewModel.requestDeleteCustomCategory(category) },
    )
  }

  // Rename custom category dialog
  uiState.renameCategoryState?.let { renameState ->
    CategoryNameDialog(
      title = stringResource(R.string.rename_category_title),
      confirmLabel = stringResource(R.string.rename_category_confirm),
      initialName = renameState.category.name,
      errorMessage = renameState.errorMessage,
      onDismiss = viewModel::dismissRenameCustomCategory,
      onConfirm = viewModel::renameCustomCategory,
    )
  }

  // Delete custom category confirmation dialog
  uiState.deleteConfirmCategory?.let { category ->
    DeleteCategoryConfirmDialog(
      category = category,
      onDismiss = viewModel::dismissDeleteConfirm,
      onConfirm = viewModel::confirmDeleteCustomCategory,
    )
  }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? =
  when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
  }

private fun launchApp(
  context: Context,
  app: AppItem,
  onError: (String) -> Unit,
) {
  val launchIntent =
    context.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  if (launchIntent == null) {
    onError(context.getString(R.string.snackbar_launch_failed, app.label))
    return
  }

  runCatching { context.startActivity(launchIntent) }
    .onFailure { onError(context.getString(R.string.snackbar_launch_failed, app.label)) }
}

private fun openPlayStore(
  context: Context,
  onError: (String) -> Unit,
) {
  val directIntent =
    context.packageManager.getLaunchIntentForPackage("com.android.vending")?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  if (directIntent != null && runCatching { context.startActivity(directIntent) }.isSuccess) {
    return
  }

  val marketIntent =
    Intent(Intent.ACTION_VIEW, Uri.parse("market://")).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(marketIntent) }
    .onFailure { onError(context.getString(R.string.snackbar_play_store_unavailable)) }
}

private fun openPlayStoreIconPacks(
  context: Context,
  onError: (String) -> Unit,
) {
  val marketSearchIntent =
    Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon+pack&c=apps")).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  if (runCatching { context.startActivity(marketSearchIntent) }.isSuccess) {
    return
  }

  val webSearchIntent =
    Intent(
      Intent.ACTION_VIEW,
      Uri.parse("https://play.google.com/store/search?q=icon+pack&c=apps"),
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
  runCatching { context.startActivity(webSearchIntent) }
    .onFailure { onError(context.getString(R.string.snackbar_play_store_unavailable)) }
}

private fun openAppInfo(
  context: Context,
  packageName: String,
  onError: (String) -> Unit,
) {
  val intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", packageName, null)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  runCatching { context.startActivity(intent) }
    .onFailure { onError(context.getString(R.string.snackbar_app_info_failed)) }
}

private fun openDuckDuckGoSearch(
  context: Context,
  query: String,
): Boolean {
  val url = "https://duckduckgo.com/?q=${Uri.encode(query.trim())}"
  val intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  return runCatching {
    context.startActivity(intent)
    true
  }.getOrElse { false }
}

private fun openArticleUrl(
  context: Context,
  url: String,
  onError: (String) -> Unit,
) {
  if (!RssFeedParser.isValidHttpUrl(url)) {
    onError(context.getString(R.string.snackbar_news_open_failed))
    return
  }
  val intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(url.trim())).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(intent) }
    .onFailure { onError(context.getString(R.string.snackbar_news_open_failed)) }
}

private fun shareArticle(
  context: Context,
  article: NewsArticle,
) {
  val shareText = "${article.title}\n${article.link}"
  val shareIntent =
    Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_SUBJECT, article.title)
      putExtra(Intent.EXTRA_TEXT, shareText)
    }
  val chooser =
    Intent.createChooser(shareIntent, context.getString(R.string.news_action_share)).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
  runCatching { context.startActivity(chooser) }
}
