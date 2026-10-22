package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CommandType {
    LAUNCH_APP,
    CALL_PHONE,
    WHATSAPP_MESSAGE,
    WHATSAPP_VIDEO_CALL,
    WHATSAPP_VOICE_CALL,
    YOUTUBE_SEARCH,
    READ_CALENDAR,
    AMBIGUOUS_CLARIFICATION,
    EMERGENCY_STOP,
    UNKNOWN
}

data class ParsedIntent(
    val commandType: CommandType,
    val targetApp: String? = null,
    val targetPackage: String? = null,
    val contactName: String? = null,
    val resolvedPhoneNumber: String? = null,
    val messageBody: String? = null,
    val searchQuery: String? = null,
    val spokenResponse: String,
    val confidence: Float = 1.0f,
    val isConsequential: Boolean = false,
    val clarificationOptions: List<String> = emptyList()
)

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawCommand: String,
    val commandType: String,
    val targetSummary: String,
    val executionStatus: String, // "SUCCESS", "FAILED", "CANCELLED", "CONFIRMED"
    val statusMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ResolvedContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null
)

data class CalendarEventItem(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String? = null
)
