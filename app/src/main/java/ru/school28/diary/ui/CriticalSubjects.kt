package ru.school28.diary.ui

import ru.school28.diary.api.SubjectMarks
import java.util.Locale

/**
 * Критический предмет: по нему стоит вмешаться, пока не поздно.
 *
 * Признаки:
 *  • средний балл ниже 3,5 (тянет на «3» и хуже);
 *  • три и больше «2».
 */
data class CriticalSubject(
    val subjectId: Int,
    val subjectName: String,
    val average: Double?,
    val twos: Int,
    val totalMarks: Int,
    val reasons: List<String>
) {
    val severity: Double
        get() = (average ?: 5.0) - twos * 0.5
}

object CriticalSubjects {

    /** Ниже этого среднего балла предмет считаем проблемным. */
    const val AVG_LIMIT = 3.5

    /** Сколько «2» делают предмет критическим. */
    const val TWOS_LIMIT = 3

    fun find(subjects: List<SubjectMarks>, periodKey: String = "YEAR"): List<CriticalSubject> =
        subjects.mapNotNull { subject ->
            val marks = GoalMath.marksFor(subject, periodKey)
            if (marks.isEmpty()) return@mapNotNull null

            val avg = GoalMath.average(marks)
            val twos = marks.count { it <= 2 }
            val reasons = mutableListOf<String>()

            if (avg != null && avg < AVG_LIMIT) {
                reasons += "средний балл " + String.format(Locale("ru"), "%.2f", avg)
            }
            if (twos >= TWOS_LIMIT) {
                reasons += "$twos «2»"
            }

            if (reasons.isEmpty()) null
            else CriticalSubject(
                subjectId = subject.subjectId,
                subjectName = subject.subjectName,
                average = avg,
                twos = twos,
                totalMarks = marks.size,
                reasons = reasons
            )
        }.sortedBy { it.severity }
}
