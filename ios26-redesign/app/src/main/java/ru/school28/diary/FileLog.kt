package ru.school28.diary

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object  FileLog {
    @Volatile
    private var logFile: File? = null

    // SimpleDateFormat не потокобезопасен, а лог пишут и UI, и NotifierWorker
    // с фонового потока — поэтому и форматирование, и запись идут под одним локом.
    private val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        if (logFile == null) {
            logFile = File(context.filesDir, "diary.log")
            log("=== APP START ===")
        }
    }

    fun log(msg: String) {
        try {
            val f = logFile ?: return
            synchronized(this) {
                FileWriter(f, true).use { w ->
                    w.write("${sdf.format(Date())} $msg\n")
                }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun logException(tag: String, e: Exception) {
        log("$tag EXCEPTION: ${e.javaClass.simpleName}: ${e.message}")
        e.stackTrace.take(15).forEach { log("$tag   at $it") }
    }
}