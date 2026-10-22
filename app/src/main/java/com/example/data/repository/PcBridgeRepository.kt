package com.example.data.repository

import android.content.Context
import com.example.data.model.PcAction
import com.example.data.model.PcServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class PcBridgeResult {
    data class Success(val message: String) : PcBridgeResult()
    data class Failure(val error: String) : PcBridgeResult()
}

class PcBridgeRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_pc_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<PcServerConfig> = _config.asStateFlow()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    private fun loadConfig(): PcServerConfig {
        return PcServerConfig(
            host = prefs.getString("pc_host", "192.168.1.100") ?: "192.168.1.100",
            port = prefs.getInt("pc_port", 8000),
            authToken = prefs.getString("pc_token", "jarvis_secret_key") ?: "jarvis_secret_key",
            isEnabled = prefs.getBoolean("pc_enabled", false)
        )
    }

    fun updateConfig(newConfig: PcServerConfig) {
        prefs.edit()
            .putString("pc_host", newConfig.host.trim())
            .putInt("pc_port", newConfig.port)
            .putString("pc_token", newConfig.authToken.trim())
            .putBoolean("pc_enabled", newConfig.isEnabled)
            .apply()
        _config.value = newConfig
    }

    suspend fun pingPc(): PcBridgeResult = withContext(Dispatchers.IO) {
        val current = _config.value
        val url = "http://${current.host}:${current.port}/api/ping"
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${current.authToken}")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = try { JSONObject(bodyStr) } catch (e: Exception) { null }
                val pcName = json?.optString("machine_name", "Windows PC") ?: "Windows PC"
                PcBridgeResult.Success("Connected to $pcName (${current.host}:${current.port})")
            } else {
                PcBridgeResult.Failure("PC responded with HTTP ${response.code}. Check auth token.")
            }
        } catch (e: Exception) {
            PcBridgeResult.Failure("Cannot connect to PC at ${current.host}:${current.port}: ${e.message}")
        }
    }

    suspend fun sendPcAction(action: PcAction, payload: String? = null): PcBridgeResult = withContext(Dispatchers.IO) {
        val current = _config.value
        if (!current.isEnabled) {
            return@withContext PcBridgeResult.Failure("PC Control is disabled in Jarvis settings. Enable it first.")
        }

        val url = "http://${current.host}:${current.port}/api/action"
        val jsonPayload = JSONObject().apply {
            put("action", action.name.lowercase())
            if (!payload.isNullOrEmpty()) {
                put("payload", payload)
            }
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${current.authToken}")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string() ?: ""
                val resJson = try { JSONObject(resBody) } catch (e: Exception) { null }
                val msg = resJson?.optString("message", "PC command executed.") ?: "PC command executed."
                PcBridgeResult.Success(msg)
            } else {
                PcBridgeResult.Failure("PC rejected command: HTTP ${response.code}")
            }
        } catch (e: Exception) {
            PcBridgeResult.Failure("Network error communicating with PC: ${e.message}")
        }
    }
}
