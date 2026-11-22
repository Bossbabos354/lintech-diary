package ru.school28.diary.widgets

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ru.school28.diary.FileLog
import ru.school28.diary.Prefs

/**
 * Немедленная синхронизация виджетов после загрузки данных в приложении.
 *
 * 1. Сразу перерисовывает виджеты из текущего снимка (без сети).
 * 2. Ставит разовую задачу [WidgetDataWorker], чтобы обновить сам снимок.
 *
 * Частые вызовы схлопываются: не чаще одного раза в минуту.
 */
object WidgetSync {
    private const val MIN_INTERVAL_MS = 60_000L
    private const val WORK_NAME = "widget_sync"

    @Volatile
    private var lastRequest = 0L

    fun request(context: Context) {
        val ctx = context.applicationContext
        if (Prefs.getStudentId(ctx) == -1) {
            FileLog.log("WidgetSync: пропуск, нет studentId")
            return
        }

        val now = System.currentTimeMillis()
        synchronized(this) {
            if (now - lastRequest < MIN_INTERVAL_MS) {
                FileLog.log("WidgetSync: троттлинг")
                return
            }
            lastRequest = now
        }

        FileLog.log("WidgetSync: обновляю виджеты")
        WidgetUpdater.updateAll(ctx)
        refreshSnapshot(ctx)
    }

    /** Принудительно, без троттлинга — например, после ручного обновления. */
    fun force(context: Context) {
        val ctx = context.applicationContext
        if (Prefs.getStudentId(ctx) == -1) return
        WidgetUpdater.updateAll(ctx)
        refreshSnapshot(ctx)
    }

    private fun refreshSnapshot(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetDataWorker>().build()
        )
    }
}
