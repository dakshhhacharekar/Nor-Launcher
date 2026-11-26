package com.example.data.apps

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.content.ContextCompat
import com.example.data.icons.IconPackRepository
import com.example.data.icons.LoadedIconPack
import com.example.data.model.BuiltInCategory
import com.example.data.model.InstalledApp
import com.example.data.model.PackageEvent
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

class AppRepository(
  private val appContext: Context,
  private val iconPackRepository: IconPackRepository = IconPackRepository(appContext),
) {
  private val packageManager: PackageManager = appContext.packageManager
  private val iconCache = ConcurrentHashMap<String, ImageBitmap>()
  @Volatile private var lastCachedPackTag: String = DEFAULT_PACK_TAG

  suspend fun loadInstalledApps(activeIconPack: LoadedIconPack? = null): List<InstalledApp> =
    withContext(Dispatchers.IO) {
      val packTag = activeIconPack?.cacheTag ?: DEFAULT_PACK_TAG
      if (lastCachedPackTag != packTag) {
        iconCache.clear()
        lastCachedPackTag = packTag
      }

      val launcherIntent =
        Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }

      val resolvedActivities: List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(
            launcherIntent,
            PackageManager.ResolveInfoFlags.of(0L),
          )
        } else {
          @Suppress("DEPRECATION") packageManager.queryIntentActivities(launcherIntent, 0)
        }

      val seenPackages = HashSet<String>()
      val apps = ArrayList<InstalledApp>(resolvedActivities.size)

      for (resolveInfo in resolvedActivities) {
        val activityInfo = resolveInfo.activityInfo ?: continue
        val packageName = activityInfo.packageName ?: continue
        if (packageName == appContext.packageName || !seenPackages.add(packageName)) continue

        val activityName = activityInfo.name.orEmpty()
        val label =
          runCatching { resolveInfo.loadLabel(packageManager).toString().trim() }
            .getOrDefault(packageName)
            .ifBlank { packageName }

        val appInfo = activityInfo.applicationInfo
        val lastUpdateTime = getLastUpdateTime(packageName)
        val cacheKey = "$packTag:$packageName:$lastUpdateTime"

        val icon =
          iconCache[cacheKey]
            ?: iconPackRepository
              .resolveAppIcon(
                pack = activeIconPack,
                packageName = packageName,
                activityName = activityName,
                iconSizePx = ICON_RENDER_SIZE_PX,
                defaultDrawableProvider = { resolveInfo.loadIcon(packageManager) },
              )
              ?.also { rendered -> iconCache[cacheKey] = rendered }

        val autoCategory = resolveAutoCategory(appInfo, packageName, label)

        apps.add(
          InstalledApp(
            packageName = packageName,
            activityName = activityName,
            label = label,
            autoCategory = autoCategory,
            lastUpdateTime = lastUpdateTime,
            icon = icon,
          )
        )
      }

      apps.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
      apps
    }

  fun clearIconCache() {
    iconCache.clear()
  }

  fun observePackageEvents(): Flow<PackageEvent> = callbackFlow {
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
          val action = intent?.action ?: return
          val packageName = intent.data?.schemeSpecificPart ?: return
          val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

          // Evict stale icon cache entries whenever a package changes or is removed.
          iconCache.keys.removeIf { it.contains(":$packageName:") }

          val event =
            when (action) {
              Intent.ACTION_PACKAGE_ADDED -> PackageEvent.Added(packageName, isReplacing)
              Intent.ACTION_PACKAGE_REMOVED -> PackageEvent.Removed(packageName, isReplacing)
              Intent.ACTION_PACKAGE_CHANGED -> PackageEvent.Changed(packageName)
              Intent.ACTION_PACKAGE_REPLACED -> PackageEvent.Replaced(packageName)
              else -> null
            }

          if (event != null) {
            trySend(event)
          }
        }
      }

    val filter =
      IntentFilter().apply {
        addAction(Intent.ACTION_PACKAGE_ADDED)
        addAction(Intent.ACTION_PACKAGE_REMOVED)
        addAction(Intent.ACTION_PACKAGE_CHANGED)
        addAction(Intent.ACTION_PACKAGE_REPLACED)
        addDataScheme("package")
      }

    // System package broadcasts require RECEIVER_EXPORTED on Android 13+ (API 33+).
    ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_EXPORTED)

    awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
  }

  private fun getLastUpdateTime(packageName: String): Long =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager
            .getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            .lastUpdateTime
        } else {
          @Suppress("DEPRECATION") packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        }
      }
      .getOrDefault(0L)

  private fun resolveAutoCategory(
    appInfo: ApplicationInfo?,
    packageName: String,
    label: String,
  ): BuiltInCategory {
    val pkg = packageName.lowercase()
    val token = "$pkg ${label.lowercase()}"

    // Check explicit high-confidence package overrides first so core apps are always neatly sorted.
    when {
      COMMUNICATION_PACKAGES.any { pkg.contains(it) } -> return BuiltInCategory.COMMUNICATION
      INTERNET_PACKAGES.any { pkg.contains(it) } -> return BuiltInCategory.INTERNET
      MEDIA_PACKAGES.any { pkg.contains(it) } -> return BuiltInCategory.MEDIA
      PRODUCTIVITY_PACKAGES.any { pkg.contains(it) } -> return BuiltInCategory.PRODUCTIVITY
      UTILITIES_PACKAGES.any { pkg.contains(it) } -> return BuiltInCategory.UTILITIES
    }

    if (appInfo != null) {
      when (appInfo.category) {
        ApplicationInfo.CATEGORY_GAME -> return BuiltInCategory.GAMES
        ApplicationInfo.CATEGORY_SOCIAL -> return BuiltInCategory.COMMUNICATION
        ApplicationInfo.CATEGORY_AUDIO,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_IMAGE -> return BuiltInCategory.MEDIA
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> return BuiltInCategory.PRODUCTIVITY
        ApplicationInfo.CATEGORY_NEWS -> return BuiltInCategory.INTERNET
        ApplicationInfo.CATEGORY_MAPS,
        ApplicationInfo.CATEGORY_ACCESSIBILITY -> return BuiltInCategory.UTILITIES
      }
    }

    // Fallback keyword heuristics for apps declaring CATEGORY_UNDEFINED.
    return when {
      COMMUNICATION_HINTS.any { token.contains(it) } -> BuiltInCategory.COMMUNICATION
      INTERNET_HINTS.any { token.contains(it) } -> BuiltInCategory.INTERNET
      MEDIA_HINTS.any { token.contains(it) } -> BuiltInCategory.MEDIA
      GAMES_HINTS.any { token.contains(it) } -> BuiltInCategory.GAMES
      PRODUCTIVITY_HINTS.any { token.contains(it) } -> BuiltInCategory.PRODUCTIVITY
      UTILITIES_HINTS.any { token.contains(it) } -> BuiltInCategory.UTILITIES
      else -> BuiltInCategory.UTILITIES
    }
  }

  companion object {
    private const val DEFAULT_PACK_TAG = "default"
    private const val ICON_RENDER_SIZE_PX = 144

    private val COMMUNICATION_PACKAGES =
      listOf(
        "dialer",
        "contacts",
        "messaging",
        "android.mms",
        "android.gm",
        "whatsapp",
        "telegram",
        "signal",
        "discord",
        "slack",
        "orca",
        "tachyon",
        "meetings",
      )

    private val INTERNET_PACKAGES =
      listOf(
        "android.chrome",
        "firefox",
        "fenix",
        "brave",
        "emmx",
        "duckduckgo",
        "vivaldi",
        "opera",
        "googlequicksearchbox",
        "reddit",
        "twitter",
        "instagram",
        "facebook.katana",
        "linkedin",
        "threads",
        "bsky",
      )

    private val MEDIA_PACKAGES =
      listOf(
        "camera",
        "photos",
        "gallery",
        "youtube",
        "spotify",
        "netflix",
        "vlc",
        "soundcloud",
        "music",
        "podcasts",
        "twitch",
        "primevideo",
        "disneyplus",
      )

    private val PRODUCTIVITY_PACKAGES =
      listOf(
        "calendar",
        "apps.docs",
        "apps.sheets",
        "apps.slides",
        "keep",
        "tasks",
        "notion",
        "office",
        "outlook",
        "onenote",
        "evernote",
        "trello",
        "todoist",
        "drive",
        "dropbox",
        "acrobat",
      )

    private val UTILITIES_PACKAGES =
      listOf(
        "android.settings",
        "deskclock",
        "calculator",
        "documentsui",
        "nbu.files",
        "apps.maps",
        "android.vending",
        "recorder",
        "soundrecorder",
        "compass",
        "wallet",
        "authenticator",
        "findmydevice",
        "apps.fitness",
        "projection.gearhead",
        "chromecast.app",
      )

    private val COMMUNICATION_HINTS =
      listOf(
        "phone",
        "call",
        "sms",
        "chat",
        "mail",
        "messenger",
        "meet",
        "zoom",
        "teams",
        "voicemail",
      )

    private val INTERNET_HINTS =
      listOf(
        "browser",
        "web",
        "search",
        "news",
        "feed",
        "rss",
        "wiki",
        "amazon",
        "shop",
        "ebay",
      )

    private val MEDIA_HINTS =
      listOf(
        "video",
        "audio",
        "player",
        "radio",
        "tv",
        "stream",
        "photo",
        "image",
        "edit",
        "snapseed",
      )

    private val GAMES_HINTS =
      listOf(
        "game",
        "play.games",
        "chess",
        "sudoku",
        "minecraft",
        "roblox",
        "arcade",
        "puzzle",
        "solitaire",
      )

    private val PRODUCTIVITY_HINTS =
      listOf(
        "note",
        "todo",
        "task",
        "doc",
        "pdf",
        "scan",
        "work",
        "bank",
        "pay",
        "finance",
        "budget",
        "sheet",
        "slide",
        "translate",
      )

    private val UTILITIES_HINTS =
      listOf(
        "clock",
        "alarm",
        "timer",
        "calc",
        "file",
        "download",
        "weather",
        "map",
        "nav",
        "tool",
        "flashlight",
        "qr",
        "vpn",
        "backup",
        "cleaner",
        "device",
        "home",
        "fit",
        "health",
        "store",
        "system",
      )
  }
}
