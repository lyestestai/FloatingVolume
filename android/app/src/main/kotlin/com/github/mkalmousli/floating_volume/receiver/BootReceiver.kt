package com.github.mkalmousli.floating_volume.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.os.UserManager
import android.util.Log
import com.github.mkalmousli.floating_volume.CrashHandler
import com.github.mkalmousli.floating_volume.FloatingVolumeService

/**
 * BroadcastReceiver to start FloatingVolumeService automatically on device boot
 * or head-unit MCU wakeup (ACC ON / QuickBoot).
 *
 * Designed specifically for Android 8.0+ (Oreo) Automotive Head-Units (including
 * Chinese head-units spoofing Android 12+) as well as modern Android 12+ devices:
 * 1. Starts FloatingVolumeService immediately as a ForegroundService while the
 *    system broadcast exemption is active (preventing MCU task killers from
 *    killing an idle cached process and avoiding ForegroundServiceStartNotAllowedException).
 * 2. Passes the configured delay (10s–60s) to FloatingVolumeService so the overlay
 *    and AudioManager initialization wait until the car's audio HAL & launcher are ready.
 * 3. Also schedules a monotonic ELAPSED_REALTIME_WAKEUP alarm via AlarmManager as a
 *    safety net in case the head-unit MCU kills all services during the first seconds of boot.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        const val PREFS_NAME = "floating_volume_prefs"
        const val KEY_AUTO_START = "auto_start_enabled"
        const val KEY_RESTORE_STATE = "restore_service_state"
        const val KEY_LAST_SERVICE_STATE = "last_service_state"
        const val KEY_AUTO_START_DELAY = "auto_start_delay_ms"
        const val DEFAULT_DELAY_MS = 10_000L // 10 seconds default delay
        const val MIN_DELAY_MS = 10_000L
        const val MAX_DELAY_MS = 60_000L
        const val ACTION_START_SERVICE_DELAYED = "com.github.mkalmousli.floating_volume.START_SERVICE_DELAYED"

        private val BOOT_AND_WAKE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_POWER_CONNECTED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.syu.ms.action.ACC_ON",
            "com.tw.action.ACC_ON",
            "com.glsx.boot.ACCON",
            "com.microntek.bootcheck",
            "autochips.intent.action.QB_POWERON"
        )

        fun scheduleServiceStart(context: Context, delayMs: Long) {
            if (delayMs <= 0L) {
                startFloatingVolumeService(context, 0L)
                return
            }

            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager == null) {
                    startFloatingVolumeService(context, 0L)
                    return
                }

                val intent = Intent(context, BootReceiver::class.java).apply {
                    action = ACTION_START_SERVICE_DELAYED
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    1001,
                    intent,
                    flags
                )

                // Use monotonic clock (ELAPSED_REALTIME_WAKEUP) so GPS/RDS/NTP clock jumps
                // during car startup never invalidate the scheduled alarm.
                val triggerAtElapsed = SystemClock.elapsedRealtime() + delayMs

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtElapsed,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            triggerAtElapsed,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtElapsed,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        triggerAtElapsed,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled safety-net monotonic alarm in ${delayMs}ms")
                CrashHandler.logEvent(context, TAG, "Scheduled safety-net monotonic alarm in ${delayMs}ms")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to schedule alarm, attempting direct service start", e)
                CrashHandler.logEvent(context, TAG, "Failed to schedule alarm (${e.message}), attempting direct service start")
                startFloatingVolumeService(context, 0L)
            }
        }

        fun startFloatingVolumeService(context: Context, bootDelayMs: Long = 0L) {
            try {
                val serviceIntent = Intent(context, FloatingVolumeService::class.java).apply {
                    putExtra(FloatingVolumeService.EXTRA_BOOT_DELAY_MS, bootDelayMs)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.i(TAG, "FloatingVolumeService start requested (bootDelayMs=${bootDelayMs}ms)")
                CrashHandler.logEvent(context, TAG, "FloatingVolumeService start requested (bootDelayMs=${bootDelayMs}ms)")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to start FloatingVolumeService", e)
                CrashHandler.logEvent(context, TAG, "ERROR starting FloatingVolumeService: ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    private fun isUserUnlocked(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
                userManager?.isUserUnlocked ?: true
            } else {
                true
            }
        } catch (e: Throwable) {
            true
        }
    }

    private fun handleBootOrWake(context: Context, action: String) {
        if (!isUserUnlocked(context)) {
            Log.d(TAG, "User storage still locked on $action; waiting for unlock/BOOT_COMPLETED.")
            return
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)

            if (!autoStartEnabled) {
                Log.d(TAG, "Auto-start is disabled. Ignoring $action.")
                CrashHandler.logEvent(context, TAG, "Ignored $action because auto_start_enabled=false")
                return
            }

            // If the service is already running in memory (e.g., on USER_PRESENT / ACC_ON),
            // simply ping it with 0ms delay so it ensures the overlay is attached.
            if (FloatingVolumeService.instance != null) {
                Log.d(TAG, "Service already active on $action; ensuring overlay visibility.")
                CrashHandler.logEvent(context, TAG, "Service already active on $action; ensuring overlay visibility")
                startFloatingVolumeService(context, 0L)
                return
            }

            val rawDelay = prefs.getLong(KEY_AUTO_START_DELAY, DEFAULT_DELAY_MS)
            val delayMs = rawDelay.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)

            Log.i(TAG, "Auto-start triggered by $action with delay=${delayMs}ms")
            CrashHandler.logEvent(context, TAG, "Auto-start triggered by $action (delay=${delayMs}ms)")

            // Stage 1: Start ForegroundService immediately with internal coroutine delay
            // so the process is immediately protected by startForeground()
            startFloatingVolumeService(context, delayMs)

            // Stage 2: Schedule monotonic AlarmManager safety net in case the head-unit MCU
            // kills running processes during early boot cleanup
            scheduleServiceStart(context, delayMs)
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling boot/wake event ($action)", e)
            CrashHandler.logEvent(context, TAG, "Error in handleBootOrWake($action): ${e.message}")
        }
    }

    private fun handleAppUpdate(context: Context) {
        if (!isUserUnlocked(context)) return

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val autoStartEnabled = prefs.getBoolean(KEY_AUTO_START, false)
            val wasServiceRunning = prefs.getBoolean(KEY_LAST_SERVICE_STATE, false)

            if (autoStartEnabled || wasServiceRunning) {
                Log.i(TAG, "App updated. Restarting FloatingVolumeService.")
                CrashHandler.logEvent(context, TAG, "App updated (MY_PACKAGE_REPLACED). Restarting service.")
                startFloatingVolumeService(context, 1000L)
                scheduleServiceStart(context, 2000L)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error handling app update", e)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "BootReceiver received action: $action")
        if (isUserUnlocked(context)) {
            CrashHandler.logEvent(context, TAG, "Received broadcast: $action")
        }

        when {
            action in BOOT_AND_WAKE_ACTIONS -> {
                handleBootOrWake(context, action)
            }
            action == Intent.ACTION_MY_PACKAGE_REPLACED -> {
                handleAppUpdate(context)
            }
            action == ACTION_START_SERVICE_DELAYED -> {
                if (!isUserUnlocked(context)) return
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                if (prefs.getBoolean(KEY_AUTO_START, false)) {
                    Log.i(TAG, "Safety-net alarm triggered: ensuring FloatingVolumeService is running")
                    CrashHandler.logEvent(context, TAG, "Safety-net alarm fired (ACTION_START_SERVICE_DELAYED)")
                    startFloatingVolumeService(context, 0L)
                }
            }
            else -> {
                Log.w(TAG, "Received unexpected intent: $action")
            }
        }
    }
}
