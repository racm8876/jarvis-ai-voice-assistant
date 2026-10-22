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
    PC_COMMAND,
    SYSTEM_SETTING,
    WEB_SEARCH,
    OPEN_FILE,
    TORCH_TOGGLE,
    VOLUME_CONTROL,
    AMBIGUOUS_CLARIFICATION,
    EMERGENCY_STOP,
    UNKNOWN
}

enum class PcAction {
    LOCK_PC,
    SHUTDOWN_PC,
    MEDIA_PLAY_PAUSE,
    MEDIA_NEXT,
    MEDIA_PREV,
    VOLUME_UP,
    VOLUME_DOWN,
    MUTE,
    OPEN_APP,
    OPEN_URL,
    RUN_SCRIPT,
    PING
}

data class PcServerConfig(
    val host: String = "192.168.1.100",
    val port: Int = 8000,
    val authToken: String = "jarvis_secret_key",
    val isEnabled: Boolean = false
)

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
    val clarificationOptions: List<String> = emptyList(),
    val pcAction: PcAction? = null,
    val pcPayload: String? = null,
    val settingTarget: String? = null, // "wifi", "bluetooth", "torch", "volume", "brightness", "battery"
    val settingValue: String? = null   // "on", "off", "up", "down", "open"
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
