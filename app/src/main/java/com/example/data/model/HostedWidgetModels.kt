package com.example.data.model

import android.appwidget.AppWidgetProviderInfo
import androidx.compose.ui.graphics.ImageBitmap

data class HostedWidgetRecord(
  val appWidgetId: Int,
  val providerComponent: String,
  val label: String,
  val widthSpan: Int = DEFAULT_WIDTH_SPAN,
  val heightDp: Int = DEFAULT_HEIGHT_DP,
) {
  val packageName: String
    get() = providerComponent.substringBefore('/')

  companion object {
    const val MIN_WIDTH_SPAN = 2
    const val MAX_WIDTH_SPAN = 4
    const val DEFAULT_WIDTH_SPAN = 4
    const val MIN_HEIGHT_DP = 96
    const val MAX_HEIGHT_DP = 360
    const val DEFAULT_HEIGHT_DP = 160
    const val HEIGHT_STEP_DP = 36
  }
}

data class AvailableWidgetEntry(
  val providerInfo: AppWidgetProviderInfo,
  val providerComponent: String,
  val label: String,
  val description: String?,
  val cellWidth: Int,
  val cellHeight: Int,
  val defaultWidthSpan: Int,
  val defaultHeightDp: Int,
  val previewImage: ImageBitmap?,
  val requiresConfigure: Boolean,
  val isReconfigurable: Boolean,
)

data class AvailableWidgetGroup(
  val packageName: String,
  val appLabel: String,
  val appIcon: ImageBitmap?,
  val widgets: List<AvailableWidgetEntry>,
)
