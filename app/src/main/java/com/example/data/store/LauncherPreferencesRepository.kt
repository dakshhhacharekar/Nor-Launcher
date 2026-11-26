package com.example.data.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.BuiltInCategory
import com.example.data.model.CategoryMutationResult
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
import com.example.data.model.LauncherPreferencesState
import com.example.data.model.NewsFeedSource
import com.example.data.model.NewsPersonalization
import com.example.data.model.NewsSourceMutationResult
import com.example.data.model.NewsTopic
import com.example.data.model.PinSecurityState
import com.example.data.model.QuickNoteData
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
import com.example.data.security.HashedPin
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.norLauncherDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "nor_launcher_prefs_ds")

class LauncherPreferencesRepository(
  private val appContext: Context,
) {
  private val dataStore: DataStore<Preferences>
    get() = appContext.norLauncherDataStore

  val preferencesFlow: Flow<LauncherPreferencesState> =
    dataStore.data
      .catch { exception ->
        if (exception is IOException) {
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
        val pinHash = prefs[KEY_PIN_HASH]
        val pinSalt = prefs[KEY_PIN_SALT]
        val pinIterations = prefs[KEY_PIN_ITERATIONS] ?: 0
        val storedPin =
          if (!pinHash.isNullOrBlank() && !pinSalt.isNullOrBlank() && pinIterations > 0) {
            HashedPin(hashBase64 = pinHash, saltBase64 = pinSalt, iterations = pinIterations)
          } else {
            null
          }

        LauncherPreferencesState(
          categoryOverrides = decodeOverrides(prefs[KEY_CATEGORY_OVERRIDES]),
          customCategories = decodeCustomCategories(prefs[KEY_CUSTOM_CATEGORIES]),
          hiddenPackages = prefs[KEY_HIDDEN_PACKAGES] ?: emptySet(),
          dockPackages = decodeDockPackages(prefs[KEY_DOCK_PACKAGES]),
          pinSecurity =
            PinSecurityState(
              hashedPin = storedPin,
              failedAttempts = prefs[KEY_PIN_FAILED_ATTEMPTS] ?: 0,
              lockoutUntilEpochMs = prefs[KEY_PIN_LOCKOUT_UNTIL] ?: 0L,
            ),
          notificationOnboardingDismissed = prefs[KEY_NOTIF_ONBOARDING_DISMISSED] ?: false,
          homeAppsSeeded = prefs[KEY_HOME_APPS_SEEDED] ?: false,
          homeWidgetStyle = HomeWidgetStyle.fromId(prefs[KEY_HOME_WIDGET_STYLE]),
          gestureConfig =
            GestureConfig(
              swipeUp = GestureAction.fromId(prefs[KEY_GESTURE_SWIPE_UP], GestureAction.OPEN_DRAWER),
              swipeDown =
                GestureAction.fromId(prefs[KEY_GESTURE_SWIPE_DOWN], GestureAction.OPEN_NOTIFICATIONS),
              doubleTap =
                GestureAction.fromId(prefs[KEY_GESTURE_DOUBLE_TAP], GestureAction.LOCK_SCREEN),
            ),
          themeConfig =
            ThemeConfig(
              colorSource = ThemeColorSource.fromId(prefs[KEY_THEME_COLOR_SOURCE]),
              themeMode = ThemeModeOption.fromId(prefs[KEY_THEME_MODE]),
              fontChoice = FontChoice.fromId(prefs[KEY_THEME_FONT]),
              iconShape = IconShapeOption.fromId(prefs[KEY_THEME_ICON_SHAPE]),
              hideSystemBars = prefs[KEY_THEME_HIDE_SYSTEM_BARS] ?: false,
            ),
          wallpaperConfig =
            WallpaperConfig(
              preset = WallpaperPreset.fromId(prefs[KEY_WALLPAPER_PRESET]),
              hasCustomPhoto = prefs[KEY_WALLPAPER_HAS_CUSTOM] ?: false,
              dimPercent = (prefs[KEY_WALLPAPER_DIM_PERCENT] ?: 20).coerceIn(0, 60),
              photoVersion = prefs[KEY_WALLPAPER_PHOTO_VERSION] ?: 0L,
            ),
          weatherLocation = decodeWeatherLocation(prefs[KEY_WEATHER_LOCATION_JSON]),
          temperatureUnit = TemperatureUnit.fromId(prefs[KEY_WEATHER_UNIT]),
          cachedWeather = decodeWeatherSnapshot(prefs[KEY_WEATHER_CACHE_JSON]),
          hostedWidgets = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON]),
          selectedIconPackPackage = prefs[KEY_ICON_PACK_PACKAGE]?.takeIf { it.isNotBlank() },
          newsSources =
            resolveNewsSources(
              enabledSet = prefs[KEY_NEWS_ENABLED_SOURCES],
              customJson = prefs[KEY_NEWS_CUSTOM_SOURCES_JSON],
            ),
          newsPersonalization =
            NewsPersonalization(
              isCompleted = prefs[KEY_NEWS_PERSONALIZED] ?: false,
              selectedContexts =
                (prefs[KEY_NEWS_CONTEXTS] ?: emptySet())
                  .map { NewsTopic.fromId(it) }
                  .filter { it != NewsTopic.ALL }
                  .toSet(),
              focusKeywords = decodeStringList(prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON]),
              strictKeywordFilter = prefs[KEY_NEWS_STRICT_KEYWORDS] ?: false,
            ),
          recentSearchQueries = decodeStringList(prefs[KEY_RECENT_SEARCH_QUERIES_JSON]),
          smartWidgetsConfig =
            SmartWidgetsConfig(
              stackWidgets =
                decodeSmartWidgetList(
                  json = prefs[KEY_SMART_STACK_WIDGETS_JSON],
                  fallback = SmartWidgetType.defaultStackOrder,
                ),
              pinnedWidgets =
                decodeSmartWidgetList(
                  json = prefs[KEY_SMART_PINNED_WIDGETS_JSON],
                  fallback = emptyList(),
                ),
              quickNote =
                QuickNoteData(
                  title = prefs[KEY_QUICK_NOTE_TITLE].orEmpty(),
                  body = prefs[KEY_QUICK_NOTE_BODY].orEmpty(),
                  updatedAtEpochMs = prefs[KEY_QUICK_NOTE_UPDATED_MS] ?: 0L,
                ),
              iconGroupPackages = decodeStringList(prefs[KEY_ICON_GROUP_PACKAGES_JSON]),
            ),
        )
      }

  /**
   * One-time migration from the base SharedPreferences favorites dock into DataStore.
   * Preserves previously pinned apps without failing if SharedPreferences is empty.
   */
  suspend fun migrateLegacyDockIfNeeded() {
    dataStore.edit { prefs ->
      if (prefs[KEY_LEGACY_MIGRATED] == true) return@edit

      val existingDock = decodeDockPackages(prefs[KEY_DOCK_PACKAGES])
      if (existingDock.isEmpty()) {
        val legacyPrefs =
          appContext.getSharedPreferences(LEGACY_SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        val legacySet = legacyPrefs.getStringSet(LEGACY_DOCK_KEY, null)
        if (!legacySet.isNullOrEmpty()) {
          val migrated = legacySet.take(MAX_DOCK_APPS)
          prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(migrated)
        }
      }
      prefs[KEY_LEGACY_MIGRATED] = true
    }
  }

  suspend fun setCategoryOverride(packageName: String, categoryId: String) {
    dataStore.edit { prefs ->
      val current = decodeOverrides(prefs[KEY_CATEGORY_OVERRIDES]).toMutableMap()
      current[packageName] = categoryId
      prefs[KEY_CATEGORY_OVERRIDES] = encodeOverrides(current)
    }
  }

  suspend fun clearCategoryOverride(packageName: String) {
    dataStore.edit { prefs ->
      val current = decodeOverrides(prefs[KEY_CATEGORY_OVERRIDES]).toMutableMap()
      if (current.remove(packageName) != null) {
        prefs[KEY_CATEGORY_OVERRIDES] = encodeOverrides(current)
      }
    }
  }

  suspend fun createCustomCategory(
    rawName: String,
    builtInDisplayNames: Set<String>,
  ): CategoryMutationResult {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return CategoryMutationResult.EmptyName

    var result: CategoryMutationResult = CategoryMutationResult.EmptyName
    dataStore.edit { prefs ->
      val existing = decodeCustomCategories(prefs[KEY_CUSTOM_CATEGORIES])
      val isDuplicate =
        existing.any { it.name.equals(trimmed, ignoreCase = true) } ||
          builtInDisplayNames.any { it.equals(trimmed, ignoreCase = true) }

      if (isDuplicate) {
        result = CategoryMutationResult.DuplicateName
        return@edit
      }

      val created =
        CustomCategory(
          id = "$CUSTOM_CATEGORY_PREFIX${UUID.randomUUID()}",
          name = trimmed,
        )
      prefs[KEY_CUSTOM_CATEGORIES] = encodeCustomCategories(existing + created)
      result = CategoryMutationResult.Success(created)
    }
    return result
  }

  suspend fun renameCustomCategory(
    categoryId: String,
    rawName: String,
    builtInDisplayNames: Set<String>,
  ): CategoryMutationResult {
    val trimmed = rawName.trim()
    if (trimmed.isEmpty()) return CategoryMutationResult.EmptyName

    var result: CategoryMutationResult = CategoryMutationResult.EmptyName
    dataStore.edit { prefs ->
      val existing = decodeCustomCategories(prefs[KEY_CUSTOM_CATEGORIES])
      val isDuplicate =
        existing.any { it.id != categoryId && it.name.equals(trimmed, ignoreCase = true) } ||
          builtInDisplayNames.any { it.equals(trimmed, ignoreCase = true) }

      if (isDuplicate) {
        result = CategoryMutationResult.DuplicateName
        return@edit
      }

      val updatedCategory = CustomCategory(id = categoryId, name = trimmed)
      val updatedList = existing.map { if (it.id == categoryId) updatedCategory else it }
      prefs[KEY_CUSTOM_CATEGORIES] = encodeCustomCategories(updatedList)
      result = CategoryMutationResult.Success(updatedCategory)
    }
    return result
  }

  suspend fun deleteCustomCategory(categoryId: String) {
    dataStore.edit { prefs ->
      val remainingCategories =
        decodeCustomCategories(prefs[KEY_CUSTOM_CATEGORIES]).filterNot { it.id == categoryId }
      prefs[KEY_CUSTOM_CATEGORIES] = encodeCustomCategories(remainingCategories)

      // Clear any app overrides referencing the deleted custom category so they return to auto.
      val remainingOverrides =
        decodeOverrides(prefs[KEY_CATEGORY_OVERRIDES]).filterValues { it != categoryId }
      prefs[KEY_CATEGORY_OVERRIDES] = encodeOverrides(remainingOverrides)
    }
  }

  suspend fun hideApp(packageName: String) {
    dataStore.edit { prefs ->
      val currentHidden = prefs[KEY_HIDDEN_PACKAGES] ?: emptySet()
      prefs[KEY_HIDDEN_PACKAGES] = currentHidden + packageName

      // Also remove hidden apps from the favorites dock so they stay genuinely hidden.
      val currentDock = decodeDockPackages(prefs[KEY_DOCK_PACKAGES])
      if (packageName in currentDock) {
        prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(currentDock.filterNot { it == packageName })
      }
    }
  }

  suspend fun unhideApp(packageName: String) {
    dataStore.edit { prefs ->
      val currentHidden = prefs[KEY_HIDDEN_PACKAGES] ?: emptySet()
      if (packageName in currentHidden) {
        prefs[KEY_HIDDEN_PACKAGES] = currentHidden - packageName
      }
    }
  }

  suspend fun toggleDockPin(packageName: String): DockPinResult {
    var result: DockPinResult = DockPinResult.Unpinned
    dataStore.edit { prefs ->
      val currentDock = decodeDockPackages(prefs[KEY_DOCK_PACKAGES]).toMutableList()
      if (currentDock.remove(packageName)) {
        prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(currentDock)
        result = DockPinResult.Unpinned
      } else if (currentDock.size >= MAX_DOCK_APPS) {
        result = DockPinResult.DockFull
      } else {
        currentDock.add(packageName)
        prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(currentDock)
        result = DockPinResult.Pinned
      }
    }
    return result
  }

  suspend fun saveHashedPin(hashedPin: HashedPin) {
    dataStore.edit { prefs ->
      prefs[KEY_PIN_HASH] = hashedPin.hashBase64
      prefs[KEY_PIN_SALT] = hashedPin.saltBase64
      prefs[KEY_PIN_ITERATIONS] = hashedPin.iterations
      prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
      prefs[KEY_PIN_LOCKOUT_UNTIL] = 0L
    }
  }

  /**
   * Increments failed PIN counter and engages a 30-second lockout every 5 failures.
   * Returns Pair(remainingAttemptsBeforeLockout, lockoutUntilEpochMs).
   */
  suspend fun recordFailedPinAttempt(nowEpochMs: Long): Pair<Int, Long> {
    var remaining = MAX_PIN_ATTEMPTS
    var lockoutUntil = 0L
    dataStore.edit { prefs ->
      val nextCount = (prefs[KEY_PIN_FAILED_ATTEMPTS] ?: 0) + 1
      if (nextCount >= MAX_PIN_ATTEMPTS) {
        lockoutUntil = nowEpochMs + PIN_LOCKOUT_DURATION_MS
        prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
        prefs[KEY_PIN_LOCKOUT_UNTIL] = lockoutUntil
        remaining = 0
      } else {
        prefs[KEY_PIN_FAILED_ATTEMPTS] = nextCount
        remaining = MAX_PIN_ATTEMPTS - nextCount
      }
    }
    return remaining to lockoutUntil
  }

  suspend fun resetPinFailures() {
    dataStore.edit { prefs ->
      prefs[KEY_PIN_FAILED_ATTEMPTS] = 0
      prefs[KEY_PIN_LOCKOUT_UNTIL] = 0L
    }
  }

  suspend fun dismissNotificationOnboarding() {
    dataStore.edit { prefs -> prefs[KEY_NOTIF_ONBOARDING_DISMISSED] = true }
  }

  suspend fun setHomeWidgetStyle(style: HomeWidgetStyle) {
    dataStore.edit { prefs -> prefs[KEY_HOME_WIDGET_STYLE] = style.id }
  }

  suspend fun setGestureAction(gesture: HomeGesture, action: GestureAction) {
    dataStore.edit { prefs ->
      val key =
        when (gesture) {
          HomeGesture.SWIPE_UP -> KEY_GESTURE_SWIPE_UP
          HomeGesture.SWIPE_DOWN -> KEY_GESTURE_SWIPE_DOWN
          HomeGesture.DOUBLE_TAP -> KEY_GESTURE_DOUBLE_TAP
        }
      prefs[key] = action.id
    }
  }

  suspend fun resetGestureDefaults() {
    val defaults = GestureConfig()
    dataStore.edit { prefs ->
      prefs[KEY_GESTURE_SWIPE_UP] = defaults.swipeUp.id
      prefs[KEY_GESTURE_SWIPE_DOWN] = defaults.swipeDown.id
      prefs[KEY_GESTURE_DOUBLE_TAP] = defaults.doubleTap.id
    }
  }

  suspend fun setThemeColorSource(source: ThemeColorSource) {
    dataStore.edit { prefs -> prefs[KEY_THEME_COLOR_SOURCE] = source.id }
  }

  suspend fun setThemeMode(mode: ThemeModeOption) {
    dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.id }
  }

  suspend fun setFontChoice(font: FontChoice) {
    dataStore.edit { prefs -> prefs[KEY_THEME_FONT] = font.id }
  }

  suspend fun setIconShape(shape: IconShapeOption) {
    dataStore.edit { prefs -> prefs[KEY_THEME_ICON_SHAPE] = shape.id }
  }

  suspend fun setHideSystemBars(hide: Boolean) {
    dataStore.edit { prefs -> prefs[KEY_THEME_HIDE_SYSTEM_BARS] = hide }
  }

  suspend fun resetThemeDefaults() {
    val defaults = ThemeConfig()
    dataStore.edit { prefs ->
      prefs[KEY_THEME_COLOR_SOURCE] = defaults.colorSource.id
      prefs[KEY_THEME_MODE] = defaults.themeMode.id
      prefs[KEY_THEME_FONT] = defaults.fontChoice.id
      prefs[KEY_THEME_ICON_SHAPE] = defaults.iconShape.id
      prefs[KEY_THEME_HIDE_SYSTEM_BARS] = defaults.hideSystemBars
      prefs[KEY_ICON_PACK_PACKAGE] = ""
    }
  }

  suspend fun setSelectedIconPack(packageName: String?) {
    dataStore.edit { prefs ->
      prefs[KEY_ICON_PACK_PACKAGE] = packageName?.trim().orEmpty()
    }
  }

  suspend fun setWallpaperPreset(preset: WallpaperPreset) {
    dataStore.edit { prefs -> prefs[KEY_WALLPAPER_PRESET] = preset.id }
  }

  suspend fun setCustomWallpaperSelected(versionEpochMs: Long) {
    dataStore.edit { prefs ->
      prefs[KEY_WALLPAPER_HAS_CUSTOM] = true
      prefs[KEY_WALLPAPER_PRESET] = WallpaperPreset.CUSTOM_PHOTO.id
      prefs[KEY_WALLPAPER_PHOTO_VERSION] = versionEpochMs
    }
  }

  suspend fun setWallpaperDimPercent(dimPercent: Int) {
    dataStore.edit { prefs -> prefs[KEY_WALLPAPER_DIM_PERCENT] = dimPercent.coerceIn(0, 60) }
  }

  suspend fun saveWeatherLocation(location: WeatherLocation) {
    dataStore.edit { prefs ->
      prefs[KEY_WEATHER_LOCATION_JSON] = encodeWeatherLocation(location)
    }
  }

  suspend fun saveWeatherSnapshot(snapshot: WeatherSnapshot) {
    dataStore.edit { prefs ->
      prefs[KEY_WEATHER_CACHE_JSON] = encodeWeatherSnapshot(snapshot)
    }
  }

  suspend fun toggleTemperatureUnit() {
    dataStore.edit { prefs ->
      val current = TemperatureUnit.fromId(prefs[KEY_WEATHER_UNIT])
      val next =
        if (current == TemperatureUnit.CELSIUS) TemperatureUnit.FAHRENHEIT
        else TemperatureUnit.CELSIUS
      prefs[KEY_WEATHER_UNIT] = next.id
    }
  }

  suspend fun addHostedWidget(record: HostedWidgetRecord) {
    dataStore.edit { prefs ->
      val current = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON]).toMutableList()
      current.removeAll { it.appWidgetId == record.appWidgetId }
      current.add(record)
      prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(current)
    }
  }

  suspend fun updateHostedWidgetSize(
    appWidgetId: Int,
    widthSpan: Int,
    heightDp: Int,
  ) {
    val safeSpan =
      widthSpan.coerceIn(HostedWidgetRecord.MIN_WIDTH_SPAN, HostedWidgetRecord.MAX_WIDTH_SPAN)
    val safeHeight =
      heightDp.coerceIn(HostedWidgetRecord.MIN_HEIGHT_DP, HostedWidgetRecord.MAX_HEIGHT_DP)
    dataStore.edit { prefs ->
      val current = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON])
      val updated =
        current.map { item ->
          if (item.appWidgetId == appWidgetId) {
            item.copy(widthSpan = safeSpan, heightDp = safeHeight)
          } else {
            item
          }
        }
      prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(updated)
    }
  }

  suspend fun moveHostedWidget(appWidgetId: Int, delta: Int) {
    dataStore.edit { prefs ->
      val list = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON]).toMutableList()
      val idx = list.indexOfFirst { it.appWidgetId == appWidgetId }
      if (idx == -1) return@edit
      val target = (idx + delta).coerceIn(0, list.lastIndex)
      if (target == idx) return@edit
      val item = list.removeAt(idx)
      list.add(target, item)
      prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(list)
    }
  }

  suspend fun removeHostedWidget(appWidgetId: Int) {
    dataStore.edit { prefs ->
      val current = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON])
      val remaining = current.filterNot { it.appWidgetId == appWidgetId }
      if (remaining.size != current.size) {
        prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(remaining)
      }
    }
  }

  suspend fun pruneMissingWidgetIds(invalidWidgetIds: Set<Int>) {
    if (invalidWidgetIds.isEmpty()) return
    dataStore.edit { prefs ->
      val current = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON])
      val remaining = current.filterNot { it.appWidgetId in invalidWidgetIds }
      if (remaining.size != current.size) {
        prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(remaining)
      }
    }
  }

  /**
   * One-time initial home screen seeding so a fresh install immediately displays up to 8
   * commonly used apps on the home screen (which the user can unpin or customize anytime).
   */
  suspend fun seedInitialDockIfNeeded(candidatePackages: List<String>) {
    if (candidatePackages.isEmpty()) return
    dataStore.edit { prefs ->
      if (prefs[KEY_HOME_APPS_SEEDED] == true) return@edit
      val existing = decodeDockPackages(prefs[KEY_DOCK_PACKAGES])
      if (existing.isEmpty()) {
        prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(candidatePackages.take(INITIAL_HOME_APP_COUNT))
      }
      prefs[KEY_HOME_APPS_SEEDED] = true
    }
  }

  /**
   * Removes persisted references to an uninstalled package, returning any hosted widget IDs
   * belonging to that package so the caller can release them via AppWidgetHost.deleteAppWidgetId.
   * Must NOT be called when EXTRA_REPLACING is true (during app updates).
   */
  suspend fun pruneRemovedPackage(packageName: String): List<Int> {
    val removedWidgetIds = mutableListOf<Int>()
    dataStore.edit { prefs ->
      val overrides = decodeOverrides(prefs[KEY_CATEGORY_OVERRIDES]).toMutableMap()
      if (overrides.remove(packageName) != null) {
        prefs[KEY_CATEGORY_OVERRIDES] = encodeOverrides(overrides)
      }

      val dock = decodeDockPackages(prefs[KEY_DOCK_PACKAGES])
      if (packageName in dock) {
        prefs[KEY_DOCK_PACKAGES] = encodeDockPackages(dock.filterNot { it == packageName })
      }

      val hidden = prefs[KEY_HIDDEN_PACKAGES] ?: emptySet()
      if (packageName in hidden) {
        prefs[KEY_HIDDEN_PACKAGES] = hidden - packageName
      }

      val widgets = decodeHostedWidgets(prefs[KEY_HOSTED_WIDGETS_JSON])
      val (matching, keeping) = widgets.partition { it.packageName == packageName }
      if (matching.isNotEmpty()) {
        removedWidgetIds.addAll(matching.map { it.appWidgetId })
        prefs[KEY_HOSTED_WIDGETS_JSON] = encodeHostedWidgets(keeping)
      }

      if (prefs[KEY_ICON_PACK_PACKAGE] == packageName) {
        prefs[KEY_ICON_PACK_PACKAGE] = ""
      }
    }
    return removedWidgetIds
  }

  private fun decodeOverrides(raw: String?): Map<String, String> {
    if (raw.isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(raw)
        buildMap {
          val keys = json.keys()
          while (keys.hasNext()) {
            val pkg = keys.next()
            val catId = json.optString(pkg)
            if (pkg.isNotBlank() && catId.isNotBlank() && catId != "all") {
              put(pkg, catId)
            }
          }
        }
      }
      .getOrDefault(emptyMap())
  }

  private fun encodeOverrides(overrides: Map<String, String>): String {
    val json = JSONObject()
    overrides.forEach { (pkg, catId) -> json.put(pkg, catId) }
    return json.toString()
  }

  private fun decodeCustomCategories(raw: String?): List<CustomCategory> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) {
          for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val name = obj.optString("name")
            if (id.isNotBlank() && name.isNotBlank()) {
              add(CustomCategory(id = id, name = name))
            }
          }
        }
      }
      .getOrDefault(emptyList())
  }

  private fun encodeCustomCategories(categories: List<CustomCategory>): String {
    val array = JSONArray()
    categories.forEach { category ->
      array.put(
        JSONObject().apply {
          put("id", category.id)
          put("name", category.name)
        }
      )
    }
    return array.toString()
  }

  private fun decodeDockPackages(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) {
          for (i in 0 until array.length()) {
            val pkg = array.optString(i)
            if (pkg.isNotBlank() && !contains(pkg)) add(pkg)
          }
        }
      }
      .getOrDefault(emptyList())
  }

  private fun encodeDockPackages(packages: List<String>): String {
    val array = JSONArray()
    packages.distinct().take(MAX_DOCK_APPS).forEach { array.put(it) }
    return array.toString()
  }

  private fun encodeWeatherLocation(location: WeatherLocation): String =
    JSONObject().apply {
      put("city", location.cityName)
      put("lat", location.latitude)
      put("lon", location.longitude)
    }.toString()

  private fun decodeWeatherLocation(raw: String?): WeatherLocation? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
      val obj = JSONObject(raw)
      val city = obj.optString("city").trim()
      if (city.isEmpty()) return null
      WeatherLocation(
        cityName = city,
        latitude = obj.getDouble("lat"),
        longitude = obj.getDouble("lon"),
      )
    }.getOrNull()
  }

  private fun encodeWeatherSnapshot(snapshot: WeatherSnapshot): String =
    JSONObject().apply {
      put("city", snapshot.cityName)
      put("temp", snapshot.temperatureC)
      put("feels", snapshot.feelsLikeC)
      put("high", snapshot.highC)
      put("low", snapshot.lowC)
      put("humidity", snapshot.humidityPercent)
      put("wind", snapshot.windKmh)
      put("code", snapshot.weatherCode)
      put("isDay", snapshot.isDay)
      put("updatedAt", snapshot.updatedAtEpochMs)
    }.toString()

  private fun decodeWeatherSnapshot(raw: String?): WeatherSnapshot? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
      val obj = JSONObject(raw)
      WeatherSnapshot(
        cityName = obj.optString("city"),
        temperatureC = obj.getDouble("temp"),
        feelsLikeC = obj.optDouble("feels", obj.getDouble("temp")),
        highC = obj.optDouble("high", obj.getDouble("temp")),
        lowC = obj.optDouble("low", obj.getDouble("temp")),
        humidityPercent = obj.optInt("humidity", 0),
        windKmh = obj.optInt("wind", 0),
        weatherCode = obj.optInt("code", 0),
        isDay = obj.optBoolean("isDay", true),
        updatedAtEpochMs = obj.optLong("updatedAt", 0L),
      )
    }.getOrNull()
  }

  private fun decodeHostedWidgets(raw: String?): List<HostedWidgetRecord> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(raw)
      buildList(array.length()) {
        for (i in 0 until array.length()) {
          val obj = array.optJSONObject(i) ?: continue
          val id = obj.optInt("id", -1)
          val component = obj.optString("component").trim()
          if (id <= 0 || component.isEmpty()) continue
          val label = obj.optString("label").ifBlank { component.substringAfterLast('.') }
          val widthSpan =
            obj.optInt("widthSpan", HostedWidgetRecord.DEFAULT_WIDTH_SPAN)
              .coerceIn(HostedWidgetRecord.MIN_WIDTH_SPAN, HostedWidgetRecord.MAX_WIDTH_SPAN)
          val heightDp =
            obj.optInt("heightDp", HostedWidgetRecord.DEFAULT_HEIGHT_DP)
              .coerceIn(HostedWidgetRecord.MIN_HEIGHT_DP, HostedWidgetRecord.MAX_HEIGHT_DP)
          add(
            HostedWidgetRecord(
              appWidgetId = id,
              providerComponent = component,
              label = label,
              widthSpan = widthSpan,
              heightDp = heightDp,
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeHostedWidgets(widgets: List<HostedWidgetRecord>): String {
    val array = JSONArray()
    widgets.forEach { w ->
      array.put(
        JSONObject().apply {
          put("id", w.appWidgetId)
          put("component", w.providerComponent)
          put("label", w.label)
          put("widthSpan", w.widthSpan)
          put("heightDp", w.heightDp)
        }
      )
    }
    return array.toString()
  }

  suspend fun saveNewsPersonalization(
    selectedContexts: Set<NewsTopic>,
    enabledChannelIds: Set<String>,
    focusKeywords: List<String>,
    strictKeywordFilter: Boolean = false,
  ) {
    val cleanContexts =
      selectedContexts.filter { it != NewsTopic.ALL }.map { it.id }.toSet()
    val cleanKeywords =
      focusKeywords
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }
        .take(MAX_FOCUS_KEYWORDS)

    dataStore.edit { prefs ->
      prefs[KEY_NEWS_PERSONALIZED] = true
      prefs[KEY_NEWS_CONTEXTS] = cleanContexts
      prefs[KEY_NEWS_ENABLED_SOURCES] = enabledChannelIds
      prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON] = encodeStringList(cleanKeywords)
      prefs[KEY_NEWS_STRICT_KEYWORDS] = strictKeywordFilter
    }
  }

  suspend fun toggleNewsContext(topic: NewsTopic, enabled: Boolean) {
    if (topic == NewsTopic.ALL) return
    dataStore.edit { prefs ->
      val current = (prefs[KEY_NEWS_CONTEXTS] ?: emptySet()).toMutableSet()
      if (enabled) {
        current.add(topic.id)
      } else if (current.size > 1) {
        // Keep at least one context selected so the feed stays meaningful
        current.remove(topic.id)
      }
      prefs[KEY_NEWS_CONTEXTS] = current
    }
  }

  suspend fun addNewsFocusKeyword(rawKeyword: String) {
    val trimmed = rawKeyword.trim()
    if (trimmed.length < 2) return
    dataStore.edit { prefs ->
      val current = decodeStringList(prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON]).toMutableList()
      if (current.none { it.equals(trimmed, ignoreCase = true) } && current.size < MAX_FOCUS_KEYWORDS) {
        current.add(trimmed)
        prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON] = encodeStringList(current)
      }
    }
  }

  suspend fun removeNewsFocusKeyword(keyword: String) {
    dataStore.edit { prefs ->
      val current = decodeStringList(prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON])
      val updated = current.filterNot { it.equals(keyword, ignoreCase = true) }
      prefs[KEY_NEWS_FOCUS_KEYWORDS_JSON] = encodeStringList(updated)
      if (updated.isEmpty()) {
        prefs[KEY_NEWS_STRICT_KEYWORDS] = false
      }
    }
  }

  suspend fun setStrictNewsKeywordFilter(strict: Boolean) {
    dataStore.edit { prefs -> prefs[KEY_NEWS_STRICT_KEYWORDS] = strict }
  }

  suspend fun toggleNewsSource(sourceId: String, enabled: Boolean) {
    dataStore.edit { prefs ->
      val currentEnabled =
        prefs[KEY_NEWS_ENABLED_SOURCES]?.toMutableSet()
          ?: buildDefaultEnabledIds(decodeCustomNewsSources(prefs[KEY_NEWS_CUSTOM_SOURCES_JSON]))
      if (enabled) {
        currentEnabled.add(sourceId)
      } else {
        currentEnabled.remove(sourceId)
      }
      prefs[KEY_NEWS_ENABLED_SOURCES] = currentEnabled
    }
  }

  suspend fun addCustomNewsSource(
    rawName: String,
    rawUrl: String,
    topic: NewsTopic = NewsTopic.CUSTOM,
  ): NewsSourceMutationResult {
    val trimmedName = rawName.trim()
    if (trimmedName.isEmpty()) return NewsSourceMutationResult.EmptyName

    val normalizedUrl = normalizeFeedUrl(rawUrl) ?: return NewsSourceMutationResult.InvalidUrl
    val resolvedTopic = if (topic == NewsTopic.ALL) NewsTopic.CUSTOM else topic

    var result: NewsSourceMutationResult = NewsSourceMutationResult.InvalidUrl
    dataStore.edit { prefs ->
      val customSources = decodeCustomNewsSources(prefs[KEY_NEWS_CUSTOM_SOURCES_JSON])
      val allUrls =
        DefaultNewsSources.builtIn.map { it.feedUrl.lowercase() } +
          customSources.map { it.feedUrl.lowercase() }
      if (normalizedUrl.lowercase() in allUrls) {
        result = NewsSourceMutationResult.DuplicateUrl
        return@edit
      }

      val created =
        NewsFeedSource(
          id = "custom_rss_${UUID.randomUUID()}",
          name = trimmedName,
          feedUrl = normalizedUrl,
          topic = resolvedTopic,
          isEnabled = true,
          isCustom = true,
        )
      val updatedCustom = customSources + created
      prefs[KEY_NEWS_CUSTOM_SOURCES_JSON] = encodeCustomNewsSources(updatedCustom)

      val enabled =
        prefs[KEY_NEWS_ENABLED_SOURCES]?.toMutableSet()
          ?: buildDefaultEnabledIds(customSources)
      enabled.add(created.id)
      prefs[KEY_NEWS_ENABLED_SOURCES] = enabled

      // Automatically include this channel's context if personalization is active
      if (prefs[KEY_NEWS_PERSONALIZED] == true && resolvedTopic != NewsTopic.CUSTOM) {
        val contexts = (prefs[KEY_NEWS_CONTEXTS] ?: emptySet()).toMutableSet()
        contexts.add(resolvedTopic.id)
        prefs[KEY_NEWS_CONTEXTS] = contexts
      }

      result = NewsSourceMutationResult.Success(created)
    }
    return result
  }

  suspend fun removeCustomNewsSource(sourceId: String) {
    dataStore.edit { prefs ->
      val customSources = decodeCustomNewsSources(prefs[KEY_NEWS_CUSTOM_SOURCES_JSON])
      val filtered = customSources.filterNot { it.id == sourceId }
      if (filtered.size != customSources.size) {
        prefs[KEY_NEWS_CUSTOM_SOURCES_JSON] = encodeCustomNewsSources(filtered)
      }
      val enabled = prefs[KEY_NEWS_ENABLED_SOURCES]?.toMutableSet()
      if (enabled != null && enabled.remove(sourceId)) {
        prefs[KEY_NEWS_ENABLED_SOURCES] = enabled
      }
    }
  }

  suspend fun recordRecentSearchQuery(rawQuery: String) {
    val trimmed = rawQuery.trim().take(MAX_RECENT_SEARCH_LENGTH)
    if (trimmed.isEmpty()) return
    dataStore.edit { prefs ->
      val current = decodeStringList(prefs[KEY_RECENT_SEARCH_QUERIES_JSON])
      val updated =
        buildList(MAX_RECENT_SEARCHES) {
          add(trimmed)
          current.forEach { existing ->
            if (!existing.equals(trimmed, ignoreCase = true) && size < MAX_RECENT_SEARCHES) {
              add(existing)
            }
          }
        }
      prefs[KEY_RECENT_SEARCH_QUERIES_JSON] = encodeStringList(updated)
    }
  }

  suspend fun removeRecentSearchQuery(query: String) {
    dataStore.edit { prefs ->
      val current = decodeStringList(prefs[KEY_RECENT_SEARCH_QUERIES_JSON])
      val updated = current.filterNot { it.equals(query, ignoreCase = true) }
      if (updated.size != current.size) {
        prefs[KEY_RECENT_SEARCH_QUERIES_JSON] = encodeStringList(updated)
      }
    }
  }

  suspend fun clearRecentSearchQueries() {
    dataStore.edit { prefs ->
      prefs.remove(KEY_RECENT_SEARCH_QUERIES_JSON)
    }
  }

  suspend fun toggleStackWidget(type: SmartWidgetType, enabled: Boolean) {
    dataStore.edit { prefs ->
      val current =
        decodeSmartWidgetList(
          json = prefs[KEY_SMART_STACK_WIDGETS_JSON],
          fallback = SmartWidgetType.defaultStackOrder,
        ).toMutableList()
      if (enabled) {
        if (type !in current) {
          current.add(type)
        }
      } else if (current.size > 1) {
        current.remove(type)
      }
      prefs[KEY_SMART_STACK_WIDGETS_JSON] = encodeStringList(current.map { it.id })
    }
  }

  suspend fun moveStackWidget(type: SmartWidgetType, delta: Int) {
    dataStore.edit { prefs ->
      val current =
        decodeSmartWidgetList(
          json = prefs[KEY_SMART_STACK_WIDGETS_JSON],
          fallback = SmartWidgetType.defaultStackOrder,
        ).toMutableList()
      val idx = current.indexOf(type)
      if (idx < 0) return@edit
      val target = (idx + delta).coerceIn(0, current.lastIndex)
      if (target == idx) return@edit
      val item = current.removeAt(idx)
      current.add(target, item)
      prefs[KEY_SMART_STACK_WIDGETS_JSON] = encodeStringList(current.map { it.id })
    }
  }

  suspend fun togglePinnedSmartWidget(type: SmartWidgetType, pinned: Boolean): Boolean {
    var applied = true
    dataStore.edit { prefs ->
      val current =
        decodeSmartWidgetList(
          json = prefs[KEY_SMART_PINNED_WIDGETS_JSON],
          fallback = emptyList(),
        ).toMutableList()
      if (pinned) {
        if (type in current) return@edit
        if (current.size >= MAX_PINNED_SMART_WIDGETS) {
          applied = false
          return@edit
        }
        current.add(type)
      } else {
        current.remove(type)
      }
      prefs[KEY_SMART_PINNED_WIDGETS_JSON] = encodeStringList(current.map { it.id })
    }
    return applied
  }

  suspend fun saveQuickNote(title: String, body: String) {
    val cleanTitle = title.trim().take(MAX_NOTE_TITLE_LENGTH)
    val cleanBody = body.trim().take(MAX_NOTE_BODY_LENGTH)
    dataStore.edit { prefs ->
      prefs[KEY_QUICK_NOTE_TITLE] = cleanTitle
      prefs[KEY_QUICK_NOTE_BODY] = cleanBody
      prefs[KEY_QUICK_NOTE_UPDATED_MS] = System.currentTimeMillis()
    }
  }

  suspend fun setIconGroupPackages(packages: List<String>) {
    val clean = packages.distinct().take(MAX_ICON_GROUP_APPS)
    dataStore.edit { prefs ->
      prefs[KEY_ICON_GROUP_PACKAGES_JSON] = encodeStringList(clean)
    }
  }

  private fun decodeSmartWidgetList(
    json: String?,
    fallback: List<SmartWidgetType>,
  ): List<SmartWidgetType> {
    if (json == null) return fallback
    if (json.isBlank()) return emptyList()
    val parsed = decodeStringList(json).mapNotNull { SmartWidgetType.fromId(it) }.distinct()
    return if (parsed.isEmpty() && fallback.isNotEmpty()) fallback else parsed
  }

  private fun decodeStringList(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(json)
      buildList(array.length()) {
        for (i in 0 until array.length()) {
          val value = array.optString(i).trim()
          if (value.isNotEmpty()) add(value)
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeStringList(items: List<String>): String {
    val array = JSONArray()
    items.forEach { array.put(it) }
    return array.toString()
  }

  private fun normalizeFeedUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return null
    val withScheme =
      if (
        trimmed.startsWith("https://", ignoreCase = true) ||
          trimmed.startsWith("http://", ignoreCase = true)
      ) {
        trimmed
      } else {
        "https://$trimmed"
      }
    val parsed = runCatching { android.net.Uri.parse(withScheme) }.getOrNull() ?: return null
    val scheme = parsed.scheme?.lowercase() ?: return null
    val host = parsed.host?.trim().orEmpty()
    if (scheme !in setOf("http", "https") || host.isEmpty() || !host.contains('.')) return null
    return withScheme
  }

  private fun buildDefaultEnabledIds(customSources: List<NewsFeedSource>): MutableSet<String> =
    (DefaultNewsSources.defaultEnabledIds + customSources.map { it.id }).toMutableSet()

  private fun resolveNewsSources(
    enabledSet: Set<String>?,
    customJson: String?,
  ): List<NewsFeedSource> {
    val customSources = decodeCustomNewsSources(customJson)
    val effectiveEnabled = enabledSet ?: buildDefaultEnabledIds(customSources)
    val builtInResolved =
      DefaultNewsSources.builtIn.map { source ->
        source.copy(isEnabled = source.id in effectiveEnabled)
      }
    val customResolved =
      customSources.map { source ->
        source.copy(isEnabled = source.id in effectiveEnabled)
      }
    return builtInResolved + customResolved
  }

  private fun decodeCustomNewsSources(json: String?): List<NewsFeedSource> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
      val array = JSONArray(json)
      buildList {
        for (i in 0 until array.length()) {
          val obj = array.optJSONObject(i) ?: continue
          val id = obj.optString("id").takeIf { it.isNotBlank() } ?: continue
          val name = obj.optString("name").takeIf { it.isNotBlank() } ?: continue
          val url = obj.optString("url").takeIf { it.isNotBlank() } ?: continue
          add(
            NewsFeedSource(
              id = id,
              name = name,
              feedUrl = url,
              topic = NewsTopic.fromId(obj.optString("topic")),
              isEnabled = true,
              isCustom = true,
            )
          )
        }
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeCustomNewsSources(sources: List<NewsFeedSource>): String {
    val array = JSONArray()
    sources.forEach { source ->
      array.put(
        JSONObject().apply {
          put("id", source.id)
          put("name", source.name)
          put("url", source.feedUrl)
          put("topic", source.topic.id)
        }
      )
    }
    return array.toString()
  }

  companion object {
    const val MAX_DOCK_APPS = 10
    const val INITIAL_HOME_APP_COUNT = 8
    const val MAX_PIN_ATTEMPTS = 5
    const val PIN_LOCKOUT_DURATION_MS = 30_000L
    const val MAX_FOCUS_KEYWORDS = 10
    const val MAX_RECENT_SEARCHES = 10
    const val MAX_PINNED_SMART_WIDGETS = 4
    const val MAX_ICON_GROUP_APPS = 6
    private const val MAX_RECENT_SEARCH_LENGTH = 120
    private const val MAX_NOTE_TITLE_LENGTH = 60
    private const val MAX_NOTE_BODY_LENGTH = 500
    const val CUSTOM_CATEGORY_PREFIX = "custom_"
    private const val LEGACY_SHARED_PREFS_NAME = "nor_launcher_prefs"
    private const val LEGACY_DOCK_KEY = "dock_packages"

    private val KEY_CATEGORY_OVERRIDES = stringPreferencesKey("category_overrides_json")
    private val KEY_CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories_json")
    private val KEY_HIDDEN_PACKAGES = stringSetPreferencesKey("hidden_packages")
    private val KEY_DOCK_PACKAGES = stringPreferencesKey("dock_packages_json")
    private val KEY_LEGACY_MIGRATED = booleanPreferencesKey("legacy_dock_migrated")
    private val KEY_PIN_HASH = stringPreferencesKey("hidden_pin_hash_b64")
    private val KEY_PIN_SALT = stringPreferencesKey("hidden_pin_salt_b64")
    private val KEY_PIN_ITERATIONS = intPreferencesKey("hidden_pin_iterations")
    private val KEY_PIN_FAILED_ATTEMPTS = intPreferencesKey("hidden_pin_failed_attempts")
    private val KEY_PIN_LOCKOUT_UNTIL = longPreferencesKey("hidden_pin_lockout_until_ms")
    private val KEY_NOTIF_ONBOARDING_DISMISSED = booleanPreferencesKey("notif_onboarding_dismissed")
    private val KEY_HOME_APPS_SEEDED = booleanPreferencesKey("home_apps_seeded")
    private val KEY_HOME_WIDGET_STYLE = stringPreferencesKey("home_widget_style")
    private val KEY_GESTURE_SWIPE_UP = stringPreferencesKey("gesture_swipe_up")
    private val KEY_GESTURE_SWIPE_DOWN = stringPreferencesKey("gesture_swipe_down")
    private val KEY_GESTURE_DOUBLE_TAP = stringPreferencesKey("gesture_double_tap")
    private val KEY_THEME_COLOR_SOURCE = stringPreferencesKey("theme_color_source")
    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_THEME_FONT = stringPreferencesKey("theme_font")
    private val KEY_THEME_ICON_SHAPE = stringPreferencesKey("theme_icon_shape")
    private val KEY_THEME_HIDE_SYSTEM_BARS = booleanPreferencesKey("theme_hide_system_bars")
    private val KEY_WALLPAPER_PRESET = stringPreferencesKey("wallpaper_preset")
    private val KEY_WALLPAPER_HAS_CUSTOM = booleanPreferencesKey("wallpaper_has_custom")
    private val KEY_WALLPAPER_DIM_PERCENT = intPreferencesKey("wallpaper_dim_percent")
    private val KEY_WALLPAPER_PHOTO_VERSION = longPreferencesKey("wallpaper_photo_version")
    private val KEY_WEATHER_LOCATION_JSON = stringPreferencesKey("weather_location_json")
    private val KEY_WEATHER_UNIT = stringPreferencesKey("weather_unit")
    private val KEY_WEATHER_CACHE_JSON = stringPreferencesKey("weather_cache_json")
    private val KEY_HOSTED_WIDGETS_JSON = stringPreferencesKey("hosted_widgets_json")
    private val KEY_ICON_PACK_PACKAGE = stringPreferencesKey("selected_icon_pack_package")
    private val KEY_NEWS_ENABLED_SOURCES = stringSetPreferencesKey("news_enabled_sources")
    private val KEY_NEWS_CUSTOM_SOURCES_JSON = stringPreferencesKey("news_custom_sources_json")
    private val KEY_NEWS_PERSONALIZED = booleanPreferencesKey("news_personalized_v1")
    private val KEY_NEWS_CONTEXTS = stringSetPreferencesKey("news_selected_contexts")
    private val KEY_NEWS_FOCUS_KEYWORDS_JSON = stringPreferencesKey("news_focus_keywords_json")
    private val KEY_NEWS_STRICT_KEYWORDS = booleanPreferencesKey("news_strict_keyword_filter")
    private val KEY_RECENT_SEARCH_QUERIES_JSON = stringPreferencesKey("recent_search_queries_json")
    private val KEY_SMART_STACK_WIDGETS_JSON = stringPreferencesKey("smart_stack_widgets_json")
    private val KEY_SMART_PINNED_WIDGETS_JSON = stringPreferencesKey("smart_pinned_widgets_json")
    private val KEY_QUICK_NOTE_TITLE = stringPreferencesKey("smart_quick_note_title")
    private val KEY_QUICK_NOTE_BODY = stringPreferencesKey("smart_quick_note_body")
    private val KEY_QUICK_NOTE_UPDATED_MS = longPreferencesKey("smart_quick_note_updated_ms")
    private val KEY_ICON_GROUP_PACKAGES_JSON = stringPreferencesKey("smart_icon_group_packages_json")
  }
}
