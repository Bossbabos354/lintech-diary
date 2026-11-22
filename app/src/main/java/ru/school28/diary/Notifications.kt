package ru.school28.diary

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Запись в истории уведомлений.
 *
 * @param channel "diary" | "events" | "update" — откуда пришло уведомление.
 * @param tab вкладка MainActivity, которую открыть по тапу.
 */
data class AppNotification(
    val id: Long,
    val title: String,
    val text: String,
    val channel: String,
    val time: Long,
    val read: Boolean = false,
    val tab: Int = 0,
    val forceUpdate: Boolean = false
)

/**
 * История всех показанных уведомлений. Хранится локально в SharedPreferences,
 * чтобы центр уведомлений работал без сети и переживал перезапуск приложения.
 */
object NotificationStore {
    private val gson = Gson()
    private const val MAX_ITEMS = 200

    fun load(context: Context): List<AppNotification> {
        val raw = Prefs.getNotificationsJson(context) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<AppNotification>>() {}.type
            gson.fromJson<List<AppNotification>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, items: List<AppNotification>) {
        val trimmed = items.take(MAX_ITEMS)
        Prefs.setNotificationsJson(context, gson.toJson(trimmed))
    }

    /** Добавляет уведомление в историю (новые — сверху). */
    fun append(
        context: Context,
        title: String,
        text: String,
        channel: String,
        tab: Int,
        forceUpdate: Boolean
    ) {
        val items = load(context).toMutableList()
        items.add(
            0,
            AppNotification(
                id = System.currentTimeMillis(),
                title = title,
                text = text,
                channel = channel,
                time = System.currentTimeMillis(),
                read = false,
                tab = tab,
                forceUpdate = forceUpdate
            )
        )
        save(context, items)
    }

    fun unreadCount(context: Context): Int = load(context).count { !it.read }

    fun markAllRead(context: Context) {
        val items = load(context)
        if (items.none { !it.read }) return
        save(context, items.map { it.copy(read = true) })
    }

    fun markRead(context: Context, id: Long) {
        val items = load(context)
        save(context, items.map { if (it.id == id) it.copy(read = true) else it })
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }
}