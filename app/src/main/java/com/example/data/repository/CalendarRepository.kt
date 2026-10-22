package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.data.model.CalendarEventItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CalendarRepository(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getUpcomingEvents(maxCount: Int = 5): List<CalendarEventItem> = withContext(Dispatchers.IO) {
        val events = mutableListOf<CalendarEventItem>()
        if (!hasCalendarPermission()) return@withContext events

        val now = System.currentTimeMillis()
        val calendarEnd = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 3)
        }.timeInMillis

        val uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(now.toString(), calendarEnd.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (cursor.moveToNext() && events.size < maxCount) {
                    val id = if (idIdx >= 0) cursor.getLong(idIdx) else 0L
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "Untitled Event" else "Untitled Event"
                    val start = if (startIdx >= 0) cursor.getLong(startIdx) else 0L
                    val end = if (endIdx >= 0) cursor.getLong(endIdx) else 0L
                    val loc = if (locIdx >= 0) cursor.getString(locIdx) else null

                    events.add(CalendarEventItem(id, title, start, end, loc))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        events
    }

    fun formatEventsForSpeech(events: List<CalendarEventItem>): String {
        if (events.isEmpty()) {
            return "You have no upcoming calendar events scheduled for the next 48 hours."
        }
        val sdf = SimpleDateFormat("h:mm a, EEEE", Locale.getDefault())
        val sb = StringBuilder("You have ${events.size} upcoming event${if (events.size > 1) "s" else ""}: ")
        events.forEachIndexed { index, event ->
            val formattedTime = sdf.format(event.startTime)
            sb.append("${event.title} at $formattedTime")
            if (index < events.size - 1) {
                sb.append("; ")
            } else {
                sb.append(".")
            }
        }
        return sb.toString()
    }
}
