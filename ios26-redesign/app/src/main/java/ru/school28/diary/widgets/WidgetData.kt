package ru.school28.diary.widgets

import android.content.Context
import com.google.gson.Gson
import ru.school28.diary.Prefs

/** Строка дневника на сегодня для виджета 4×4. */
data class DiaryRow(
    val time: String? = null,
    val subject: String? = null,
    val homework: String? = null,
    val mark: String? = null
)

/** Строка домашки для виджета с галочками. */
data class HomeworkRow(
    val key: String = "",
    val subject: String? = null,
    val task: String? = null
)

/**
 * Снимок данных для виджетов. Обновляется фоновым воркером,
 * чтобы сами виджеты (RemoteViews) не ходили в сеть.
 */
data class WidgetData(
    // Ближайший урок
    val nearestLabel: String? = null,
    val nearestTime: String? = null,
    val nearestSubject: String? = null,
    val nearestDetails: String? = null,
    val nearestStartAt: Long? = null,
    // Средний балл
    val avgMark: String? = null,
    val avgDelta: String? = null,
    // Дневник на сегодня
    val diaryHeader: String? = null,
    val diaryRows: List<DiaryRow> = emptyList(),
    val diaryMore: String? = null,
    // Домашка на сегодня (только уроки с заданием)
    val homeworkRows: List<HomeworkRow> = emptyList(),
    /** Дневник хотя бы раз успешно прочитан — иначе «нет данных», а не «домашки нет». */
    val homeworkLoaded: Boolean = false,
    // Цель
    val goalTitle: String? = null,
    val goalLine: String? = null,
    val goalCurrent: String? = null,
    val goalTarget: String? = null,
    val goalProgress: Int = 0,
    // Расписание на завтра
    val scheduleHeader: String? = null,
    val scheduleLines: List<String> = emptyList(),
    // Служебное
    val updatedAt: Long = 0L
)

object WidgetDataStore {
    private val gson = Gson()

    fun load(context: Context): WidgetData {
        val raw = Prefs.getWidgetDataJson(context) ?: return WidgetData()
        return try {
            gson.fromJson(raw, WidgetData::class.java) ?: WidgetData()
        } catch (e: Exception) {
            WidgetData()
        }
    }

    fun save(context: Context, data: WidgetData) {
        Prefs.setWidgetDataJson(context, gson.toJson(data))
    }
}
