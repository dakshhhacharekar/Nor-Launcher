package com.example.service

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

object AccessibilityAccessHelper {

  fun isServiceEnabled(context: Context): Boolean {
    if (NorGestureAccessibilityService.isConnected.value) return true

    val expectedComponent =
      ComponentName(context, NorGestureAccessibilityService::class.java).flattenToString()
    val expectedShort =
      ComponentName(context, NorGestureAccessibilityService::class.java).flattenToShortString()

    val manager =
      context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    val enabledList =
      runCatching {
          manager?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        }
        .getOrNull()
        .orEmpty()

    if (
      enabledList.any { info ->
        info.id.equals(expectedComponent, ignoreCase = true) ||
          info.id.equals(expectedShort, ignoreCase = true) ||
          info.resolveInfo?.serviceInfo?.name == NorGestureAccessibilityService::class.java.name
      }
    ) {
      return true
    }

    val rawSetting =
      runCatching {
          Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
          )
        }
        .getOrNull()
        .orEmpty()

    return rawSetting
      .split(':')
      .any { entry ->
        entry.equals(expectedComponent, ignoreCase = true) ||
          entry.equals(expectedShort, ignoreCase = true)
      }
  }

  fun openAccessibilitySettings(context: Context): Boolean {
    val primary =
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    if (runCatching { context.startActivity(primary) }.isSuccess) return true

    val fallback =
      Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return runCatching { context.startActivity(fallback) }.isSuccess
  }
}
