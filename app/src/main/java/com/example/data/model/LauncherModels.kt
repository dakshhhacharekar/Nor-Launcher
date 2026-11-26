package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap
import com.example.data.security.HashedPin

enum class BuiltInCategory(val id: String) {
  COMMUNICATION("communication"),
  INTERNET("internet"),
  GAMES("games"),
  MEDIA("media"),
  PRODUCTIVITY("productivity"),
  UTILITIES("utilities"),
  OTHER("other");

  companion object {
    val assignableCategories: List<BuiltInCategory> = entries.toList()

    fun fromId(id: String?): BuiltInCategory? =
      entries.firstOrNull { it.id == id }
  }
}

enum class HomeWidgetStyle(val id: String) {
  MINIMAL_CLOCK("minimal_clock"),
  GLASS_CLOCK_BATTERY("glass_clock_battery"),
  COMPACT_BATTERY_DATE("compact_battery_date"),
  ANALOG_CLOCK("analog_clock");

  companion object {
    fun fromId(id: String?): HomeWidgetStyle =
      entries.firstOrNull { it.id == id } ?: MINIMAL_CLOCK
  }
}

enum class ThemeColorSource(val id: String) {
  MATERIAL_YOU("material_you"),
  WALLPAPER("wallpaper"),
  CYBER_Y2K("cyber_y2k"),
  MATCHA_CREAM("matcha_cream"),
  NEON_BERRY("neon_berry"),
  NORDIC("nordic"),
  FOREST("forest");

  companion object {
    fun fromId(id: String?): ThemeColorSource =
      entries.firstOrNull { it.id == id } ?: MATERIAL_YOU
  }
}

enum class ThemeModeOption(val id: String) {
  SYSTEM("system"),
  LIGHT("light"),
  DARK("dark");

  companion object {
    fun fromId(id: String?): ThemeModeOption =
      entries.firstOrNull { it.id == id } ?: SYSTEM
  }
}

enum class FontChoice(val id: String) {
  OUTFIT("outfit"),
  PLUS_JAKARTA("plus_jakarta"),
  SPACE_GROTESK("space_grotesk"),
  JETBRAINS_MONO("jetbrains_mono"),
  SYSTEM("system");

  companion object {
    fun fromId(id: String?): FontChoice =
      entries.firstOrNull { it.id == id } ?: OUTFIT
  }
}

enum class IconShapeOption(val id: String) {
  SQUIRCLE("squircle"),
  CIRCLE("circle"),
  ROUNDED_SQUARE("rounded_square"),
  TEARDROP("teardrop");

  companion object {
    fun fromId(id: String?): IconShapeOption =
      entries.firstOrNull { it.id == id } ?: SQUIRCLE
  }
}

data class ThemeConfig(
  val colorSource: ThemeColorSource = ThemeColorSource.MATERIAL_YOU,
  val themeMode: ThemeModeOption = ThemeModeOption.SYSTEM,
  val fontChoice: FontChoice = FontChoice.OUTFIT,
  val iconShape: IconShapeOption = IconShapeOption.SQUIRCLE,
  val hideSystemBars: Boolean = false,
)

data class CustomCategory(
  val id: String,
  val name: String,
)

data class CategoryChipItem(
  val id: String,
  val name: String,
  val isCustom: Boolean,
  val builtIn: BuiltInCategory? = null,
  val appCount: Int = 0,
)

data class InstalledApp(
  val packageName: String,
  val activityName: String = "",
  val label: String,
  val autoCategory: BuiltInCategory,
  val lastUpdateTime: Long,
  val icon: ImageBitmap?,
)

data class AppItem(
  val packageName: String,
  val label: String,
  val autoCategory: BuiltInCategory,
  val effectiveCategoryId: String,
  val effectiveCategoryName: String,
  val isCategoryOverridden: Boolean,
  val isPinnedToDock: Boolean,
  val notificationCount: Int = 0,
  val icon: ImageBitmap?,
)

data class PinSecurityState(
  val hashedPin: HashedPin? = null,
  val failedAttempts: Int = 0,
  val lockoutUntilEpochMs: Long = 0L,
) {
  val isPinConfigured: Boolean
    get() = hashedPin != null
}

enum class HomeGesture(val id: String) {
  SWIPE_UP("swipe_up"),
  SWIPE_DOWN("swipe_down"),
  DOUBLE_TAP("double_tap"),
}

enum class GestureAction(
  val id: String,
  val requiresAccessibility: Boolean = false,
) {
  OPEN_DRAWER("open_drawer", requiresAccessibility = false),
  LOCK_SCREEN("lock_screen", requiresAccessibility = true),
  OPEN_NOTIFICATIONS("open_notifications", requiresAccessibility = true),
  OPEN_QUICK_SETTINGS("open_quick_settings", requiresAccessibility = true),
  OPEN_SEARCH("open_search", requiresAccessibility = false),
  OPEN_HIDDEN_APPS("open_hidden_apps", requiresAccessibility = false),
  NONE("none", requiresAccessibility = false);

  companion object {
    fun fromId(id: String?, fallback: GestureAction): GestureAction =
      entries.firstOrNull { it.id == id } ?: fallback
  }
}

data class GestureConfig(
  val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
  val swipeDown: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
  val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
) {
  fun actionFor(gesture: HomeGesture): GestureAction =
    when (gesture) {
      HomeGesture.SWIPE_UP -> swipeUp
      HomeGesture.SWIPE_DOWN -> swipeDown
      HomeGesture.DOUBLE_TAP -> doubleTap
    }
}

data class LauncherPreferencesState(
  val categoryOverrides: Map<String, String> = emptyMap(),
  val customCategories: List<CustomCategory> = emptyList(),
  val hiddenPackages: Set<String> = emptySet(),
  val dockPackages: List<String> = emptyList(),
  val pinSecurity: PinSecurityState = PinSecurityState(),
  val notificationOnboardingDismissed: Boolean = false,
  val homeAppsSeeded: Boolean = false,
  val homeWidgetStyle: HomeWidgetStyle = HomeWidgetStyle.MINIMAL_CLOCK,
  val gestureConfig: GestureConfig = GestureConfig(),
  val themeConfig: ThemeConfig = ThemeConfig(),
  val wallpaperConfig: WallpaperConfig = WallpaperConfig(),
  val weatherLocation: WeatherLocation? = null,
  val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
  val cachedWeather: WeatherSnapshot? = null,
  val hostedWidgets: List<HostedWidgetRecord> = emptyList(),
  val selectedIconPackPackage: String? = null,
  val newsSources: List<NewsFeedSource> = DefaultNewsSources.builtIn,
  val newsPersonalization: NewsPersonalization = NewsPersonalization(),
  val recentSearchQueries: List<String> = emptyList(),
  val smartWidgetsConfig: SmartWidgetsConfig = SmartWidgetsConfig(),
)

sealed interface DockPinResult {
  data object Pinned : DockPinResult
  data object Unpinned : DockPinResult
  data object DockFull : DockPinResult
}

sealed interface CategoryMutationResult {
  data class Success(val category: CustomCategory) : CategoryMutationResult
  data object EmptyName : CategoryMutationResult
  data object DuplicateName : CategoryMutationResult
}

sealed interface PackageEvent {
  data class Added(val packageName: String, val isReplacing: Boolean) : PackageEvent
  data class Removed(val packageName: String, val isReplacing: Boolean) : PackageEvent
  data class Changed(val packageName: String) : PackageEvent
  data class Replaced(val packageName: String) : PackageEvent
}
