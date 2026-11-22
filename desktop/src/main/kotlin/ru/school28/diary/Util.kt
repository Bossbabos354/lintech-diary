package ru.school28.diary

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Util {
    val RU: Locale = Locale("ru")

    /** Диапазон недели (пн 00:00 — вс 23:59) со сдвигом на weeks*7 дней. */
    fun weekRange(weeks: Int): Pair<Date, Date> {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
        cal.add(Calendar.DAY_OF_YEAR, weeks * 7)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monday = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        return monday to cal.time
    }

    fun weekLabel(monday: Date, sunday: Date): String {
        val fmt = SimpleDateFormat("d MMM", RU)
        return fmt.format(monday) + " — " + fmt.format(sunday)
    }

    fun dayKey(date: Date): Int {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    fun dayLabel(date: Date): String = SimpleDateFormat("EEEE, d MMMM", RU).format(date)

    fun shortTime(ts: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

    fun hourText(hour: Int): String = "%02d:00".format(hour)

    /** Русское склонение слова «оценка»: 1 оценку, 2 оценки, 5 оценок. */
    fun marksWord(n: Int): String {
        val mod10 = n % 10
        val mod100 = n % 100
        return when {
            mod10 == 1 && mod100 != 11 -> "оценку"
            mod10 in 2..4 && mod100 !in 12..14 -> "оценки"
            else -> "оценок"
        }
    }

    /** Сегодняшний ключ дня для расписания. */
    fun todayKey(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "mon"
        Calendar.TUESDAY -> "tue"
        Calendar.WEDNESDAY -> "wed"
        Calendar.THURSDAY -> "thu"
        Calendar.FRIDAY -> "fri"
        Calendar.SATURDAY -> "sat"
        else -> "mon"
    }
}
