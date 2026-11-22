package ru.school28.diary.widgets

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.school28.diary.FileLog
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalPeriods
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.HomeworkChecklist
import java.util.Calendar
import java.util.Locale

/**
 * Фоновое обновление данных для виджетов: ближайший урок, средний балл,
 * дневник на сегодня, цель и расписание на завтра.
 */
class WidgetDataWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private data class Nearest(
        val time: String?,
        val subject: String?,
        val details: String?,
        val label: String,
        val startAt: Long?
    )

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val login = Prefs.getLogin(ctx)
        val password = Prefs.getPassword(ctx)
        val studentId = Prefs.getStudentId(ctx)
        val classId = Prefs.getClassId(ctx)

        if (login == null || password == null || studentId == -1) {
            FileLog.log("WidgetDataWorker: нет данных входа")
            return Result.success()
        }

        try {
            ApiClient.restoreSession(login, password)
            // Прошлый снимок: при сбое сети не затираем им же добытые данные.
            val previous = WidgetDataStore.load(ctx)

            val schedule = runCatching { ApiClient.getSchedule() }.getOrNull()
            val className = Prefs.getSelectedClass(ctx)
                ?: schedule?.classes?.firstOrNull()

            val now = Calendar.getInstance()
            val todayKey = dayKey(now.get(Calendar.DAY_OF_WEEK))

            val nearest = nearestLesson(schedule, className, now)
            val scheduleDay = nextSchoolDay(schedule, className, now)
            val scheduleLines = if (schedule == null) previous.scheduleLines
            else scheduleDay?.let { scheduleLines(schedule, className, it.key) }.orEmpty()
            val scheduleHeader = if (schedule == null) previous.scheduleHeader ?: "Расписание"
            else scheduleDay?.let { day ->
                when {
                    day.offset == 1 -> "Завтра · " + dayNameRu(day.key).uppercase()
                    else -> dayNameRu(day.key).uppercase() + ", " + prettyDate(day.date).uppercase()
                }
            } ?: "Расписание"

            // Оценки
            val subjects = if (classId != -1) {
                runCatching {
                    ApiClient.getMarks(classId, studentId, Prefs.getUchYear(ctx))
                }.getOrNull()
            } else null

            val periodMarks = subjects?.flatMap { GoalMath.currentPeriodLessons(it) }.orEmpty()
            val avg = GoalMath.weightedAverage(periodMarks)?.let {
                String.format(Locale("ru"), "%.2f", it)
            }

            val avgDelta = deltaText(previous.avgMark, avg)

            // Дневник на сегодня
            val todayBells = schedule?.bellsFor(todayKey).orEmpty()
            val todayLessons = schedule?.lessonsFor(className ?: "", todayKey).orEmpty()
            val dairyEntries = if (classId != -1) {
                // Тот же диапазон, что в дневнике (пн–вс): так совпадает ключ
                // офлайн-кэша и виджет не остаётся без данных при сбое сети.
                val weekStart = (now.clone() as Calendar).apply {
                    val dow = get(Calendar.DAY_OF_WEEK)
                    add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val weekEnd = (weekStart.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 6)
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                }
                runCatching { ApiClient.getDairy(classId, studentId, weekStart.time, weekEnd.time) }
                    .getOrNull()
                    // В виджет берём только сегодняшние уроки
                    ?.filter { sameDay(it.date, now) }
            } else null

            val diaryRows = dairyEntries?.map { e ->
                    // Номер урока в журнале может не совпадать со звонком —
                    // страхуемся совпадением по предмету и позицией в списке.
                    val byNumber = todayBells.firstOrNull { it.n == e.lessonNumber }?.start
                    val bySubject = todayLessons
                        .firstOrNull { sameSubject(it.subject, e.subjectName) }
                        ?.let { lesson -> todayBells.firstOrNull { it.n == lesson.n }?.start }
                    val byIndex = todayBells.getOrNull(e.lessonNumber - 1)?.start
                    val time = byNumber ?: bySubject ?: byIndex
                    DiaryRow(
                        time = time,
                        subject = e.subjectName.trim(),
                        homework = shorten(e.homework.ifBlank { e.theme }, 44),
                        mark = e.mark.removePrefix("!").trim().ifBlank { null }
                    )
                // Порядок — по времени звонка, а не по номеру в журнале
            }?.sortedBy { it.time ?: "99:99" } ?: previous.diaryRows

            val homeworkRows = dairyEntries
                ?.filter { it.homework.isNotBlank() }
                ?.map { e ->
                    HomeworkRow(
                        key = HomeworkChecklist.keyOf(e),
                        subject = e.subjectName.trim(),
                        task = shorten(e.homework, 60)
                    )
                }
                ?.distinctBy { it.key }
                ?.take(8)
                ?: previous.homeworkRows

            val diaryTotal = diaryRows.size
            val diaryHeader = "Сегодня, " + prettyDate(now)
            val diaryMore = when {
                diaryTotal == 0 -> "Нет уроков"
                diaryTotal > 3 -> "ещё ${diaryTotal - 3} ${pluralLessons(diaryTotal - 3)} →"
                else -> "Открыть дневник →"
            }
            val diaryRowsShort = diaryRows.take(3)

            // Цель: показываем выбранную в галерее виджетов, иначе первую
            val goals = GoalStore.load(ctx)
            val savedGoalId = Prefs.getWidgetGoalId(ctx)
            val firstGoal = goals.firstOrNull { it.id == savedGoalId } ?: goals.firstOrNull()
            var goalTitle: String? = null
            var goalLine: String? = null
            var goalCurrent: String? = null
            var goalTarget: String? = null
            var goalProgress = 0
            if (firstGoal != null) {
                goalTitle = "Цель: ${firstGoal.subjectName}"
                goalTarget = firstGoal.targetMark.toString()
                val subject = subjects?.firstOrNull { it.subjectId == firstGoal.subjectId }
                if (subject != null) {
                    val marks = GoalMath.marksForLessons(subject, firstGoal.periodKey)
                    goalLine = GoalMath.hint(marks, firstGoal)
                    val avgValue = GoalMath.weightedAverage(marks)
                    val threshold = GoalMath.threshold(firstGoal.targetMark)
                    goalProgress = if (avgValue == null) 0
                    else ((avgValue / threshold) * 100).toInt().coerceIn(0, 100)
                    goalCurrent = avgValue?.let {
                        "сейчас " + String.format(Locale("ru"), "%.2f", it)
                    }
                } else {
                    goalLine = GoalPeriods.byKey(firstGoal.periodKey).label
                }
            }

            val data = WidgetData(
                nearestLabel = nearest?.label,
                nearestTime = nearest?.time,
                nearestSubject = nearest?.subject,
                nearestDetails = nearest?.details,
                nearestStartAt = nearest?.startAt,
                avgMark = avg,
                avgDelta = avgDelta,
                diaryHeader = diaryHeader,
                diaryRows = diaryRowsShort,
                homeworkRows = homeworkRows,
                homeworkLoaded = dairyEntries != null || previous.homeworkLoaded,
                diaryMore = diaryMore,
                goalTitle = goalTitle,
                goalLine = goalLine,
                goalCurrent = goalCurrent,
                goalTarget = goalTarget,
                goalProgress = goalProgress,
                scheduleHeader = scheduleHeader,
                scheduleLines = scheduleLines,
                updatedAt = System.currentTimeMillis()
            )
            WidgetDataStore.save(ctx, data)
            WidgetUpdater.updateAll(ctx)
            FileLog.log("WidgetDataWorker: обновлено")
            return Result.success()
        } catch (e: Exception) {
            FileLog.logException("WidgetDataWorker", e)
            // Сеть недоступна — показываем хотя бы прошлый снимок,
            // иначе виджет «замирает» до повторного добавления.
            runCatching { WidgetUpdater.updateAll(ctx) }
            return Result.retry()
        }
    }

    private fun sameDay(date: java.util.Date, now: Calendar): Boolean {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    private fun deltaText(previous: String?, current: String?): String? {
        val p = previous?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val c = current?.replace(',', '.')?.toDoubleOrNull() ?: return null
        val diff = c - p
        if (diff < 0.005) return null
        val up = diff > 0
        return (if (up) "▲ " else "▼ ") + String.format(Locale("ru"), "%.1f", kotlin.math.abs(diff))
    }

    private fun prettyDate(now: Calendar): String {
        val day = now.get(Calendar.DAY_OF_MONTH)
        val month = when (now.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "января"
            Calendar.FEBRUARY -> "февраля"
            Calendar.MARCH -> "марта"
            Calendar.APRIL -> "апреля"
            Calendar.MAY -> "мая"
            Calendar.JUNE -> "июня"
            Calendar.JULY -> "июля"
            Calendar.AUGUST -> "августа"
            Calendar.SEPTEMBER -> "сентября"
            Calendar.OCTOBER -> "октября"
            Calendar.NOVEMBER -> "ноября"
            else -> "декабря"
        }
        return "$day $month"
    }

    private fun pluralLessons(n: Int): String {
        val mod100 = n % 100
        val mod10 = n % 10
        return when {
            mod100 in 11..14 -> "уроков"
            mod10 == 1 -> "урок"
            mod10 in 2..4 -> "урока"
            else -> "уроков"
        }
    }

    private fun dayKey(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "mon"
        Calendar.TUESDAY -> "tue"
        Calendar.WEDNESDAY -> "wed"
        Calendar.THURSDAY -> "thu"
        Calendar.FRIDAY -> "fri"
        Calendar.SATURDAY -> "sat"
        else -> "sun"
    }

    /** Сравнение названий предметов без регистра, пробелов и пунктуации. */
    private fun sameSubject(a: String, b: String): Boolean {
        fun norm(s: String) = s.lowercase().replace(Regex("[^а-яёa-z0-9]"), "")
        val x = norm(a)
        val y = norm(b)
        return x.isNotEmpty() && y.isNotEmpty() && (x.contains(y) || y.contains(x))
    }

    /** Обрезает длинный текст по границе слова, чтобы не рвать «(с…». */    private fun shorten(text: String, limit: Int): String {
        val t = text.trim().replace(Regex("\\s+"), " ")
        if (t.length <= limit) return t
        val cut = t.substring(0, limit)
        val space = cut.lastIndexOf(' ')
        val base = if (space > limit / 2) cut.substring(0, space) else cut
        return base.trimEnd(' ', ',', '.', ';', ':', '-') + "…"
    }

    private fun dayNameRu(key: String): String = when (key) {
        "mon" -> "понедельник"
        "tue" -> "вторник"
        "wed" -> "среда"
        "thu" -> "четверг"
        "fri" -> "пятница"
        "sat" -> "суббота"
        else -> "воскресенье"
    }

    private fun parseMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    /** Ближайший (ещё не закончившийся) урок сегодня или первый урок завтра. */
    private fun nearestLesson(
        schedule: SchoolSchedule?,
        className: String?,
        now: Calendar
    ): Nearest? {
        if (schedule == null || className == null) return null

        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayKey = dayKey(now.get(Calendar.DAY_OF_WEEK))
        val todayLessons = schedule.lessonsFor(className, todayKey)
        val todayBells = schedule.bellsFor(todayKey)

        val upcoming = todayLessons.firstOrNull { l ->
            val bell = todayBells.firstOrNull { it.n == l.n }
            val end = bell?.end?.let { parseMinutes(it) }
            end != null && end > nowMinutes
        }
        if (upcoming != null) {
            val bell = todayBells.firstOrNull { it.n == upcoming.n }
            val details = detailsFor(upcoming.room, upcoming.teacher)
            return Nearest(
                bell?.start,
                upcoming.subject,
                details,
                "СЛЕДУЮЩИЙ УРОК",
                bell?.start?.let { startAtFor(now, it) }
            )
        }

        // Ближайший учебный день впереди (завтра или после выходных)
        val nextDay = nextSchoolDay(schedule, className, now) ?: return null
        val lessons = schedule.lessonsFor(className, nextDay.key)
        val bells = schedule.bellsFor(nextDay.key)
        val first = lessons.firstOrNull() ?: return null
        val bell = bells.firstOrNull { it.n == first.n }
        val details = detailsFor(first.room, first.teacher)
        val label = if (nextDay.offset == 1) "ЗАВТРА ПЕРВЫЙ УРОК" else "ПЕРВЫЙ УРОК"
        return Nearest(
            bell?.start,
            first.subject,
            details,
            label,
            bell?.start?.let { startAtFor(nextDay.date, it) }
        )
    }

    private data class SchoolDay(val key: String, val date: Calendar, val offset: Int)

    /** Ближайший день (начиная с завтрашнего), в котором есть уроки. */
    private fun nextSchoolDay(
        schedule: SchoolSchedule?,
        className: String?,
        now: Calendar
    ): SchoolDay? {
        if (schedule == null || className == null) return null
        for (offset in 1..7) {
            val day = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            val key = dayKey(day.get(Calendar.DAY_OF_WEEK))
            if (key == "mon" && day.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue
            if (schedule.lessonsFor(className, key).isNotEmpty()) {
                return SchoolDay(key, day, offset)
            }
        }
        return null
    }

    /** Epoch-время начала урока в указанный день по строке «08:30». */
    private fun startAtFor(day: Calendar, time: String): Long? {
        val minutes = parseMinutes(time) ?: return null
        val cal = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** «каб. 24 · Петрова А. И.» — как в макете. */
    private fun detailsFor(room: String?, teacher: String?): String {
        return listOfNotNull(
            room?.takeIf { it.isNotBlank() }?.let { "каб. $it" },
            teacher?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
    }

    private fun scheduleLines(
        schedule: SchoolSchedule?,
        className: String?,
        dayKey: String
    ): List<String> {
        if (schedule == null || className == null) return emptyList()
        val bells = schedule.bellsFor(dayKey)
        return schedule.lessonsFor(className, dayKey).take(12).map { l ->
            val bell = bells.firstOrNull { it.n == l.n }
            if (bell != null) "${l.n}. ${l.subject} · ${bell.start}" else "${l.n}. ${l.subject}"
        }
    }
}
