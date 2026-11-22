package ru.school28.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.school28.diary.api.LessonMarks
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.Goal
import ru.school28.diary.ui.GoalMath

class GoalMathTest {

    private fun subject(vararg marks: Pair<Int?, String>): SubjectMarks = SubjectMarks(
        subjectId = 1,
        subjectName = "Математика",
        teacherName = null,
        marks = marks.mapIndexed { index, (periodId, mark) ->
            LessonMarks(
                lessonId = index,
                periodId = periodId,
                date = null,
                subjectName = "Математика",
                theme = null,
                homework = null,
                mark = mark,
                weight = null,
            )
        },
        totalMarks = emptyMap(),
    )

    @Test
    fun goalAlreadyAchieved() {
        assertEquals(0, GoalMath.needed(listOf(5, 5), target = 5, allowedMarks = listOf(5)))
        assertEquals(0, GoalMath.needed(listOf(5), target = 4, allowedMarks = listOf(5)))
    }

    @Test
    fun needsTwoFivesToReachFive() {
        assertEquals(2, GoalMath.needed(listOf(4, 4), target = 5, allowedMarks = listOf(5)))
    }

    @Test
    fun oneFiveIsEnough() {
        assertEquals(1, GoalMath.needed(listOf(5, 4, 4), target = 5, allowedMarks = listOf(5)))
    }

    @Test
    fun impossibleWhenBestMarkBelowThreshold() {
        assertNull(GoalMath.needed(listOf(3, 3), target = 5, allowedMarks = listOf(4)))
    }

    @Test
    fun impossibleWithoutAllowedMarks() {
        assertNull(GoalMath.needed(listOf(4, 4), target = 5, allowedMarks = emptyList()))
    }

    @Test
    fun noMarksNeedsOneGoodMark() {
        assertEquals(1, GoalMath.needed(emptyList(), target = 5, allowedMarks = listOf(5)))
    }

    @Test
    fun thresholds() {
        assertEquals(4.5, GoalMath.threshold(5), 0.0001)
        assertEquals(3.5, GoalMath.threshold(4), 0.0001)
        assertEquals(2.5, GoalMath.threshold(3), 0.0001)
    }

    @Test
    fun roundedMark() {
        assertEquals(5, GoalMath.roundedMark(4.5))
        assertEquals(4, GoalMath.roundedMark(4.49))
        assertEquals(4, GoalMath.roundedMark(3.5))
        assertEquals(3, GoalMath.roundedMark(3.49))
        assertEquals(2, GoalMath.roundedMark(2.49))
        assertNull(GoalMath.roundedMark(null))
    }

    @Test
    fun quarterFiltering() {
        val s = subject(360000 to "5", 360001 to "3", null to "4")
        assertEquals(listOf(5), GoalMath.marksFor(s, "Q1"))
        assertEquals(listOf(3), GoalMath.marksFor(s, "Q2"))
        assertEquals(listOf(5, 3, 4), GoalMath.marksFor(s, "YEAR"))
    }

    @Test
    fun marksWithoutPeriodsFallBackToAll() {
        val s = subject(null to "5", null to "4")
        assertEquals(listOf(5, 4), GoalMath.marksFor(s, "Q1"))
    }

    @Test
    fun hintForAchievedGoal() {
        val goal = Goal(1, "Математика", "YEAR", targetMark = 5, allowedMarks = listOf(5))
        assertTrue(GoalMath.hint(listOf(5, 5), goal).startsWith("Цель достигнута"))
    }

    @Test
    fun hintForUnreachableGoal() {
        val goal = Goal(1, "Математика", "YEAR", targetMark = 5, allowedMarks = listOf(4))
        assertTrue(GoalMath.hint(listOf(3, 3), goal).contains("не достичь"))
    }
}
