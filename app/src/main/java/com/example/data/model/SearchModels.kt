package com.example.data.model

data class ContactMatch(
  val id: Long,
  val displayName: String,
  val phoneNumber: String,
)

data class CalculatorResult(
  val expression: String,
  val formattedResult: String,
)

enum class QuickActionType {
  CALL,
  ALARM,
  WIFI,
  BLUETOOTH,
  SMS,
  SETTINGS,
}

data class QuickActionItem(
  val id: String,
  val type: QuickActionType,
  val title: String,
  val subtitle: String,
  val phoneNumber: String? = null,
  val alarmHour: Int? = null,
  val alarmMinute: Int? = null,
)

data class SmartSearchState(
  val query: String = "",
  val recentQueries: List<String> = emptyList(),
  val matchingApps: List<AppItem> = emptyList(),
  val calculatorResult: CalculatorResult? = null,
  val quickActions: List<QuickActionItem> = emptyList(),
  val contacts: List<ContactMatch> = emptyList(),
  val hasContactsPermission: Boolean = false,
  val contactsPermissionDeniedOnce: Boolean = false,
  val webSuggestions: List<String> = emptyList(),
  val isLoadingSuggestions: Boolean = false,
)
