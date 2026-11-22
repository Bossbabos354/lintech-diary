package ru.school28.diary

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class UtilTest {

    @Test
    fun weekStartsMondayAndEndsSunday() {
        val (monday, sunday) = Util.weekRange(0)
        val cal = Calendar.getInstance()

        cal.time = monday
        assertEquals(Calendar.MONDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))

        cal.time = sunday
        assertEquals(Calendar.SUNDAY, cal.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))

        assertTrue(monday.before(sunday))
    }

    @Test
    fun weekOffsetMovesBySevenDays() {
        val (thisMonday, _) = Util.weekRange(0)
        val (nextMonday, _) = Util.weekRange(1)
        val diffDays = (nextMonday.time - thisMonday.time) / (24 * 3600 * 1000)
        assertEquals(7L, diffDays)
    }

    @Test
    fun weekLabelHasDash() {
        val (monday, sunday) = Util.weekRange(0)
        assertTrue(Util.weekLabel(monday, sunday).contains("—"))
    }

    @Test
    fun marksWordDeclension() {
        assertEquals("оценку", Util.marksWord(1))
        assertEquals("оценки", Util.marksWord(2))
        assertEquals("оценки", Util.marksWord(4))
        assertEquals("оценок", Util.marksWord(5))
        assertEquals("оценок", Util.marksWord(11))
        assertEquals("оценку", Util.marksWord(21))
        assertEquals("оценок", Util.marksWord(25))
    }

    @Test
    fun hourTextIsPadded() {
        assertEquals("08:00", Util.hourText(8))
        assertEquals("22:00", Util.hourText(22))
    }

    @Test
    fun dayKeyIsStableWithinDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 22, 9, 0)
        val morning = Util.dayKey(cal.time)
        cal.set(Calendar.HOUR_OF_DAY, 21)
        assertEquals(morning, Util.dayKey(cal.time))
    }
}
