package com.example.data.widgets

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEventItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads real upcoming calendar events from Android's [CalendarContract.Instances]
 * with on-demand [Manifest.permission.READ_CALENDAR] permission checking and safe intent launchers.
 */
class CalendarRepository(
  private val context: Context,
) {
  fun hasCalendarPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
      PackageManager.PERMISSION_GRANTED

  suspend fun loadUpcomingEvents(
    maxCount: Int = MAX_EVENTS,
    horizonDays: Long = HORIZON_DAYS,
  ): List<CalendarEventItem> =
    withContext(Dispatchers.IO) {
      if (!hasCalendarPermission()) return@withContext emptyList()

      val nowMs = System.currentTimeMillis()
      val endMs = nowMs + horizonDays * MILLIS_PER_DAY

      val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
      ContentUris.appendId(builder, nowMs)
      ContentUris.appendId(builder, endMs)

      val projection =
        arrayOf(
          CalendarContract.Instances.EVENT_ID,
          CalendarContract.Instances.TITLE,
          CalendarContract.Instances.BEGIN,
          CalendarContract.Instances.END,
          CalendarContract.Instances.ALL_DAY,
          CalendarContract.Instances.EVENT_LOCATION,
        )

      val results = ArrayList<CalendarEventItem>(maxCount)
      val seenEventKeys = HashSet<String>()

      runCatching {
        context.contentResolver.query(
          builder.build(),
          projection,
          "${CalendarContract.Instances.VISIBLE} = 1",
          null,
          "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
          val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
          val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
          val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
          val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
          val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
          val locIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

          while (cursor.moveToNext() && results.size < maxCount) {
            if (idIdx < 0 || beginIdx < 0) continue
            val eventId = cursor.getLong(idIdx)
            val start = cursor.getLong(beginIdx)
            val end = if (endIdx >= 0) cursor.getLong(endIdx) else start
            val allDay = allDayIdx >= 0 && cursor.getInt(allDayIdx) == 1
            // Skip past events that have already ended (except all-day events for today)
            if (!allDay && end > 0L && end < nowMs) continue

            val title =
              (if (titleIdx >= 0) cursor.getString(titleIdx) else null)
                ?.trim()
                .orEmpty()
                .ifEmpty { "Untitled event" }
            val location =
              (if (locIdx >= 0) cursor.getString(locIdx) else null)?.trim().orEmpty()

            val dedupKey = "$eventId:$start"
            if (!seenEventKeys.add(dedupKey)) continue

            results.add(
              CalendarEventItem(
                id = eventId,
                title = title,
                startMillis = start,
                endMillis = end,
                allDay = allDay,
                location = location,
              )
            )
          }
        }
      }

      results
    }

  companion object {
    private const val MAX_EVENTS = 6
    private const val HORIZON_DAYS = 7L
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun openCalendarEvent(context: Context, eventId: Long): Boolean =
      runCatching {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent =
          Intent(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
      }.getOrElse { openCalendarApp(context) }

    fun openCalendarApp(context: Context): Boolean {
      val timeUri =
        CalendarContract.CONTENT_URI.buildUpon()
          .appendPath("time")
          .appendPath(System.currentTimeMillis().toString())
          .build()
      val viewTimeIntent =
        Intent(Intent.ACTION_VIEW, timeUri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      if (runCatching { context.startActivity(viewTimeIntent) }.isSuccess) {
        return true
      }

      val categoryIntent =
        Intent(Intent.ACTION_MAIN).apply {
          addCategory(Intent.CATEGORY_APP_CALENDAR)
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
      return runCatching {
        context.startActivity(categoryIntent)
        true
      }.getOrElse { false }
    }
  }
}
