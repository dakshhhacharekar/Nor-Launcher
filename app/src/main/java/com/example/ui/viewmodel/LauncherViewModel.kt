package com.example.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.R
import com.example.data.apps.AppRepository
import com.example.data.icons.IconPackRepository
import com.example.data.icons.LoadedIconPack
import com.example.data.model.AppItem
import com.example.data.model.AvailableWidgetEntry
import com.example.data.model.AvailableWidgetGroup
import com.example.data.model.BuiltInCategory
import com.example.data.model.CalendarEventItem
import com.example.data.model.CategoryChipItem
import com.example.data.model.CategoryMutationResult
import com.example.data.model.ContactMatch
import com.example.data.model.CustomCategory
import com.example.data.model.DefaultNewsSources
import com.example.data.model.DockPinResult
import com.example.data.model.FontChoice
import com.example.data.model.GestureAction
import com.example.data.model.GestureConfig
import com.example.data.model.HomeGesture
import com.example.data.model.HomeWidgetStyle
import com.example.data.model.HostedWidgetRecord
import com.example.data.model.IconShapeOption
import com.example.data.model.InstalledApp
import com.example.data.model.InstalledIconPack
import com.example.data.model.LauncherPreferencesState
import com.example.data.model.MediaPlaybackState
import com.example.data.model.NewsArticle
import com.example.data.model.NewsFeedCache
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsPersonalization
import com.example.data.model.NewsSourceMutationResult
import com.example.data.model.NewsTopic
import com.example.data.model.PackageEvent
import com.example.data.model.PinSecurityState
import com.example.data.model.SmartSearchState
import com.example.data.model.SmartWidgetType
import com.example.data.model.SmartWidgetsConfig
import com.example.data.model.TemperatureUnit
import com.example.data.model.ThemeColorSource
import com.example.data.model.ThemeConfig
import com.example.data.model.ThemeModeOption
import com.example.data.model.WallpaperConfig
import com.example.data.model.WallpaperPreset
import com.example.data.model.WeatherLocation
import com.example.data.model.WeatherSnapshot
import com.example.data.news.NewsFeedRepository
import com.example.data.news.NewsSyncScheduler
import com.example.data.search.ContactSearchRepository
import com.example.data.search.MathExpressionEvaluator
import com.example.data.search.QuickActionMatcher
import com.example.data.search.WebSuggestionRepository
import com.example.data.security.BiometricAvailability
import com.example.data.security.PinHasher
import com.example.data.store.LauncherPreferencesRepository
import com.example.data.wallpaper.WallpaperRepository
import com.example.data.weather.WeatherRepository
import com.example.data.widgets.CalendarRepository
import com.example.data.widgets.LauncherWidgetHostManager
import com.example.data.widgets.MediaControlManager
import com.example.service.AccessibilityAccessHelper
import com.example.service.GlobalActionResult
import com.example.service.NorGestureAccessibilityService
import com.example.service.NotificationAccessHelper
import com.example.service.NotificationDotStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HiddenAppsFlowState {
  data object Closed : HiddenAppsFlowState

  data class SetupPin(
    val firstEntry: String? = null,
    val errorMessage: String? = null,
    val isBusy: Boolean = false,
  ) : HiddenAppsFlowState

  data class VerifyPin(
    val biometricNotice: String? = null,
    val errorMessage: String? = null,
    val lockoutSecondsRemaining: Int = 0,
    val isBusy: Boolean = false,
  ) : HiddenAppsFlowState

  data class Unlocked(
    val menuHiddenApp: AppItem? = null,
  ) : HiddenAppsFlowState

  data class ChangePin(
    val firstEntry: String? = null,
    val errorMessage: String? = null,
    val isBusy: Boolean = false,
  ) : HiddenAppsFlowState
}

data class LauncherUiState(
  val isLoading: Boolean = true,
  val isDrawerOpen: Boolean = false,
  val isSearchExpanded: Boolean = false,
  val isSmartSearchOpen: Boolean = false,
  val isGestureSettingsOpen: Boolean = false,
  val isThemeSettingsOpen: Boolean = false,
  val showNotificationOnboarding: Boolean = false,
  val isNotificationAccessGranted: Boolean = false,
  val isAccessibilityEnabled: Boolean = false,
  val homeWidgetStyle: HomeWidgetStyle = HomeWidgetStyle.MINIMAL_CLOCK,
  val smartWidgetsConfig: SmartWidgetsConfig = SmartWidgetsConfig(),
  val iconGroupApps: List<AppItem> = emptyList(),
  val allVisibleApps: List<AppItem> = emptyList(),
  val calendarEvents: List<CalendarEventItem> = emptyList(),
  val hasCalendarPermission: Boolean = false,
  val isCalendarLoading: Boolean = false,
  val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState(),
  val gestureConfig: GestureConfig = GestureConfig(),
  val themeConfig: ThemeConfig = ThemeConfig(),
  val wallpaperConfig: WallpaperConfig = WallpaperConfig(),
  val customWallpaperBitmap: ImageBitmap? = null,
  val weatherSnapshot: WeatherSnapshot? = null,
  val weatherLocationCity: String = "",
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val isWeatherLoading: Boolean = false,
  val hasLocationPermission: Boolean = false,
  val hostedWidgets: List<HostedWidgetRecord> = emptyList(),
  val editingWidgetId: Int? = null,
  val showWidgetPickerSheet: Boolean = false,
  val availableWidgetGroups: List<AvailableWidgetGroup> = emptyList(),
  val isWidgetPickerLoading: Boolean = false,
  val smartSearch: SmartSearchState = SmartSearchState(),
  val selectedIconPackPackage: String? = null,
  val activeIconPackLabel: String? = null,
  val showIconPackSheet: Boolean = false,
  val installedIconPacks: List<InstalledIconPack> = emptyList(),
  val isIconPacksLoading: Boolean = false,
  val newsArticles: List<NewsArticle> = emptyList(),
  val newsSources: List<NewsFeedSource> = DefaultNewsSources.builtIn,
  val newsPersonalization: NewsPersonalization = NewsPersonalization(),
  val selectedNewsTopic: NewsTopic = NewsTopic.ALL,
  val newsLastUpdatedEpochMs: Long = 0L,
  val isNewsRefreshing: Boolean = false,
  val showNewsSourcesSheet: Boolean = false,
  val newsAddSourceError: String? = null,
  val showWidgetCustomizeSheet: Boolean = false,
  val showNoteEditorDialog: Boolean = false,
  val showIconGroupPickerDialog: Boolean = false,
  val showWallpaperSheet: Boolean = false,
  val showWeatherCityDialog: Boolean = false,
  val accessibilityPromptAction: GestureAction? = null,
  val visibleApps: List<AppItem> = emptyList(),
  val hiddenApps: List<AppItem> = emptyList(),
  val dockApps: List<AppItem> = emptyList(),
  val categories: List<CategoryChipItem> = emptyList(),
  val customCategories: List<CustomCategory> = emptyList(),
  val selectedCategoryId: String = "",
  val selectedCategoryTitle: String = "",
  val searchQuery: String = "",
  val totalVisibleAppCount: Int = 0,
  val isPinConfigured: Boolean = false,
  val hiddenAppsFlow: HiddenAppsFlowState = HiddenAppsFlowState.Closed,
  val menuApp: AppItem? = null,
  val moveCategoryApp: AppItem? = null,
  val createCategoryState: CreateCategoryDialogState? = null,
  val manageCustomCategory: CustomCategory? = null,
  val renameCategoryState: RenameCategoryDialogState? = null,
  val deleteConfirmCategory: CustomCategory? = null,
)

data class CreateCategoryDialogState(
  val assignToPackage: String? = null,
  val assignToAppLabel: String? = null,
  val errorMessage: String? = null,
)

data class RenameCategoryDialogState(
  val category: CustomCategory,
  val errorMessage: String? = null,
)

sealed interface LauncherEvent {
  data object ScrollToTop : LauncherEvent
  data object ReturnToHomePage : LauncherEvent
  data object TriggerBiometricPrompt : LauncherEvent
  data class LaunchWidgetBindPermission(
    val appWidgetId: Int,
    val providerInfo: AppWidgetProviderInfo,
  ) : LauncherEvent
  data class LaunchWidgetConfigure(
    val appWidgetId: Int,
    val isReconfigure: Boolean,
  ) : LauncherEvent
  data class ShowSnackbar(
    val message: String,
    val undoHiddenPackage: String? = null,
  ) : LauncherEvent
}

private data class PendingWidgetAddition(
  val appWidgetId: Int,
  val entry: AvailableWidgetEntry,
)

private data class ModalState(
  val menuPackage: String? = null,
  val moveCategoryPackage: String? = null,
  val createCategoryState: CreateCategoryDialogState? = null,
  val manageCustomCategory: CustomCategory? = null,
  val renameCategoryState: RenameCategoryDialogState? = null,
  val deleteConfirmCategory: CustomCategory? = null,
  val hiddenMenuPackage: String? = null,
  val showWidgetCustomizeSheet: Boolean = false,
  val showWidgetPickerSheet: Boolean = false,
  val showNoteEditorDialog: Boolean = false,
  val showIconGroupPickerDialog: Boolean = false,
  val showWallpaperSheet: Boolean = false,
  val showWeatherCityDialog: Boolean = false,
  val showIconPackSheet: Boolean = false,
  val showNewsSourcesSheet: Boolean = false,
  val accessibilityPromptAction: GestureAction? = null,
)

private data class DrawerNavigationState(
  val isDrawerOpen: Boolean = false,
  val isSearchExpanded: Boolean = false,
  val isSmartSearchOpen: Boolean = false,
  val isGestureSettingsOpen: Boolean = false,
  val isThemeSettingsOpen: Boolean = false,
  val isNotificationAccessGranted: Boolean = false,
  val isAccessibilityEnabled: Boolean = false,
)

private data class AsyncSearchState(
  val query: String = "",
  val contacts: List<ContactMatch> = emptyList(),
  val hasContactsPermission: Boolean = false,
  val contactsPermissionDeniedOnce: Boolean = false,
  val webSuggestions: List<String> = emptyList(),
  val isLoadingSuggestions: Boolean = false,
)

private data class HomeEnvState(
  val customWallpaperBitmap: ImageBitmap? = null,
  val isWeatherLoading: Boolean = false,
  val hasLocationPermission: Boolean = false,
  val calendarEvents: List<CalendarEventItem> = emptyList(),
  val hasCalendarPermission: Boolean = false,
  val isCalendarLoading: Boolean = false,
  val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState(),
  val editingWidgetId: Int? = null,
  val availableWidgetGroups: List<AvailableWidgetGroup> = emptyList(),
  val isWidgetPickerLoading: Boolean = false,
  val activeIconPackLabel: String? = null,
  val installedIconPacks: List<InstalledIconPack> = emptyList(),
  val isIconPacksLoading: Boolean = false,
  val selectedNewsTopic: NewsTopic = NewsTopic.ALL,
  val isNewsRefreshing: Boolean = false,
  val newsAddSourceError: String? = null,
)

class LauncherViewModel(
  application: Application,
  private val savedStateHandle: SavedStateHandle,
  private val appRepository: AppRepository,
  private val iconPackRepository: IconPackRepository,
  private val preferencesRepository: LauncherPreferencesRepository,
  private val contactSearchRepository: ContactSearchRepository,
  private val webSuggestionRepository: WebSuggestionRepository,
  private val weatherRepository: WeatherRepository,
  private val wallpaperRepository: WallpaperRepository,
  private val newsFeedRepository: NewsFeedRepository,
  private val calendarRepository: CalendarRepository,
  private val mediaControlManager: MediaControlManager,
  val widgetHostManager: LauncherWidgetHostManager,
) : AndroidViewModel(application) {

  private val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
  private val isLoading = MutableStateFlow(true)
  private val modalState = MutableStateFlow(ModalState())
  private val hiddenAppsFlow = MutableStateFlow<HiddenAppsFlowState>(HiddenAppsFlowState.Closed)
  private val latestPinSecurity = MutableStateFlow(PinSecurityState())
  private val latestGestureConfig = MutableStateFlow(GestureConfig())
  private val latestWeatherLocation = MutableStateFlow<WeatherLocation?>(null)
  private val latestHostedWidgets = MutableStateFlow<List<HostedWidgetRecord>>(emptyList())
  private val latestNewsSources = MutableStateFlow<List<NewsFeedSource>>(DefaultNewsSources.builtIn)
  private val latestNewsPersonalization = MutableStateFlow(NewsPersonalization())
  private var pendingWidgetAddition: PendingWidgetAddition? = null
  @Volatile private var activeLoadedIconPack: LoadedIconPack? = null

  private val isNotificationAccessGranted =
    MutableStateFlow(NotificationAccessHelper.isAccessGranted(application.applicationContext))
  private val isAccessibilityEnabledSetting =
    MutableStateFlow(AccessibilityAccessHelper.isServiceEnabled(application.applicationContext))
  private val isSearchExpanded = MutableStateFlow(false)

  private val asyncSearchState =
    MutableStateFlow(
      AsyncSearchState(hasContactsPermission = contactSearchRepository.hasContactsPermission())
    )

  private val homeEnvState =
    MutableStateFlow(
      HomeEnvState(
        hasLocationPermission = weatherRepository.hasLocationPermission(),
        hasCalendarPermission = calendarRepository.hasCalendarPermission(),
      )
    )

  private var lockoutTickerJob: Job? = null
  private var contactSearchJob: Job? = null
  private var webSuggestionJob: Job? = null
  private var weatherJob: Job? = null
  private var calendarJob: Job? = null
  private var newsRefreshJob: Job? = null

  private val selectedCategoryId: StateFlow<String> =
    savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORY, "")

  private val searchQuery: StateFlow<String> =
    savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

  private val isDrawerOpen: StateFlow<Boolean> =
    savedStateHandle.getStateFlow(KEY_DRAWER_OPEN, false)

  private val isSmartSearchOpen: StateFlow<Boolean> =
    savedStateHandle.getStateFlow(KEY_SMART_SEARCH_OPEN, false)

  private val smartSearchQuery: StateFlow<String> =
    savedStateHandle.getStateFlow(KEY_SMART_SEARCH_QUERY, "")

  private val isGestureSettingsOpen: StateFlow<Boolean> =
    savedStateHandle.getStateFlow(KEY_GESTURE_SETTINGS_OPEN, false)

  private val isThemeSettingsOpen: StateFlow<Boolean> =
    savedStateHandle.getStateFlow(KEY_THEME_SETTINGS_OPEN, false)

  private val _events = MutableSharedFlow<LauncherEvent>(extraBufferCapacity = 8)
  val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

  private val baseInputs =
    combine(
      installedApps,
      preferencesRepository.preferencesFlow,
      NotificationDotStore.countsByPackage,
      selectedCategoryId,
      searchQuery,
    ) { apps, prefs, notifCounts, selectedCat, query ->
      latestPinSecurity.value = prefs.pinSecurity
      latestGestureConfig.value = prefs.gestureConfig
      latestWeatherLocation.value = prefs.weatherLocation
      latestHostedWidgets.value = prefs.hostedWidgets
      latestNewsSources.value = prefs.newsSources
      latestNewsPersonalization.value = prefs.newsPersonalization
      BaseSnapshot(apps, prefs, notifCounts, selectedCat, query)
    }

  private val accessibilityEnabledFlow =
    combine(isAccessibilityEnabledSetting, NorGestureAccessibilityService.isConnected) {
      settingEnabled,
      connected ->
      settingEnabled || connected
    }

  private val settingsNavFlow =
    combine(isGestureSettingsOpen, isThemeSettingsOpen, isSmartSearchOpen) {
      gesturesOpen,
      themeOpen,
      smartOpen ->
      Triple(gesturesOpen, themeOpen, smartOpen)
    }

  private val navInputs =
    combine(
      isDrawerOpen,
      isSearchExpanded,
      settingsNavFlow,
      isNotificationAccessGranted,
      accessibilityEnabledFlow,
    ) { drawerOpen, searchExp, (gesturesOpen, themeOpen, smartOpen), notifGranted, a11yEnabled ->
      DrawerNavigationState(
        isDrawerOpen = drawerOpen,
        isSearchExpanded = searchExp,
        isSmartSearchOpen = smartOpen,
        isGestureSettingsOpen = gesturesOpen,
        isThemeSettingsOpen = themeOpen,
        isNotificationAccessGranted = notifGranted,
        isAccessibilityEnabled = a11yEnabled,
      )
    }

  private val searchInputs =
    combine(smartSearchQuery, asyncSearchState) { query, asyncState ->
      asyncState.copy(query = query)
    }

  private val modalAndSearchInputs =
    combine(isLoading, modalState, hiddenAppsFlow, searchInputs, homeEnvState) {
      loading,
      modals,
      hiddenFlow,
      searchState,
      envState ->
      UiExtras(loading, modals, hiddenFlow, searchState, envState)
    }

  val uiState: StateFlow<LauncherUiState> =
    combine(baseInputs, navInputs, modalAndSearchInputs, newsFeedRepository.cacheFlow) {
        snapshot,
        navState,
        extras,
        newsCache ->
        buildUiState(
          snapshot = snapshot,
          navState = navState,
          loading = extras.isLoading,
          modals = extras.modals,
          hiddenFlow = extras.hiddenFlow,
          asyncSearch = extras.searchState,
          homeEnv = extras.homeEnv,
          newsCache = newsCache,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LauncherUiState(selectedCategoryTitle = builtInLabel(BuiltInCategory.UTILITIES)),
      )

  init {
    NewsSyncScheduler.schedulePeriodicSync(application.applicationContext)

    viewModelScope.launch {
      mediaControlManager.playbackState.collect { playback ->
        homeEnvState.value = homeEnvState.value.copy(mediaPlaybackState = playback)
      }
    }
    mediaControlManager.refreshSessions()
    if (calendarRepository.hasCalendarPermission()) {
      refreshCalendarEvents()
    }

    viewModelScope.launch {
      val cachedNews = newsFeedRepository.loadCachedFeed()
      val initialPrefs = preferencesRepository.preferencesFlow.first()
      latestNewsSources.value = initialPrefs.newsSources
      latestNewsPersonalization.value = initialPrefs.newsPersonalization
      val isNewsStale =
        cachedNews.articles.isEmpty() ||
          (System.currentTimeMillis() - cachedNews.lastUpdatedEpochMs) > NEWS_STALE_MS
      if (
        initialPrefs.newsPersonalization.isCompleted &&
          isNewsStale &&
          initialPrefs.newsSources.any { it.isEnabled }
      ) {
        refreshNewsFeeds(showSnackbar = false, explicitSources = initialPrefs.newsSources)
      }
    }
    viewModelScope.launch {
      preferencesRepository.migrateLegacyDockIfNeeded()
      val initialPrefs = preferencesRepository.preferencesFlow.first()
      val savedPackPkg = initialPrefs.selectedIconPackPackage
      if (!savedPackPkg.isNullOrBlank()) {
        val loadedPack = iconPackRepository.loadIconPack(savedPackPkg)
        if (loadedPack != null) {
          activeLoadedIconPack = loadedPack
          homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = loadedPack.label)
        } else {
          // Saved icon pack was uninstalled while launcher wasn't running -> fall back cleanly
          activeLoadedIconPack = null
          homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = null)
          preferencesRepository.setSelectedIconPack(null)
        }
      }
      refreshInstalledApps()
    }

    viewModelScope.launch {
      val bitmap = wallpaperRepository.loadCustomWallpaperBitmap()
      homeEnvState.value = homeEnvState.value.copy(customWallpaperBitmap = bitmap)
    }

    viewModelScope.launch {
      val initialPrefs = preferencesRepository.preferencesFlow.first()
      val savedLocation = initialPrefs.weatherLocation
      val cached = initialPrefs.cachedWeather
      val isStale =
        cached == null || (System.currentTimeMillis() - cached.updatedAtEpochMs) > WEATHER_STALE_MS
      if (savedLocation != null && isStale) {
        refreshWeatherForLocation(savedLocation, showErrorSnackbar = false)
      }

      // Prune orphaned widget IDs whose provider app was removed while launcher wasn't running
      val missingIds =
        initialPrefs.hostedWidgets
          .filter { widgetHostManager.getAppWidgetInfo(it.appWidgetId) == null }
          .map { it.appWidgetId }
          .toSet()
      if (missingIds.isNotEmpty()) {
        missingIds.forEach { widgetHostManager.deleteAppWidgetId(it) }
        preferencesRepository.pruneMissingWidgetIds(missingIds)
      }
    }

    viewModelScope.launch {
      appRepository.observePackageEvents().collect { event ->
        when (event) {
          is PackageEvent.Removed -> {
            if (!event.isReplacing) {
              val removedWidgetIds = preferencesRepository.pruneRemovedPackage(event.packageName)
              removedWidgetIds.forEach { widgetId ->
                widgetHostManager.deleteAppWidgetId(widgetId)
                if (homeEnvState.value.editingWidgetId == widgetId) {
                  homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
                }
              }
              iconPackRepository.invalidateCache(event.packageName)
              if (activeLoadedIconPack?.packageName == event.packageName) {
                activeLoadedIconPack = null
                appRepository.clearIconCache()
                homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = null)
                _events.tryEmit(
                  LauncherEvent.ShowSnackbar(str(R.string.snackbar_icon_pack_uninstalled))
                )
              }
            }
          }
          is PackageEvent.Replaced -> {
            iconPackRepository.invalidateCache(event.packageName)
            if (activeLoadedIconPack?.packageName == event.packageName) {
              val reloaded = iconPackRepository.loadIconPack(event.packageName)
              activeLoadedIconPack = reloaded
              appRepository.clearIconCache()
              homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = reloaded?.label)
              if (reloaded == null) {
                preferencesRepository.setSelectedIconPack(null)
              }
            }
          }
          is PackageEvent.Changed -> {
            iconPackRepository.invalidateCache(event.packageName)
            if (activeLoadedIconPack?.packageName == event.packageName) {
              val reloaded = iconPackRepository.loadIconPack(event.packageName)
              activeLoadedIconPack = reloaded
              appRepository.clearIconCache()
              homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = reloaded?.label)
            }
          }
          is PackageEvent.Added -> {
            iconPackRepository.invalidateCache(event.packageName)
          }
        }
        refreshInstalledApps()
        if (modalState.value.showWidgetPickerSheet) {
          loadAvailableWidgets()
        }
        if (modalState.value.showIconPackSheet) {
          loadInstalledIconPacks()
        }
      }
    }
  }

  fun refreshInstalledApps() {
    viewModelScope.launch {
      val loaded = appRepository.loadInstalledApps(activeLoadedIconPack)
      installedApps.value = loaded
      isLoading.value = false
      val rankedCandidates = pickInitialHomeCandidates(loaded)
      preferencesRepository.seedInitialDockIfNeeded(rankedCandidates)
    }
  }

  // --- Step 3 & 4: System Access Refresh, Drawer & Gesture Controls ---

  fun refreshSystemAccessStates() {
    val context = getApplication<Application>().applicationContext
    val notifGranted = NotificationAccessHelper.isAccessGranted(context)
    isNotificationAccessGranted.value = notifGranted
    if (notifGranted) {
      NotificationAccessHelper.requestRebindIfPermitted(context)
    }
    isAccessibilityEnabledSetting.value = AccessibilityAccessHelper.isServiceEnabled(context)

    val contactsGranted = contactSearchRepository.hasContactsPermission()
    val prev = asyncSearchState.value.hasContactsPermission
    asyncSearchState.value =
      asyncSearchState.value.copy(
        hasContactsPermission = contactsGranted,
        contactsPermissionDeniedOnce =
          if (contactsGranted) false else asyncSearchState.value.contactsPermissionDeniedOnce,
      )
    if (contactsGranted && !prev && isSmartSearchOpen.value) {
      triggerAsyncSmartSearch(smartSearchQuery.value)
    }
    val calendarGranted = calendarRepository.hasCalendarPermission()
    homeEnvState.value =
      homeEnvState.value.copy(
        hasLocationPermission = weatherRepository.hasLocationPermission(),
        hasCalendarPermission = calendarGranted,
      )
    if (calendarGranted) {
      refreshCalendarEvents()
    } else if (homeEnvState.value.calendarEvents.isNotEmpty()) {
      homeEnvState.value = homeEnvState.value.copy(calendarEvents = emptyList())
    }
    mediaControlManager.refreshSessions()
  }

  override fun onCleared() {
    super.onCleared()
    mediaControlManager.stopObserving()
  }

  // --- Step 6: Smarter Search ---

  fun openSmartSearch() {
    modalState.value = ModalState()
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_GESTURE_SETTINGS_OPEN] = false
    savedStateHandle[KEY_THEME_SETTINGS_OPEN] = false
    isSearchExpanded.value = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = true
    refreshSystemAccessStates()
    triggerAsyncSmartSearch(smartSearchQuery.value)
  }

  fun closeSmartSearch() {
    contactSearchJob?.cancel()
    webSuggestionJob?.cancel()
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_QUERY] = ""
    asyncSearchState.value =
      asyncSearchState.value.copy(
        contacts = emptyList(),
        webSuggestions = emptyList(),
        isLoadingSuggestions = false,
      )
  }

  fun updateSmartSearchQuery(query: String) {
    savedStateHandle[KEY_SMART_SEARCH_QUERY] = query
    triggerAsyncSmartSearch(query)
  }

  fun clearSmartSearchQuery() {
    updateSmartSearchQuery("")
  }

  fun recordRecentSearchQuery(query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    viewModelScope.launch { preferencesRepository.recordRecentSearchQuery(trimmed) }
  }

  fun removeRecentSearchQuery(query: String) {
    viewModelScope.launch { preferencesRepository.removeRecentSearchQuery(query) }
  }

  fun clearRecentSearchQueries() {
    viewModelScope.launch {
      preferencesRepository.clearRecentSearchQueries()
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_recent_searches_cleared)))
    }
  }

  fun onContactsPermissionResult(granted: Boolean) {
    asyncSearchState.value =
      asyncSearchState.value.copy(
        hasContactsPermission = granted,
        contactsPermissionDeniedOnce = !granted,
      )
    if (granted && isSmartSearchOpen.value) {
      triggerAsyncSmartSearch(smartSearchQuery.value)
    }
  }

  private fun triggerAsyncSmartSearch(rawQuery: String) {
    contactSearchJob?.cancel()
    webSuggestionJob?.cancel()

    val trimmed = rawQuery.trim()
    if (trimmed.length < 2) {
      asyncSearchState.value =
        asyncSearchState.value.copy(
          contacts = emptyList(),
          webSuggestions = emptyList(),
          isLoadingSuggestions = false,
        )
      return
    }

    val contactFilter = QuickActionMatcher.extractContactQuery(trimmed)
    if (asyncSearchState.value.hasContactsPermission && contactFilter.length >= 2) {
      contactSearchJob =
        viewModelScope.launch {
          delay(80L)
          val matches = contactSearchRepository.searchContacts(contactFilter)
          asyncSearchState.value = asyncSearchState.value.copy(contacts = matches)
        }
    } else {
      asyncSearchState.value = asyncSearchState.value.copy(contacts = emptyList())
    }

    val isPureMath = MathExpressionEvaluator.evaluate(trimmed) != null
    if (isPureMath) {
      asyncSearchState.value =
        asyncSearchState.value.copy(webSuggestions = emptyList(), isLoadingSuggestions = false)
      return
    }

    asyncSearchState.value = asyncSearchState.value.copy(isLoadingSuggestions = true)
    webSuggestionJob =
      viewModelScope.launch {
        delay(220L)
        val suggestions = webSuggestionRepository.fetchSuggestions(trimmed)
        asyncSearchState.value =
          asyncSearchState.value.copy(
            webSuggestions = suggestions,
            isLoadingSuggestions = false,
          )
      }
  }

  fun dismissNotificationOnboarding() {
    viewModelScope.launch { preferencesRepository.dismissNotificationOnboarding() }
  }

  fun openDrawer(focusSearch: Boolean = false) {
    if (focusSearch) {
      openSmartSearch()
      return
    }
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    savedStateHandle[KEY_DRAWER_OPEN] = true
    isSearchExpanded.value = false
  }

  fun closeDrawer() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    isSearchExpanded.value = false
    if (searchQuery.value.isNotEmpty()) {
      savedStateHandle[KEY_SEARCH_QUERY] = ""
    }
  }

  fun toggleDrawerSearch() {
    val next = !isSearchExpanded.value
    isSearchExpanded.value = next
    if (!next) {
      savedStateHandle[KEY_SEARCH_QUERY] = ""
    }
  }

  fun openGestureSettings() {
    modalState.value = ModalState()
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    savedStateHandle[KEY_THEME_SETTINGS_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    savedStateHandle[KEY_GESTURE_SETTINGS_OPEN] = true
    refreshSystemAccessStates()
  }

  fun closeGestureSettings() {
    savedStateHandle[KEY_GESTURE_SETTINGS_OPEN] = false
  }

  // --- Step 5: Theming & Appearance ---

  fun openThemeSettings() {
    modalState.value = ModalState()
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    savedStateHandle[KEY_GESTURE_SETTINGS_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    savedStateHandle[KEY_THEME_SETTINGS_OPEN] = true
  }

  fun closeThemeSettings() {
    savedStateHandle[KEY_THEME_SETTINGS_OPEN] = false
  }

  fun setThemeColorSource(source: ThemeColorSource) {
    viewModelScope.launch { preferencesRepository.setThemeColorSource(source) }
  }

  fun setThemeMode(mode: ThemeModeOption) {
    viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
  }

  fun setFontChoice(choice: FontChoice) {
    viewModelScope.launch { preferencesRepository.setFontChoice(choice) }
  }

  fun setIconShape(shape: IconShapeOption) {
    viewModelScope.launch { preferencesRepository.setIconShape(shape) }
  }

  fun setHideSystemBars(hide: Boolean) {
    viewModelScope.launch { preferencesRepository.setHideSystemBars(hide) }
  }

  fun resetThemeDefaults() {
    viewModelScope.launch {
      preferencesRepository.resetThemeDefaults()
      if (activeLoadedIconPack != null) {
        activeLoadedIconPack = null
        appRepository.clearIconCache()
        homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = null)
        refreshInstalledApps()
      }
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_theme_reset)))
    }
  }

  // --- Step 9: Third-Party Icon Packs (`appfilter.xml`) ---

  fun openIconPackPicker() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    modalState.value = ModalState(showIconPackSheet = true)
    loadInstalledIconPacks()
  }

  fun dismissIconPackPicker() {
    modalState.value = modalState.value.copy(showIconPackSheet = false)
  }

  fun refreshInstalledIconPacks() {
    iconPackRepository.invalidateCache()
    loadInstalledIconPacks()
  }

  private fun loadInstalledIconPacks() {
    viewModelScope.launch {
      homeEnvState.value = homeEnvState.value.copy(isIconPacksLoading = true)
      val packs = iconPackRepository.discoverInstalledIconPacks()
      homeEnvState.value =
        homeEnvState.value.copy(
          installedIconPacks = packs,
          isIconPacksLoading = false,
        )
    }
  }

  fun selectIconPack(packageName: String?) {
    dismissIconPackPicker()
    viewModelScope.launch {
      if (packageName.isNullOrBlank()) {
        activeLoadedIconPack = null
        appRepository.clearIconCache()
        homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = null)
        preferencesRepository.setSelectedIconPack(null)
        refreshInstalledApps()
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_icon_pack_default)))
        return@launch
      }

      val loaded = iconPackRepository.loadIconPack(packageName)
      if (loaded != null) {
        activeLoadedIconPack = loaded
        appRepository.clearIconCache()
        homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = loaded.label)
        preferencesRepository.setSelectedIconPack(loaded.packageName)
        refreshInstalledApps()
        _events.tryEmit(
          LauncherEvent.ShowSnackbar(str(R.string.snackbar_icon_pack_applied, loaded.label))
        )
      } else {
        activeLoadedIconPack = null
        appRepository.clearIconCache()
        homeEnvState.value = homeEnvState.value.copy(activeIconPackLabel = null)
        preferencesRepository.setSelectedIconPack(null)
        refreshInstalledApps()
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_icon_pack_load_failed)))
      }
    }
  }

  // --- Step 10: News Feed (RSS Reader + WorkManager) ---

  fun selectNewsTopic(topic: NewsTopic) {
    homeEnvState.value = homeEnvState.value.copy(selectedNewsTopic = topic)
  }

  fun onNewsPageOpened() {
    if (!latestNewsPersonalization.value.isCompleted) return
    val cache = newsFeedRepository.cacheFlow.value
    val isStale =
      cache.articles.isEmpty() ||
        (System.currentTimeMillis() - cache.lastUpdatedEpochMs) > NEWS_STALE_MS
    if (isStale && !homeEnvState.value.isNewsRefreshing && latestNewsSources.value.any { it.isEnabled }) {
      refreshNewsFeeds(showSnackbar = false)
    }
  }

  fun completeNewsPersonalization(
    selectedContexts: Set<NewsTopic>,
    enabledChannelIds: Set<String>,
    focusKeywords: List<String>,
  ) {
    viewModelScope.launch {
      preferencesRepository.saveNewsPersonalization(
        selectedContexts = selectedContexts,
        enabledChannelIds = enabledChannelIds,
        focusKeywords = focusKeywords,
      )
      val updated = preferencesRepository.preferencesFlow.first()
      latestNewsSources.value = updated.newsSources
      latestNewsPersonalization.value = updated.newsPersonalization
      homeEnvState.value = homeEnvState.value.copy(selectedNewsTopic = NewsTopic.ALL)
      refreshNewsFeeds(showSnackbar = false, explicitSources = updated.newsSources)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_news_personalized)))
    }
  }

  fun toggleNewsContext(topic: NewsTopic, enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.toggleNewsContext(topic, enabled)
      val updated = preferencesRepository.preferencesFlow.first()
      latestNewsPersonalization.value = updated.newsPersonalization
      if (!enabled && homeEnvState.value.selectedNewsTopic == topic) {
        if (topic !in updated.newsPersonalization.selectedContexts) {
          homeEnvState.value = homeEnvState.value.copy(selectedNewsTopic = NewsTopic.ALL)
        }
      }
    }
  }

  fun addNewsFocusKeyword(rawKeyword: String) {
    val parts =
      rawKeyword
        .split(',', ';')
        .map { it.trim() }
        .filter { it.length >= 2 }
    if (parts.isEmpty()) return
    viewModelScope.launch {
      parts.forEach { part -> preferencesRepository.addNewsFocusKeyword(part) }
    }
  }

  fun removeNewsFocusKeyword(keyword: String) {
    viewModelScope.launch { preferencesRepository.removeNewsFocusKeyword(keyword) }
  }

  fun setStrictNewsKeywordFilter(strict: Boolean) {
    viewModelScope.launch { preferencesRepository.setStrictNewsKeywordFilter(strict) }
  }

  fun refreshNewsFeeds(
    showSnackbar: Boolean = true,
    explicitSources: List<NewsFeedSource>? = null,
  ) {
    newsRefreshJob?.cancel()
    newsRefreshJob =
      viewModelScope.launch {
        homeEnvState.value = homeEnvState.value.copy(isNewsRefreshing = true)
        val sourcesToFetch = explicitSources ?: preferencesRepository.preferencesFlow.first().newsSources
        val outcome = newsFeedRepository.refreshFeeds(sourcesToFetch)
        homeEnvState.value = homeEnvState.value.copy(isNewsRefreshing = false)
        if (showSnackbar) {
          val msg =
            if (outcome.success) str(R.string.snackbar_news_refreshed)
            else str(R.string.snackbar_news_refresh_failed)
          _events.tryEmit(LauncherEvent.ShowSnackbar(msg))
        }
      }
  }

  fun openNewsSourcesSheet() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(newsAddSourceError = null, editingWidgetId = null)
    modalState.value = ModalState(showNewsSourcesSheet = true)
  }

  fun dismissNewsSourcesSheet() {
    homeEnvState.value = homeEnvState.value.copy(newsAddSourceError = null)
    modalState.value = modalState.value.copy(showNewsSourcesSheet = false)
  }

  fun clearNewsAddSourceError() {
    if (homeEnvState.value.newsAddSourceError != null) {
      homeEnvState.value = homeEnvState.value.copy(newsAddSourceError = null)
    }
  }

  fun toggleNewsSource(sourceId: String, enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.toggleNewsSource(sourceId, enabled)
      val updatedSources = preferencesRepository.preferencesFlow.first().newsSources
      if (enabled) {
        refreshNewsFeeds(showSnackbar = false, explicitSources = updatedSources)
      }
    }
  }

  fun addCustomNewsSource(
    rawName: String,
    rawUrl: String,
    topic: NewsTopic = NewsTopic.CUSTOM,
  ) {
    viewModelScope.launch {
      when (val result = preferencesRepository.addCustomNewsSource(rawName, rawUrl, topic)) {
        NewsSourceMutationResult.EmptyName -> {
          homeEnvState.value =
            homeEnvState.value.copy(newsAddSourceError = str(R.string.news_sources_error_empty_name))
        }
        NewsSourceMutationResult.InvalidUrl -> {
          homeEnvState.value =
            homeEnvState.value.copy(newsAddSourceError = str(R.string.news_sources_error_invalid_url))
        }
        NewsSourceMutationResult.DuplicateUrl -> {
          homeEnvState.value =
            homeEnvState.value.copy(newsAddSourceError = str(R.string.news_sources_error_duplicate))
        }
        is NewsSourceMutationResult.Success -> {
          homeEnvState.value = homeEnvState.value.copy(newsAddSourceError = null)
          val updated = preferencesRepository.preferencesFlow.first()
          latestNewsSources.value = updated.newsSources
          latestNewsPersonalization.value = updated.newsPersonalization
          refreshNewsFeeds(showSnackbar = false, explicitSources = updated.newsSources)
          _events.tryEmit(
            LauncherEvent.ShowSnackbar(str(R.string.snackbar_news_source_added, result.source.name))
          )
        }
      }
    }
  }

  fun removeCustomNewsSource(source: NewsFeedSource) {
    viewModelScope.launch {
      preferencesRepository.removeCustomNewsSource(source.id)
      val updatedSources = preferencesRepository.preferencesFlow.first().newsSources
      if (
        homeEnvState.value.selectedNewsTopic == NewsTopic.CUSTOM &&
          updatedSources.none { it.topic == NewsTopic.CUSTOM && it.isEnabled }
      ) {
        homeEnvState.value = homeEnvState.value.copy(selectedNewsTopic = NewsTopic.ALL)
      }
      if (updatedSources.any { it.isEnabled }) {
        refreshNewsFeeds(showSnackbar = false, explicitSources = updatedSources)
      }
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(str(R.string.snackbar_news_source_removed, source.name))
      )
    }
  }

  fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
    viewModelScope.launch { preferencesRepository.setGestureAction(gesture, action) }
  }

  fun resetGestureDefaults() {
    viewModelScope.launch {
      preferencesRepository.resetGestureDefaults()
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_gestures_reset)))
    }
  }

  fun dismissAccessibilityPrompt() {
    modalState.value = modalState.value.copy(accessibilityPromptAction = null)
  }

  fun executeHomeGesture(
    gesture: HomeGesture,
    biometricAvailability: BiometricAvailability,
  ) {
    val action = latestGestureConfig.value.actionFor(gesture)
    when (action) {
      GestureAction.OPEN_DRAWER -> openDrawer(focusSearch = false)
      GestureAction.OPEN_SEARCH -> openDrawer(focusSearch = true)
      GestureAction.OPEN_HIDDEN_APPS -> openHiddenApps(biometricAvailability)
      GestureAction.LOCK_SCREEN -> {
        handleGlobalActionResult(
          action = action,
          result = NorGestureAccessibilityService.performLockScreen(),
        )
      }
      GestureAction.OPEN_NOTIFICATIONS -> {
        handleGlobalActionResult(
          action = action,
          result = NorGestureAccessibilityService.performExpandNotifications(),
        )
      }
      GestureAction.OPEN_QUICK_SETTINGS -> {
        handleGlobalActionResult(
          action = action,
          result = NorGestureAccessibilityService.performExpandQuickSettings(),
        )
      }
      GestureAction.NONE -> Unit
    }
  }

  private fun handleGlobalActionResult(action: GestureAction, result: GlobalActionResult) {
    when (result) {
      GlobalActionResult.Success -> Unit
      GlobalActionResult.ServiceNotConnected -> {
        modalState.value = modalState.value.copy(accessibilityPromptAction = action)
      }
      GlobalActionResult.UnsupportedApiLevel -> {
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_lock_unsupported_api)))
      }
      GlobalActionResult.Failed -> {
        val actionName = gestureActionName(action)
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_gesture_failed, actionName)))
      }
    }
  }

  fun openWidgetCustomizeSheet() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    modalState.value = ModalState(showWidgetCustomizeSheet = true)
  }

  fun dismissWidgetCustomizeSheet() {
    modalState.value = modalState.value.copy(showWidgetCustomizeSheet = false)
  }

  fun selectHomeWidgetStyle(style: HomeWidgetStyle) {
    viewModelScope.launch { preferencesRepository.setHomeWidgetStyle(style) }
  }

  // --- Smart Widgets Pack (Stack, Pinned, Music, Calendar, Quick Note, Icon Group) ---

  fun toggleStackWidget(type: SmartWidgetType, enabled: Boolean) {
    viewModelScope.launch { preferencesRepository.toggleStackWidget(type, enabled) }
  }

  fun moveStackWidget(type: SmartWidgetType, delta: Int) {
    viewModelScope.launch { preferencesRepository.moveStackWidget(type, delta) }
  }

  fun togglePinnedSmartWidget(type: SmartWidgetType, pinned: Boolean) {
    viewModelScope.launch {
      val ok = preferencesRepository.togglePinnedSmartWidget(type, pinned)
      if (!ok && pinned) {
        _events.tryEmit(
          LauncherEvent.ShowSnackbar(
            str(
              R.string.snackbar_smart_widget_pin_limit,
              LauncherPreferencesRepository.MAX_PINNED_SMART_WIDGETS,
            )
          )
        )
      }
    }
  }

  fun openNoteEditor() {
    modalState.value = modalState.value.copy(showNoteEditorDialog = true)
  }

  fun dismissNoteEditor() {
    modalState.value = modalState.value.copy(showNoteEditorDialog = false)
  }

  fun saveQuickNote(title: String, body: String) {
    dismissNoteEditor()
    viewModelScope.launch {
      preferencesRepository.saveQuickNote(title, body)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_note_saved)))
    }
  }

  fun openIconGroupPicker() {
    modalState.value = modalState.value.copy(showIconGroupPickerDialog = true)
  }

  fun dismissIconGroupPicker() {
    modalState.value = modalState.value.copy(showIconGroupPickerDialog = false)
  }

  fun saveIconGroupPackages(packages: List<String>) {
    dismissIconGroupPicker()
    viewModelScope.launch {
      preferencesRepository.setIconGroupPackages(packages)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_icon_group_saved)))
    }
  }

  fun onCalendarPermissionResult(granted: Boolean) {
    homeEnvState.value = homeEnvState.value.copy(hasCalendarPermission = granted)
    if (granted) {
      refreshCalendarEvents()
    } else {
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(str(R.string.snackbar_calendar_permission_denied))
      )
    }
  }

  fun refreshCalendarEvents() {
    if (!calendarRepository.hasCalendarPermission()) {
      homeEnvState.value =
        homeEnvState.value.copy(
          hasCalendarPermission = false,
          calendarEvents = emptyList(),
          isCalendarLoading = false,
        )
      return
    }
    calendarJob?.cancel()
    calendarJob =
      viewModelScope.launch {
        homeEnvState.value = homeEnvState.value.copy(isCalendarLoading = true)
        val events = calendarRepository.loadUpcomingEvents()
        homeEnvState.value =
          homeEnvState.value.copy(
            calendarEvents = events,
            hasCalendarPermission = true,
            isCalendarLoading = false,
          )
      }
  }

  fun musicTogglePlayPause() {
    mediaControlManager.togglePlayPause()
  }

  fun musicSkipPrevious() {
    mediaControlManager.skipPrevious()
  }

  fun musicSkipNext() {
    mediaControlManager.skipNext()
  }

  // --- Step 7: Home Wallpaper Changer & Live Weather ---

  fun openWallpaperSheet() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    modalState.value = ModalState(showWallpaperSheet = true)
  }

  fun dismissWallpaperSheet() {
    modalState.value = modalState.value.copy(showWallpaperSheet = false)
  }

  fun selectWallpaperPreset(preset: WallpaperPreset) {
    viewModelScope.launch { preferencesRepository.setWallpaperPreset(preset) }
  }

  fun importCustomWallpaper(uri: Uri) {
    viewModelScope.launch {
      val imported = wallpaperRepository.importPhotoFromUri(uri)
      if (imported) {
        val bitmap = wallpaperRepository.loadCustomWallpaperBitmap()
        homeEnvState.value = homeEnvState.value.copy(customWallpaperBitmap = bitmap)
        preferencesRepository.setCustomWallpaperSelected(System.currentTimeMillis())
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_wallpaper_updated)))
      } else {
        _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_wallpaper_failed)))
      }
    }
  }

  fun setWallpaperDimPercent(dimPercent: Int) {
    viewModelScope.launch { preferencesRepository.setWallpaperDimPercent(dimPercent) }
  }

  fun applyCustomWallpaperToSystem() {
    viewModelScope.launch {
      val ok = wallpaperRepository.applyCustomPhotoToSystemWallpaper()
      val msg =
        if (ok) str(R.string.snackbar_wallpaper_system_applied)
        else str(R.string.snackbar_system_wallpaper_failed)
      _events.tryEmit(LauncherEvent.ShowSnackbar(msg))
    }
  }

  fun openSystemWallpaperPicker() {
    dismissWallpaperSheet()
    val opened = wallpaperRepository.openSystemWallpaperPicker()
    if (!opened) {
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_system_wallpaper_failed)))
    }
  }

  fun openWeatherCityDialog() {
    modalState.value = modalState.value.copy(showWeatherCityDialog = true)
  }

  fun dismissWeatherCityDialog() {
    modalState.value = modalState.value.copy(showWeatherCityDialog = false)
  }

  fun setWeatherCity(rawCity: String) {
    dismissWeatherCityDialog()
    val trimmed = rawCity.trim()
    if (trimmed.length < 2) return
    weatherJob?.cancel()
    weatherJob =
      viewModelScope.launch {
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = true)
        val location = weatherRepository.geocodeCity(trimmed)
        if (location == null) {
          homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = false)
          _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.weather_error_city_not_found)))
          return@launch
        }
        preferencesRepository.saveWeatherLocation(location)
        val snapshot = weatherRepository.fetchCurrentWeather(location)
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = false)
        if (snapshot != null) {
          preferencesRepository.saveWeatherSnapshot(snapshot)
        } else {
          _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.weather_error_fetch_failed)))
        }
      }
  }

  fun onLocationPermissionResult(granted: Boolean) {
    homeEnvState.value = homeEnvState.value.copy(hasLocationPermission = granted)
    if (granted) {
      fetchWeatherFromLocation()
    } else {
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.weather_error_location_unavailable)))
      openWeatherCityDialog()
    }
  }

  fun fetchWeatherFromLocation() {
    weatherJob?.cancel()
    weatherJob =
      viewModelScope.launch {
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = true)
        val location = weatherRepository.resolveDeviceCoarseLocation()
        if (location == null) {
          homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = false)
          _events.tryEmit(
            LauncherEvent.ShowSnackbar(str(R.string.weather_error_location_unavailable))
          )
          openWeatherCityDialog()
          return@launch
        }
        preferencesRepository.saveWeatherLocation(location)
        val snapshot = weatherRepository.fetchCurrentWeather(location)
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = false)
        if (snapshot != null) {
          preferencesRepository.saveWeatherSnapshot(snapshot)
        } else {
          _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.weather_error_fetch_failed)))
        }
      }
  }

  fun refreshWeather() {
    val saved = latestWeatherLocation.value
    if (saved != null) {
      refreshWeatherForLocation(saved, showErrorSnackbar = true)
    } else if (homeEnvState.value.hasLocationPermission) {
      fetchWeatherFromLocation()
    } else {
      openWeatherCityDialog()
    }
  }

  fun toggleTemperatureUnit() {
    viewModelScope.launch { preferencesRepository.toggleTemperatureUnit() }
  }

  private fun refreshWeatherForLocation(
    location: WeatherLocation,
    showErrorSnackbar: Boolean,
  ) {
    weatherJob?.cancel()
    weatherJob =
      viewModelScope.launch {
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = true)
        val snapshot = weatherRepository.fetchCurrentWeather(location)
        homeEnvState.value = homeEnvState.value.copy(isWeatherLoading = false)
        if (snapshot != null) {
          preferencesRepository.saveWeatherSnapshot(snapshot)
        } else if (showErrorSnackbar) {
          _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.weather_error_fetch_failed)))
        }
      }
  }

  // --- Step 8: Real Android AppWidget Hosting (Bind, Picker, Resize, Remove) ---

  fun openWidgetPicker() {
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    modalState.value = ModalState(showWidgetPickerSheet = true)
    loadAvailableWidgets()
  }

  fun dismissWidgetPicker() {
    modalState.value = modalState.value.copy(showWidgetPickerSheet = false)
  }

  private fun loadAvailableWidgets() {
    viewModelScope.launch {
      homeEnvState.value = homeEnvState.value.copy(isWidgetPickerLoading = true)
      val groups = widgetHostManager.loadAvailableWidgetGroups()
      homeEnvState.value =
        homeEnvState.value.copy(
          availableWidgetGroups = groups,
          isWidgetPickerLoading = false,
        )
    }
  }

  fun onPickAvailableWidget(entry: AvailableWidgetEntry) {
    if (latestHostedWidgets.value.size >= LauncherWidgetHostManager.MAX_HOSTED_WIDGETS) {
      dismissWidgetPicker()
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(
          str(R.string.snackbar_widget_limit_reached, LauncherWidgetHostManager.MAX_HOSTED_WIDGETS)
        )
      )
      return
    }

    dismissWidgetPicker()

    // Release any stale abandoned pending allocation before starting a new bind flow
    pendingWidgetAddition?.let { stale ->
      widgetHostManager.deleteAppWidgetId(stale.appWidgetId)
      pendingWidgetAddition = null
    }

    val appWidgetId = widgetHostManager.allocateAppWidgetId()
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_allocate_failed)))
      return
    }

    val pending = PendingWidgetAddition(appWidgetId = appWidgetId, entry = entry)
    pendingWidgetAddition = pending

    val alreadyAllowed = widgetHostManager.tryBindWidgetIfAllowed(appWidgetId, entry.providerInfo)
    if (alreadyAllowed) {
      continueAfterWidgetBound(pending)
    } else {
      _events.tryEmit(
        LauncherEvent.LaunchWidgetBindPermission(
          appWidgetId = appWidgetId,
          providerInfo = entry.providerInfo,
        )
      )
    }
  }

  fun onWidgetBindPermissionResult(granted: Boolean) {
    val pending = pendingWidgetAddition ?: return
    if (!granted) {
      pendingWidgetAddition = null
      widgetHostManager.deleteAppWidgetId(pending.appWidgetId)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_bind_denied)))
      return
    }
    continueAfterWidgetBound(pending)
  }

  fun onWidgetBindLaunchFailed() {
    val pending = pendingWidgetAddition
    pendingWidgetAddition = null
    if (pending != null) {
      widgetHostManager.deleteAppWidgetId(pending.appWidgetId)
    }
    _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_bind_failed)))
  }

  private fun continueAfterWidgetBound(pending: PendingWidgetAddition) {
    if (pending.entry.requiresConfigure) {
      _events.tryEmit(
        LauncherEvent.LaunchWidgetConfigure(
          appWidgetId = pending.appWidgetId,
          isReconfigure = false,
        )
      )
    } else {
      finalizeWidgetAddition(pending)
    }
  }

  fun onWidgetConfigureResult(resultOk: Boolean, isReconfigure: Boolean) {
    if (isReconfigure) {
      return
    }
    val pending = pendingWidgetAddition ?: return
    pendingWidgetAddition = null
    if (resultOk) {
      finalizeWidgetAddition(pending)
    } else {
      widgetHostManager.deleteAppWidgetId(pending.appWidgetId)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_configure_canceled)))
    }
  }

  private fun finalizeWidgetAddition(pending: PendingWidgetAddition) {
    pendingWidgetAddition = null
    val record =
      HostedWidgetRecord(
        appWidgetId = pending.appWidgetId,
        providerComponent = pending.entry.providerComponent,
        label = pending.entry.label,
        widthSpan = pending.entry.defaultWidthSpan,
        heightDp = pending.entry.defaultHeightDp,
      )
    viewModelScope.launch {
      preferencesRepository.addHostedWidget(record)
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_added, record.label))
      )
    }
  }

  fun startEditingWidget(appWidgetId: Int) {
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = appWidgetId)
  }

  fun stopEditingWidget() {
    if (homeEnvState.value.editingWidgetId != null) {
      homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    }
  }

  fun resizeHostedWidget(appWidgetId: Int, widthSpan: Int, heightDp: Int) {
    viewModelScope.launch {
      preferencesRepository.updateHostedWidgetSize(
        appWidgetId = appWidgetId,
        widthSpan = widthSpan,
        heightDp = heightDp,
      )
    }
  }

  fun moveHostedWidget(appWidgetId: Int, delta: Int) {
    viewModelScope.launch { preferencesRepository.moveHostedWidget(appWidgetId, delta) }
  }

  fun requestReconfigureWidget(appWidgetId: Int) {
    _events.tryEmit(
      LauncherEvent.LaunchWidgetConfigure(
        appWidgetId = appWidgetId,
        isReconfigure = true,
      )
    )
  }

  fun removeHostedWidget(appWidgetId: Int) {
    if (homeEnvState.value.editingWidgetId == appWidgetId) {
      homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    }
    widgetHostManager.deleteAppWidgetId(appWidgetId)
    viewModelScope.launch {
      preferencesRepository.removeHostedWidget(appWidgetId)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_widget_removed)))
    }
  }

  fun selectCategory(categoryId: String) {
    if (selectedCategoryId.value != categoryId) {
      savedStateHandle[KEY_SELECTED_CATEGORY] = categoryId
      _events.tryEmit(LauncherEvent.ScrollToTop)
    }
  }

  fun updateSearchQuery(query: String) {
    savedStateHandle[KEY_SEARCH_QUERY] = query
  }

  fun clearSearchQuery() {
    if (searchQuery.value.isNotEmpty()) {
      savedStateHandle[KEY_SEARCH_QUERY] = ""
    }
  }

  /**
   * Called from MainActivity.onNewIntent when the Home button is pressed.
   * Immediately locks Hidden Apps, exits widget edit mode, closes Drawer / settings / sheets, resets filters, and scrolls top.
   */
  fun onHomePressed() {
    lockHiddenApps()
    closeSmartSearch()
    stopEditingWidget()
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_GESTURE_SETTINGS_OPEN] = false
    savedStateHandle[KEY_THEME_SETTINGS_OPEN] = false
    isSearchExpanded.value = false
    modalState.value = ModalState()
    savedStateHandle[KEY_SEARCH_QUERY] = ""
    savedStateHandle[KEY_SELECTED_CATEGORY] = ""
    _events.tryEmit(LauncherEvent.ReturnToHomePage)
    _events.tryEmit(LauncherEvent.ScrollToTop)
  }

  fun lockHiddenApps() {
    lockoutTickerJob?.cancel()
    lockoutTickerJob = null
    hiddenAppsFlow.value = HiddenAppsFlowState.Closed
    modalState.value = modalState.value.copy(hiddenMenuPackage = null)
  }

  // --- Step 2: Hidden Apps Authentication & Management ---

  fun openHiddenApps(biometricAvailability: BiometricAvailability) {
    modalState.value = ModalState()
    savedStateHandle[KEY_DRAWER_OPEN] = false
    savedStateHandle[KEY_SMART_SEARCH_OPEN] = false
    isSearchExpanded.value = false
    homeEnvState.value = homeEnvState.value.copy(editingWidgetId = null)
    val pinSecurity = latestPinSecurity.value

    if (!pinSecurity.isPinConfigured) {
      hiddenAppsFlow.value = HiddenAppsFlowState.SetupPin()
      return
    }

    val remainingLockoutSec = computeRemainingLockoutSeconds(pinSecurity.lockoutUntilEpochMs)
    val notice =
      when (biometricAvailability) {
        BiometricAvailability.AVAILABLE -> null
        BiometricAvailability.NOT_ENROLLED -> str(R.string.biometric_not_enrolled_note)
        BiometricAvailability.UNAVAILABLE -> str(R.string.biometric_unavailable_note)
      }

    hiddenAppsFlow.value =
      HiddenAppsFlowState.VerifyPin(
        biometricNotice = notice,
        lockoutSecondsRemaining = remainingLockoutSec,
      )

    if (remainingLockoutSec > 0) {
      startLockoutTicker(pinSecurity.lockoutUntilEpochMs)
    }

    if (biometricAvailability == BiometricAvailability.AVAILABLE) {
      _events.tryEmit(LauncherEvent.TriggerBiometricPrompt)
    }
  }

  fun requestBiometricsAgain() {
    if (hiddenAppsFlow.value is HiddenAppsFlowState.VerifyPin) {
      _events.tryEmit(LauncherEvent.TriggerBiometricPrompt)
    }
  }

  fun onBiometricAuthSuccess() {
    lockoutTickerJob?.cancel()
    lockoutTickerJob = null
    viewModelScope.launch { preferencesRepository.resetPinFailures() }
    hiddenAppsFlow.value = HiddenAppsFlowState.Unlocked()
  }

  fun onBiometricUsePin() {
    val current = hiddenAppsFlow.value as? HiddenAppsFlowState.VerifyPin ?: return
    hiddenAppsFlow.value = current.copy(biometricNotice = null)
  }

  fun onBiometricLockout() {
    val current = hiddenAppsFlow.value as? HiddenAppsFlowState.VerifyPin ?: return
    hiddenAppsFlow.value = current.copy(biometricNotice = str(R.string.biometric_lockout_note))
  }

  fun onBiometricCanceled() {
    // Keep VerifyPin screen visible so user can either enter PIN or tap Back.
  }

  fun submitSetupPin(pin: String) {
    val state = hiddenAppsFlow.value as? HiddenAppsFlowState.SetupPin ?: return
    if (!PinHasher.isValidPinFormat(pin)) {
      hiddenAppsFlow.value = state.copy(errorMessage = str(R.string.pin_error_too_short))
      return
    }

    val firstEntry = state.firstEntry
    if (firstEntry == null) {
      hiddenAppsFlow.value = HiddenAppsFlowState.SetupPin(firstEntry = pin)
      return
    }

    if (firstEntry != pin) {
      hiddenAppsFlow.value =
        HiddenAppsFlowState.SetupPin(
          firstEntry = null,
          errorMessage = str(R.string.pin_error_mismatch),
        )
      return
    }

    hiddenAppsFlow.value = state.copy(isBusy = true, errorMessage = null)
    viewModelScope.launch {
      val hashed = PinHasher.hashPin(pin)
      preferencesRepository.saveHashedPin(hashed)
      hiddenAppsFlow.value = HiddenAppsFlowState.Unlocked()
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_pin_saved)))
    }
  }

  fun submitVerifyPin(pin: String) {
    val state = hiddenAppsFlow.value as? HiddenAppsFlowState.VerifyPin ?: return
    val security = latestPinSecurity.value
    val storedHash = security.hashedPin ?: return

    val now = System.currentTimeMillis()
    val lockoutSec = computeRemainingLockoutSeconds(security.lockoutUntilEpochMs, now)
    if (lockoutSec > 0) {
      hiddenAppsFlow.value = state.copy(lockoutSecondsRemaining = lockoutSec, errorMessage = null)
      startLockoutTicker(security.lockoutUntilEpochMs)
      return
    }

    if (!PinHasher.isValidPinFormat(pin)) {
      hiddenAppsFlow.value = state.copy(errorMessage = str(R.string.pin_error_too_short))
      return
    }

    hiddenAppsFlow.value = state.copy(isBusy = true, errorMessage = null)
    viewModelScope.launch {
      val matches = PinHasher.verifyPin(pin, storedHash)
      if (matches) {
        lockoutTickerJob?.cancel()
        lockoutTickerJob = null
        preferencesRepository.resetPinFailures()
        hiddenAppsFlow.value = HiddenAppsFlowState.Unlocked()
      } else {
        val (remaining, lockoutUntil) =
          preferencesRepository.recordFailedPinAttempt(System.currentTimeMillis())
        val updatedState = hiddenAppsFlow.value as? HiddenAppsFlowState.VerifyPin ?: return@launch
        if (lockoutUntil > 0L) {
          val seconds = computeRemainingLockoutSeconds(lockoutUntil)
          hiddenAppsFlow.value =
            updatedState.copy(
              isBusy = false,
              errorMessage = null,
              lockoutSecondsRemaining = seconds,
            )
          startLockoutTicker(lockoutUntil)
        } else {
          hiddenAppsFlow.value =
            updatedState.copy(
              isBusy = false,
              errorMessage = str(R.string.pin_error_incorrect, remaining),
            )
        }
      }
    }
  }

  fun openChangePin() {
    if (hiddenAppsFlow.value is HiddenAppsFlowState.Unlocked) {
      modalState.value = modalState.value.copy(hiddenMenuPackage = null)
      hiddenAppsFlow.value = HiddenAppsFlowState.ChangePin()
    }
  }

  fun submitChangePin(pin: String) {
    val state = hiddenAppsFlow.value as? HiddenAppsFlowState.ChangePin ?: return
    if (!PinHasher.isValidPinFormat(pin)) {
      hiddenAppsFlow.value = state.copy(errorMessage = str(R.string.pin_error_too_short))
      return
    }

    val firstEntry = state.firstEntry
    if (firstEntry == null) {
      hiddenAppsFlow.value = HiddenAppsFlowState.ChangePin(firstEntry = pin)
      return
    }

    if (firstEntry != pin) {
      hiddenAppsFlow.value =
        HiddenAppsFlowState.ChangePin(
          firstEntry = null,
          errorMessage = str(R.string.pin_error_mismatch),
        )
      return
    }

    hiddenAppsFlow.value = state.copy(isBusy = true, errorMessage = null)
    viewModelScope.launch {
      val hashed = PinHasher.hashPin(pin)
      preferencesRepository.saveHashedPin(hashed)
      hiddenAppsFlow.value = HiddenAppsFlowState.Unlocked()
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_pin_updated)))
    }
  }

  fun handleHiddenScreenBack() {
    when (hiddenAppsFlow.value) {
      is HiddenAppsFlowState.ChangePin -> {
        hiddenAppsFlow.value = HiddenAppsFlowState.Unlocked()
      }
      else -> lockHiddenApps()
    }
  }

  fun openHiddenAppMenu(app: AppItem) {
    if (hiddenAppsFlow.value is HiddenAppsFlowState.Unlocked) {
      modalState.value = modalState.value.copy(hiddenMenuPackage = app.packageName)
    }
  }

  fun dismissHiddenAppMenu() {
    modalState.value = modalState.value.copy(hiddenMenuPackage = null)
  }

  fun unhideAppFromHiddenScreen(app: AppItem) {
    dismissHiddenAppMenu()
    viewModelScope.launch {
      preferencesRepository.unhideApp(app.packageName)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_unhidden, app.label)))
    }
  }

  private fun startLockoutTicker(lockoutUntilEpochMs: Long) {
    lockoutTickerJob?.cancel()
    lockoutTickerJob =
      viewModelScope.launch {
        while (true) {
          val remainingSec = computeRemainingLockoutSeconds(lockoutUntilEpochMs)
          val current = hiddenAppsFlow.value as? HiddenAppsFlowState.VerifyPin ?: break
          hiddenAppsFlow.value = current.copy(lockoutSecondsRemaining = remainingSec)
          if (remainingSec <= 0) break
          delay(1_000L)
        }
      }
  }

  private fun computeRemainingLockoutSeconds(
    lockoutUntilEpochMs: Long,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): Int {
    val diffMs = lockoutUntilEpochMs - nowEpochMs
    if (diffMs <= 0L) return 0
    return ((diffMs + 999L) / 1_000L).toInt()
  }

  // --- Step 1: App Actions, Category Overrides & Custom Categories ---

  fun openAppMenu(app: AppItem) {
    modalState.value = ModalState(menuPackage = app.packageName)
  }

  fun dismissAppMenu() {
    modalState.value = modalState.value.copy(menuPackage = null)
  }

  fun openMoveToCategory(app: AppItem) {
    modalState.value = ModalState(moveCategoryPackage = app.packageName)
  }

  fun dismissMoveToCategory() {
    modalState.value = modalState.value.copy(moveCategoryPackage = null)
  }

  fun toggleDockPin(app: AppItem) {
    dismissAppMenu()
    viewModelScope.launch {
      val result = preferencesRepository.toggleDockPin(app.packageName)
      val message =
        when (result) {
          DockPinResult.Pinned -> str(R.string.snackbar_pinned, app.label)
          DockPinResult.Unpinned -> str(R.string.snackbar_unpinned, app.label)
          DockPinResult.DockFull ->
            str(R.string.snackbar_dock_full, LauncherPreferencesRepository.MAX_DOCK_APPS)
        }
      _events.tryEmit(LauncherEvent.ShowSnackbar(message))
    }
  }

  fun assignCategory(app: AppItem, targetCategoryId: String, targetCategoryName: String) {
    dismissMoveToCategory()
    viewModelScope.launch {
      if (targetCategoryId == app.autoCategory.id) {
        preferencesRepository.clearCategoryOverride(app.packageName)
        _events.tryEmit(
          LauncherEvent.ShowSnackbar(str(R.string.snackbar_category_reset, app.label))
        )
      } else {
        preferencesRepository.setCategoryOverride(app.packageName, targetCategoryId)
        _events.tryEmit(
          LauncherEvent.ShowSnackbar(str(R.string.snackbar_moved, app.label, targetCategoryName))
        )
      }
    }
  }

  fun resetCategoryToAuto(app: AppItem) {
    dismissMoveToCategory()
    viewModelScope.launch {
      preferencesRepository.clearCategoryOverride(app.packageName)
      _events.tryEmit(LauncherEvent.ShowSnackbar(str(R.string.snackbar_category_reset, app.label)))
    }
  }

  fun hideApp(app: AppItem) {
    dismissAppMenu()
    viewModelScope.launch {
      preferencesRepository.hideApp(app.packageName)
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(
          message = str(R.string.snackbar_hidden, app.label),
          undoHiddenPackage = app.packageName,
        )
      )
    }
  }

  fun unhideApp(packageName: String) {
    viewModelScope.launch { preferencesRepository.unhideApp(packageName) }
  }

  fun openCreateCategoryDialog(assignToApp: AppItem? = null) {
    modalState.value =
      ModalState(
        createCategoryState =
          CreateCategoryDialogState(
            assignToPackage = assignToApp?.packageName,
            assignToAppLabel = assignToApp?.label,
          )
      )
  }

  fun dismissCreateCategoryDialog() {
    modalState.value = modalState.value.copy(createCategoryState = null)
  }

  fun createCustomCategory(rawName: String) {
    val dialogState = modalState.value.createCategoryState ?: return
    viewModelScope.launch {
      val builtInNames = BuiltInCategory.entries.map { builtInLabel(it) }.toSet()
      when (val result = preferencesRepository.createCustomCategory(rawName, builtInNames)) {
        CategoryMutationResult.EmptyName -> {
          modalState.value =
            modalState.value.copy(
              createCategoryState = dialogState.copy(errorMessage = str(R.string.error_category_empty))
            )
        }
        CategoryMutationResult.DuplicateName -> {
          modalState.value =
            modalState.value.copy(
              createCategoryState =
                dialogState.copy(errorMessage = str(R.string.error_category_duplicate))
            )
        }
        is CategoryMutationResult.Success -> {
          modalState.value = modalState.value.copy(createCategoryState = null)
          val assignPkg = dialogState.assignToPackage
          if (assignPkg != null) {
            preferencesRepository.setCategoryOverride(assignPkg, result.category.id)
            val appLabel = dialogState.assignToAppLabel ?: assignPkg
            _events.tryEmit(
              LauncherEvent.ShowSnackbar(
                str(R.string.snackbar_moved, appLabel, result.category.name)
              )
            )
          } else {
            savedStateHandle[KEY_SELECTED_CATEGORY] = result.category.id
            _events.tryEmit(
              LauncherEvent.ShowSnackbar(str(R.string.snackbar_category_created, result.category.name))
            )
          }
        }
      }
    }
  }

  fun openManageCustomCategory(category: CustomCategory) {
    modalState.value = ModalState(manageCustomCategory = category)
  }

  fun dismissManageCustomCategory() {
    modalState.value = modalState.value.copy(manageCustomCategory = null)
  }

  fun openRenameCustomCategory(category: CustomCategory) {
    modalState.value = ModalState(renameCategoryState = RenameCategoryDialogState(category))
  }

  fun dismissRenameCustomCategory() {
    modalState.value = modalState.value.copy(renameCategoryState = null)
  }

  fun renameCustomCategory(rawName: String) {
    val dialogState = modalState.value.renameCategoryState ?: return
    viewModelScope.launch {
      val builtInNames = BuiltInCategory.entries.map { builtInLabel(it) }.toSet()
      when (
        val result =
          preferencesRepository.renameCustomCategory(
            categoryId = dialogState.category.id,
            rawName = rawName,
            builtInDisplayNames = builtInNames,
          )
      ) {
        CategoryMutationResult.EmptyName -> {
          modalState.value =
            modalState.value.copy(
              renameCategoryState = dialogState.copy(errorMessage = str(R.string.error_category_empty))
            )
        }
        CategoryMutationResult.DuplicateName -> {
          modalState.value =
            modalState.value.copy(
              renameCategoryState =
                dialogState.copy(errorMessage = str(R.string.error_category_duplicate))
            )
        }
        is CategoryMutationResult.Success -> {
          modalState.value = modalState.value.copy(renameCategoryState = null)
        }
      }
    }
  }

  fun requestDeleteCustomCategory(category: CustomCategory) {
    modalState.value = ModalState(deleteConfirmCategory = category)
  }

  fun dismissDeleteConfirm() {
    modalState.value = modalState.value.copy(deleteConfirmCategory = null)
  }

  fun confirmDeleteCustomCategory() {
    val category = modalState.value.deleteConfirmCategory ?: return
    modalState.value = modalState.value.copy(deleteConfirmCategory = null)
    viewModelScope.launch {
      preferencesRepository.deleteCustomCategory(category.id)
      if (selectedCategoryId.value == category.id) {
        savedStateHandle[KEY_SELECTED_CATEGORY] = ""
      }
      _events.tryEmit(
        LauncherEvent.ShowSnackbar(str(R.string.snackbar_category_deleted, category.name))
      )
    }
  }

  fun notifyMessage(message: String) {
    _events.tryEmit(LauncherEvent.ShowSnackbar(message))
  }

  fun builtInLabel(category: BuiltInCategory): String =
    when (category) {
      BuiltInCategory.COMMUNICATION -> str(R.string.category_communication)
      BuiltInCategory.INTERNET -> str(R.string.category_internet)
      BuiltInCategory.MEDIA -> str(R.string.category_media)
      BuiltInCategory.GAMES -> str(R.string.category_games)
      BuiltInCategory.PRODUCTIVITY -> str(R.string.category_productivity)
      BuiltInCategory.UTILITIES -> str(R.string.category_utilities)
      BuiltInCategory.OTHER -> str(R.string.category_other)
    }

  private fun gestureActionName(action: GestureAction): String =
    when (action) {
      GestureAction.OPEN_DRAWER -> str(R.string.gesture_action_open_drawer)
      GestureAction.LOCK_SCREEN -> str(R.string.gesture_action_lock_screen)
      GestureAction.OPEN_NOTIFICATIONS -> str(R.string.gesture_action_open_notifications)
      GestureAction.OPEN_QUICK_SETTINGS -> str(R.string.gesture_action_quick_settings)
      GestureAction.OPEN_SEARCH -> str(R.string.gesture_action_open_search)
      GestureAction.OPEN_HIDDEN_APPS -> str(R.string.gesture_action_hidden_apps)
      GestureAction.NONE -> str(R.string.gesture_action_none)
    }

  private fun pickInitialHomeCandidates(apps: List<InstalledApp>): List<String> {
    val preferredKeywords =
      listOf(
        "dialer",
        "phone",
        "messaging",
        "sms",
        "camera",
        "chrome",
        "browser",
        "photos",
        "gallery",
        "music",
        "calendar",
        "settings",
      )
    val prioritized =
      apps
        .filter { app ->
          val token = "${app.packageName.lowercase()} ${app.label.lowercase()}"
          preferredKeywords.any { token.contains(it) }
        }
        .map { it.packageName }
    val remainder = apps.map { it.packageName }.filterNot { it in prioritized }
    return (prioritized + remainder)
      .distinct()
      .take(LauncherPreferencesRepository.INITIAL_HOME_APP_COUNT)
  }

  private fun buildUiState(
    snapshot: BaseSnapshot,
    navState: DrawerNavigationState,
    loading: Boolean,
    modals: ModalState,
    hiddenFlow: HiddenAppsFlowState,
    asyncSearch: AsyncSearchState,
    homeEnv: HomeEnvState,
    newsCache: NewsFeedCache,
  ): LauncherUiState {
    val prefs = snapshot.prefs
    val customById = prefs.customCategories.associateBy { it.id }
    val dockSet = prefs.dockPackages.toSet()

    val allMappedItems =
      snapshot.installedApps.map { app ->
        val rawOverride = prefs.categoryOverrides[app.packageName]
        val validOverrideId =
          rawOverride?.takeIf { id ->
            BuiltInCategory.fromId(id) != null || customById.containsKey(id)
          }

        val effectiveId = validOverrideId ?: app.autoCategory.id
        val effectiveName = resolveCategoryName(effectiveId, customById)
        val isOverridden = validOverrideId != null && validOverrideId != app.autoCategory.id
        val notifCount =
          if (navState.isNotificationAccessGranted) {
            snapshot.notifCounts[app.packageName] ?: 0
          } else {
            0
          }

        AppItem(
          packageName = app.packageName,
          label = app.label,
          autoCategory = app.autoCategory,
          effectiveCategoryId = effectiveId,
          effectiveCategoryName = effectiveName,
          isCategoryOverridden = isOverridden,
          isPinnedToDock = app.packageName in dockSet,
          notificationCount = notifCount,
          icon = app.icon,
        )
      }

    val (hiddenItems, nonHiddenItems) =
      allMappedItems.partition { it.packageName in prefs.hiddenPackages }

    val itemByPackage = nonHiddenItems.associateBy { it.packageName }
    val hiddenByPackage = hiddenItems.associateBy { it.packageName }
    val countsByCategory = nonHiddenItems.groupingBy { it.effectiveCategoryId }.eachCount()

    val chips = buildList {
      BuiltInCategory.assignableCategories.forEach { builtIn ->
        val count = countsByCategory[builtIn.id] ?: 0
        if (count > 0 || snapshot.selectedCategoryId == builtIn.id) {
          add(
            CategoryChipItem(
              id = builtIn.id,
              name = builtInLabel(builtIn),
              isCustom = false,
              builtIn = builtIn,
              appCount = count,
            )
          )
        }
      }

      prefs.customCategories.forEach { custom ->
        add(
          CategoryChipItem(
            id = custom.id,
            name = custom.name,
            isCustom = true,
            appCount = countsByCategory[custom.id] ?: 0,
          )
        )
      }
    }

    val defaultCategoryId = chips.firstOrNull()?.id ?: BuiltInCategory.UTILITIES.id
    val effectiveSelectedId =
      if (snapshot.selectedCategoryId.isNotEmpty() && chips.any { it.id == snapshot.selectedCategoryId }) {
        snapshot.selectedCategoryId
      } else {
        defaultCategoryId
      }

    val trimmedQuery = snapshot.searchQuery.trim()
    val filteredApps =
      if (trimmedQuery.isNotEmpty()) {
        nonHiddenItems.filter { app ->
          app.label.contains(trimmedQuery, ignoreCase = true) ||
            app.packageName.contains(trimmedQuery, ignoreCase = true)
        }
      } else {
        nonHiddenItems.filter { app -> app.effectiveCategoryId == effectiveSelectedId }
      }

    val dockApps = prefs.dockPackages.mapNotNull { pkg -> itemByPackage[pkg] }
    val selectedTitle =
      if (trimmedQuery.isNotEmpty()) {
        str(R.string.drawer_search_title)
      } else {
        chips.firstOrNull { it.id == effectiveSelectedId }?.name
          ?: builtInLabel(BuiltInCategory.UTILITIES)
      }

    val resolvedHiddenFlow =
      if (hiddenFlow is HiddenAppsFlowState.Unlocked) {
        HiddenAppsFlowState.Unlocked(
          menuHiddenApp = modals.hiddenMenuPackage?.let { hiddenByPackage[it] }
        )
      } else {
        hiddenFlow
      }

    val shouldShowNotifOnboarding =
      !navState.isNotificationAccessGranted && !prefs.notificationOnboardingDismissed

    val smartQueryTrimmed = asyncSearch.query.trim()
    val smartMatchingApps =
      if (smartQueryTrimmed.isEmpty()) {
        emptyList()
      } else {
        nonHiddenItems
          .filter { app ->
            app.label.contains(smartQueryTrimmed, ignoreCase = true) ||
              app.packageName.contains(smartQueryTrimmed, ignoreCase = true)
          }
          .sortedWith(
            compareByDescending<AppItem> { it.label.startsWith(smartQueryTrimmed, ignoreCase = true) }
              .thenBy { it.label.lowercase() }
          )
          .take(12)
      }

    val smartSearchState =
      SmartSearchState(
        query = asyncSearch.query,
        recentQueries = prefs.recentSearchQueries,
        matchingApps = smartMatchingApps,
        calculatorResult = MathExpressionEvaluator.evaluate(smartQueryTrimmed),
        quickActions = QuickActionMatcher.match(getApplication(), smartQueryTrimmed),
        contacts = asyncSearch.contacts,
        hasContactsPermission = asyncSearch.hasContactsPermission,
        contactsPermissionDeniedOnce = asyncSearch.contactsPermissionDeniedOnce,
        webSuggestions = asyncSearch.webSuggestions,
        isLoadingSuggestions = asyncSearch.isLoadingSuggestions,
      )

    val newsPersonalization = prefs.newsPersonalization
    val enabledSources = prefs.newsSources.filter { it.isEnabled }
    val enabledSourceIds = enabledSources.map { it.id }.toSet()
    val selectedContexts = newsPersonalization.selectedContexts
    val hasEnabledCustomContext = enabledSources.any { it.topic == NewsTopic.CUSTOM }
    val effectiveNewsTopic =
      when (val current = homeEnv.selectedNewsTopic) {
        NewsTopic.ALL -> NewsTopic.ALL
        NewsTopic.CUSTOM -> if (hasEnabledCustomContext) NewsTopic.CUSTOM else NewsTopic.ALL
        else ->
          if (selectedContexts.isEmpty() || current in selectedContexts) {
            current
          } else {
            NewsTopic.ALL
          }
      }
    val keywords = newsPersonalization.focusKeywords
    val filteredNewsArticles =
      newsCache.articles
        .asSequence()
        .filter { article ->
          article.sourceId in enabledSourceIds &&
            (selectedContexts.isEmpty() ||
              article.topic in selectedContexts ||
              article.topic == NewsTopic.CUSTOM) &&
            (effectiveNewsTopic == NewsTopic.ALL || article.topic == effectiveNewsTopic)
        }
        .map { article ->
          val matched =
            if (keywords.isEmpty()) {
              null
            } else {
              keywords.firstOrNull { kw ->
                article.title.contains(kw, ignoreCase = true) ||
                  article.summary.contains(kw, ignoreCase = true)
              }
            }
          if (matched != null) article.copy(matchedKeyword = matched) else article
        }
        .filter { article ->
          !newsPersonalization.strictKeywordFilter ||
            keywords.isEmpty() ||
            article.matchedKeyword != null
        }
        .sortedWith(
          compareByDescending<NewsArticle> { it.matchedKeyword != null }
            .thenByDescending { it.publishedAtEpochMs }
        )
        .toList()

    val resolvedIconGroupApps =
      if (prefs.smartWidgetsConfig.iconGroupPackages.isNotEmpty()) {
        prefs.smartWidgetsConfig.iconGroupPackages.mapNotNull { pkg -> itemByPackage[pkg] }
      } else {
        val nonDockApps = nonHiddenItems.filterNot { it.packageName in dockSet }
        (nonDockApps.ifEmpty { nonHiddenItems }).take(5)
      }

    return LauncherUiState(
      isLoading = loading,
      isDrawerOpen = navState.isDrawerOpen,
      isSearchExpanded = navState.isSearchExpanded,
      isSmartSearchOpen = navState.isSmartSearchOpen,
      isGestureSettingsOpen = navState.isGestureSettingsOpen,
      isThemeSettingsOpen = navState.isThemeSettingsOpen,
      showNotificationOnboarding = shouldShowNotifOnboarding,
      isNotificationAccessGranted = navState.isNotificationAccessGranted,
      isAccessibilityEnabled = navState.isAccessibilityEnabled,
      homeWidgetStyle = prefs.homeWidgetStyle,
      smartWidgetsConfig = prefs.smartWidgetsConfig,
      iconGroupApps = resolvedIconGroupApps,
      allVisibleApps = nonHiddenItems,
      calendarEvents = homeEnv.calendarEvents,
      hasCalendarPermission = homeEnv.hasCalendarPermission,
      isCalendarLoading = homeEnv.isCalendarLoading,
      mediaPlaybackState = homeEnv.mediaPlaybackState,
      gestureConfig = prefs.gestureConfig,
      themeConfig = prefs.themeConfig,
      wallpaperConfig = prefs.wallpaperConfig,
      customWallpaperBitmap = homeEnv.customWallpaperBitmap,
      weatherSnapshot = prefs.cachedWeather,
      weatherLocationCity = prefs.weatherLocation?.cityName ?: prefs.cachedWeather?.cityName.orEmpty(),
      temperatureUnit = prefs.temperatureUnit,
      isWeatherLoading = homeEnv.isWeatherLoading,
      hasLocationPermission = homeEnv.hasLocationPermission,
      hostedWidgets = prefs.hostedWidgets,
      editingWidgetId = homeEnv.editingWidgetId,
      showWidgetPickerSheet = modals.showWidgetPickerSheet,
      availableWidgetGroups = homeEnv.availableWidgetGroups,
      isWidgetPickerLoading = homeEnv.isWidgetPickerLoading,
      smartSearch = smartSearchState,
      selectedIconPackPackage = prefs.selectedIconPackPackage,
      activeIconPackLabel = homeEnv.activeIconPackLabel,
      showIconPackSheet = modals.showIconPackSheet,
      installedIconPacks = homeEnv.installedIconPacks,
      isIconPacksLoading = homeEnv.isIconPacksLoading,
      newsArticles = filteredNewsArticles,
      newsSources = prefs.newsSources,
      newsPersonalization = newsPersonalization,
      selectedNewsTopic = effectiveNewsTopic,
      newsLastUpdatedEpochMs = newsCache.lastUpdatedEpochMs,
      isNewsRefreshing = homeEnv.isNewsRefreshing,
      showNewsSourcesSheet = modals.showNewsSourcesSheet,
      newsAddSourceError = homeEnv.newsAddSourceError,
      showWidgetCustomizeSheet = modals.showWidgetCustomizeSheet,
      showNoteEditorDialog = modals.showNoteEditorDialog,
      showIconGroupPickerDialog = modals.showIconGroupPickerDialog,
      showWallpaperSheet = modals.showWallpaperSheet,
      showWeatherCityDialog = modals.showWeatherCityDialog,
      accessibilityPromptAction = modals.accessibilityPromptAction,
      visibleApps = filteredApps,
      hiddenApps = hiddenItems,
      dockApps = dockApps,
      categories = chips,
      customCategories = prefs.customCategories,
      selectedCategoryId = effectiveSelectedId,
      selectedCategoryTitle = selectedTitle,
      searchQuery = snapshot.searchQuery,
      totalVisibleAppCount = filteredApps.size,
      isPinConfigured = prefs.pinSecurity.isPinConfigured,
      hiddenAppsFlow = resolvedHiddenFlow,
      menuApp = modals.menuPackage?.let { itemByPackage[it] },
      moveCategoryApp = modals.moveCategoryPackage?.let { itemByPackage[it] },
      createCategoryState = modals.createCategoryState,
      manageCustomCategory = modals.manageCustomCategory,
      renameCategoryState = modals.renameCategoryState,
      deleteConfirmCategory = modals.deleteConfirmCategory,
    )
  }

  private fun resolveCategoryName(
    categoryId: String,
    customById: Map<String, CustomCategory>,
  ): String {
    customById[categoryId]?.let { return it.name }
    BuiltInCategory.fromId(categoryId)?.let { return builtInLabel(it) }
    return builtInLabel(BuiltInCategory.OTHER)
  }

  private fun str(resId: Int, vararg args: Any): String =
    getApplication<Application>().getString(resId, *args)

  private data class BaseSnapshot(
    val installedApps: List<InstalledApp>,
    val prefs: LauncherPreferencesState,
    val notifCounts: Map<String, Int>,
    val selectedCategoryId: String,
    val searchQuery: String,
  )

  private data class UiExtras(
    val isLoading: Boolean,
    val modals: ModalState,
    val hiddenFlow: HiddenAppsFlowState,
    val searchState: AsyncSearchState,
    val homeEnv: HomeEnvState,
  )

  companion object {
    private const val WEATHER_STALE_MS = 20 * 60 * 1_000L
    private const val NEWS_STALE_MS = 20 * 60 * 1_000L
    private const val KEY_SELECTED_CATEGORY = "selected_category_id"
    private const val KEY_SEARCH_QUERY = "search_query"
    private const val KEY_DRAWER_OPEN = "drawer_open"
    private const val KEY_SMART_SEARCH_OPEN = "smart_search_open"
    private const val KEY_SMART_SEARCH_QUERY = "smart_search_query"
    private const val KEY_GESTURE_SETTINGS_OPEN = "gesture_settings_open"
    private const val KEY_THEME_SETTINGS_OPEN = "theme_settings_open"

    val Factory: ViewModelProvider.Factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
          val application =
            checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
          val savedStateHandle = extras.createSavedStateHandle()
          val appContext = application.applicationContext
          val iconPackRepository = IconPackRepository(appContext)
          return LauncherViewModel(
            application = application,
            savedStateHandle = savedStateHandle,
            appRepository = AppRepository(appContext, iconPackRepository),
            iconPackRepository = iconPackRepository,
            preferencesRepository = LauncherPreferencesRepository(appContext),
            contactSearchRepository = ContactSearchRepository(appContext),
            webSuggestionRepository = WebSuggestionRepository(appContext),
            weatherRepository = WeatherRepository(appContext),
            wallpaperRepository = WallpaperRepository(appContext),
            newsFeedRepository = NewsFeedRepository(appContext),
            calendarRepository = CalendarRepository(appContext),
            mediaControlManager = MediaControlManager(appContext),
            widgetHostManager = LauncherWidgetHostManager.getInstance(appContext),
          )
            as T
        }
      }
  }
}
