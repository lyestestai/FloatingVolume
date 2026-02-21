package com.github.mkalmousli.floating_volume.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.github.mkalmousli.floating_volume.FloatingVolumeService

/**
 * BroadcastReceiver to start FloatingVolumeService automatically on device boot.
 *
 * This receiver listens for:
 * - ACTION_BOOT_COMPLETED: Device finished booting
 * - ACTION_MY_PACKAGE_REPLACED: App was updated
 *
 * The service will only start if:
 * 1. Auto-start is enabled in preferences (default: false)
 * 2. The service was running when the device shut down (if "restore state" is enabled)
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val PREFS_NAME = "floating_volume_prefs"
        private const val KEY_AUTO_START = "auto_start_enabled"
        private const val KEY_RESTORE_STATE = "restore_service_state"
        private const val KEY_LAST_SERVICE_STATE = "last_service_state"
        private const val KEY_AUTO_START_DELAY = "auto_start_delay_ms"
        private const val DEFAULT_DELAY_MS = 3000L // 3 seconds default delay
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "Device boot completed")
                handleBootComplete(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "App package replaced (updated)")
                handleAppUpdate(context)
            }
            else -> {
                Log.w(TAG, "Received unexpected intent: ${intent.action}")
            }
        }
    }

    private fun handleBootComplete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)

        if (!autoStartEnabled) {
            Log.d(TAG, "Auto-start is disabled. Service will not start.")
            return
        }

        val restoreState = prefs.getBoolean(KEY_RESTORE_STATE, true)
        val wasServiceRunning = prefs.getBoolean(KEY_LAST_SERVICE_STATE, false)

        if (restoreState && !wasServiceRunning) {
            Log.d(TAG, "Restore state enabled, but service was not running before boot. Not starting.")
            return
        }

        val delayMs = prefs.getLong(KEY_AUTO_START_DELAY, DEFAULT_DELAY_MS)

        Log.d(TAG, "Starting service after ${delayMs}ms delay...")

        // Use Handler to delay service start (gives system time to stabilize)
        Handler(Looper.getMainLooper()).postDelayed({
            startFloatingVolumeService(context)
        }, delayMs)
    }

    private fun handleAppUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)
        val wasServiceRunning = prefs.getBoolean(KEY_LAST_SERVICE_STATE, false)

        // After app update, restart service if it was running before
        if (autoStartEnabled && wasServiceRunning) {
            Log.d(TAG, "App updated. Restarting service as it was previously running.")

            Handler(Looper.getMainLooper()).postDelayed({
                startFloatingVolumeService(context)
            }, 1000L) // Short delay after update
        }
    }

    private fun startFloatingVolumeService(context: Context) {
        try {
            val serviceIntent = Intent(context, FloatingVolumeService::class.java)
            context.startForegroundService(serviceIntent)
            Log.i(TAG, "FloatingVolumeService started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start FloatingVolumeService", e)
        }
    }
}
