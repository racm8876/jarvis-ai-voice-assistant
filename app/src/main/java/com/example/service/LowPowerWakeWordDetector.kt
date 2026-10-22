package com.example.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class LowPowerWakeWordDetector(
    private val context: Context,
    private val onWakeWordSpotted: (String) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsEnergy = MutableStateFlow(0f)
    val rmsEnergy: StateFlow<Float> = _rmsEnergy.asStateFlow()

    private var speechManager: SpeechManager? = null
    private var powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    // Energy threshold to distinguish ambient silence from voice activity
    var energyThreshold: Float = 1200f

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true

        initSpeechManager()

        recordingJob = scope.launch {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = minBufferSize.coerceAtLeast(3200) // 100ms frames at 16kHz
            val buffer = ShortArray(bufferSize / 2)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w("WakeWordDetector", "AudioRecord init failed, falling back to MIC source")
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )
                }

                audioRecord?.startRecording()
                Log.i("WakeWordDetector", "Low-power wake-word detector active")

                var voiceFramesConsecutive = 0

                while (isActive && _isListening.value) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // Compute Root Mean Square (RMS) energy
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read).toFloat()
                        _rmsEnergy.value = (rms / 300f).coerceIn(0f, 10f)

                        // VAD check
                        if (rms > energyThreshold) {
                            voiceFramesConsecutive++
                            // When sustained voice is detected (e.g. user spoke "Hello Jarvis"),
                            // switch to acoustic speech recognition to extract command
                            if (voiceFramesConsecutive >= 2) {
                                voiceFramesConsecutive = 0
                                handlePotentialWakeWord()
                                delay(1800) // Debounce window
                            }
                        } else {
                            voiceFramesConsecutive = 0
                            // Low-power sleep to conserve CPU and battery during silence
                            delay(50)
                        }
                    } else {
                        delay(50)
                    }
                }
            } catch (e: Exception) {
                Log.e("WakeWordDetector", "AudioRecord loop exception: ${e.message}")
            } finally {
                stopAudioRecord()
            }
        }
    }

    private fun handlePotentialWakeWord() {
        // Play subtle futuristic Jarvis chime
        playJarvisChime()

        // Wake screen if permitted
        wakeDeviceScreen()

        // Trigger speech recognition for the utterance
        scope.launch(Dispatchers.Main) {
            speechManager?.startListening()
        }
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(context)
        scope.launch(Dispatchers.Main) {
            speechManager?.speechState?.collect { state ->
                when (state) {
                    is SpeechState.FinalResult -> {
                        val text = state.text.trim()
                        val lower = text.lowercase()
                        val wakeWords = listOf("hello jarvis", "hi jarvis", "hey jarvis", "ok jarvis", "jarvis")
                        val isWakeWord = wakeWords.any { lower.contains(it) }

                        if (isWakeWord || lower.startsWith("jarvis")) {
                            onWakeWordSpotted(text)
                        }
                    }
                    is SpeechState.Error -> {
                        // Reset ready for next audio trigger
                        speechManager?.resetToIdle()
                    }
                    else -> {}
                }
            }
        }
    }

    fun playJarvisChime() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    fun wakeDeviceScreen() {
        try {
            val wakeLock = powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "JarvisAssistant:WakeScreen"
            )
            wakeLock?.acquire(4000L) // 4 seconds
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudioRecord() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        _isListening.value = false
        recordingJob?.cancel()
        recordingJob = null
        stopAudioRecord()
        speechManager?.destroyRecognizer()
        speechManager = null
    }
}
