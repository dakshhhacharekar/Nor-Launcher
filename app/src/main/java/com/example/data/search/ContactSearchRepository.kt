package com.example.data.search

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.ContactMatch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Queries device contacts safely using [ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI]
 * only when [Manifest.permission.READ_CONTACTS] has been granted at runtime.
 */
class ContactSearchRepository(
  private val context: Context,
) {

  fun hasContactsPermission(): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
      PackageManager.PERMISSION_GRANTED

  suspend fun searchContacts(rawQuery: String, limit: Int = 6): List<ContactMatch> =
    withContext(Dispatchers.IO) {
      val query = rawQuery.trim()
      if (query.length < 2 || !hasContactsPermission()) {
        return@withContext emptyList()
      }

      val filterUri =
        Uri.withAppendedPath(
          ContactsContract.CommonDataKinds.Phone.CONTENT_FILTER_URI,
          Uri.encode(query),
        )
      val projection =
        arrayOf(
          ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
          ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
          ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

      val results = mutableListOf<ContactMatch>()
      val seenKeys = mutableSetOf<String>()

      runCatching {
        context.contentResolver.query(
          filterUri,
          projection,
          null,
          null,
          "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC",
        )?.use { cursor ->
          val idCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
          val nameCol =
            cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
          val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

          while (cursor.moveToNext() && results.size < limit) {
            if (idCol < 0 || nameCol < 0 || numCol < 0) break
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol)?.trim().orEmpty()
            val number = cursor.getString(numCol)?.trim().orEmpty()
            if (name.isEmpty() || number.isEmpty()) continue

            val normalizedNumber = number.filter { it.isDigit() || it == '+' }
            val dedupeKey = "$id|$normalizedNumber"
            if (seenKeys.add(dedupeKey)) {
              results += ContactMatch(id = id, displayName = name, phoneNumber = number)
            }
          }
        }
      }

      results
    }
}
