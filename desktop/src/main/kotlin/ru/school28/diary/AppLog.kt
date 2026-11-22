package ru.school28.diary

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Простой файловый лог: те же события, что писал Android-клиент. */
object AppLog {
    private const val MAX_BYTES = 1_000_000L
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun log(msg: String) {
        try {
            AppPaths.ensureDirs()
            val file = AppPaths.logFile
            if (file.length() > MAX_BYTES) {
                val old = File(AppPaths.dataDir, "diary.log.old")
                old.delete()
                file.renameTo(old)
            }
            file.appendText("${sdf.format(Date())} $msg\n")
        } catch (_: Exception) {
            // лог не должен ломать приложение
        }
    }

    fun logException(tag: String, e: Throwable) {
        log("$tag EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
        e.stackTrace.take(15).forEach { log("$tag   at $it") }
    }
}
