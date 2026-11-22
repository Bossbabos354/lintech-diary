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

    // Кэш в памяти: isDone() вызывается на каждый урок в списке,
    // поэтому JSON из SharedPreferences разбираем один раз.
    private var cached: MutableMap<String, Boolean>? = null

    // Ключ считается один раз на урок: SimpleDateFormat на каждую строку
    // списка заметно тормозит прокрутку.
    private val keyCache = java.util.WeakHashMap<DairyEntry, String>()

    fun keyOf(entry: DairyEntry): String = synchronized(keyCache) {
        keyCache.getOrPut(entry) {
            dateFmt.format(entry.date) + "|" + entry.lessonNumber + "|" + entry.subjectId
        }
    }

    fun isDone(ctx: Context, entry: DairyEntry): Boolean =
        isDone(ctx, keyOf(entry))

    /** То же по готовому ключу — так виджет работает без объекта урока. */
    fun isDone(ctx: Context, key: String): Boolean = load(ctx)[key] == true

    /** Переключает отметку. Возвращает новое состояние. */
    fun toggle(ctx: Context, entry: DairyEntry): Boolean =
        toggleKey(ctx, keyOf(entry))

    /** Переключает отметку по ключу (виджет на главном экране). */
    fun toggleKey(ctx: Context, key: String): Boolean {
        val map = load(ctx)
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

    fun doneCountKeys(ctx: Context, keys: List<String>): Int {
        val map = load(ctx)
        return keys.count { map[it] == true }
    }

    private fun load(ctx: Context): MutableMap<String, Boolean> {
        cached?.let { return it }
        val raw = Prefs.getHomeworkDone(ctx)
        val map = if (raw == null) {
            mutableMapOf()
        } else try {
            val type = object : TypeToken<MutableMap<String, Boolean>>() {}.type
            gson.fromJson<MutableMap<String, Boolean>>(raw, type) ?: mutableMapOf()
        } catch (e: Exception) {
            mutableMapOf()
        }
        cached = map
        return map
    }

    private fun dateOf(key: String): java.util.Date? =
        runCatching { dateFmt.parse(key.substringBefore('|')) }.getOrNull()
}
