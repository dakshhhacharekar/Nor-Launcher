package com.example.data.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import com.example.R
import com.example.data.model.QuickActionItem
import com.example.data.model.QuickActionType
import java.util.Locale

/**
 * Matches typed queries against actionable system shortcuts ("call", "alarm 7:30", "wifi",
 * "bluetooth", "sms", "settings") and launches the appropriate Android Intent safely.
 */
object QuickActionMatcher {

  private val timeRegex =
    Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b""", RegexOption.IGNORE_CASE)
  private val phoneDigitsRegex = Regex("""^[\d\s+\-().]{4,}$""")

  fun match(context: Context, rawQuery: String): List<QuickActionItem> {
    val trimmed = rawQuery.trim()
    val lower = trimmed.lowercase(Locale.getDefault())

    if (lower.isEmpty()) {
      return defaultQuickActions(context)
    }

    val actions = mutableListOf<QuickActionItem>()

    // 1. Call quick action ("call", "dial", "call 555-1234", or raw phone number)
    val isCallKeyword = lower.startsWith("call") || lower.startsWith("dial") || lower == "phone"
    val looksLikeDirectPhone =
      phoneDigitsRegex.matches(trimmed) && trimmed.count { it.isDigit() } >= 5

    if (isCallKeyword || looksLikeDirectPhone) {
      val arg =
        if (isCallKeyword) {
          trimmed.substringAfter(' ', missingDelimiterValue = "").trim()
        } else {
          trimmed
        }
      val dialNumber = arg.takeIf { it.any(Char::isDigit) }
      actions +=
        QuickActionItem(
          id = "quick_call_${dialNumber.orEmpty()}",
          type = QuickActionType.CALL,
          title =
            if (dialNumber != null) {
              context.getString(R.string.quick_action_call_number_title, dialNumber)
            } else {
              context.getString(R.string.quick_action_call_title)
            },
          subtitle = context.getString(R.string.quick_action_call_subtitle),
          phoneNumber = dialNumber,
        )
    }

    // 2. Alarm quick action ("alarm", "alarm 7:30", "set alarm 6:45 am")
    if (lower.contains("alarm") || lower.startsWith("wake")) {
      val parsedTime = parseAlarmTime(trimmed)
      val formattedTime = parsedTime?.let { (h, m) -> String.format(Locale.getDefault(), "%02d:%02d", h, m) }
      actions +=
        QuickActionItem(
          id = "quick_alarm_${formattedTime.orEmpty()}",
          type = QuickActionType.ALARM,
          title =
            if (formattedTime != null) {
              context.getString(R.string.quick_action_alarm_time_title, formattedTime)
            } else {
              context.getString(R.string.quick_action_alarm_title)
            },
          subtitle = context.getString(R.string.quick_action_alarm_subtitle),
          alarmHour = parsedTime?.first,
          alarmMinute = parsedTime?.second,
        )
    }

    // 3. Wi-Fi quick action ("wifi", "wi-fi", "internet", "network")
    if (
      "wifi".startsWith(lower) ||
        lower.contains("wifi") ||
        lower.contains("wi-fi") ||
        lower.startsWith("internet") ||
        lower.startsWith("netw")
    ) {
      actions +=
        QuickActionItem(
          id = "quick_wifi",
          type = QuickActionType.WIFI,
          title = context.getString(R.string.quick_action_wifi_title),
          subtitle = context.getString(R.string.quick_action_wifi_subtitle),
        )
    }

    // 4. Bluetooth quick action
    if ("bluetooth".startsWith(lower) && lower.length >= 2 || lower == "bt") {
      actions +=
        QuickActionItem(
          id = "quick_bluetooth",
          type = QuickActionType.BLUETOOTH,
          title = context.getString(R.string.quick_action_bluetooth_title),
          subtitle = context.getString(R.string.quick_action_bluetooth_subtitle),
        )
    }

    // 5. SMS / Message quick action
    if (lower.startsWith("sms") || lower.startsWith("text") || lower.startsWith("msg")) {
      val arg = trimmed.substringAfter(' ', missingDelimiterValue = "").trim()
      val number = arg.takeIf { it.any(Char::isDigit) }
      actions +=
        QuickActionItem(
          id = "quick_sms_${number.orEmpty()}",
          type = QuickActionType.SMS,
          title = context.getString(R.string.quick_action_sms_title),
          subtitle = context.getString(R.string.quick_action_sms_subtitle),
          phoneNumber = number,
        )
    }

    // 6. Settings quick action
    if ("settings".startsWith(lower) && lower.length >= 3) {
      actions +=
        QuickActionItem(
          id = "quick_settings",
          type = QuickActionType.SETTINGS,
          title = context.getString(R.string.quick_action_settings_title),
          subtitle = context.getString(R.string.quick_action_settings_subtitle),
        )
    }

    return actions
  }

  private fun defaultQuickActions(context: Context): List<QuickActionItem> =
    listOf(
      QuickActionItem(
        id = "quick_call_default",
        type = QuickActionType.CALL,
        title = context.getString(R.string.quick_action_call_title),
        subtitle = context.getString(R.string.quick_action_call_subtitle),
      ),
      QuickActionItem(
        id = "quick_alarm_default",
        type = QuickActionType.ALARM,
        title = context.getString(R.string.quick_action_alarm_title),
        subtitle = context.getString(R.string.quick_action_alarm_subtitle),
      ),
      QuickActionItem(
        id = "quick_wifi_default",
        type = QuickActionType.WIFI,
        title = context.getString(R.string.quick_action_wifi_title),
        subtitle = context.getString(R.string.quick_action_wifi_subtitle),
      ),
      QuickActionItem(
        id = "quick_settings_default",
        type = QuickActionType.SETTINGS,
        title = context.getString(R.string.quick_action_settings_title),
        subtitle = context.getString(R.string.quick_action_settings_subtitle),
      ),
    )

  /** Extracts contact filter text when the user types e.g. "call Alex" or "sms Maya". */
  fun extractContactQuery(rawQuery: String): String {
    val trimmed = rawQuery.trim()
    val lower = trimmed.lowercase(Locale.getDefault())
    val prefixes = listOf("call ", "dial ", "sms ", "text ", "msg ")
    val matchedPrefix = prefixes.firstOrNull { lower.startsWith(it) }
    return if (matchedPrefix != null) {
      trimmed.drop(matchedPrefix.length).trim()
    } else {
      trimmed
    }
  }

  private fun parseAlarmTime(text: String): Pair<Int, Int>? {
    val match = timeRegex.find(text) ?: return null
    var hour = match.groupValues[1].toIntOrNull() ?: return null
    val minute = match.groupValues[2].toIntOrNull() ?: 0
    val amPm = match.groupValues[3].lowercase(Locale.US)

    if (minute !in 0..59) return null
    when (amPm) {
      "pm" -> {
        if (hour !in 1..12) return null
        if (hour < 12) hour += 12
      }
      "am" -> {
        if (hour !in 1..12) return null
        if (hour == 12) hour = 0
      }
      else -> {
        if (hour !in 0..23) return null
      }
    }
    return hour to minute
  }

  fun execute(context: Context, action: QuickActionItem): Boolean =
    when (action.type) {
      QuickActionType.CALL -> openDialer(context, action.phoneNumber)
      QuickActionType.ALARM -> openAlarm(context, action.alarmHour, action.alarmMinute)
      QuickActionType.WIFI -> openWifiPanelOrSettings(context)
      QuickActionType.BLUETOOTH -> startSafe(context, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
      QuickActionType.SMS -> openSms(context, action.phoneNumber)
      QuickActionType.SETTINGS -> startSafe(context, Intent(Settings.ACTION_SETTINGS))
    }

  fun openDialer(context: Context, phoneNumber: String?): Boolean {
    val uri =
      if (!phoneNumber.isNullOrBlank()) {
        Uri.fromParts("tel", phoneNumber, null)
      } else {
        Uri.parse("tel:")
      }
    return startSafe(context, Intent(Intent.ACTION_DIAL, uri))
  }

  fun openSms(context: Context, phoneNumber: String?): Boolean {
    val uri =
      if (!phoneNumber.isNullOrBlank()) {
        Uri.parse("smsto:${Uri.encode(phoneNumber)}")
      } else {
        Uri.parse("smsto:")
      }
    return startSafe(context, Intent(Intent.ACTION_SENDTO, uri))
  }

  private fun openAlarm(context: Context, hour: Int?, minute: Int?): Boolean {
    if (hour != null && minute != null) {
      val setIntent =
        Intent(AlarmClock.ACTION_SET_ALARM).apply {
          putExtra(AlarmClock.EXTRA_HOUR, hour)
          putExtra(AlarmClock.EXTRA_MINUTES, minute)
          putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        }
      if (startSafe(context, setIntent)) return true
    }
    return startSafe(context, Intent(AlarmClock.ACTION_SHOW_ALARMS)) ||
      startSafe(context, Intent(AlarmClock.ACTION_SET_ALARM))
  }

  private fun openWifiPanelOrSettings(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      if (startSafe(context, Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))) {
        return true
      }
    }
    return startSafe(context, Intent(Settings.ACTION_WIFI_SETTINGS))
  }

  private fun startSafe(context: Context, intent: Intent): Boolean =
    runCatching {
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
      true
    }.getOrElse { false }
}
