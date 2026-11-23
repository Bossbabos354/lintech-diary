package ru.school28.diary.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Обновляет виджеты при событиях, после которых данные гарантированно
 * устарели: перезагрузка телефона, обновление приложения, смена времени
 * или часового пояса. Без этого виджеты «замирали» до повторного добавления.
 */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                WidgetUpdater.updateAll(context)
                WidgetUpdateScheduler.schedule(context)
            }
        }
    }
}
