package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-scoped reactive store updated by [NorNotificationListenerService].
 * Holds only package-name -> unread count mappings (never notification content or text).
 */
object NotificationDotStore {
  private val _countsByPackage = MutableStateFlow<Map<String, Int>>(emptyMap())
  val countsByPackage: StateFlow<Map<String, Int>> = _countsByPackage.asStateFlow()

  private val _isConnected = MutableStateFlow(false)
  val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

  fun updateCounts(counts: Map<String, Int>) {
    _countsByPackage.value = counts
  }

  fun setConnected(connected: Boolean) {
    _isConnected.value = connected
    if (!connected) {
      _countsByPackage.value = emptyMap()
    }
  }
}
