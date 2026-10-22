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
import com.example.MainActivity
import com.example.R
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
    private var speechManager: SpeechManager? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        _isServiceRunning.value = true

        createNotificationChannel()
        acquireWakeLock()
        initBackgroundListener()
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
                "JarvisAssistant:WakeLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // 10 minutes timeout per cycle
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initBackgroundListener() {
        speechManager = SpeechManager(applicationContext)
        serviceScope.launch {
            speechManager?.speechState?.collect { state ->
                when (state) {
                    is SpeechState.PartialResult -> {
                        checkWakeWord(state.text)
                    }
                    is SpeechState.FinalResult -> {
                        val detected = checkWakeWord(state.text)
                        if (!detected) {
                            // Cycle restart for continuous hands-free wake-word detection
                            if (_isServiceRunning.value && _isListeningForWakeWord.value) {
                                speechManager?.startListening()
                            }
                        }
                    }
                    is SpeechState.Error -> {
                        // Restart listener if speech timed out waiting for wake word
                        if (_isServiceRunning.value && _isListeningForWakeWord.value) {
                            speechManager?.startListening()
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun checkWakeWord(text: String): Boolean {
        val lower = text.lowercase().trim()
        val wakeWords = listOf("hi jarvis", "hey jarvis", "ok jarvis", "jarvis", "namaste jarvis", "suno jarvis")
        val match = wakeWords.any { lower.contains(it) }

        if (match) {
            _wakeWordDetectedEvent.value = text
            launchMainActivityFromLockscreen(text)
            return true
        }
        return false
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
        speechManager?.startListening()
        updateNotification("Listening for \"Hi Jarvis\"...")
    }

    fun stopWakeWordListening() {
        _isListeningForWakeWord.value = false
        speechManager?.stopListening()
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
                description = "Keeps Jarvis wake word listening active in background."
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(statusText: String = "Listening for \"Hi Jarvis\"..."): Notification {
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
            .setContentTitle("Jarvis AI Assistant // Active")
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
        speechManager?.destroyRecognizer()
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

        private val _wakeWordDetectedEvent = MutableStateFlow<String?>(null)
        val wakeWordDetectedEvent: StateFlow<String?> = _wakeWordDetectedEvent.asStateFlow()

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
