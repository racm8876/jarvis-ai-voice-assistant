package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.net.URLEncoder

sealed class ActionExecutionResult {
    data class Success(val message: String) : ActionExecutionResult()
    data class Failure(val reason: String) : ActionExecutionResult()
    data class AppNotInstalled(val appName: String, val storeUri: Uri) : ActionExecutionResult()
}

class AppLauncherRepository(private val context: Context) {

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun openApp(packageName: String, appName: String): ActionExecutionResult {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ActionExecutionResult.Success("Opening $appName.")
        } else {
            val playStoreUri = Uri.parse("market://details?id=$packageName")
            ActionExecutionResult.AppNotInstalled(appName, playStoreUri)
        }
    }

    fun openYouTubeSearch(query: String): ActionExecutionResult {
        val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query }
        val youtubePackage = "com.google.android.youtube"

        val appIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage(youtubePackage)
            putExtra("query", query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            if (isAppInstalled(youtubePackage)) {
                context.startActivity(appIntent)
                ActionExecutionResult.Success("Searching YouTube for \"$query\".")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                ActionExecutionResult.Success("Opening YouTube search in browser for \"$query\".")
            }
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
            ActionExecutionResult.Success("Searching YouTube for \"$query\".")
        }
    }

    fun openWhatsAppMessage(rawPhoneNumber: String?, messageText: String?): ActionExecutionResult {
        val cleanNumber = rawPhoneNumber?.replace(Regex("[^0-9]"), "") ?: ""
        val text = messageText ?: ""
        val encodedText = try { URLEncoder.encode(text, "UTF-8") } catch (e: Exception) { text }

        val uriString = if (cleanNumber.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedText"
        } else {
            "whatsapp://send?text=$encodedText"
        }

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            if (isAppInstalled("com.whatsapp") || isAppInstalled("com.whatsapp.w4b")) {
                context.startActivity(intent)
                ActionExecutionResult.Success(
                    if (cleanNumber.isNotEmpty()) "Opening WhatsApp chat with message prefilled."
                    else "Opening WhatsApp share sheet with message."
                )
            } else {
                ActionExecutionResult.AppNotInstalled("WhatsApp", Uri.parse("market://details?id=com.whatsapp"))
            }
        } catch (e: Exception) {
            // Fallback without package restriction
            intent.setPackage(null)
            try {
                context.startActivity(intent)
                ActionExecutionResult.Success("Opening WhatsApp message.")
            } catch (ex: Exception) {
                ActionExecutionResult.Failure("Unable to open WhatsApp: ${ex.message}")
            }
        }
    }

    fun startWhatsAppCall(phoneNumber: String, isVideo: Boolean): ActionExecutionResult {
        val mimeType = if (isVideo) {
            "vnd.android.cursor.item/vnd.com.whatsapp.video.call"
        } else {
            "vnd.android.cursor.item/vnd.com.whatsapp.voip.call"
        }

        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val dataId = getWhatsAppContactDataId(cleanNumber, mimeType)

        if (dataId != null) {
            val callIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse("content://com.android.contacts/data/$dataId"), mimeType)
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return try {
                context.startActivity(callIntent)
                ActionExecutionResult.Success("Initiating WhatsApp ${if (isVideo) "video" else "voice"} call.")
            } catch (e: Exception) {
                // Fallback to opening the chat
                openWhatsAppMessage(phoneNumber, null)
            }
        } else {
            // Fallback: Open WhatsApp conversation with contact
            val res = openWhatsAppMessage(phoneNumber, null)
            return if (res is ActionExecutionResult.Success) {
                ActionExecutionResult.Success("Opened WhatsApp chat for contact. Tap ${if (isVideo) "video" else "voice"} call icon.")
            } else {
                res
            }
        }
    }

    private fun getWhatsAppContactDataId(cleanNumber: String, mimeType: String): Long? {
        val uri = ContactsContract.Data.CONTENT_URI
        val projection = arrayOf(ContactsContract.Data._ID, ContactsContract.Data.DATA1)
        val selection = "${ContactsContract.Data.MIMETYPE} = ?"
        val selectionArgs = arrayOf(mimeType)

        try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.Data._ID)
                val data1Idx = cursor.getColumnIndex(ContactsContract.Data.DATA1)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else -1L
                    val data1 = if (data1Idx >= 0) cursor.getString(data1Idx) ?: "" else ""
                    val cleanData = data1.replace(Regex("[^0-9]"), "")
                    if (cleanData.endsWith(cleanNumber) || cleanNumber.endsWith(cleanData)) {
                        return id
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun makePhoneCall(phoneNumber: String, useDirectCall: Boolean = true): ActionExecutionResult {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (cleanNumber.isEmpty()) {
            return ActionExecutionResult.Failure("Invalid phone number.")
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        val intent = if (useDirectCall && hasCallPermission) {
            Intent(Intent.ACTION_CALL, Uri.parse("tel:$cleanNumber"))
        } else {
            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            ActionExecutionResult.Success(
                if (useDirectCall && hasCallPermission) "Calling $cleanNumber..."
                else "Opening dialer with $cleanNumber."
            )
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Could not initiate call: ${e.message}")
        }
    }
}
