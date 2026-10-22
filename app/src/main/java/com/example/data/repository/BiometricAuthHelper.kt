package com.example.data.repository

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

class BiometricAuthHelper(private val context: Context) {

    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure == true
    }

    fun isDeviceLocked(): Boolean {
        return keyguardManager?.isDeviceLocked == true
    }

    fun isBatteryOptimizationIgnored(): Boolean {
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    fun requestIgnoreBatteryOptimization(activity: Activity) {
        if (!isBatteryOptimizationIgnored()) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                // Fallback to battery saver settings
                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                activity.startActivity(fallbackIntent)
            }
        }
    }

    fun dismissKeyguard(activity: Activity, onDismissed: () -> Unit, onCancelled: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    onDismissed()
                }

                override fun onDismissCancelled() {
                    super.onDismissCancelled()
                    onCancelled()
                }

                override fun onDismissError() {
                    super.onDismissError()
                    onCancelled()
                }
            })
        } else {
            onDismissed()
        }
    }
}
