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

    @Volatile
    private var isRecognizingSpeech = false

    // Energy threshold to distinguish ambient silence from voice activity
    var energyThreshold: Float = 1100f

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true

        initSpeechManager()
        startAudioRecordLoop()
    }

    private fun startAudioRecordLoop() {
        recordingJob?.cancel()
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

                if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord?.startRecording()
                    Log.i("WakeWordDetector", "Low-power background voice listener active")
                } else {
                    Log.w("WakeWordDetector", "AudioRecord initialization unviable")
                }

                var voiceFramesConsecutive = 0

                while (isActive && _isListening.value) {
                    if (isRecognizingSpeech) {
                        delay(100)
                        continue
                    }

                    val record = audioRecord
                    if (record == null || record.state != AudioRecord.STATE_INITIALIZED || record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        delay(100)
                        continue
                    }

                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        // Compute Root Mean Square (RMS) energy
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += buffer[i] * buffer[i]
                        }
                        val rms = sqrt(sum / read).toFloat()
                        _rmsEnergy.value = (rms / 300f).coerceIn(0f, 10f)

                        // VAD check: detect sustained human speech
                        if (rms > energyThreshold) {
                            voiceFramesConsecutive++
                            if (voiceFramesConsecutive >= 2 && !isRecognizingSpeech) {
                                voiceFramesConsecutive = 0
                                pauseAudioAndRecognize()
                            }
                        } else {
                            voiceFramesConsecutive = 0
                            delay(40)
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

    private fun pauseAudioAndRecognize() {
        isRecognizingSpeech = true
        try {
            audioRecord?.stop()
        } catch (e: Exception) {
            Log.w("WakeWordDetector", "Failed to pause AudioRecord: ${e.message}")
        }

        // Play subtle futuristic Jarvis chime
        playJarvisChime()

        // Wake screen if permitted
        wakeDeviceScreen()

        // Trigger speech recognition for the utterance
        scope.launch(Dispatchers.Main) {
            speechManager?.startListening()
        }
    }

    private fun resumeAudioRecording() {
        scope.launch {
            delay(400) // Allow speech recognizer to fully release microphone hardware
            if (_isListening.value) {
                try {
                    val record = audioRecord
                    if (record != null && record.state == AudioRecord.STATE_INITIALIZED && record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        record.startRecording()
                    }
                } catch (e: Exception) {
                    Log.w("WakeWordDetector", "Failed to resume AudioRecord: ${e.message}")
                }
                isRecognizingSpeech = false
            }
        }
    }

    private fun initSpeechManager() {
        speechManager = SpeechManager(context)
        scope.launch(Dispatchers.Main) {
            speechManager?.speechState?.collect { state ->
                when (state) {
                    is SpeechState.FinalResult -> {
                        val text = state.text.trim()
                        if (text.isNotEmpty()) {
                            Log.i("WakeWordDetector", "Spoken command recognized in background: $text")
                            onWakeWordSpotted(text)
                        }
                        resumeAudioRecording()
                    }
                    is SpeechState.Error -> {
                        speechManager?.resetToIdle()
                        resumeAudioRecording()
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
        isRecognizingSpeech = false
        recordingJob?.cancel()
        recordingJob = null
        stopAudioRecord()
        speechManager?.destroyRecognizer()
        speechManager = null
    }
}
