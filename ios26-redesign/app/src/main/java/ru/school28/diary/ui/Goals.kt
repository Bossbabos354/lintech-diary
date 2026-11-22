package ru.school28.diary.ui

import android.content.Context
import androidx.compose.runtime.Immutable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.school28.diary.Prefs
import ru.school28.diary.api.LessonMarks
import ru.school28.diary.api.SubjectMarks
import kotlin.math.ceil

/**
 * Цель по предмету: какой балл хотим получить за выбранный период
 * и какими оценками готовы его набирать.
 */
@Immutable
data class Goal(
    val subjectId: Int,
    val subjectName: String,
    val periodKey: String,
    val targetMark: Int,
    val allowedMarks: List<Int>
) {
    /** Уникальный ключ цели: один предмет + один период = одна цель. */
    val id: String get() = "$subjectId|$periodKey"
}

/** Период, по которому считается цель. */
@Immutable
data class GoalPeriod(
    val key: String,
    val label: String,
    val quarterIds: List<Int>
)

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
        GoalPeriod("YEAR", "Учебный год", emptyList())
    )

    fun byKey(key: String): GoalPeriod =
        all.firstOrNull { it.key == key } ?: all.last()
}

object GoalStore {
    private val gson = Gson()

    // Резервная копия целей в файле: SharedPreferences могут не успеть записаться
    // при обновлении приложения, а файл пишем сразу и читаем, если prefs пусты.
    private const val BACKUP_FILE = "goals_backup.json"

    // Кэш в памяти: load() вызывается на каждый предмет в списках,
    // читать SharedPreferences и парсить JSON каждый раз слишком дорого.
    @Volatile
    private var cached: List<Goal>? = null

    fun load(context: Context): List<Goal> {
        cached?.let { return it }
        val fromPrefs = readPrefs(context)
        if (fromPrefs.isNotEmpty()) {
            cached = fromPrefs
            return fromPrefs
        }

        // prefs пусты/повреждены — пробуем восстановить из резервной копии
        val fromFile = readFile(context)
        if (fromFile.isNotEmpty()) {
            Prefs.setGoalsJson(context, gson.toJson(fromFile))
            cached = fromFile
            return fromFile
        }
        cached = emptyList()
        return emptyList()
    }

    private fun readPrefs(context: Context): List<Goal> {
        val raw = Prefs.getGoalsJson(context) ?: return emptyList()
        return parse(raw)
    }

    private fun readFile(context: Context): List<Goal> {
        return try {
            val file = java.io.File(context.filesDir, BACKUP_FILE)
            if (!file.exists()) emptyList() else parse(file.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parse(raw: String): List<Goal> {
        return try {
            val type = object : TypeToken<List<Goal>>() {}.type
            gson.fromJson<List<Goal>>(raw, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, goals: List<Goal>) {
        cached = goals
        val json = gson.toJson(goals)
        // Синхронно в prefs + сразу в файл (двойная защита от потери при обновлении)
        Prefs.setGoalsJson(context, json)
        try {
            java.io.File(context.filesDir, BACKUP_FILE).writeText(json)
        } catch (e: Exception) {
            // резервная копия не критична — основное уже сохранено
        }
    }

    /** Добавляет цель или заменяет существующую по тому же предмету и периоду. */
    fun upsert(context: Context, goal: Goal) {
        val goals = load(context).filterNot { it.id == goal.id }.toMutableList()
        goals.add(goal)
        save(context, goals)
    }

    fun remove(context: Context, subjectId: Int, periodKey: String) {
        val goals = load(context).filterNot {
            it.subjectId == subjectId && it.periodKey == periodKey
        }
        save(context, goals)
    }

    fun find(context: Context, subjectId: Int, periodKey: String): Goal? =
        load(context).firstOrNull { it.subjectId == subjectId && it.periodKey == periodKey }
}

object GoalMath {

    /** Порог среднего балла для оценки 5/4/3 (стандартное школьное округление). */
    fun threshold(target: Int): Double = when (target) {
        5 -> 4.5
        4 -> 3.5
        3 -> 2.5
        else -> 1.5
    }

    /** Оценки предмета, относящиеся к выбранному периоду. */
    /**
     * Оценки предмета за период вместе с весами.
     *
     * Вес задаёт учитель в журнале: контрольная обычно весит 2, обычный
     * ответ — 1. Журнал считает средний балл по весам, поэтому простое
     * среднее арифметическое даёт неверный результат.
     */
    fun marksForLessons(subject: SubjectMarks, periodKey: String): List<LessonMarks> {
        val period = GoalPeriods.byKey(periodKey)
        val withPeriod = subject.marks.filter { it.periodId != null }
        val filtered = if (period.quarterIds.isEmpty()) {
            subject.marks
        } else if (withPeriod.isEmpty()) {
            // В выгрузке нет привязки к периодам — считаем по всем оценкам
            subject.marks
        } else {
            val inPeriod = subject.marks.filter { it.periodId in period.quarterIds }
            // В школе без четвертей (полугодия/триместры) id не совпадут —
            // тогда не оставляем цель без оценок.
            if (inPeriod.isNotEmpty()) inPeriod else subject.marks
        }
        return filtered.filter { it.mark.toIntOrNull() != null }
    }

    fun marksFor(subject: SubjectMarks, periodKey: String): List<Int> =
        marksForLessons(subject, periodKey).mapNotNull { it.mark.toIntOrNull() }

    /** Вес оценки в журнале; если учитель его не задал — 1. */
    fun weightOf(lesson: LessonMarks): Double =
        lesson.weight?.takeIf { it > 0.0 } ?: 1.0

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

    /** Период, к которому относится последняя оценка — то есть текущая четверть/полугодие. */
    fun currentPeriodId(subject: SubjectMarks): Int? =
        subject.marks.filter { it.periodId != null }
            .maxByOrNull { it.date?.time ?: Long.MIN_VALUE }
            ?.periodId

    /** Оценки текущего периода; если периодов в выгрузке нет — все оценки. */
    fun currentPeriodLessons(subject: SubjectMarks): List<LessonMarks> {
        val all = subject.marks.filter { it.mark.toIntOrNull() != null }
        val id = currentPeriodId(subject) ?: return all
        return all.filter { it.periodId == id }.ifEmpty { all }
    }

    /** Средний балл за текущий период — то, что показывает журнал АВЕРС. */
    fun currentPeriodAverage(subject: SubjectMarks): Double? =
        weightedAverage(currentPeriodLessons(subject))

    /** Простое среднее — для рядов без весов (например, оценок за неделю). */
    fun average(marks: List<Int>): Double? =
        if (marks.isEmpty()) null else marks.sum().toDouble() / marks.size

    /** Оценка, которую «вытягивает» текущий средний балл. */
    fun roundedMark(avg: Double?): Int? = when {
        avg == null -> null
        avg >= 4.5 -> 5
        avg >= 3.5 -> 4
        avg >= 2.5 -> 3
        else -> 2
    }

    /**
     * Сколько ещё оценок нужно, чтобы средний балл дошёл до порога цели.
     * Для минимизации количества берём лучшую из разрешённых оценок.
     *
     * @return 0 — цель уже достигнута; null — с этим набором оценок недостижимо.
     */
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

        // Новая оценка добавляется с весом 1: (sum + k·best) / (weight + k) ≥ t
        val k = ceil((t * totalWeight - sum) / (best - t)).toInt()
        return k.coerceAtLeast(1)
    }

    /** Текстовая подсказка по цели. */
    fun hint(lessons: List<LessonMarks>, goal: Goal): String {
        val need = needed(lessons, goal.targetMark, goal.allowedMarks)
        val avg = weightedAverage(lessons)
        val avgText = if (avg == null) "нет оценок" else String.format(java.util.Locale("ru"), "%.2f", avg)
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