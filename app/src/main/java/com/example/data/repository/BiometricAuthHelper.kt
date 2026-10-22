package com.example.data.repository

import android.app.KeyguardManager
import android.content.Context
import android.os.Build

class BiometricAuthHelper(private val context: Context) {

    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    fun isDeviceSecure(): Boolean {
        return keyguardManager?.isDeviceSecure == true
    }

    fun isDeviceLocked(): Boolean {
        return keyguardManager?.isDeviceLocked == true
    }

    fun canPerformSensitiveAction(): Boolean {
        // If device is not secure or currently unlocked, action is authorized
        return !isDeviceLocked()
    }
}
