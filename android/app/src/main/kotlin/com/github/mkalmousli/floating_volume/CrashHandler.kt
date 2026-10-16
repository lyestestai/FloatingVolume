package com.github.mkalmousli.floating_volume

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
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
    private const val LOG_FILE_EXTENSION = ".txt"
    private const val MAX_LOG_SIZE_BYTES = 5 * 1024 * 1024 // 5 MB
    private const val TAG = "CrashHandler"

    fun init(context: Context) {
        this.context = context
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashLog(e)
        defaultHandler?.uncaughtException(t, e)
    }

    /**
     * Sauvegarde un log de crash dans le répertoire logs
     * Effectue automatiquement la rotation si le fichier dépasse MAX_LOG_SIZE_BYTES
     */
    private fun saveCrashLog(e: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            e.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val logId = System.currentTimeMillis()
            val logContent = "[$logId] Time: $timestamp\nThread: ${Thread.currentThread().name}\n$stackTrace\n${"=".repeat(80)}\n\n"

            val logDir = getLogsDirectory()
            if (logDir != null) {
                val currentLogFile = File(logDir, "${LOG_FILE_PREFIX}current${LOG_FILE_EXTENSION}")

                // Vérifier si rotation nécessaire
                if (currentLogFile.exists() && currentLogFile.length() + logContent.length > MAX_LOG_SIZE_BYTES) {
                    rotateLogFile(currentLogFile)
                }

                currentLogFile.appendText(logContent)
                Log.e(TAG, "Crash log saved to: ${currentLogFile.absolutePath}")
            } else {
                Log.e(TAG, "Could not get logs directory")
            }

        } catch (ex: Exception) {
            Log.e(TAG, "Error saving crash log", ex)
        }
    }

    /**
     * Archive le fichier de log courant avec un timestamp
     */
    private fun rotateLogFile(currentFile: File) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val archivedFile = File(
                currentFile.parentFile,
                "${LOG_FILE_PREFIX}${timestamp}${LOG_FILE_EXTENSION}"
            )
            currentFile.renameTo(archivedFile)
            Log.d(TAG, "Log file rotated to: ${archivedFile.absolutePath}")
        } catch (ex: Exception) {
            Log.e(TAG, "Error rotating log file", ex)
        }
    }

    /**
     * Obtient ou crée le répertoire des logs
     * Chemin: /storage/emulated/0/Android/data/com.github.mkalmousli.floating_volume/files/logs/
     */
    private fun getLogsDirectory(): File? {
        return try {
            val dir = File(context.getExternalFilesDir(null), LOG_DIR_NAME)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        } catch (ex: Exception) {
            Log.e(TAG, "Error getting logs directory", ex)
            null
        }
    }

    /**
     * Retourne le chemin complet du répertoire des logs
     */
    fun getLogsDirectoryPath(): String? = getLogsDirectory()?.absolutePath

    /**
     * Liste tous les fichiers de log disponibles
     */
    fun getLogFiles(): List<File> {
        val logDir = getLogsDirectory() ?: return emptyList()
        return logDir.listFiles { file ->
            file.isFile && file.name.startsWith(LOG_FILE_PREFIX)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Obtient la taille totale des logs en bytes
     */
    fun getTotalLogsSize(): Long {
        return getLogFiles().sumOf { it.length() }
    }

    /**
     * Export les logs vers le répertoire Documents avec création d'Intent de partage
     * Retourne l'Uri du fichier ZIP créé
     */
    fun exportLogs(): Uri? {
        return try {
            val logFiles = getLogFiles()
            if (logFiles.isEmpty()) {
                Log.w(TAG, "No log files to export")
                return null
            }

            val exportFile = createExportFile()

            // Créer un ZIP contenant tous les logs
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

            Log.d(TAG, "Logs exported to: ${exportFile.absolutePath}")

            // Retourner l'Uri via FileProvider
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

    /**
     * Crée le fichier ZIP d'export dans le répertoire Documents
     */
    private fun createExportFile(): File {
        val exportDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Sur Android 10+, utiliser le répertoire Documents de l'app
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
     * Retourne un Intent prêt pour partager les logs exportés
     */
    fun createShareLogsIntent(): Intent? {
        val exportUri = exportLogs() ?: return null

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, exportUri)
            putExtra(Intent.EXTRA_SUBJECT, "FloatingVolume Crash Logs")
            putExtra(Intent.EXTRA_TEXT, "Attached are the crash logs from FloatingVolume application")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Efface tous les fichiers de log
     * Retourne true si succès, false sinon
     */
    fun clearLogs(): Boolean {
        return try {
            val logFiles = getLogFiles()
            var allDeleted = true
            logFiles.forEach { file ->
                if (!file.delete()) {
                    Log.w(TAG, "Failed to delete log file: ${file.absolutePath}")
                    allDeleted = false
                } else {
                    Log.d(TAG, "Deleted log file: ${file.absolutePath}")
                }
            }
            allDeleted
        } catch (ex: Exception) {
            Log.e(TAG, "Error clearing logs", ex)
            false
        }
    }

    /**
     * Efface un fichier de log spécifique
     */
    fun deleteLogFile(logFile: File): Boolean {
        return try {
            if (logFile.delete()) {
                Log.d(TAG, "Deleted log file: ${logFile.absolutePath}")
                true
            } else {
                Log.w(TAG, "Failed to delete log file: ${logFile.absolutePath}")
                false
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error deleting log file", ex)
            false
        }
    }

    /**
     * Lecture du contenu d'un fichier de log
     */
    fun readLogFile(logFile: File): String? {
        return try {
            logFile.readText()
        } catch (ex: Exception) {
            Log.e(TAG, "Error reading log file", ex)
            null
        }
    }

    /**
     * Obtient les stats sur les logs
     */
    data class LogStats(
        val totalFiles: Int,
        val totalSizeBytes: Long,
        val oldestLogDate: Date?,
        val newestLogDate: Date?
    )

    fun getLogStats(): LogStats {
        val logFiles = getLogFiles()
        return LogStats(
            totalFiles = logFiles.size,
            totalSizeBytes = logFiles.sumOf { it.length() },
            oldestLogDate = logFiles.lastOrNull()?.let { Date(it.lastModified()) },
            newestLogDate = logFiles.firstOrNull()?.let { Date(it.lastModified()) }
        )
    }
}
