package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Ready : SpeechState()
    object Listening : SpeechState()
    data class PartialResult(val text: String) : SpeechState()
    data class FinalResult(val text: String) : SpeechState()
    data class Error(val message: String, val errorCode: Int) : SpeechState()
}

class SpeechManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun initRecognizer() {
        if (speechRecognizer != null) {
            destroyRecognizer()
        }

        if (!isRecognitionAvailable()) {
            _speechState.value = SpeechState.Error("Speech recognition is not available on this device.", -1)
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _speechState.value = SpeechState.Ready
                }

                override fun onBeginningOfSpeech() {
                    _speechState.value = SpeechState.Listening
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Normalize RMS to 0..10
                    val normalized = if (rmsdB < 0f) 0f else rmsdB.coerceIn(0f, 10f)
                    _rmsLevel.value = normalized
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _rmsLevel.value = 0f
                }

                override fun onError(error: Int) {
                    _rmsLevel.value = 0f
                    val errorMsg = mapErrorCodeToMessage(error)
                    _speechState.value = SpeechState.Error(errorMsg, error)
                }

                override fun onResults(results: Bundle?) {
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        _speechState.value = SpeechState.FinalResult(text)
                    } else {
                        _speechState.value = SpeechState.Error("No speech detected. Please try again.", SpeechRecognizer.ERROR_NO_MATCH)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = partialMatches?.firstOrNull()?.trim() ?: ""
                    if (text.isNotEmpty()) {
                        _speechState.value = SpeechState.PartialResult(text)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    fun startListening(languagePreference: String = "en-IN") {
        initRecognizer()
        if (speechRecognizer == null) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languagePreference)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languagePreference)
            // Enable offline recognition if available on device
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _speechState.value = SpeechState.Error("Failed to start voice recognition: ${e.message}", -1)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            _rmsLevel.value = 0f
            _speechState.value = SpeechState.Idle
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetToIdle() {
        _speechState.value = SpeechState.Idle
        _rmsLevel.value = 0f
    }

    fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun mapErrorCodeToMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Client error occurred."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Try speaking closer to the mic."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognition is already busy."
            SpeechRecognizer.ERROR_SERVER -> "Recognition server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected in time."
            else -> "Speech recognition error ($errorCode)."
        }
    }
}
