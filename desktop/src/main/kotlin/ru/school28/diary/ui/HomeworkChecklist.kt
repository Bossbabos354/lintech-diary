package ru.school28.diary.ui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.school28.diary.Settings
import ru.school28.diary.api.DairyEntry
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Локальный чек-лист домашних заданий: отметки «сделал» хранятся
 * на компьютере, в журнал не пишутся. Ключ — дата + урок + предмет.
 */
object HomeworkChecklist {
    private const val KEEP_DAYS = 60L
    private val gson = Gson()
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun keyOf(entry: DairyEntry): String =
        dateFmt.format(entry.date) + "|" + entry.lessonNumber + "|" + entry.subjectId

    fun isDone(entry: DairyEntry): Boolean = load()[keyOf(entry)] == true

    /** Переключает отметку. Возвращает новое состояние. */
    fun toggle(entry: DairyEntry): Boolean {
        val map = load()
        val key = keyOf(entry)
        val next = map[key] != true
        if (next) map[key] = true else map.remove(key)

        val cutoff = System.currentTimeMillis() - KEEP_DAYS * 24 * 3600 * 1000
        map.keys.removeAll { dateOf(it)?.time?.let { t -> t < cutoff } == true }

        Settings.homeworkDoneJson = gson.toJson(map)
        return next
    }

    fun doneCount(entries: List<DairyEntry>): Int {
        val map = load()
        return entries.count { map[keyOf(it)] == true }
    }

    private fun load(): MutableMap<String, Boolean> {
        val raw = Settings.homeworkDoneJson ?: return mutableMapOf()
        return try {
            val type = object : TypeToken<MutableMap<String, Boolean>>() {}.type
            gson.fromJson<MutableMap<String, Boolean>>(raw, type).orEmpty().toMutableMap()
        } catch (e: Exception) {
            mutableMapOf()
        }
    }

    private fun dateOf(key: String): java.util.Date? =
        runCatching { dateFmt.parse(key.substringBefore('|')) }.getOrNull()
}
