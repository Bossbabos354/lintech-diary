package ru.school28.diary

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Уведомления приложения. Три независимых канала:
 *  - дневник (оценки и домашние задания),
 *  - события с сайта школы,
 *  - выход новой версии приложения.
 *
 * У каждого канала свой id уведомления, поэтому они не перетирают друг друга.
 * Тап по уведомлению открывает приложение на нужной вкладке.
 */
object NotificationHelper {

    // Каналы
    const val CHANNEL_DIARY = "diary_notifications"
    const val CHANNEL_EVENTS = "events_notifications"
    const val CHANNEL_UPDATES = "updates_notifications"

    // Идентификаторы уведомлений (разные, чтобы не перезаписывались)
    private const val ID_DIARY = 1001
    private const val ID_EVENTS = 1002
    private const val ID_UPDATE = 1003
    private const val ID_DIGEST = 1004

    // Индексы вкладок MainActivity
    // Порядок: Дневник, Расписание, Оценки, Цели, События, Настройки
    const val TAB_DIARY = 0
    const val TAB_SCHEDULE = 1
    const val TAB_MARKS = 2
    const val TAB_GOALS = 3
    const val TAB_EVENTS = 4
    const val TAB_SETTINGS = 5

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DIARY,
                "Оценки и домашние задания",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Новые оценки и изменения домашних заданий" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_EVENTS,
                "События школы",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Новые события с сайта школы" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_UPDATES,
                "Обновления приложения",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Выход новых версий приложения" }
        )
    }

    /** Совместимость со старым вызовом. */
    fun createChannel(context: Context) = createChannels(context)

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    // ---- Публичные методы ----

    fun showDiary(context: Context, title: String, text: String) {
        show(context, CHANNEL_DIARY, ID_DIARY, title, text, TAB_DIARY, false)
    }

    fun showEvents(context: Context, title: String, text: String) {
        show(context, CHANNEL_EVENTS, ID_EVENTS, title, text, TAB_EVENTS, false)
    }

    /** Один ежедневный дайджест вместо потока уведомлений. */
    fun showDigest(context: Context, title: String, text: String) {
        show(context, CHANNEL_DIARY, ID_DIGEST, title, text, TAB_MARKS, false)
    }

    fun showUpdate(context: Context, title: String, text: String) {
        // Тап по уведомлению об обновлении открывает приложение и запускает проверку,
        // чтобы пользователь сразу увидел диалог скачивания.
        show(context, CHANNEL_UPDATES, ID_UPDATE, title, text, TAB_DIARY, true)
    }

    // ---- Внутреннее ----

    /** FLAG_IMMUTABLE появился в Android 6, на Android 5 его быть не должно. */
    private fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    private fun show(
        context: Context,
        channelId: String,
        notificationId: Int,
        title: String,
        text: String,
        tab: Int,
        forceCheckUpdate: Boolean
    ) {
        // Сначала кладём в локальную историю — центр уведомлений в настройках
        // должен показывать их даже если системные уведомления запрещены.
        NotificationStore.append(context, title, text, channelId, tab, forceCheckUpdate)

        if (!canPost(context)) return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TAB, tab)
            if (forceCheckUpdate) putExtra(MainActivity.EXTRA_CHECK_UPDATE, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingFlags()
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Разрешение отозвали между проверкой и показом
        }
    }
}