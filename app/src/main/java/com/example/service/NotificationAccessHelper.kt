package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

object NotificationAccessHelper {

  fun isAccessGranted(context: Context): Boolean =
    runCatching {
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
      }
      .getOrDefault(false)

  fun openNotificationAccessSettings(context: Context): Boolean {
    val primaryIntent =
      Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    val openedPrimary = runCatching { context.startActivity(primaryIntent) }.isSuccess
    if (openedPrimary) return true

    val fallbackIntent =
      Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    return runCatching { context.startActivity(fallbackIntent) }.isSuccess
  }

  fun requestRebindIfPermitted(context: Context) {
    if (!isAccessGranted(context)) return
    runCatching {
      NotificationListenerService.requestRebind(
        ComponentName(context, NorNotificationListenerService::class.java)
      )
    }
  }
}
