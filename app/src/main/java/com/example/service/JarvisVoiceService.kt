package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.JarvisApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.CommandType
import com.example.data.model.ParsedIntent
import com.example.data.model.PcAction
import com.example.data.repository.ActionExecutionResult
import com.example.data.repository.AppLauncherRepository
import com.example.data.repository.CalendarRepository
import com.example.data.repository.ContactLookupResult
import com.example.data.repository.ContactRepository
import com.example.data.repository.GeminiIntentParser
import com.example.data.repository.PcBridgeRepository
import com.example.data.repository.PcBridgeResult
import com.example.data.repository.SystemControlRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisVoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeWordDetector: LowPowerWakeWordDetector? = null

    private lateinit var ttsManager: TtsManager
    private lateinit var intentParser: GeminiIntentParser
    private lateinit var systemControl: SystemControlRepository
    private lateinit var pcBridge: PcBridgeRepository
    private lateinit var appLauncher: AppLauncherRepository
    private lateinit var contactRepo: ContactRepository
    private lateinit var calendarRepo: CalendarRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true

        ttsManager = TtsManager(applicationContext)
        intentParser = GeminiIntentParser()
        systemControl = SystemControlRepository(applicationContext)
        pcBridge = PcBridgeRepository(applicationContext)
        appLauncher = AppLauncherRepository(applicationContext)
        contactRepo = ContactRepository(applicationContext)
        calendarRepo = CalendarRepository(applicationContext)

        createNotificationChannel()
        acquireWakeLock()
        initWakeWordDetector()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TOGGLE_LISTENING -> {
                toggleBackgroundListening()
            }
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startWakeWordListening()

        return START_STICKY
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "JarvisAssistant:BackgroundServiceLock"
            )?.apply {
                acquire(24 * 60 * 60 * 1000L) // 24 hours lock
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initWakeWordDetector() {
        wakeWordDetector = LowPowerWakeWordDetector(applicationContext) { spokenText ->
            handleSpokenCommandInBackground(spokenText)
        }
    }

    private fun handleSpokenCommandInBackground(fullCommand: String) {
        _lastDetectedCommand.value = fullCommand
        updateNotification("Command received: \"$fullCommand\"")

        serviceScope.launch {
            val parsed = intentParser.parseCommand(fullCommand)

            // If command is an emergency stop
            if (parsed.commandType == CommandType.EMERGENCY_STOP) {
                ttsManager.stop()
                ttsManager.speak("Emergency stop activated. Halting all operations.")
                logCommandToDb(fullCommand, "EMERGENCY_STOP", "Emergency halt", "CANCELLED", "Halted")
                return@launch
            }

            // If action is consequential (calling, sending message, pc shutdown) or requires UI,
            // bring up MainActivity safely on top of lock screen
            if (parsed.isConsequential || parsed.commandType == CommandType.CALL_PHONE || parsed.commandType == CommandType.WHATSAPP_MESSAGE) {
                ttsManager.speak(parsed.spokenResponse)
                launchMainActivityFromLockscreen(fullCommand)
                return@launch
            }

            // Direct hands-free background execution without opening app!
            when (parsed.commandType) {
                CommandType.TORCH_TOGGLE -> {
                    val turnOn = parsed.settingValue == "on"
                    systemControl.toggleTorch(turnOn)
                    ttsManager.speak(parsed.spokenResponse)
                    logCommandToDb(fullCommand, "TORCH_TOGGLE", "Flashlight $turnOn", "SUCCESS", parsed.spokenResponse)
                }

                CommandType.VOLUME_CONTROL -> {
                    when (parsed.settingValue) {
                        "up" -> systemControl.adjustVolume(true)
                        "down" -> systemControl.adjustVolume(false)
                        "mute" -> systemControl.setMute(true)
                    }
                    ttsManager.speak(parsed.spokenResponse)
                    logCommandToDb(fullCommand, "VOLUME_CONTROL", "Volume ${parsed.settingValue}", "SUCCESS", parsed.spokenResponse)
                }

                CommandType.PC_COMMAND -> {
                    val pcAct = parsed.pcAction ?: PcAction.PING
                    val result = pcBridge.sendPcAction(pcAct, parsed.pcPayload)
                    val reply = when (result) {
                        is PcBridgeResult.Success -> result.message
                        is PcBridgeResult.Failure -> result.error
                    }
                    ttsManager.speak(reply)
                    logCommandToDb(fullCommand, "PC_COMMAND", pcAct.name, "SUCCESS", reply)
                }

                CommandType.READ_CALENDAR -> {
                    val events = calendarRepo.getUpcomingEvents(3)
                    val summary = calendarRepo.formatEventsForSpeech(events)
                    ttsManager.speak(summary)
                    logCommandToDb(fullCommand, "READ_CALENDAR", "Calendar check", "SUCCESS", summary)
                }

                CommandType.SYSTEM_SETTING -> {
                    systemControl.openSystemSettings(parsed.settingTarget ?: "settings")
                    ttsManager.speak(parsed.spokenResponse)
                    logCommandToDb(fullCommand, "SYSTEM_SETTING", parsed.settingTarget ?: "Settings", "SUCCESS", parsed.spokenResponse)
                }

                CommandType.LAUNCH_APP -> {
                    val pkg = parsed.targetPackage ?: when (parsed.targetApp?.lowercase()) {
                        "whatsapp" -> "com.whatsapp"
                        "instagram" -> "com.instagram.android"
                        "youtube" -> "com.google.android.youtube"
                        else -> null
                    }
                    if (pkg != null) {
                        appLauncher.openApp(pkg, parsed.targetApp ?: "App")
                        ttsManager.speak(parsed.spokenResponse)
                        logCommandToDb(fullCommand, "LAUNCH_APP", parsed.targetApp ?: "App", "SUCCESS", parsed.spokenResponse)
                    }
                }

                CommandType.YOUTUBE_SEARCH -> {
                    val q = parsed.searchQuery ?: "Python tutorials"
                    appLauncher.openYouTubeSearch(q)
                    ttsManager.speak("Searching YouTube for $q.")
                    logCommandToDb(fullCommand, "YOUTUBE_SEARCH", q, "SUCCESS", "YouTube search initiated.")
                }

                else -> {
                    ttsManager.speak(parsed.spokenResponse)
                    launchMainActivityFromLockscreen(fullCommand)
                }
            }

            updateNotification("Listening for \"Hello Jarvis\"...")
        }
    }

    private suspend fun logCommandToDb(raw: String, type: String, summary: String, status: String, msg: String) {
        try {
            val db = (application as JarvisApplication).database
            db.commandDao().insertCommand(
                CommandHistoryEntity(
                    rawCommand = raw,
                    commandType = type,
                    targetSummary = summary,
                    executionStatus = status,
                    statusMessage = msg
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun launchMainActivityFromLockscreen(command: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_WAKE_COMMAND", command)
        }
        startActivity(intent)
    }

    fun startWakeWordListening() {
        _isListeningForWakeWord.value = true
        wakeWordDetector?.startListening()
        updateNotification("Listening for \"Hello Jarvis\"...")
    }

    fun stopWakeWordListening() {
        _isListeningForWakeWord.value = false
        wakeWordDetector?.stopListening()
        updateNotification("Wake word paused.")
    }

    private fun toggleBackgroundListening() {
        if (_isListeningForWakeWord.value) {
            stopWakeWordListening()
        } else {
            startWakeWordListening()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Background Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Jarvis hands-free wake word listening active in background."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String = "Listening for \"Hello Jarvis\"..."): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, JarvisVoiceService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis AI Assistant // Hands-Free Active")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(statusText))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceRunning.value = false
        _isListeningForWakeWord.value = false
        wakeWordDetector?.stopListening()
        ttsManager.shutdown()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "jarvis_voice_service_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_SERVICE = "com.example.service.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.service.action.STOP_SERVICE"
        const val ACTION_TOGGLE_LISTENING = "com.example.service.action.TOGGLE_LISTENING"

        var instance: JarvisVoiceService? = null
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        private val _isListeningForWakeWord = MutableStateFlow(false)
        val isListeningForWakeWord: StateFlow<Boolean> = _isListeningForWakeWord.asStateFlow()

        private val _lastDetectedCommand = MutableStateFlow<String?>(null)
        val lastDetectedCommand: StateFlow<String?> = _lastDetectedCommand.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, JarvisVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }
}
