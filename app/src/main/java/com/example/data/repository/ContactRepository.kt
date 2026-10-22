package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.data.model.ResolvedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ContactLookupResult {
    data class Single(val contact: ResolvedContact) : ContactLookupResult()
    data class Multiple(val candidates: List<ResolvedContact>) : ContactLookupResult()
    object NotFound : ContactLookupResult()
    object PermissionRequired : ContactLookupResult()
}

class ContactRepository(private val context: Context) {

    fun hasContactPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun resolveContact(queryName: String): ContactLookupResult = withContext(Dispatchers.IO) {
        if (!hasContactPermission()) {
            return@withContext ContactLookupResult.PermissionRequired
        }

        val cleanQuery = queryName.trim().lowercase()
        val allContacts = fetchAllPhoneContacts()

        if (allContacts.isEmpty()) {
            return@withContext ContactLookupResult.NotFound
        }

        // Aliases mapping (e.g. Papa -> Dad, Father, Papa Ji)
        val aliases = getAliasesFor(cleanQuery)

        // 1. Exact match
        val exactMatches = allContacts.filter { contact ->
            val name = contact.name.lowercase()
            name == cleanQuery || aliases.any { alias -> name == alias }
        }
        if (exactMatches.size == 1) {
            return@withContext ContactLookupResult.Single(exactMatches.first())
        }
        if (exactMatches.size > 1) {
            return@withContext ContactLookupResult.Multiple(exactMatches)
        }

        // 2. Contains match
        val partialMatches = allContacts.filter { contact ->
            val name = contact.name.lowercase()
            name.contains(cleanQuery) || aliases.any { alias -> name.contains(alias) }
        }

        when {
            partialMatches.size == 1 -> ContactLookupResult.Single(partialMatches.first())
            partialMatches.size > 1 -> ContactLookupResult.Multiple(partialMatches.take(5))
            else -> ContactLookupResult.NotFound
        }
    }

    suspend fun fetchAllPhoneContacts(): List<ResolvedContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<ResolvedContact>()
        if (!hasContactPermission()) return@withContext contacts

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )

        try {
            context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                val seenNumbers = mutableSetOf<String>()

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                    val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                    val cleanNum = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isNotEmpty() && seenNumbers.add(cleanNum)) {
                        contacts.add(
                            ResolvedContact(
                                id = id,
                                name = name,
                                phoneNumber = number,
                                photoUri = photo
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        contacts
    }

    private fun getAliasesFor(name: String): List<String> {
        return when (name) {
            "papa", "pitaji", "dad", "father", "daddy" -> listOf("papa", "dad", "father", "daddy", "papa ji", "pitaji")
            "mummy", "mom", "mother", "maa", "mataji" -> listOf("mummy", "mom", "mother", "maa", "mum", "mataji")
            "bhai", "brother" -> listOf("bhai", "brother", "bro")
            "behan", "sister", "didi" -> listOf("sister", "didi", "sis", "behan")
            else -> emptyList()
        }
    }
}
