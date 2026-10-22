package com.example.data.repository

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URLEncoder

class SystemControlRepository(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    init {
        try {
            cameraManager?.registerTorchCallback(object : CameraManager.TorchCallback() {
                override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                    _isTorchOn.value = enabled
                }
            }, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTorch(turnOn: Boolean? = null): ActionExecutionResult {
        if (cameraManager == null) {
            return ActionExecutionResult.Failure("Camera service not available for flashlight.")
        }
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionExecutionResult.Failure("No flashlight hardware detected on this device.")

            val targetState = turnOn ?: !_isTorchOn.value
            cameraManager.setTorchMode(cameraId, targetState)
            _isTorchOn.value = targetState
            ActionExecutionResult.Success(if (targetState) "Flashlight turned on." else "Flashlight turned off.")
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Failed to toggle flashlight: ${e.message}")
        }
    }

    fun adjustVolume(increase: Boolean): ActionExecutionResult {
        if (audioManager == null) return ActionExecutionResult.Failure("Audio manager not available.")
        return try {
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val percent = (currentVol * 100) / maxVol
            ActionExecutionResult.Success("Volume adjusted to $percent%.")
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Could not adjust volume: ${e.message}")
        }
    }

    fun setMute(mute: Boolean): ActionExecutionResult {
        if (audioManager == null) return ActionExecutionResult.Failure("Audio manager not available.")
        return try {
            val direction = if (mute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            ActionExecutionResult.Success(if (mute) "Audio muted." else "Audio unmuted.")
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Failed to change mute state: ${e.message}")
        }
    }

    fun openSystemSettings(target: String): ActionExecutionResult {
        val action = when (target.lowercase()) {
            "wifi", "wi-fi", "internet" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Settings.Panel.ACTION_WIFI
                } else {
                    Settings.ACTION_WIFI_SETTINGS
                }
            }
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "brightness", "display" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery", "battery saver" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "accessibility" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionExecutionResult.Success("Opening $target settings.")
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Unable to open $target settings: ${e.message}")
        }
    }

    fun performWebSearch(query: String): ActionExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionExecutionResult.Success("Searching web for \"$query\".")
        } catch (e: Exception) {
            // Fallback to browser URL
            val encoded = try { URLEncoder.encode(query, "UTF-8") } catch (ex: Exception) { query }
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$encoded")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            ActionExecutionResult.Success("Opened browser search for \"$query\".")
        }
    }

    fun openFileManager(): ActionExecutionResult {
        return try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            ActionExecutionResult.Success("Opening device file explorer.")
        } catch (e: Exception) {
            ActionExecutionResult.Failure("Unable to launch file explorer: ${e.message}")
        }
    }
}
