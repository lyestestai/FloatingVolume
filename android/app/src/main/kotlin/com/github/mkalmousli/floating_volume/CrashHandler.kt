package com.github.mkalmousli.floating_volume

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.github.mkalmousli.floating_volume.receiver.BootReceiver
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler : Thread.UncaughtExceptionHandler {

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private lateinit var context: Context

    // Configuration
    private const val LOG_DIR_NAME = "logs"
    private const val LOG_FILE_PREFIX = "crash_log_"
    private const val DIAG_FILE_NAME = "diagnostic_log_current.txt"
    private const val LOG_FILE_EXTENSION = ".txt"
    private const val MAX_LOG_SIZE_BYTES = 1 * 1024 * 1024 // 1 MB max for fast UI loading
    private const val TAG = "CrashHandler"
    const val KEY_DIAGNOSTIC_LOGS_ENABLED = "diagnostic_logs_enabled"

    fun init(ctx: Context) {
        this.context = ctx.applicationContext ?: ctx
        val currentDefault = Thread.getDefaultUncaughtExceptionHandler()
        if (currentDefault !== this) {
            defaultHandler = currentDefault
            Thread.setDefaultUncaughtExceptionHandler(this)
        }
    }

    fun setLogsEnabled(ctx: Context, enabled: Boolean) {
        init(ctx)
        val prefs = ctx.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DIAGNOSTIC_LOGS_ENABLED, enabled).commit()
        logEvent(ctx, TAG, "Diagnostic log window toggled: enabled=$enabled")
    }

    fun isLogsEnabled(ctx: Context): Boolean {
        init(ctx)
        val prefs = ctx.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DIAGNOSTIC_LOGS_ENABLED, false)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashLog(e)
        defaultHandler?.uncaughtException(t, e)
    }

    /**
     * Enregistre une ligne d'événement horodatée (BootReceiver, Service, Audio, UI)
     * avec le temps écoulé depuis le démarrage matériel (uptime) pour distinguer
     * un vrai Cold Boot d'une veille MCU sur autoradio Android 8.
     */
    @Synchronized
    fun logEvent(ctx: Context?, tag: String, message: String) {
        Log.i(tag, message)
        try {
            val resolvedCtx = ctx?.applicationContext ?: ctx ?: if (::context.isInitialized) context else return
            if (!::context.isInitialized) {
                context = resolvedCtx
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val uptimeSec = SystemClock.elapsedRealtime() / 1000L
            val line = "[$timestamp] [up:${uptimeSec}s] [$tag] $message\n"

            val logDir = getInternalLogsDirectory(resolvedCtx)
            if (logDir != null) {
                val diagFile = File(logDir, DIAG_FILE_NAME)
                if (diagFile.exists() && diagFile.length() + line.length > MAX_LOG_SIZE_BYTES) {
                    rotateLogFile(diagFile)
                }
                diagFile.appendText(line)
            }
        } catch (ex: Throwable) {
            Log.e(TAG, "Error writing diagnostic log event", ex)
        }
    }

    /**
     * Sauvegarde un log de crash dans le répertoire logs
     */
    @Synchronized
    private fun saveCrashLog(e: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val uptimeSec = SystemClock.elapsedRealtime() / 1000L
            val logContent = "\n[CRASH] [$timestamp] [up:${uptimeSec}s] Thread: ${Thread.currentThread().name}\n$stackTrace\n${"=".repeat(60)}\n"

            if (::context.isInitialized) {
                val internalDir = getInternalLogsDirectory(context)
                if (internalDir != null) {
                    val diagFile = File(internalDir, DIAG_FILE_NAME)
                    diagFile.appendText(logContent)
                }
            }

            val logDir = getLogsDirectory()
            if (logDir != null) {
                val currentLogFile = File(logDir, "${LOG_FILE_PREFIX}current${LOG_FILE_EXTENSION}")
                if (currentLogFile.exists() && currentLogFile.length() + logContent.length > MAX_LOG_SIZE_BYTES) {
                    rotateLogFile(currentLogFile)
                }
                currentLogFile.appendText(logContent)
                Log.e(TAG, "Crash log saved to: ${currentLogFile.absolutePath}")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error saving crash log", ex)
        }
    }

    private fun rotateLogFile(currentFile: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val archivedFile = File(
                currentFile.parentFile,
                "${LOG_FILE_PREFIX}${timestamp}${LOG_FILE_EXTENSION}"
            )
            currentFile.renameTo(archivedFile)
        } catch (ex: Exception) {
            Log.e(TAG, "Error rotating log file", ex)
        }
    }

    /**
     * Répertoire interne toujours disponible dès la première milliseconde du boot
     * (/data/data/com.github.mkalmousli.floating_volume/files/logs)
     */
    private fun getInternalLogsDirectory(ctx: Context): File? {
        return try {
            val dir = File(ctx.filesDir, LOG_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (ex: Exception) {
            null
        }
    }

    /**
     * Répertoire externe de l'application
     * (/storage/emulated/0/Android/data/com.github.mkalmousli.floating_volume/files/logs/)
     */
    private fun getLogsDirectory(): File? {
        if (!::context.isInitialized) return null
        return try {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            val dir = File(base, LOG_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting logs directory", ex)
            null
        }
    }

    fun getLogsDirectoryPath(): String? = getLogsDirectory()?.absolutePath

    fun getLogFiles(): List<File> {
        val files = mutableListOf<File>()
        if (::context.isInitialized) {
            getInternalLogsDirectory(context)?.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }?.let {
                files.addAll(it)
            }
        }
        getLogsDirectory()?.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }?.forEach { extFile ->
            if (files.none { it.absolutePath == extFile.absolutePath }) {
                files.add(extFile)
            }
        }
        return files.sortedByDescending { it.lastModified() }
    }

    /**
     * Génère le rapport complet affiché dans la fenêtre de logs Flutter et utilisé
     * pour les boutons Copier, Partager et Enregistrer.
     */
    fun getFullDiagnosticReport(ctx: Context): String {
        init(ctx)
        val sb = StringBuilder()
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val uptimeMs = SystemClock.elapsedRealtime()
        val uptimeMin = uptimeMs / 60000L
        val uptimeSec = (uptimeMs % 60000L) / 1000L

        val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try { Settings.canDrawOverlays(ctx) } catch (_: Throwable) { false }
        } else true

        val isIgnoringBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: false
            } catch (_: Throwable) { false }
        } else true

        val prefs = ctx.getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)
        val autoStart = prefs.getBoolean(BootReceiver.KEY_AUTO_START, false)
        val delayMs = prefs.getLong(BootReceiver.KEY_AUTO_START_DELAY, BootReceiver.DEFAULT_DELAY_MS)
        val lastState = prefs.getBoolean(BootReceiver.KEY_LAST_SERVICE_STATE, false)
        val logsEnabled = prefs.getBoolean(KEY_DIAGNOSTIC_LOGS_ENABLED, false)
        val serviceRunning = FloatingVolumeService.instance != null

        sb.appendLine("=== FLOATING VOLUME v0.9.11+101 — DIAGNOSTIC ===")
        sb.appendLine("Date rapport    : $now")
        sb.appendLine("Android affiché : Android ${Build.VERSION.RELEASE} (SDK_INT réel = ${Build.VERSION.SDK_INT})")
        sb.appendLine("Matériel        : ${Build.MANUFACTURER} | ${Build.MODEL} | ${Build.BOARD} | ${Build.HARDWARE}")
        sb.appendLine("Uptime système  : ${uptimeMin}m ${uptimeSec}s (${uptimeMs / 1000}s depuis vrai boot)")
        sb.appendLine("Permissions     : Overlay=$canOverlay | BatterieIgnorée=$isIgnoringBattery")
        sb.appendLine("Config AutoStart: Activé=$autoStart | Délai=${delayMs}ms | LastState=$lastState | LogsUI=$logsEnabled")
        sb.appendLine("Service Actif   : $serviceRunning")
        sb.appendLine("================================================")
        sb.appendLine()
        sb.appendLine("--- JOURNAL DES ÉVÉNEMENTS & CRASHS ---")

        val allFiles = getLogFiles()
        if (allFiles.isEmpty()) {
            sb.appendLine("(Aucun événement enregistré pour le moment)")
        } else {
            allFiles.sortedBy { it.lastModified() }.forEach { file ->
                try {
                    val text = file.readText()
                    if (text.isNotBlank()) {
                        if (allFiles.size > 1) {
                            sb.appendLine("--- Fichier : ${file.name} ---")
                        }
                        // Limiter aux 300 dernières lignes pour garder une UI ultra-fluide
                        val lines = text.lines()
                        val tailLines = if (lines.size > 300) lines.takeLast(300) else lines
                        sb.appendLine(tailLines.joinToString("\n"))
                    }
                } catch (e: Exception) {
                    sb.appendLine("Erreur lecture ${file.name}: ${e.message}")
                }
            }
        }
        return sb.toString()
    }

    /**
     * Enregistre le rapport complet sous forme de fichier .txt directement dans
     * le dossier public Download/ (ou à défaut dans le dossier externe de l'application).
     */
    fun saveLogsToPublicDisk(ctx: Context): String {
        init(ctx)
        val content = getFullDiagnosticReport(ctx)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "FloatingVolume_Log_${timestamp}.txt"

        // 1. Essayer dans /storage/emulated/0/Download/
        try {
            @Suppress("DEPRECATION")
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir != null) {
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val outFile = File(downloadDir, fileName)
                outFile.writeText(content)
                logEvent(ctx, TAG, "Logs sauvegardés sur disque : ${outFile.absolutePath}")
                return outFile.absolutePath
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Fallback from public Download dir: ${e.message}")
        }

        // 2. Repli dans /storage/emulated/0/Android/data/.../files/logs/
        val fallbackDir = getLogsDirectory() ?: getInternalLogsDirectory(ctx)
            ?: throw IllegalStateException("Impossible d'accéder au stockage")
        val fallbackFile = File(fallbackDir, fileName)
        fallbackFile.writeText(content)
        logEvent(ctx, TAG, "Logs sauvegardés dans : ${fallbackFile.absolutePath}")
        return fallbackFile.absolutePath
    }

    fun getTotalLogsSize(): Long {
        return getLogFiles().sumOf { it.length() }
    }

    fun exportLogs(): Uri? {
        return try {
            val logFiles = getLogFiles()
            if (logFiles.isEmpty()) return null

            val exportFile = createExportFile()
            java.util.zip.ZipOutputStream(exportFile.outputStream()).use { zos ->
                logFiles.forEach { logFile ->
                    val entry = java.util.zip.ZipEntry(logFile.name)
                    zos.putNextEntry(entry)
                    logFile.inputStream().use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )
        } catch (ex: Exception) {
            Log.e(TAG, "Error exporting logs", ex)
            null
        }
    }

    private fun createExportFile(): File {
        val exportDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            File(context.getExternalFilesDir(null)?.parentFile, "export").apply {
                if (!exists()) mkdirs()
            }
        } else {
            context.getExternalFilesDir(null)
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(exportDir, "FloatingVolume_Logs_${timestamp}.zip")
    }

    /**
     * Crée un Intent de partage en texte brut (universel pour Email, Éditeurs de texte,
     * Bluetooth, Notes sur Android 8+).
     */
    fun createShareLogsIntent(ctx: Context? = null): Intent? {
        val resolvedCtx = ctx ?: if (::context.isInitialized) context else return null
        val reportText = getFullDiagnosticReport(resolvedCtx)

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "FloatingVolume v0.9.11 - Logs de Diagnostic")
            putExtra(Intent.EXTRA_TEXT, reportText)
        }
    }

    /**
     * Efface tous les fichiers de log (internes et externes)
     */
    @Synchronized
    fun clearLogs(ctx: Context? = null): Boolean {
        ctx?.let { init(it) }
        return try {
            val logFiles = getLogFiles()
            var allDeleted = true
            logFiles.forEach { file ->
                if (!file.delete()) {
                    allDeleted = false
                }
            }
            if (::context.isInitialized) {
                logEvent(context, TAG, "Journal effacé manuellement par l'utilisateur.")
            }
            allDeleted
        } catch (ex: Exception) {
            Log.e(TAG, "Error clearing logs", ex)
            false
        }
    }

    fun deleteLogFile(logFile: File): Boolean {
        return try {
            logFile.delete()
        } catch (ex: Exception) {
            false
        }
    }

    fun readLogFile(logFile: File): String? {
        return try {
            logFile.readText()
        } catch (ex: Exception) {
            null
        }
    }

    data class LogStats(
        val totalFiles: Int,
        val totalSizeBytes: Long,
        val oldestLogDate: Date?,
        val newestLogDate: Date?
    )

    fun getLogStats(ctx: Context? = null): LogStats {
        ctx?.let { init(it) }
        val logFiles = getLogFiles()
        return LogStats(
            totalFiles = logFiles.size,
            totalSizeBytes = logFiles.sumOf { it.length() },
            oldestLogDate = logFiles.lastOrNull()?.let { Date(it.lastModified()) },
            newestLogDate = logFiles.firstOrNull()?.let { Date(it.lastModified()) }
        )
    }
}
