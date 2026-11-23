package ru.school28.diary.api

import androidx.compose.runtime.Immutable

/** Звонок: номер урока и время начала/конца. */
@Immutable
data class BellTime(
    val n: Int,
    val start: String,
    val end: String
)

/** Урок в расписании класса. */
@Immutable
data class ScheduleLesson(
    val n: Int,
    val subject: String,
    val teacher: String? = null,
    val room: String? = null
)

@Immutable
data class Bells(
    val weekday: List<BellTime> = emptyList(),
    val saturday: List<BellTime> = emptyList()
)

/**
 * Расписание школы: список классов, звонки (будни/суббота)
 * и уроки по классам и дням (mon..sat).
 */
@Immutable
data class SchoolSchedule(
    val classes: List<String> = emptyList(),
    val bells: Bells = Bells(),
    val lessons: Map<String, Map<String, List<ScheduleLesson>>> = emptyMap()
) {
    /** Уроки класса на конкретный день (ключи: mon, tue, ...). */
    fun lessonsFor(className: String, dayKey: String): List<ScheduleLesson> =
        lessons[className]?.get(dayKey).orEmpty().sortedBy { it.n }

    /** Звонки на день: суббота — отдельный набор, остальные — будни. */
    fun bellsFor(dayKey: String): List<BellTime> =
        if (dayKey == "sat") bells.saturday else bells.weekday
}