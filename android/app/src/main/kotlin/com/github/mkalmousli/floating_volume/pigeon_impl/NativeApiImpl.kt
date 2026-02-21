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
        scope.inIO {
            ServiceStatusBloc.event.emit(
                ServiceStatusBloc.Event.Start
            )
            inMain {
                callback(Result.success(Unit))
            }
        }
    }

    override fun stopService(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            ServiceStatusBloc.event.emit(
                ServiceStatusBloc.Event.Stop
            )
            inMain {
                callback(Result.success(Unit))
            }
        }
    }

    override fun hideFloatingVolume(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            VisibilityBloc.event.emit(
                VisibilityBloc.Event.Hide
            )
            inMain {
                callback(Result.success(Unit))
            }
        }
    }

    override fun showFloatingVolume(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            VisibilityBloc.event.emit(
                VisibilityBloc.Event.Show
            )
            inMain {
                callback(Result.success(Unit))
            }
        }
    }

    override fun setMaxVolume(maxVolume: Long, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
//            SystemVolumeBloc
        }
    }

    override fun setMinVolume(minVolume: Long, callback: (Result<Unit>) -> Unit) {
//        TODO("Not yet implemented")
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
        private const val DEFAULT_DELAY_MS = 3000L // 3 seconds
        private const val TAG = "NativeApiImpl"
    }

    override fun setAutoStartEnabled(enabled: Boolean, callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putBoolean(KEY_AUTO_START, enabled).apply()
                Log.d(TAG, "Auto-start set to: $enabled")
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
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putLong(KEY_AUTO_START_DELAY, delayMs).apply()
                Log.d(TAG, "Auto-start delay set to: ${delayMs}ms")
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
                val delay = prefs.getLong(KEY_AUTO_START_DELAY, DEFAULT_DELAY_MS)
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
                prefs.edit().putBoolean(KEY_RESTORE_STATE, enabled).apply()
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

    override fun exportLogsAndShare(callback: (Result<Unit>) -> Unit) {
        scope.inIO {
            try {
                val intent = CrashHandler.createShareLogsIntent()
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(Intent.createChooser(intent, "Share Logs"))
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
                val success = CrashHandler.clearLogs()
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
                val stats = CrashHandler.getLogStats()
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
}