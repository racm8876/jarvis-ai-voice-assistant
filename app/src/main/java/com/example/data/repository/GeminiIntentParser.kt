package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.CommandType
import com.example.data.model.ParsedIntent
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

        // 1. Check for emergency stop immediately
        if (isEmergencyStop(trimmed)) {
            return@withContext ParsedIntent(
                commandType = CommandType.EMERGENCY_STOP,
                spokenResponse = "Emergency stop activated. Halting all actions.",
                isConsequential = false
            )
        }

        // 2. High-speed local pattern matching (English, Hindi, Hinglish)
        val localIntent = parseWithLocalEngine(trimmed)
        if (localIntent != null && localIntent.confidence >= 0.85f) {
            return@withContext localIntent
        }

        // 3. Fallback to Gemini AI if API key is configured
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotEmpty() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiIntent = parseWithGeminiApi(trimmed, apiKey)
                if (aiIntent != null) {
                    return@withContext aiIntent
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Fallback to local intent or unknown response
        return@withContext localIntent ?: ParsedIntent(
            commandType = CommandType.UNKNOWN,
            spokenResponse = "I heard: \"$trimmed\", but could not determine the exact command. Try saying \"Call Papa\", \"Open WhatsApp\", or \"Search YouTube\"."
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
        // "Send a WhatsApp message to Rahul saying I will arrive in ten minutes"
        // "Rahul ko WhatsApp message bhejo ki I will arrive in ten minutes"
        // "WhatsApp Rahul I will arrive soon"
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
        // "Open YouTube and search for Python tutorials"
        // "YouTube pe Python tutorials search karo"
        // "Search Python tutorials on YouTube"
        val ytRegex1 = Regex("""(?:open\s+youtube\s+and\s+search\s+(?:for\s+)?|search\s+youtube\s+for\s+|search\s+for\s+)(.+?)(?:\s+on\s+youtube)?$""", RegexOption.IGNORE_CASE)
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
        // "Read my upcoming calendar events" / "What are my calendar events" / "Calendar check karo"
        if (lower.contains("calendar") || lower.contains("upcoming events") || lower.contains("schedule") || lower.contains("meeting")) {
            return ParsedIntent(
                commandType = CommandType.READ_CALENDAR,
                spokenResponse = "Checking your upcoming calendar events.",
                confidence = 0.95f,
                isConsequential = false
            )
        }

        // --- Phone Call ---
        // Hinglish calling patterns first: "Papa ko call lagao", "Rahul ko phone karo"
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

        // English calling patterns: "Call Papa", "Call Rahul", "Open phone dialer and call Rahul"
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
        // "Open WhatsApp"
        if (lower.matches(Regex("""(?:open|launch|start|kholo)\s+whatsapp.*""")) || lower == "whatsapp") {
            return ParsedIntent(
                commandType = CommandType.LAUNCH_APP,
                targetApp = "WhatsApp",
                targetPackage = "com.whatsapp",
                spokenResponse = "Opening WhatsApp.",
                confidence = 0.98f
            )
        }

        // "Open Instagram"
        if (lower.matches(Regex("""(?:open|launch|start|kholo)\s+instagram.*""")) || lower == "instagram") {
            return ParsedIntent(
                commandType = CommandType.LAUNCH_APP,
                targetApp = "Instagram",
                targetPackage = "com.instagram.android",
                spokenResponse = "Opening Instagram.",
                confidence = 0.98f
            )
        }

        // "Open YouTube"
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
            You are Jarvis, an advanced Android mobile voice assistant intent parser.
            The user command can be in English, Hindi, or Hinglish.
            Classify the user intent into one of these types:
            - LAUNCH_APP (for opening apps like WhatsApp, Instagram, YouTube, etc.)
            - CALL_PHONE (for placing phone calls or opening dialer to a contact or number)
            - WHATSAPP_MESSAGE (for sending a WhatsApp message to a contact)
            - WHATSAPP_VIDEO_CALL (for video calls on WhatsApp)
            - WHATSAPP_VOICE_CALL (for voice calls on WhatsApp)
            - YOUTUBE_SEARCH (for searching YouTube)
            - READ_CALENDAR (for reading upcoming calendar events or schedule)
            - EMERGENCY_STOP (for stop, cancel, halt commands)
            - AMBIGUOUS_CLARIFICATION (if user command is missing contact, message, or ambiguous)
            - UNKNOWN

            Respond ONLY with a valid JSON object matching this schema:
            {
              "commandType": "LAUNCH_APP | CALL_PHONE | WHATSAPP_MESSAGE | WHATSAPP_VIDEO_CALL | WHATSAPP_VOICE_CALL | YOUTUBE_SEARCH | READ_CALENDAR | EMERGENCY_STOP | AMBIGUOUS_CLARIFICATION | UNKNOWN",
              "targetApp": "AppName or null",
              "targetPackage": "packageName or null",
              "contactName": "ContactName or null",
              "messageBody": "Message content or null",
              "searchQuery": "Search query or null",
              "spokenResponse": "Crisp, polite Jarvis vocal response to say to the user",
              "isConsequential": boolean (true for calling or messaging, false for reading or launching apps),
              "clarificationQuestion": "Question if clarification is needed, else null"
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

        val spoken = parsedJson.optString("spokenResponse", "Understood.")
        val targetApp = parsedJson.optString("targetApp", "").ifEmpty { null }
        val targetPackage = parsedJson.optString("targetPackage", "").ifEmpty { null }
        val contactName = parsedJson.optString("contactName", "").ifEmpty { null }
        val messageBody = parsedJson.optString("messageBody", "").ifEmpty { null }
        val searchQuery = parsedJson.optString("searchQuery", "").ifEmpty { null }
        val isConsequential = parsedJson.optBoolean("isConsequential", false)

        return@withContext ParsedIntent(
            commandType = commandType,
            targetApp = targetApp,
            targetPackage = targetPackage,
            contactName = contactName,
            messageBody = messageBody,
            searchQuery = searchQuery,
            spokenResponse = spoken,
            confidence = 0.98f,
            isConsequential = isConsequential
        )
    }
}
