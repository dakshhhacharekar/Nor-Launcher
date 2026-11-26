package com.example.data.model

import androidx.compose.ui.graphics.ImageBitmap

enum class SmartWidgetType(val id: String) {
  CLOCK("clock"),
  WEATHER("weather"),
  MUSIC("music"),
  CALENDAR("calendar"),
  BATTERY("battery"),
  NOTE("note"),
  ICON_GROUP("icon_group");

  companion object {
    val defaultStackOrder: List<SmartWidgetType> =
      listOf(CLOCK, WEATHER, MUSIC, CALENDAR, BATTERY, NOTE, ICON_GROUP)

    fun fromId(id: String?): SmartWidgetType? =
      entries.firstOrNull { it.id == id }
  }
}

data class QuickNoteData(
  val title: String = "",
  val body: String = "",
  val updatedAtEpochMs: Long = 0L,
) {
  val isEmpty: Boolean
    get() = title.isBlank() && body.isBlank()
}

data class CalendarEventItem(
  val id: Long,
  val title: String,
  val startMillis: Long,
  val endMillis: Long,
  val allDay: Boolean,
  val location: String = "",
)

data class MediaPlaybackState(
  val title: String = "",
  val artist: String = "",
  val packageName: String? = null,
  val isPlaying: Boolean = false,
  val hasActiveSession: Boolean = false,
  val artwork: ImageBitmap? = null,
)

data class SmartWidgetsConfig(
  val stackWidgets: List<SmartWidgetType> = SmartWidgetType.defaultStackOrder,
  val pinnedWidgets: List<SmartWidgetType> = emptyList(),
  val quickNote: QuickNoteData = QuickNoteData(),
  val iconGroupPackages: List<String> = emptyList(),
)
