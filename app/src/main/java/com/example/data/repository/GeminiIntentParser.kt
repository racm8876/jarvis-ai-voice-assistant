package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.CommandType
import com.example.data.model.ParsedIntent
import com.example.data.model.PcAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiIntentParser {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    suspend fun parseCommand(rawSpeech: String): ParsedIntent = withContext(Dispatchers.IO) {
        val trimmed = rawSpeech.trim()
        if (trimmed.isEmpty()) {
            return@withContext ParsedIntent(
                commandType = CommandType.UNKNOWN,
                spokenResponse = "I didn't catch that. Please speak a command."
            )
        }

        // 1. Strip wake word prefix if present (e.g. "Hi Jarvis, call Papa")
        val wakeWordRegex = Regex("""^(?:hi|hey|ok|hello|namaste|suno)?\s*jarvis[,:\s]*""", RegexOption.IGNORE_CASE)
        val cleanedSpeech = trimmed.replace(wakeWordRegex, "").trim()
        val speechToParse = if (cleanedSpeech.isNotEmpty()) cleanedSpeech else trimmed

        // 2. Check for emergency stop immediately
        if (isEmergencyStop(speechToParse)) {
            return@withContext ParsedIntent(
                commandType = CommandType.EMERGENCY_STOP,
                spokenResponse = "Emergency stop activated. Halting all actions.",
                isConsequential = false
            )
        }

        // 3. High-speed local pattern matching (English, Hindi, Hinglish)
        val localIntent = parseWithLocalEngine(speechToParse)
        if (localIntent != null && localIntent.confidence >= 0.85f) {
            return@withContext localIntent
        }

        // 4. Fallback to Gemini AI if API key is configured
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiIntent = parseWithGeminiApi(speechToParse, apiKey)
                if (aiIntent != null) {
                    return@withContext aiIntent
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 5. Fallback to local intent or unknown response
        return@withContext localIntent ?: ParsedIntent(
            commandType = CommandType.UNKNOWN,
            spokenResponse = "I heard: \"$speechToParse\", but could not determine the exact command. Try saying \"Call Papa\", \"Open WhatsApp\", \"Lock PC\", or \"Turn on flashlight\"."
        )
    }

    private fun isEmergencyStop(text: String): Boolean {
        val lower = text.lowercase()
        val stopTriggers = listOf(
            "stop", "emergency stop", "cancel", "abort", "halt",
            "ruk jao", "ruko", "band karo", "chup", "chup ho jao", "bas karo"
        )
        return stopTriggers.any { lower == it || lower.startsWith("$it ") }
    }

    private fun parseWithLocalEngine(text: String): ParsedIntent? {
        val lower = text.lowercase()

        // --- Windows PC Commands ---
        // "Lock my PC", "Lock PC"
        if (lower.matches(Regex("""(?:lock\s+(?:my\s+)?pc|lock\s+computer)"""))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.LOCK_PC,
                spokenResponse = "Locking your Windows PC workstation.",
                confidence = 0.98f,
                isConsequential = false
            )
        }

        // "Shutdown PC", "Turn off PC"
        if (lower.matches(Regex("""(?:shutdown\s+(?:my\s+)?pc|turn\s+off\s+(?:my\s+)?pc)"""))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.SHUTDOWN_PC,
                spokenResponse = "Initiating PC shutdown in 5 seconds. Say cancel to abort.",
                confidence = 0.98f,
                isConsequential = true
            )
        }

        // Media controls on PC
        if (lower.contains("pc") && (lower.contains("play") || lower.contains("pause") || lower.contains("media"))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.MEDIA_PLAY_PAUSE,
                spokenResponse = "Toggling media playback on PC.",
                confidence = 0.95f
            )
        }
        if (lower.contains("pc") && (lower.contains("next song") || lower.contains("skip song") || lower.contains("next track"))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.MEDIA_NEXT,
                spokenResponse = "Skipping to next track on PC.",
                confidence = 0.95f
            )
        }
        if (lower.contains("pc") && (lower.contains("volume up") || lower.contains("increase volume"))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.VOLUME_UP,
                spokenResponse = "Increasing volume on PC.",
                confidence = 0.95f
            )
        }
        if (lower.contains("pc") && (lower.contains("volume down") || lower.contains("decrease volume"))) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.VOLUME_DOWN,
                spokenResponse = "Decreasing volume on PC.",
                confidence = 0.95f
            )
        }
        if (lower.contains("pc") && lower.contains("mute")) {
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = PcAction.MUTE,
                spokenResponse = "Muting audio on PC.",
                confidence = 0.95f
            )
        }

        // "Open Chrome on PC", "Open Notepad on PC"
        val pcAppMatch = Regex("""open\s+([a-zA-Z0-9\s]+?)\s+on\s+pc""", RegexOption.IGNORE_CASE).find(text)
        if (pcAppMatch != null) {
            val appTarget = pcAppMatch.groupValues[1].trim()
            val isUrl = appTarget.contains(".")
            return ParsedIntent(
                commandType = CommandType.PC_COMMAND,
                pcAction = if (isUrl) PcAction.OPEN_URL else PcAction.OPEN_APP,
                pcPayload = appTarget,
                spokenResponse = "Opening $appTarget on PC.",
                confidence = 0.95f
            )
        }

        // --- System Controls: Torch / Flashlight ---
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("band")
            return ParsedIntent(
                commandType = CommandType.TORCH_TOGGLE,
                settingTarget = "torch",
                settingValue = if (turnOn) "on" else "off",
                spokenResponse = if (turnOn) "Turning on flashlight." else "Turning off flashlight.",
                confidence = 0.98f
            )
        }

        // --- System Controls: Volume & Mute ---
        if (lower.matches(Regex("""(?:volume\s+up|increase\s+volume|sound\s+up|aawaz\s+badhao)"""))) {
            return ParsedIntent(
                commandType = CommandType.VOLUME_CONTROL,
                settingTarget = "volume",
                settingValue = "up",
                spokenResponse = "Increasing volume.",
                confidence = 0.98f
            )
        }
        if (lower.matches(Regex("""(?:volume\s+down|decrease\s+volume|sound\s+down|aawaz\s+kam\s+karo)"""))) {
            return ParsedIntent(
                commandType = CommandType.VOLUME_CONTROL,
                settingTarget = "volume",
                settingValue = "down",
                spokenResponse = "Decreasing volume.",
                confidence = 0.98f
            )
        }
        if (lower.matches(Regex("""(?:mute\s+volume|mute\s+phone|mute\s+sound|silent\s+phone)"""))) {
            return ParsedIntent(
                commandType = CommandType.VOLUME_CONTROL,
                settingTarget = "volume",
                settingValue = "mute",
                spokenResponse = "Muting device audio.",
                confidence = 0.98f
            )
        }

        // --- System Settings Panels ---
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return ParsedIntent(
                commandType = CommandType.SYSTEM_SETTING,
                settingTarget = "wifi",
                spokenResponse = "Opening Wi-Fi settings panel.",
                confidence = 0.98f
            )
        }
        if (lower.contains("bluetooth")) {
            return ParsedIntent(
                commandType = CommandType.SYSTEM_SETTING,
                settingTarget = "bluetooth",
                spokenResponse = "Opening Bluetooth settings.",
                confidence = 0.98f
            )
        }
        if (lower.contains("brightness") || (lower.contains("display") && lower.contains("setting"))) {
            return ParsedIntent(
                commandType = CommandType.SYSTEM_SETTING,
                settingTarget = "brightness",
                spokenResponse = "Opening Display and Brightness settings.",
                confidence = 0.98f
            )
        }
        if (lower.contains("battery") && lower.contains("setting")) {
            return ParsedIntent(
                commandType = CommandType.SYSTEM_SETTING,
                settingTarget = "battery",
                spokenResponse = "Opening Battery settings.",
                confidence = 0.98f
            )
        }

        // --- Open Files ---
        if (lower.matches(Regex("""(?:open\s+files?|browse\s+files?|open\s+file\s+explorer|show\s+my\s+files?)"""))) {
            return ParsedIntent(
                commandType = CommandType.OPEN_FILE,
                spokenResponse = "Opening device file browser.",
                confidence = 0.98f
            )
        }

        // --- Web Search ---
        val webSearchMatch = Regex("""(?:search\s+(?:the\s+)?web\s+for\s+|google\s+search\s+(?:for\s+)?|search\s+on\s+google\s+(?:for\s+)?)(.+)""", RegexOption.IGNORE_CASE).find(text)
        if (webSearchMatch != null) {
            val q = webSearchMatch.groupValues[1].trim()
            return ParsedIntent(
                commandType = CommandType.WEB_SEARCH,
                searchQuery = q,
                spokenResponse = "Searching the web for $q.",
                confidence = 0.96f
            )
        }

        // --- WhatsApp Video Call ---
        // "Open WhatsApp and video call Papa" / "WhatsApp pe Papa ko video call karo"
        val videoCallRegex = Regex("""(?:open\s+whatsapp\s+and\s+video\s+call|video\s+call\s+(?:on\s+whatsapp\s+to\s+)?|whatsapp\s+(?:pe\s+)?video\s+call\s+(?:karo\s+to\s+)?)(.+)""", RegexOption.IGNORE_CASE)
        val videoCallMatch = videoCallRegex.find(lower)
        if (videoCallMatch != null) {
            val contact = cleanContactName(videoCallMatch.groupValues[1])
            return ParsedIntent(
                commandType = CommandType.WHATSAPP_VIDEO_CALL,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                contactName = contact,
                spokenResponse = "Setting up a WhatsApp video call to $contact.",
                confidence = 0.95f,
                isConsequential = true
            )
        }

        // --- WhatsApp Message ---
        val waMsgRegex1 = Regex("""(?:send\s+(?:a\s+)?whatsapp\s+message\s+to\s+)([a-zA-Z0-9\s]+?)(?:\s+saying\s+|\s+that\s+|\s*:\s*)(.+)""", RegexOption.IGNORE_CASE)
        val waMsgMatch1 = waMsgRegex1.find(text)
        if (waMsgMatch1 != null) {
            val contact = cleanContactName(waMsgMatch1.groupValues[1])
            val msg = waMsgMatch1.groupValues[2].trim()
            return ParsedIntent(
                commandType = CommandType.WHATSAPP_MESSAGE,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                contactName = contact,
                messageBody = msg,
                spokenResponse = "Sending WhatsApp message to $contact: \"$msg\".",
                confidence = 0.95f,
                isConsequential = true
            )
        }

        // Hinglish message pattern: "Rahul ko WhatsApp pe message bhejo ki 10 minute me aa raha hu"
        val waMsgRegexHinglish = Regex("""([a-zA-Z0-9\s]+?)\s+ko\s+whatsapp\s+(?:pe\s+)?message\s+bhejo\s+ki\s+(.+)""", RegexOption.IGNORE_CASE)
        val waMsgHinglishMatch = waMsgRegexHinglish.find(text)
        if (waMsgHinglishMatch != null) {
            val contact = cleanContactName(waMsgHinglishMatch.groupValues[1])
            val msg = waMsgHinglishMatch.groupValues[2].trim()
            return ParsedIntent(
                commandType = CommandType.WHATSAPP_MESSAGE,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                contactName = contact,
                messageBody = msg,
                spokenResponse = "Preparing WhatsApp message for $contact: \"$msg\".",
                confidence = 0.95f,
                isConsequential = true
            )
        }

        // Generic WhatsApp message: "message Rahul on WhatsApp: Hello"
        val waMsgRegex3 = Regex("""(?:message|text)\s+([a-zA-Z0-9\s]+?)\s+on\s+whatsapp\s*(?:saying|that|:)?\s*(.+)""", RegexOption.IGNORE_CASE)
        val waMsgMatch3 = waMsgRegex3.find(text)
        if (waMsgMatch3 != null) {
            val contact = cleanContactName(waMsgMatch3.groupValues[1])
            val msg = waMsgMatch3.groupValues[2].trim()
            return ParsedIntent(
                commandType = CommandType.WHATSAPP_MESSAGE,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                contactName = contact,
                messageBody = msg,
                spokenResponse = "Sending WhatsApp message to $contact: \"$msg\".",
                confidence = 0.95f,
                isConsequential = true
            )
        }

        // --- YouTube Search ---
        if (lower.contains("youtube")) {
            val query = when {
                lower.contains("search for ") -> text.substringAfter("search for ", "").replace(Regex("""(?i)\s+on\s+youtube"""), "").trim()
                lower.contains("search ") -> text.substringAfter("search ", "").replace(Regex("""(?i)\s+on\s+youtube"""), "").trim()
                lower.contains("pe ") && lower.contains("search") -> text.substringAfter("pe ").substringBefore(" search").trim()
                else -> text.substringAfter("youtube", "").replace(Regex("""(?i)\s+search\s+for\s+|\s+search\s+"""), "").trim()
            }
            if (query.isNotEmpty() && query != "open" && query != "kholo") {
                return ParsedIntent(
                    commandType = CommandType.YOUTUBE_SEARCH,
                    targetApp = "YouTube",
                    targetPackage = "com.google.android.youtube",
                    searchQuery = query,
                    spokenResponse = "Searching YouTube for $query.",
                    confidence = 0.95f,
                    isConsequential = false
                )
            }
        }

        // --- Calendar Events ---
        if (lower.contains("calendar") || lower.contains("upcoming events") || lower.contains("schedule") || lower.contains("meeting")) {
            return ParsedIntent(
                commandType = CommandType.READ_CALENDAR,
                spokenResponse = "Checking your upcoming calendar events.",
                confidence = 0.95f,
                isConsequential = false
            )
        }

        // --- Phone Call ---
        val callRegexHinglish = Regex("""([a-zA-Z0-9\s]+?)\s+ko\s+(?:call\s+lagao|phone\s+karo|call\s+karo)""", RegexOption.IGNORE_CASE)
        val callMatchHinglish = callRegexHinglish.find(text)
        if (callMatchHinglish != null) {
            val contact = cleanContactName(callMatchHinglish.groupValues[1])
            return ParsedIntent(
                commandType = CommandType.CALL_PHONE,
                contactName = contact,
                spokenResponse = "Calling $contact.",
                confidence = 0.95f,
                isConsequential = true
            )
        }

        val callRegex1 = Regex("""^\s*(?:open\s+(?:the\s+)?phone\s+dialer\s+and\s+call\s+|call\s+|dial\s+|phone\s+)([a-zA-Z0-9\s]+)$""", RegexOption.IGNORE_CASE)
        val callMatch1 = callRegex1.find(text)
        if (callMatch1 != null && !lower.contains("video call") && !lower.contains("whatsapp")) {
            val contact = cleanContactName(callMatch1.groupValues[1])
            if (contact.isNotEmpty()) {
                return ParsedIntent(
                    commandType = CommandType.CALL_PHONE,
                    contactName = contact,
                    spokenResponse = "Calling $contact.",
                    confidence = 0.95f,
                    isConsequential = true
                )
            }
        }

        // --- Launch Apps ---
        if (lower.matches(Regex("""(?:open|launch|start|kholo)\s+whatsapp.*""")) || lower == "whatsapp") {
            return ParsedIntent(
                commandType = CommandType.LAUNCH_APP,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                spokenResponse = "Opening WhatsApp.",
                confidence = 0.98f
            )
        }

        if (lower.matches(Regex("""(?:open|launch|start|kholo)\s+instagram.*""")) || lower == "instagram") {
            return ParsedIntent(
                commandType = CommandType.LAUNCH_APP,
                targetApp = "Instagram",
                targetPackage = "com.instagram.android",
                spokenResponse = "Opening Instagram.",
                confidence = 0.98f
            )
        }

        if (lower.matches(Regex("""(?:open|launch|start|kholo)\s+youtube.*""")) || lower == "youtube") {
            return ParsedIntent(
                commandType = CommandType.LAUNCH_APP,
                targetApp = "YouTube",
                targetPackage = "com.google.android.youtube",
                spokenResponse = "Opening YouTube.",
                confidence = 0.98f
            )
        }

        return null
    }

    private fun cleanContactName(raw: String): String {
        return raw.trim()
            .replace(Regex("""(?i)\s+(?:on\s+whatsapp|via\s+whatsapp|now|please|ko)$"""), "")
            .replace(Regex("""(?i)^(?:to|the|my)\s+"""), "")
            .trim()
    }

    private suspend fun parseWithGeminiApi(userPrompt: String, apiKey: String): ParsedIntent? = withContext(Dispatchers.IO) {
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val systemInstruction = """
            You are Jarvis, an advanced Android mobile voice assistant intent parser that can control smartphone features and Windows PC.
            Classify user intent into:
            - LAUNCH_APP, CALL_PHONE, WHATSAPP_MESSAGE, WHATSAPP_VIDEO_CALL, YOUTUBE_SEARCH, READ_CALENDAR
            - PC_COMMAND (lock pc, shutdown pc, media on pc, volume on pc, open app on pc)
            - TORCH_TOGGLE (turn on/off flashlight)
            - VOLUME_CONTROL (volume up/down, mute)
            - SYSTEM_SETTING (open wifi, bluetooth, display, battery settings)
            - WEB_SEARCH (search web for query)
            - OPEN_FILE (open file manager)
            - EMERGENCY_STOP, UNKNOWN

            Output ONLY a JSON matching:
            {
              "commandType": "string",
              "targetApp": "string or null",
              "contactName": "string or null",
              "messageBody": "string or null",
              "searchQuery": "string or null",
              "spokenResponse": "Polite Jarvis response",
              "isConsequential": boolean,
              "settingTarget": "wifi|bluetooth|torch|volume|brightness|battery or null",
              "settingValue": "on|off|up|down|mute or null",
              "pcAction": "LOCK_PC|SHUTDOWN_PC|MEDIA_PLAY_PAUSE|MEDIA_NEXT|VOLUME_UP|VOLUME_DOWN|MUTE|OPEN_APP|OPEN_URL or null",
              "pcPayload": "string or null"
            }
        """.trimIndent()

        val rootJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", userPrompt))
                    })
                })
            })
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstruction))
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val responseBodyString = response.body?.string() ?: return@withContext null
        val responseJson = JSONObject(responseBodyString)
        val candidates = responseJson.optJSONArray("candidates") ?: return@withContext null
        if (candidates.length() == 0) return@withContext null

        val candidate = candidates.getJSONObject(0)
        val content = candidate.optJSONObject("content") ?: return@withContext null
        val parts = content.optJSONArray("parts") ?: return@withContext null
        if (parts.length() == 0) return@withContext null

        val textPayload = parts.getJSONObject(0).optString("text") ?: return@withContext null
        val parsedJson = JSONObject(textPayload)

        val commandTypeStr = parsedJson.optString("commandType", "UNKNOWN")
        val commandType = try {
            CommandType.valueOf(commandTypeStr)
        } catch (e: Exception) {
            CommandType.UNKNOWN
        }

        val pcActionStr = parsedJson.optString("pcAction", "")
        val pcAction = if (pcActionStr.isNotEmpty()) {
            try { PcAction.valueOf(pcActionStr) } catch (e: Exception) { null }
        } else null

        val spoken = parsedJson.optString("spokenResponse", "Understood.")
        val targetApp = parsedJson.optString("targetApp", "").ifEmpty { null }
        val contactName = parsedJson.optString("contactName", "").ifEmpty { null }
        val messageBody = parsedJson.optString("messageBody", "").ifEmpty { null }
        val searchQuery = parsedJson.optString("searchQuery", "").ifEmpty { null }
        val isConsequential = parsedJson.optBoolean("isConsequential", false)
        val settingTarget = parsedJson.optString("settingTarget", "").ifEmpty { null }
        val settingValue = parsedJson.optString("settingValue", "").ifEmpty { null }
        val pcPayload = parsedJson.optString("pcPayload", "").ifEmpty { null }

        return@withContext ParsedIntent(
            commandType = commandType,
            targetApp = targetApp,
            contactName = contactName,
            messageBody = messageBody,
            searchQuery = searchQuery,
            spokenResponse = spoken,
            confidence = 0.98f,
            isConsequential = isConsequential,
            pcAction = pcAction,
            pcPayload = pcPayload,
            settingTarget = settingTarget,
            settingValue = settingValue
        )
    }
}
