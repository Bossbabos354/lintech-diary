package ru.school28.diary

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val WORK_NAME = "diary_notifier"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val intervalMin = Prefs.getNotifIntervalMin(context).coerceIn(15, 1440)

        val request = PeriodicWorkRequestBuilder<NotifierWorker>(
            intervalMin.toLong(), TimeUnit.MINUTES,
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}