package com.example.data.icons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.InstalledIconPack
import java.io.InputStream
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Parsed representation of an installed icon pack's `appfilter.xml`.
 */
class LoadedIconPack(
  val packageName: String,
  val label: String,
  val versionStamp: Long,
  val resources: Resources,
  val componentDrawables: Map<String, String>,
  val packageFallbackDrawables: Map<String, String>,
  val calendarPrefixes: Map<String, String>,
  val iconBacks: List<String>,
  val iconMask: String?,
  val iconUpon: String?,
  val scaleFactor: Float,
) {
  val cacheTag: String = "$packageName@$versionStamp"
}

/**
 * Detects installed third-party Android icon packs, parses their `appfilter.xml`
 * (supporting both `res/xml/appfilter.xml` and `assets/appfilter.xml`), and resolves
 * themed icons with optional mask/backplate compositing and safe fallback to the default icon.
 */
class IconPackRepository(
  private val appContext: Context,
) {
  private val packageManager: PackageManager = appContext.packageManager
  private val loadedPackCache = ConcurrentHashMap<String, LoadedIconPack>()

  fun invalidateCache(packageName: String? = null) {
    if (packageName == null) {
      loadedPackCache.clear()
    } else {
      loadedPackCache.remove(packageName)
    }
  }

  suspend fun discoverInstalledIconPacks(): List<InstalledIconPack> =
    withContext(Dispatchers.IO) {
      val candidatePackages = LinkedHashSet<String>()

      for (action in ICON_PACK_INTENT_ACTIONS) {
        queryPackagesForIntent(Intent(action), candidatePackages)
      }
      for (category in ICON_PACK_MAIN_CATEGORIES) {
        val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(category) }
        queryPackagesForIntent(intent, candidatePackages)
      }

      candidatePackages.remove(appContext.packageName)

      val results = ArrayList<InstalledIconPack>(candidatePackages.size)
      for (pkg in candidatePackages) {
        val loaded = loadIconPackInternal(pkg) ?: continue
        val appInfo = runCatching { getApplicationInfoCompat(pkg) }.getOrNull()
        val packIcon =
          appInfo?.let {
            runCatching {
              packageManager
                .getApplicationIcon(it)
                .toBitmap(width = PACK_ICON_SIZE_PX, height = PACK_ICON_SIZE_PX)
                .asImageBitmap()
            }.getOrNull()
          }

        val previews = loadPreviewIcons(loaded, maxCount = 4)
        results.add(
          InstalledIconPack(
            packageName = pkg,
            label = loaded.label,
            icon = packIcon,
            mappedIconCount = loaded.componentDrawables.size + loaded.calendarPrefixes.size,
            previewIcons = previews,
          )
        )
      }

      results.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
      results
    }

  suspend fun loadIconPack(packageName: String?): LoadedIconPack? =
    withContext(Dispatchers.IO) {
      if (packageName.isNullOrBlank()) return@withContext null
      loadIconPackInternal(packageName)
    }

  /**
   * Resolves the themed icon for [packageName]/[activityName] from [pack].
   * Falls back cleanly to:
   * 1) Exact ComponentInfo{pkg/activity} match in `appfilter.xml`
   * 2) Dynamic `<calendar>` prefix + day-of-month match
   * 3) Package-level fallback match in `appfilter.xml`
   * 4) Pack's `<iconback>` / `<iconmask>` / `<iconupon>` composited over [defaultDrawableProvider]
   * 5) App's original default icon rendered cleanly.
   */
  fun resolveAppIcon(
    pack: LoadedIconPack?,
    packageName: String,
    activityName: String,
    iconSizePx: Int,
    defaultDrawableProvider: () -> Drawable?,
  ): ImageBitmap? {
    if (pack != null) {
      val drawableName = findDrawableName(pack, packageName, activityName)
      if (drawableName != null) {
        loadPackDrawable(pack, drawableName)?.let { themedDrawable ->
          runCatching {
            return themedDrawable.toBitmap(width = iconSizePx, height = iconSizePx).asImageBitmap()
          }
        }
      }

      // Unthemed app: apply pack's iconback/iconmask/iconupon if defined
      val hasMasking =
        pack.iconBacks.isNotEmpty() || pack.iconMask != null || pack.iconUpon != null
      if (hasMasking) {
        val defaultDrawable = runCatching { defaultDrawableProvider() }.getOrNull()
        if (defaultDrawable != null) {
          compositeWithPackMask(pack, packageName, defaultDrawable, iconSizePx)?.let { masked ->
            return masked
          }
        }
      }
    }

    // Guaranteed fallback to the app's original system icon
    val fallbackDrawable = runCatching { defaultDrawableProvider() }.getOrNull() ?: return null
    return runCatching {
      fallbackDrawable.toBitmap(width = iconSizePx, height = iconSizePx).asImageBitmap()
    }.getOrNull()
  }

  private fun findDrawableName(
    pack: LoadedIconPack,
    packageName: String,
    activityName: String,
  ): String? {
    val normalizedFullActivity =
      if (activityName.startsWith(".")) "$packageName$activityName" else activityName
    val shortActivity =
      if (normalizedFullActivity.startsWith("$packageName.")) {
        normalizedFullActivity.removePrefix(packageName)
      } else {
        normalizedFullActivity
      }

    val fullKey = "$packageName/$normalizedFullActivity"
    val shortKey = "$packageName/$shortActivity"

    pack.componentDrawables[fullKey]?.let { return it }
    pack.componentDrawables[shortKey]?.let { return it }

    // Dynamic calendar icon support (<calendar component="..." prefix="calendar_" />)
    val calPrefix = pack.calendarPrefixes[fullKey] ?: pack.calendarPrefixes[shortKey]
    if (!calPrefix.isNullOrBlank()) {
      val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
      return "$calPrefix$dayOfMonth"
    }

    return pack.packageFallbackDrawables[packageName]
  }

  private fun loadPackDrawable(pack: LoadedIconPack, drawableName: String): Drawable? {
    val cleanName = drawableName.trim()
    if (cleanName.isEmpty()) return null
    val resId = pack.resources.getIdentifier(cleanName, "drawable", pack.packageName)
    if (resId == 0) return null
    return runCatching {
      ResourcesCompat.getDrawable(pack.resources, resId, null)
    }.getOrNull()
  }

  private fun compositeWithPackMask(
    pack: LoadedIconPack,
    packageName: String,
    defaultDrawable: Drawable,
    sizePx: Int,
  ): ImageBitmap? =
    runCatching {
      val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(output)
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

      // 1. Optional backplate (deterministically selected per package so it stays stable)
      if (pack.iconBacks.isNotEmpty()) {
        val backIndex = (packageName.hashCode() and Int.MAX_VALUE) % pack.iconBacks.size
        loadPackDrawable(pack, pack.iconBacks[backIndex])?.let { backDrawable ->
          val backBmp = backDrawable.toBitmap(sizePx, sizePx)
          canvas.drawBitmap(backBmp, 0f, 0f, paint)
        }
      }

      // 2. Scaled default icon centered inside the canvas
      val scaledSize = (sizePx * pack.scaleFactor).roundToInt().coerceIn(sizePx / 2, sizePx)
      val offset = (sizePx - scaledSize) / 2f
      val iconBmp = defaultDrawable.toBitmap(scaledSize, scaledSize)

      // 3. Optional alpha mask (DST_OUT)
      val maskDrawable = pack.iconMask?.let { loadPackDrawable(pack, it) }
      if (maskDrawable != null) {
        val maskedLayer = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val layerCanvas = Canvas(maskedLayer)
        layerCanvas.drawBitmap(iconBmp, offset, offset, paint)
        val maskBmp = maskDrawable.toBitmap(sizePx, sizePx)
        val maskPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
          }
        layerCanvas.drawBitmap(maskBmp, 0f, 0f, maskPaint)
        canvas.drawBitmap(maskedLayer, 0f, 0f, paint)
      } else {
        canvas.drawBitmap(iconBmp, offset, offset, paint)
      }

      // 4. Optional foreground overlay (gloss / frame / shadow)
      pack.iconUpon?.let { loadPackDrawable(pack, it) }?.let { uponDrawable ->
        val uponBmp = uponDrawable.toBitmap(sizePx, sizePx)
        canvas.drawBitmap(uponBmp, 0f, 0f, paint)
      }

      output.asImageBitmap()
    }.getOrNull()

  private fun loadIconPackInternal(packageName: String): LoadedIconPack? {
    val versionStamp = getLastUpdateTime(packageName)
    if (versionStamp < 0L) {
      loadedPackCache.remove(packageName)
      return null
    }

    val cached = loadedPackCache[packageName]
    if (cached != null && cached.versionStamp == versionStamp) {
      return cached
    }

    val packResources =
      runCatching { packageManager.getResourcesForApplication(packageName) }.getOrNull()
        ?: return null
    val appInfo = runCatching { getApplicationInfoCompat(packageName) }.getOrNull()
    val label =
      appInfo?.let { runCatching { packageManager.getApplicationLabel(it).toString().trim() }.getOrNull() }
        ?.takeIf { it.isNotBlank() }
        ?: packageName

    val parsed = parseAppFilter(packResources, packageName) ?: return null

    // Verify the package actually provides icon mappings or masking rules
    val hasContent =
      parsed.componentDrawables.isNotEmpty() ||
        parsed.calendarPrefixes.isNotEmpty() ||
        parsed.iconBacks.isNotEmpty()
    if (!hasContent) return null

    val loaded =
      LoadedIconPack(
        packageName = packageName,
        label = label,
        versionStamp = versionStamp,
        resources = packResources,
        componentDrawables = parsed.componentDrawables,
        packageFallbackDrawables = parsed.packageFallbackDrawables,
        calendarPrefixes = parsed.calendarPrefixes,
        iconBacks = parsed.iconBacks,
        iconMask = parsed.iconMask,
        iconUpon = parsed.iconUpon,
        scaleFactor = parsed.scaleFactor,
      )
    loadedPackCache[packageName] = loaded
    return loaded
  }

  private fun parseAppFilter(
    packResources: Resources,
    packageName: String,
  ): ParsedAppFilter? {
    // 1. Prefer compiled res/xml/appfilter.xml
    val xmlResId = packResources.getIdentifier("appfilter", "xml", packageName)
    if (xmlResId != 0) {
      runCatching {
        packResources.getXml(xmlResId).use { parser ->
          return parsePullParser(parser)
        }
      }
    }

    // 2. Fall back to assets/appfilter.xml (used by many Play Store icon packs)
    runCatching {
      packResources.assets.open("appfilter.xml").use { stream ->
        return parseInputStream(stream)
      }
    }

    // 3. Fall back to res/raw/appfilter.xml
    val rawResId = packResources.getIdentifier("appfilter", "raw", packageName)
    if (rawResId != 0) {
      runCatching {
        packResources.openRawResource(rawResId).use { stream ->
          return parseInputStream(stream)
        }
      }
    }

    return null
  }

  private fun parseInputStream(stream: InputStream): ParsedAppFilter {
    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = false }
    val parser = factory.newPullParser()
    parser.setInput(stream, "UTF-8")
    return parsePullParser(parser)
  }

  private fun parsePullParser(parser: XmlPullParser): ParsedAppFilter {
    val componentDrawables = HashMap<String, String>(512)
    val packageFallbacks = HashMap<String, String>(256)
    val calendarPrefixes = HashMap<String, String>(16)
    val iconBacks = ArrayList<String>(4)
    var iconMask: String? = null
    var iconUpon: String? = null
    var scaleFactor = DEFAULT_MASK_SCALE

    var eventType = parser.eventType
    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_TAG) {
        when (parser.name?.lowercase()) {
          "item" -> {
            val componentAttr = parser.getAttributeValue(null, "component")
            val drawableAttr = parser.getAttributeValue(null, "drawable")?.trim()
            if (!componentAttr.isNullOrBlank() && !drawableAttr.isNullOrEmpty()) {
              val extracted = extractComponentKey(componentAttr)
              if (extracted != null) {
                val (pkg, compKey) = extracted
                componentDrawables[compKey] = drawableAttr
                packageFallbacks.putIfAbsent(pkg, drawableAttr)
              }
            }
          }
          "calendar" -> {
            val componentAttr = parser.getAttributeValue(null, "component")
            val prefixAttr = parser.getAttributeValue(null, "prefix")?.trim()
            if (!componentAttr.isNullOrBlank() && !prefixAttr.isNullOrEmpty()) {
              val extracted = extractComponentKey(componentAttr)
              if (extracted != null) {
                calendarPrefixes[extracted.second] = prefixAttr
              }
            }
          }
          "iconback" -> {
            for (i in 0 until parser.attributeCount) {
              val attrVal = parser.getAttributeValue(i)?.trim()
              if (!attrVal.isNullOrEmpty()) {
                iconBacks.add(attrVal)
              }
            }
          }
          "iconmask" -> {
            if (parser.attributeCount > 0) {
              iconMask = parser.getAttributeValue(0)?.trim()?.takeIf { it.isNotEmpty() }
            }
          }
          "iconupon" -> {
            if (parser.attributeCount > 0) {
              iconUpon = parser.getAttributeValue(0)?.trim()?.takeIf { it.isNotEmpty() }
            }
          }
          "scale" -> {
            val rawFactor = parser.getAttributeValue(null, "factor")?.toFloatOrNull()
            if (rawFactor != null && rawFactor in 0.4f..1.25f) {
              scaleFactor = rawFactor
            }
          }
        }
      }
      eventType = parser.next()
    }

    return ParsedAppFilter(
      componentDrawables = componentDrawables,
      packageFallbackDrawables = packageFallbacks,
      calendarPrefixes = calendarPrefixes,
      iconBacks = iconBacks,
      iconMask = iconMask,
      iconUpon = iconUpon,
      scaleFactor = scaleFactor,
    )
  }

  /**
   * Parses `ComponentInfo{com.pkg/com.pkg.MainActivity}` or `ComponentInfo{com.pkg/.MainActivity}`
   * into `Pair(packageName, "com.pkg/com.pkg.MainActivity")`.
   */
  private fun extractComponentKey(raw: String): Pair<String, String>? {
    val trimmed = raw.trim()
    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    val inner =
      if (start >= 0 && end > start) trimmed.substring(start + 1, end).trim()
      else trimmed
    val slash = inner.indexOf('/')
    if (slash <= 0 || slash >= inner.lastIndex) return null
    val pkg = inner.substring(0, slash).trim()
    val rawAct = inner.substring(slash + 1).trim()
    if (pkg.isEmpty() || rawAct.isEmpty()) return null
    val normalizedAct = if (rawAct.startsWith(".")) "$pkg$rawAct" else rawAct
    return pkg to "$pkg/$normalizedAct"
  }

  private fun loadPreviewIcons(pack: LoadedIconPack, maxCount: Int): List<ImageBitmap> {
    val previews = ArrayList<ImageBitmap>(maxCount)
    val preferredPackages =
      listOf(
        "com.android.chrome",
        "com.google.android.dialer",
        "com.google.android.apps.messaging",
        "com.google.android.apps.photos",
        "com.android.settings",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.spotify.music",
      )

    val triedDrawables = HashSet<String>()
    for (pkg in preferredPackages) {
      if (previews.size >= maxCount) break
      val name = pack.packageFallbackDrawables[pkg] ?: continue
      if (!triedDrawables.add(name)) continue
      loadPackDrawable(pack, name)?.let { drawable ->
        runCatching {
          previews.add(drawable.toBitmap(PREVIEW_ICON_SIZE_PX, PREVIEW_ICON_SIZE_PX).asImageBitmap())
        }
      }
    }

    if (previews.size < maxCount) {
      for (name in pack.componentDrawables.values) {
        if (previews.size >= maxCount) break
        if (!triedDrawables.add(name)) continue
        loadPackDrawable(pack, name)?.let { drawable ->
          runCatching {
            previews.add(drawable.toBitmap(PREVIEW_ICON_SIZE_PX, PREVIEW_ICON_SIZE_PX).asImageBitmap())
          }
        }
      }
    }

    return previews
  }

  private fun queryPackagesForIntent(intent: Intent, target: MutableSet<String>) {
    val activities =
      runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
          @Suppress("DEPRECATION") packageManager.queryIntentActivities(intent, 0)
        }
      }.getOrDefault(emptyList())

    for (info in activities) {
      val pkg = info.activityInfo?.packageName
      if (!pkg.isNullOrBlank()) {
        target.add(pkg)
      }
    }
  }

  private fun getApplicationInfoCompat(packageName: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0L))
    } else {
      @Suppress("DEPRECATION") packageManager.getApplicationInfo(packageName, 0)
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
    }.getOrDefault(-1L)

  private data class ParsedAppFilter(
    val componentDrawables: Map<String, String>,
    val packageFallbackDrawables: Map<String, String>,
    val calendarPrefixes: Map<String, String>,
    val iconBacks: List<String>,
    val iconMask: String?,
    val iconUpon: String?,
    val scaleFactor: Float,
  )

  companion object {
    private const val PACK_ICON_SIZE_PX = 96
    private const val PREVIEW_ICON_SIZE_PX = 84
    private const val DEFAULT_MASK_SCALE = 0.78f

    private val ICON_PACK_INTENT_ACTIONS =
      listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.anddoes.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.teslacoilsw.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
        "ginlemon.smartlauncher.THEMES",
        "com.lge.launcher2.THEME",
        "com.sonymobile.home.ICON_PACK",
        "net.oneplus.launcher.icons.ACTION_PICK_ICON",
      )

    private val ICON_PACK_MAIN_CATEGORIES =
      listOf(
        "com.anddoes.launcher.THEME",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.fede.launcher.THEME_ICONPACK",
      )
  }
}
