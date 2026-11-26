package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NorNotificationListenerService : NotificationListenerService() {

  override fun onListenerConnected() {
    super.onListenerConnected()
    NotificationDotStore.setConnected(true)
    refreshCounts()
  }

  override fun onListenerDisconnected() {
    super.onListenerDisconnected()
    NotificationDotStore.setConnected(false)
    // Ask the system to rebind automatically if access is still granted.
    if (NotificationAccessHelper.isAccessGranted(applicationContext)) {
      runCatching {
        requestRebind(ComponentName(this, NorNotificationListenerService::class.java))
      }
    }
  }

  override fun onNotificationPosted(sbn: StatusBarNotification?) {
    refreshCounts()
  }

  override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    refreshCounts()
  }

  private fun refreshCounts() {
    val active =
      runCatching { activeNotifications }
        .getOrNull()
        ?: return

    val counts = HashMap<String, Int>()
    val selfPackage = packageName

    for (sbn in active) {
      val pkg = sbn.packageName ?: continue
      if (pkg == selfPackage) continue

      val notification = sbn.notification ?: continue
      val flags = notification.flags

      // Skip group summary wrappers and persistent ongoing/foreground service notifications.
      val isGroupSummary = (flags and Notification.FLAG_GROUP_SUMMARY) != 0
      val isForegroundService = (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0
      if (isGroupSummary || sbn.isOngoing || isForegroundService) continue

      counts[pkg] = (counts[pkg] ?: 0) + 1
    }

    NotificationDotStore.updateCounts(counts)
  }
}
