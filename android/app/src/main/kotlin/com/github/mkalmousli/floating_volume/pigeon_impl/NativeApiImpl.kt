package com.github.mkalmousli.floating_volume.pigeon_impl

import LogStatsData
import NativeApi
import ToastDuration
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.github.mkalmousli.floating_volume.CrashHandler
import com.github.mkalmousli.floating_volume.bloc.ServiceStatusBloc
import com.github.mkalmousli.floating_volume.bloc.VisibilityBloc
import com.github.mkalmousli.floating_volume.inIO
import com.github.mkalmousli.floating_volume.inMain
import kotlinx.coroutines.CoroutineScope


class NativeApiImpl(
    private val context: Context,
    private val scope: CoroutineScope
) : NativeApi {
    override fun startService(callback: (Result<Unit>) -> Unit) {
        scope.inMain {
            try {
                CrashHandler.logEvent(context, TAG, "Manual startService requested from Flutter UI")
                com.github.mkalmousli.floating_volume.FloatingVolumeService.isStoppingIntentionally = false
                ServiceStatusBloc.updateState(ServiceStatusBloc.State.On)
                VisibilityBloc.show()
                val intent = Intent(context, com.github.mkalmousli.floating_volume.FloatingVolumeService::class.java).apply {
                    putExtra(com.github.mkalmousli.floating_volume.FloatingVolumeService.EXTRA_BOOT_DELAY_MS, 0L)
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                callback(Result.success(Unit))
            } catch (e: Exception) {
                CrashHandler.logEvent(context, TAG, "startService failed: ${e.message}")
                callback(Result.failure(e))
            }
        }
    }

    override fun stopService(callback: (Result<Unit>) -> Unit) {
        scope.inMain {
            try {
                CrashHandler.logEvent(context, TAG, "Manual stopService requested from Flutter UI")
                com.github.mkalmousli.floating_volume.FloatingVolumeService.isStoppingIntentionally = true
                VisibilityBloc.hide()
                ServiceStatusBloc.updateState(ServiceStatusBloc.State.Off)
                val intent = Intent(context, com.github.mkalmousli.floating_volume.FloatingVolumeService::class.java)
                context.stopService(intent)
                callback(Result.success(Unit))
            } catch (e: Exception) {
                CrashHandler.logEvent(context, TAG, "stopService failed: ${e.message}")
                callback(Result.failure(e))
            }
        }
    }

    override fun hideFloatingVolume(callback: (Result<Unit>) -> Unit) {
        scope.inMain {
            try {
                VisibilityBloc.hide()
                callback(Result.success(Unit))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    override fun showFloatingVolume(callback: (Result<Unit>) -> Unit) {
        scope.inMain {
            try {
                VisibilityBloc.show()
                callback(Result.success(Unit))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    override fun setMaxVolume(maxVolume: Long, callback: (Result<Unit>) -> Unit) {
        callback(Result.success(Unit))
    }

    override fun setMinVolume(minVolume: Long, callback: (Result<Unit>) -> Unit) {
        callback(Result.success(Unit))
    }

    override fun showToast(
        message: String,
        duration: ToastDuration,
        callback: (Result<Unit>) -> Unit
    ) {
        scope.inMain {
            try {
                Toast.makeText(
                    context,
                    message,
                    when (duration) {
                        ToastDuration.SHORT -> Toast.LENGTH_SHORT
                        ToastDuration.LONG -> Toast.LENGTH_LONG
                    }
                ).show()
                callback(Result.success(Unit))
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }
    }

    // ========== Auto-start Configuration ==========

    companion object {
        private const val PREFS_NAME = "floating_volume_prefs"
        private const val KEY_AUTO_START = "auto_start_enabled"
        private const val KEY_RESTORE_STATE = "restore_service_state"
        private const val KEY_AUTO_START_DELAY = "auto_start_delay_ms"
        private const val DEFAULT_DELAY_MS = 10_000L // 10 seconds default (clamped 10s-60s)
        private const val TAG = "NativeApiImpl"
    }

    override fun setAutoStartEnabled(enabled: Boolean, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentDelay = prefs.getLong(KEY_AUTO_START_DELAY, DEFAULT_DELAY_MS).coerceIn(10_000L, 60_000L)
                // Use commit() on IO thread to guarantee immediate disk fsync before car ignition power-off
                prefs.edit()
                    .putBoolean(KEY_AUTO_START, enabled)
                    .putLong(KEY_AUTO_START_DELAY, currentDelay)
                    .commit()
                Log.d(TAG, "Auto-start set to: $enabled (delay=${currentDelay}ms)")
                CrashHandler.logEvent(context, TAG, "Auto-start set to: $enabled (delay=${currentDelay}ms)")
                inMain { callback(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set auto-start", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getAutoStartEnabled(callback: (Result<Boolean>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(KEY_AUTO_START, false)
                inMain { callback(Result.success(enabled)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get auto-start", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun setAutoStartDelay(delayMs: Long, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val safeDelay = delayMs.coerceIn(10_000L, 60_000L)
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_AUTO_START_DELAY, safeDelay).commit()
                Log.d(TAG, "Auto-start delay set to: ${safeDelay}ms")
                CrashHandler.logEvent(context, TAG, "Auto-start delay set to: ${safeDelay}ms (${safeDelay / 1000}s)")
                inMain { callback(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set auto-start delay", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getAutoStartDelay(callback: (Result<Long>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val delay = prefs.getLong(KEY_AUTO_START_DELAY, DEFAULT_DELAY_MS).coerceIn(10_000L, 60_000L)
                inMain { callback(Result.success(delay)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get auto-start delay", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun setRestoreServiceState(enabled: Boolean, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_RESTORE_STATE, enabled).commit()
                Log.d(TAG, "Restore service state set to: $enabled")
                inMain { callback(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set restore service state", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getRestoreServiceState(callback: (Result<Boolean>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val enabled = prefs.getBoolean(KEY_RESTORE_STATE, true)
                inMain { callback(Result.success(enabled)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get restore service state", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    // ========== Logs Management ==========

    override fun setLogsEnabled(enabled: Boolean, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                CrashHandler.setLogsEnabled(context, enabled)
                inMain { callback(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set logs enabled", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getLogsEnabled(callback: (Result<Boolean>) -> Unit) {
        scope.inIO {
            try {
                val enabled = CrashHandler.isLogsEnabled(context)
                inMain { callback(Result.success(enabled)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get logs enabled", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getLogsContent(callback: (Result<String>) -> Unit) {
        scope.inIO {
            try {
                val report = CrashHandler.getFullDiagnosticReport(context)
                inMain { callback(Result.success(report)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get logs content", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun saveLogsToDisk(callback: (Result<String>) -> Unit) {
        scope.inIO {
            try {
                val savedPath = CrashHandler.saveLogsToPublicDisk(context)
                inMain { callback(Result.success(savedPath)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save logs to disk", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun exportLogsAndShare(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val intent = CrashHandler.createShareLogsIntent(context)
                if (intent != null) {
                    val chooser = Intent.createChooser(intent, "Partager le journal Floating Volume").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                    inMain { callback(Result.success(Unit)) }
                } else {
                    Log.w(TAG, "No logs available to export")
                    inMain { callback(Result.failure(Exception("No logs available"))) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export logs", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun clearAllLogs(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val success = CrashHandler.clearLogs(context)
                if (success) {
                    Log.d(TAG, "All logs cleared successfully")
                    inMain { callback(Result.success(Unit)) }
                } else {
                    Log.w(TAG, "Failed to clear some logs")
                    inMain { callback(Result.failure(Exception("Failed to clear logs"))) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing logs", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun getLogStats(callback: (Result<LogStatsData>) -> Unit) {
        scope.inIO {
            try {
                val stats = CrashHandler.getLogStats(context)
                val data = LogStatsData(
                    totalFiles = stats.totalFiles.toLong(),
                    totalSizeBytes = stats.totalSizeBytes,
                    oldestLogDate = stats.oldestLogDate?.toString(),
                    newestLogDate = stats.newestLogDate?.toString()
                )
                inMain { callback(Result.success(data)) }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting log stats", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    // ========== Media Controls ==========

    override fun hasNotificationAccess(callback: (Result<Boolean>) -> Unit) {
        scope.inIO {
            try {
                val enabledPackages = androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context)
                val isGranted = enabledPackages.contains(context.packageName)
                inMain { callback(Result.success(isGranted)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check notification access", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }

    override fun requestNotificationAccess(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                inMain { callback(Result.success(Unit)) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open notification settings", e)
                inMain { callback(Result.failure(e)) }
            }
        }
    }
}
