package ru.school28.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.school28.diary.api.LessonMarks
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.Goal
import ru.school28.diary.ui.GoalMath
import java.util.Date

class GoalMathTest {

    private fun lesson(
        mark: String,
        weight: Double? = null,
        periodId: Int? = null,
        date: Long = 0L,
    ): LessonMarks = LessonMarks(
        lessonId = 0,
        periodId = periodId,
        date = Date(date),
        subjectName = "Математика",
        theme = null,
        homework = null,
        mark = mark,
        weight = weight,
    )

    private fun lessons(vararg marks: Int): List<LessonMarks> =
        marks.map { lesson(it.toString()) }

    @Test
    fun weightedAverageUsesJournalWeights() {
        val marks = listOf(lesson("5", weight = 2.0), lesson("3", weight = 1.0))
        assertEquals(13.0 / 3.0, GoalMath.weightedAverage(marks)!!, 0.0001)
    }

    @Test
    fun missingWeightCountsAsOne() {
        val marks = listOf(lesson("5", weight = 0.0), lesson("3", weight = null))
        assertEquals(4.0, GoalMath.weightedAverage(marks)!!, 0.0001)
    }

    @Test
    fun goalAlreadyAchieved() {
        assertEquals(0, GoalMath.needed(lessons(5, 5), target = 5, allowedMarks = listOf(5)))
        assertEquals(0, GoalMath.needed(lessons(5), target = 4, allowedMarks = listOf(5)))
    }

    @Test
    fun needsTwoFivesToReachFive() {
        assertEquals(2, GoalMath.needed(lessons(4, 4), target = 5, allowedMarks = listOf(5)))
    }

    @Test
    fun weightedGoalAlreadyReached() {
        val marks = listOf(lesson("5", weight = 2.0), lesson("4", weight = 1.0))
        assertEquals(0, GoalMath.needed(marks, target = 5, allowedMarks = listOf(5)))
    }

    @Test
    fun impossibleWhenBestMarkBelowThreshold() {
        assertNull(GoalMath.needed(lessons(3, 3), target = 5, allowedMarks = listOf(4)))
    }

    @Test
    fun impossibleWithoutAllowedMarks() {
        assertNull(GoalMath.needed(lessons(4, 4), target = 5, allowedMarks = emptyList()))
    }

    @Test
    fun currentPeriodUsesLatestPeriod() {
        val subject = SubjectMarks(
            subjectId = 1,
            subjectName = "Математика",
            teacherName = null,
            marks = listOf(
                lesson("5", periodId = 360000, date = 100),
                lesson("4", periodId = 360001, date = 200),
                lesson("2", periodId = 360001, date = 300),
            ),
            totalMarks = emptyMap(),
        )
        assertEquals(listOf("4", "2"), GoalMath.currentPeriodLessons(subject).map { it.mark })
        assertEquals(3.0, GoalMath.currentPeriodAverage(subject)!!, 0.0001)
    }

    @Test
    fun marksForQuarterFiltersLessons() {
        val subject = SubjectMarks(
            subjectId = 1,
            subjectName = "Математика",
            teacherName = null,
            marks = listOf(
                lesson("5", periodId = 360000),
                lesson("3", periodId = 360001),
                lesson("4", periodId = null),
            ),
            totalMarks = emptyMap(),
        )
        assertEquals(listOf(5), GoalMath.marksFor(subject, "Q1"))
        assertEquals(listOf(3), GoalMath.marksFor(subject, "Q2"))
        assertEquals(listOf(5, 3, 4), GoalMath.marksFor(subject, "YEAR"))
    }

    @Test
    fun hintForAchievedGoal() {
        val goal = Goal(1, "Математика", "YEAR", targetMark = 5, allowedMarks = listOf(5))
        assertTrue(GoalMath.hint(lessons(5, 5), goal).startsWith("Цель достигнута"))
    }

    @Test
    fun hintForUnreachableGoal() {
        val goal = Goal(1, "Математика", "YEAR", targetMark = 5, allowedMarks = listOf(4))
        assertTrue(GoalMath.hint(lessons(3, 3), goal).contains("не достичь"))
    }
}
