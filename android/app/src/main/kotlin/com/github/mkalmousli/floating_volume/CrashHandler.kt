package com.github.mkalmousli.floating_volume

import android.content.Context
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var context: Context

    fun init(context: Context) {
        this.context = context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashLog(e)
        defaultHandler?.uncaughtException(t, e)
    }

    private fun saveCrashLog(e: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val logContent = "Time: $timestamp\n$stackTrace\n\n"

            // Use external files dir which doesn't require explicit storage permission for the app's own directory
            // Path: /storage/emulated/0/Android/data/com.github.mkalmousli.floating_volume/files/crash_log.txt
            val dir = context.getExternalFilesDir(null)
            if (dir != null) {
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val file = File(dir, "crash_log.txt")
                file.appendText(logContent)
                Log.e("CrashHandler", "Crash log saved to: ${file.absolutePath}")
            } else {
                 Log.e("CrashHandler", "Could not get external files dir")
            }

        } catch (ex: Exception) {
            Log.e("CrashHandler", "Error saving crash log", ex)
        }
    }
}
