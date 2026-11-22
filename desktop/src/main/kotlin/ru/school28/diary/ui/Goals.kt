package ru.school28.diary.ui

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.school28.diary.Settings
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
    /** Порог среднего балла для оценки 5/4/3. */
    fun threshold(target: Int): Double = when (target) {
        5 -> 4.5
        4 -> 3.5
        3 -> 2.5
        else -> 1.5
    }

    fun marksFor(subject: SubjectMarks, periodKey: String): List<Int> {
        val period = GoalPeriods.byKey(periodKey)
        val withPeriod = subject.marks.filter { it.periodId != null }
        val filtered = if (period.quarterIds.isEmpty() || withPeriod.isEmpty()) {
            subject.marks
        } else {
            subject.marks.filter { it.periodId in period.quarterIds }
        }
        return filtered.mapNotNull { it.mark.toIntOrNull() }
    }

    fun average(marks: List<Int>): Double? =
        if (marks.isEmpty()) null else marks.sum().toDouble() / marks.size

    fun roundedMark(avg: Double?): Int? = when {
        avg == null -> null
        avg >= 4.5 -> 5
        avg >= 3.5 -> 4
        avg >= 2.5 -> 3
        else -> 2
    }

    /** Сколько оценок ещё нужно, чтобы средний дошёл до порога. null — недостижимо. */
    fun needed(marks: List<Int>, target: Int, allowedMarks: List<Int>): Int? {
        if (allowedMarks.isEmpty()) return null
        val t = threshold(target)
        val n = marks.size
        val sum = marks.sum().toDouble()
        if (n > 0 && sum / n >= t) return 0
        if (n == 0 && t <= 0) return 0
        val best = allowedMarks.maxOrNull() ?: return null
        if (best < t) return null
        return ceil((t * n - sum) / (best - t)).toInt().coerceAtLeast(1)
    }

    fun hint(marks: List<Int>, goal: Goal): String {
        val need = needed(marks, goal.targetMark, goal.allowedMarks)
        val avg = average(marks)
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
