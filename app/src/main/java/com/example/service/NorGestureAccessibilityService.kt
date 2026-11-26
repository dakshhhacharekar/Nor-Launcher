package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GlobalActionResult {
  data object Success : GlobalActionResult
  data object ServiceNotConnected : GlobalActionResult
  data object UnsupportedApiLevel : GlobalActionResult
  data object Failed : GlobalActionResult
}

/**
 * Minimal, privacy-first AccessibilityService used strictly to execute user-assigned
 * global launcher actions (Lock Screen, Expand Notifications, Expand Quick Settings).
 * Window content inspection is disabled in xml/accessibility_service_config.xml.
 */
class NorGestureAccessibilityService : AccessibilityService() {

  override fun onServiceConnected() {
    super.onServiceConnected()
    activeService = WeakReference(this)
    _isConnected.value = true
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    // Intentionally empty: this service does not observe or inspect screen events.
  }

  override fun onInterrupt() {
    // No ongoing feedback to interrupt.
  }

  override fun onUnbind(intent: Intent?): Boolean {
    clearIfCurrent()
    return super.onUnbind(intent)
  }

  override fun onDestroy() {
    clearIfCurrent()
    super.onDestroy()
  }

  private fun clearIfCurrent() {
    if (activeService?.get() === this) {
      activeService = null
    }
    _isConnected.value = false
  }

  companion object {
    @Volatile private var activeService: WeakReference<NorGestureAccessibilityService>? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun performLockScreen(): GlobalActionResult {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        return GlobalActionResult.UnsupportedApiLevel
      }
      val service = activeService?.get() ?: return GlobalActionResult.ServiceNotConnected
      val ok = runCatching { service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) }.getOrDefault(false)
      return if (ok) GlobalActionResult.Success else GlobalActionResult.Failed
    }

    fun performExpandNotifications(): GlobalActionResult {
      val service = activeService?.get() ?: return GlobalActionResult.ServiceNotConnected
      val ok =
        runCatching { service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) }.getOrDefault(false)
      return if (ok) GlobalActionResult.Success else GlobalActionResult.Failed
    }

    fun performExpandQuickSettings(): GlobalActionResult {
      val service = activeService?.get() ?: return GlobalActionResult.ServiceNotConnected
      val ok =
        runCatching { service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS) }.getOrDefault(false)
      return if (ok) GlobalActionResult.Success else GlobalActionResult.Failed
    }
  }
}
