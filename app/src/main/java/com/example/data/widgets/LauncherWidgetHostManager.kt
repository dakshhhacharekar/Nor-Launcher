package com.example.data.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.data.model.AvailableWidgetEntry
import com.example.data.model.AvailableWidgetGroup
import com.example.data.model.HostedWidgetRecord
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Custom [AppWidgetHostView] that intercepts long-presses cleanly so the user can enter
 * resize/remove edit mode on any hosted widget without interfering with taps or scrolling
 * inside RemoteViews collections.
 */
class NorAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

  var onWidgetLongPress: (() -> Unit)? = null

  private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
  private var downX = 0f
  private var downY = 0f
  private var longPressTriggered = false

  private val longPressRunnable = Runnable {
    longPressTriggered = true
    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    onWidgetLongPress?.invoke()
  }

  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (longPressTriggered) {
      if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) {
        longPressTriggered = false
      }
      return true
    }

    when (ev.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = ev.x
        downY = ev.y
        longPressTriggered = false
        removeCallbacks(longPressRunnable)
        postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
      }
      MotionEvent.ACTION_MOVE -> {
        if (abs(ev.x - downX) > touchSlop || abs(ev.y - downY) > touchSlop) {
          removeCallbacks(longPressRunnable)
        }
      }
      MotionEvent.ACTION_UP,
      MotionEvent.ACTION_CANCEL -> {
        removeCallbacks(longPressRunnable)
      }
    }
    return false
  }

  override fun cancelLongPress() {
    super.cancelLongPress()
    removeCallbacks(longPressRunnable)
    longPressTriggered = false
  }

  override fun onDetachedFromWindow() {
    removeCallbacks(longPressRunnable)
    super.onDetachedFromWindow()
  }
}

private class NorAppWidgetHost(
  context: Context,
  hostId: Int,
) : AppWidgetHost(context, hostId) {
  override fun onCreateView(
    context: Context,
    appWidgetId: Int,
    appWidget: AppWidgetProviderInfo?,
  ): AppWidgetHostView = NorAppWidgetHostView(context)
}

/**
 * Manages real Android [AppWidgetHost] & [AppWidgetManager] integration:
 * - Lifecycle-safe listening ([startListening] / [stopListening])
 * - Enumerating installed widgets grouped by app with safe preview rendering
 * - Allocating / binding / configuring / deleting widget IDs without leaks
 * - Responsive sizing via API 31+ SizeF list and API 26-30 bounds fallback
 */
class LauncherWidgetHostManager(
  private val appContext: Context,
) {
  private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
  private val appWidgetHost: NorAppWidgetHost = NorAppWidgetHost(appContext, APPWIDGET_HOST_ID)
  private var isListening = false

  fun startListening() {
    if (isListening) return
    runCatching {
      appWidgetHost.startListening()
      isListening = true
    }
  }

  fun stopListening() {
    if (!isListening) return
    runCatching {
      appWidgetHost.stopListening()
    }
    isListening = false
  }

  fun allocateAppWidgetId(): Int =
    runCatching { appWidgetHost.allocateAppWidgetId() }
      .getOrDefault(AppWidgetManager.INVALID_APPWIDGET_ID)

  fun deleteAppWidgetId(appWidgetId: Int) {
    if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
    runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
  }

  fun getAppWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? =
    runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()

  fun isReconfigurable(appWidgetId: Int): Boolean {
    val info = getAppWidgetInfo(appWidgetId) ?: return false
    if (info.configure == null) return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0
    } else {
      false
    }
  }

  fun tryBindWidgetIfAllowed(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Boolean =
    runCatching {
      appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.profile, providerInfo.provider, null)
    }.getOrElse {
      runCatching {
        appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, providerInfo.provider)
      }.getOrDefault(false)
    }

  fun buildBindPermissionIntent(appWidgetId: Int, providerInfo: AppWidgetProviderInfo): Intent =
    Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, providerInfo.provider)
      putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, providerInfo.profile)
    }

  fun requiresInitialConfiguration(providerInfo: AppWidgetProviderInfo): Boolean {
    if (providerInfo.configure == null) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      val optional =
        (providerInfo.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_CONFIGURATION_OPTIONAL) != 0
      if (optional) return false
    }
    return true
  }

  /**
   * Uses [AppWidgetHost.startAppWidgetConfigureActivityForResult] so non-exported
   * configuration activities work properly on Android 12+ (API 31+).
   */
  fun startConfigureActivity(
    activity: Activity,
    appWidgetId: Int,
    requestCode: Int,
  ): Boolean =
    runCatching {
      appWidgetHost.startAppWidgetConfigureActivityForResult(
        activity,
        appWidgetId,
        0,
        requestCode,
        null,
      )
      true
    }.getOrElse { false }

  fun createHostView(
    viewContext: Context,
    appWidgetId: Int,
  ): AppWidgetHostView? {
    val info = getAppWidgetInfo(appWidgetId) ?: return null
    return runCatching {
      val hostView = appWidgetHost.createView(viewContext, appWidgetId, info)
      hostView.setAppWidget(appWidgetId, info)
      hostView
    }.getOrNull()
  }

  @Suppress("DEPRECATION")
  fun updateWidgetSize(
    hostView: AppWidgetHostView,
    widthDp: Int,
    heightDp: Int,
  ) {
    val safeW = widthDp.coerceAtLeast(100)
    val safeH = heightDp.coerceAtLeast(HostedWidgetRecord.MIN_HEIGHT_DP)
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hostView.updateAppWidgetSize(Bundle.EMPTY, listOf(SizeF(safeW.toFloat(), safeH.toFloat())))
      } else {
        hostView.updateAppWidgetSize(null, safeW, safeH, safeW, safeH)
      }
    }
  }

  suspend fun loadAvailableWidgetGroups(): List<AvailableWidgetGroup> =
    withContext(Dispatchers.IO) {
      val pm = appContext.packageManager
      val densityDpi = appContext.resources.displayMetrics.densityDpi
      val providers =
        runCatching { appWidgetManager.installedProviders }
          .getOrDefault(emptyList())
          .filter { info ->
            val isHomeCategory =
              (info.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0
            val isHiddenFromPicker =
              Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER) != 0
            isHomeCategory && !isHiddenFromPicker
          }

      providers
        .groupBy { it.provider.packageName }
        .mapNotNull { (packageName, pkgProviders) ->
          val appInfo = runCatching { pm.getApplicationInfo(packageName, 0) }.getOrNull()
          val appLabel =
            appInfo?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() }
              ?.takeIf { it.isNotBlank() }
              ?: packageName
          val appIcon =
            appInfo?.let {
              runCatching { drawableToSafeBitmap(pm.getApplicationIcon(it), 96) }.getOrNull()
            }

          val entries =
            pkgProviders
              .map { info ->
                val label =
                  runCatching { info.loadLabel(pm) }.getOrNull()?.takeIf { it.isNotBlank() }
                    ?: info.provider.shortClassName.substringAfterLast('.')
                val description =
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    runCatching { info.loadDescription(appContext)?.toString()?.trim() }
                      .getOrNull()
                      ?.takeIf { it.isNotEmpty() }
                  } else {
                    null
                  }

                val (cellW, cellH) = computeCellSpan(info)
                val defaultWidthSpan =
                  when {
                    cellW <= 2 -> 2
                    cellW == 3 -> 3
                    else -> 4
                  }
                val defaultHeightDp =
                  (cellH * 78)
                    .coerceIn(HostedWidgetRecord.MIN_HEIGHT_DP, 260)

                val previewDrawable =
                  runCatching { info.loadPreviewImage(appContext, densityDpi) }.getOrNull()
                    ?: runCatching { info.loadIcon(appContext, densityDpi) }.getOrNull()
                val previewBitmap = previewDrawable?.let { drawableToSafeBitmap(it, 260) }

                val isReconfigurable =
                  info.configure != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    (info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE) != 0

                AvailableWidgetEntry(
                  providerInfo = info,
                  providerComponent = info.provider.flattenToString(),
                  label = label,
                  description = description,
                  cellWidth = cellW,
                  cellHeight = cellH,
                  defaultWidthSpan = defaultWidthSpan,
                  defaultHeightDp = defaultHeightDp,
                  previewImage = previewBitmap,
                  requiresConfigure = requiresInitialConfiguration(info),
                  isReconfigurable = isReconfigurable,
                )
              }
              .sortedBy { it.label.lowercase() }

          if (entries.isEmpty()) {
            null
          } else {
            AvailableWidgetGroup(
              packageName = packageName,
              appLabel = appLabel,
              appIcon = appIcon,
              widgets = entries,
            )
          }
        }
        .sortedBy { it.appLabel.lowercase() }
    }

  private fun computeCellSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val tw = info.targetCellWidth
      val th = info.targetCellHeight
      if (tw > 0 && th > 0) {
        return tw.coerceIn(1, 4) to th.coerceIn(1, 5)
      }
    }
    val density = appContext.resources.displayMetrics.density.coerceAtLeast(1f)
    val minWdp = info.minWidth / density
    val minHdp = info.minHeight / density
    val cellsW = ceil((minWdp + 30f) / 70f).roundToInt().coerceIn(1, 4)
    val cellsH = ceil((minHdp + 30f) / 70f).roundToInt().coerceIn(1, 5)
    return cellsW to cellsH
  }

  private fun drawableToSafeBitmap(drawable: Drawable, maxDimPx: Int): ImageBitmap? {
    return runCatching {
      if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
        val src = drawable.bitmap
        val longest = max(src.width, src.height)
        if (longest <= maxDimPx) {
          return@runCatching src.asImageBitmap()
        }
        val ratio = maxDimPx.toFloat() / longest.toFloat()
        val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
        return@runCatching Bitmap.createScaledBitmap(src, w, h, true).asImageBitmap()
      }

      val intrinsicW = drawable.intrinsicWidth.takeIf { it > 0 } ?: maxDimPx
      val intrinsicH = drawable.intrinsicHeight.takeIf { it > 0 } ?: maxDimPx
      val longest = max(intrinsicW, intrinsicH)
      val scale = if (longest > maxDimPx) maxDimPx.toFloat() / longest.toFloat() else 1f
      val outW = (intrinsicW * scale).roundToInt().coerceAtLeast(1)
      val outH = (intrinsicH * scale).roundToInt().coerceAtLeast(1)

      val bitmap = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)
      drawable.setBounds(0, 0, outW, outH)
      drawable.draw(canvas)
      bitmap.asImageBitmap()
    }.getOrNull()
  }

  companion object {
    const val APPWIDGET_HOST_ID = 0x4E4F52 // "NOR"
    const val REQUEST_CONFIGURE_WIDGET = 8042
    const val REQUEST_RECONFIGURE_WIDGET = 8043
    const val MAX_HOSTED_WIDGETS = 6

    @Volatile
    private var instance: LauncherWidgetHostManager? = null

    fun getInstance(context: Context): LauncherWidgetHostManager =
      instance
        ?: synchronized(this) {
          instance ?: LauncherWidgetHostManager(context.applicationContext).also { instance = it }
        }
  }
}
