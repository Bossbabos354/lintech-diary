package ru.school28.diary.ui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.school28.diary.Settings
import ru.school28.diary.api.LessonMarks
import ru.school28.diary.api.SubjectMarks
import java.util.Locale
import kotlin.math.ceil

/** Цель по предмету: какой балл хотим получить за период. */
data class Goal(
    val subjectId: Int,
    val subjectName: String,
    val periodKey: String,
    val targetMark: Int,
    val allowedMarks: List<Int>,
) {
    val id: String get() = "$subjectId|$periodKey"
}

data class GoalPeriod(val key: String, val label: String, val quarterIds: List<Int>)

object GoalPeriods {
    private const val Q1 = 360000
    private const val Q2 = 360001
    private const val Q3 = 360002
    private const val Q4 = 360003

    val all: List<GoalPeriod> = listOf(
        GoalPeriod("Q1", "1 четверть", listOf(Q1)),
        GoalPeriod("Q2", "2 четверть", listOf(Q2)),
        GoalPeriod("Q3", "3 четверть", listOf(Q3)),
        GoalPeriod("Q4", "4 четверть", listOf(Q4)),
        GoalPeriod("H1", "1 полугодие", listOf(Q1, Q2)),
        GoalPeriod("H2", "2 полугодие", listOf(Q3, Q4)),
        GoalPeriod("YEAR", "Учебный год", emptyList()),
    )

    fun byKey(key: String): GoalPeriod = all.firstOrNull { it.key == key } ?: all.last()
}

object GoalStore {
    private val gson = Gson()

    fun load(): List<Goal> {
        val raw = Settings.goalsJson ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Goal>>() {}.type
            gson.fromJson<List<Goal>>(raw, type).orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(goals: List<Goal>) {
        Settings.goalsJson = gson.toJson(goals)
    }

    fun upsert(goal: Goal) {
        val goals = load().filterNot { it.id == goal.id }.toMutableList()
        goals.add(goal)
        save(goals)
    }

    fun remove(subjectId: Int, periodKey: String) {
        save(load().filterNot { it.subjectId == subjectId && it.periodKey == periodKey })
    }

    fun find(subjectId: Int, periodKey: String): Goal? =
        load().firstOrNull { it.subjectId == subjectId && it.periodKey == periodKey }
}

object GoalMath {

    /** Порог среднего балла для оценки 5/4/3 (стандартное школьное округление). */
    fun threshold(target: Int): Double = when (target) {
        5 -> 4.5
        4 -> 3.5
        3 -> 2.5
        else -> 1.5
    }

    /** Оценки предмета за период вместе с весами (вес задаёт учитель). */
    fun marksForLessons(subject: SubjectMarks, periodKey: String): List<LessonMarks> {
        val period = GoalPeriods.byKey(periodKey)
        val withPeriod = subject.marks.filter { it.periodId != null }
        val filtered = if (period.quarterIds.isEmpty()) {
            subject.marks
        } else if (withPeriod.isEmpty()) {
            subject.marks
        } else {
            val inPeriod = subject.marks.filter { it.periodId in period.quarterIds }
            if (inPeriod.isNotEmpty()) inPeriod else subject.marks
        }
        return filtered.filter { it.mark.toIntOrNull() != null }
    }

    fun marksFor(subject: SubjectMarks, periodKey: String): List<Int> =
        marksForLessons(subject, periodKey).mapNotNull { it.mark.toIntOrNull() }

    /** Вес оценки в журнале; если учитель его не задал — 1. */
    fun weightOf(lesson: LessonMarks): Double = lesson.weight?.takeIf { it > 0.0 } ?: 1.0

    /** Средневзвешенный балл — так его считает сам журнал. */
    fun weightedAverage(lessons: List<LessonMarks>): Double? {
        var sum = 0.0
        var totalWeight = 0.0
        for (lesson in lessons) {
            val mark = lesson.mark.toIntOrNull() ?: continue
            val weight = weightOf(lesson)
            sum += mark * weight
            totalWeight += weight
        }
        return if (totalWeight == 0.0) null else sum / totalWeight
    }

    /** Период последней оценки — текущая четверть/полугодие. */
    fun currentPeriodId(subject: SubjectMarks): Int? =
        subject.marks.filter { it.periodId != null }
            .maxByOrNull { it.date?.time ?: Long.MIN_VALUE }
            ?.periodId

    /** Оценки текущего периода; если периодов нет — все оценки. */
    fun currentPeriodLessons(subject: SubjectMarks): List<LessonMarks> {
        val all = subject.marks.filter { it.mark.toIntOrNull() != null }
        val id = currentPeriodId(subject) ?: return all
        return all.filter { it.periodId == id }.ifEmpty { all }
    }

    fun currentPeriodAverage(subject: SubjectMarks): Double? =
        weightedAverage(currentPeriodLessons(subject))

    /** Простое среднее — для рядов без весов (оценки за неделю). */
    fun average(marks: List<Int>): Double? =
        if (marks.isEmpty()) null else marks.sum().toDouble() / marks.size

    fun roundedMark(avg: Double?): Int? = when {
        avg == null -> null
        avg >= 4.5 -> 5
        avg >= 3.5 -> 4
        avg >= 2.5 -> 3
        else -> 2
    }

    /** Сколько ещё оценок нужно; новая оценка добавляется с весом 1. */
    fun needed(lessons: List<LessonMarks>, target: Int, allowedMarks: List<Int>): Int? {
        if (allowedMarks.isEmpty()) return null
        val t = threshold(target)
        var sum = 0.0
        var totalWeight = 0.0
        for (lesson in lessons) {
            val mark = lesson.mark.toIntOrNull() ?: continue
            val weight = weightOf(lesson)
            sum += mark * weight
            totalWeight += weight
        }
        if (totalWeight > 0.0 && sum / totalWeight >= t) return 0
        if (totalWeight == 0.0 && t <= 0) return 0
        val best = allowedMarks.maxOrNull() ?: return null
        if (best < t) return null
        val k = kotlin.math.ceil((t * totalWeight - sum) / (best - t)).toInt()
        return k.coerceAtLeast(1)
    }

    fun hint(lessons: List<LessonMarks>, goal: Goal): String {
        val need = needed(lessons, goal.targetMark, goal.allowedMarks)
        val avg = weightedAverage(lessons)
        val avgText = if (avg == null) "нет оценок" else String.format(Locale("ru"), "%.2f", avg)
        return when {
            need == null -> "С набором ${goal.allowedMarks.joinToString("/")} цели не достичь"
            need == 0 -> "Цель достигнута! Средний: $avgText"
            else -> {
                val best = goal.allowedMarks.maxOrNull() ?: goal.targetMark
                "Нужно ещё $need × «$best». Сейчас средний: $avgText"
            }
        }
    }
}
