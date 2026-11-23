package ru.school28.diary.ui

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.school28.diary.Prefs
import ru.school28.diary.api.DairyEntry
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Локальный чек-лист домашних заданий: отметки «сделал» хранятся
 * на устройстве, в журнал не пишутся. Ключ — дата + урок + предмет.
 */
object HomeworkChecklist {

    private const val KEEP_DAYS = 60L
    private val gson = Gson()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun keyOf(entry: DairyEntry): String =
        dateFmt.format(entry.date) + "|" + entry.lessonNumber + "|" + entry.subjectId

    fun isDone(ctx: Context, entry: DairyEntry): Boolean =
        load(ctx)[keyOf(entry)] == true

    /** Переключает отметку. Возвращает новое состояние. */
    fun toggle(ctx: Context, entry: DairyEntry): Boolean {
        val map = load(ctx)
        val key = keyOf(entry)
        val next = map[key] != true
        if (next) map[key] = true else map.remove(key)

        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24 * 3600 * 1000
        map.keys.removeAll { dateOf(it)?.time?.let { t -> t < cutoff } == true }

        Prefs.setHomeworkDone(ctx, gson.toJson(map))
        return next
    }

    fun doneCount(ctx: Context, entries: List<DairyEntry>): Int {
        val map = load(ctx)
        return entries.count { map[keyOf(it)] == true }
    }

    private fun load(ctx: Context): MutableMap<String, Boolean> {
        val raw = Prefs.getHomeworkDone(ctx) ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, Boolean>>() {}.type
            gson.fromJson<MutableMap<String, Boolean>>(raw, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun dateOf(key: String): java.util.Date? =
        runCatching { dateFmt.parse(key.substringBefore('|')) }.getOrNull()
}
