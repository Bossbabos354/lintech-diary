package ru.school28.diary

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ru.school28.diary.api.ApiClient
import ru.school28.diary.ui.HomeworkChecklist
import java.text.SimpleDateFormat
import java.util.*

/**
 * Фоновая проверка обновлений. За один запуск делает три независимые проверки:
 *  1) новые оценки и домашние задания в дневнике;
 *  2) новые события с сайта школы;
 *  3) выход новой версии приложения.
 *
 * Каждая проверка изолирована: сбой одной не мешает остальным.
 */
class NotifierWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!Prefs.isNotifEnabled(ctx)) {
            FileLog.log("NotifierWorker: уведомления выключены")
            return Result.success()
        }

        FileLog.log("NotifierWorker: старт опроса")
        var needRetry = false

        // 1. Оценки и домашние задания
        try {
            checkDiary(ctx)
        } catch (e: Exception) {
            FileLog.logException("NotifierWorker/diary", e)
            needRetry = true
        }

        // 2. События с сайта
        try {
            checkEvents(ctx)
        } catch (e: Exception) {
            FileLog.logException("NotifierWorker/events", e)
        }

        // 2.5. Контрольные на завтра и незакрытые ДЗ
        try {
            checkTomorrowTests(ctx)
            checkHomeworkReminder(ctx)
        } catch (e: Exception) {
            FileLog.logException("NotifierWorker/smart", e)
        }

        // 2.6. Дайджест в выбранный час
        try {
            sendDigestIfNeeded(ctx)
        } catch (e: Exception) {
            FileLog.logException("NotifierWorker/digest", e)
        }

        // 3. Новая версия приложения (уважаем настройку «Проверять обновления»)
        if (Prefs.isCheckUpdatesEnabled(ctx)) {
            try {
                checkVersion(ctx)
            } catch (e: Exception) {
                FileLog.logException("NotifierWorker/version", e)
            }
        }

        return if (needRetry) Result.retry() else Result.success()
    }

    // ---- Оценки и домашние задания ----

    private suspend fun checkDiary(ctx: Context) {
        val login = Prefs.getLogin(ctx)
        val password = Prefs.getPassword(ctx)
        val studentId = Prefs.getStudentId(ctx)
        val classId = Prefs.getClassId(ctx)

        if (login == null || password == null || studentId == -1 || classId == -1) {
            FileLog.log("NotifierWorker: нет данных для входа")
            return
        }

        ApiClient.restoreSession(login, password)

        val cal = Calendar.getInstance()
        val endDate = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val beginDate = cal.time

        val entries = ApiClient.getDairy(classId, studentId, beginDate, endDate)
        if (entries.isEmpty()) {
            FileLog.log("NotifierWorker: нет записей за период")
            return
        }

        val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentKeys = mutableSetOf<String>()
        for (e in entries) {
            val dateStr = dateFmt.format(e.date)
            if (e.mark.isNotBlank()) {
                currentKeys.add("M:${e.subjectId}|$dateStr|${e.lessonNumber}|${e.mark}")
            }
            if (e.homework.isNotBlank()) {
                currentKeys.add("H:${e.subjectId}|$dateStr|${e.lessonNumber}|${e.homework}")
            }
        }

        val oldRaw = Prefs.getNotifKnownKeys(ctx)
        val oldKeys = if (oldRaw != null) oldRaw.split("\n").filter { it.isNotBlank() }.toSet() else null

        // Первый запуск — просто запоминаем состояние, без уведомлений
        if (oldKeys == null) {
            Prefs.setNotifKnownKeys(ctx, currentKeys.sorted().joinToString("\n"))
            FileLog.log("NotifierWorker: первый опрос, сохранён отпечаток (${currentKeys.size})")
            return
        }

        val newKeys = currentKeys - oldKeys
        // Всегда сохраняем актуальное состояние
        Prefs.setNotifKnownKeys(ctx, currentKeys.sorted().joinToString("\n"))

        if (newKeys.isEmpty()) {
            FileLog.log("NotifierWorker: изменений в дневнике нет")
            return
        }

        val marksNew = newKeys.filter { it.startsWith("M:") }
        val hwNew = newKeys.filter { it.startsWith("H:") }
        val nameMap = entries.associate { it.subjectId to it.subjectName }

        val lines = mutableListOf<String>()
        for (k in marksNew.sorted()) {
            val parts = k.split("|")
            if (parts.size >= 4) {
                val subjId = parts[0].removePrefix("M:").toIntOrNull() ?: continue
                val subject = nameMap[subjId] ?: "Предмет $subjId"
                lines.add("$subject: новая оценка ${parts[3]}")
            }
        }
        for (k in hwNew.sorted()) {
            val parts = k.split("|")
            if (parts.size >= 4) {
                val subjId = parts[0].removePrefix("H:").toIntOrNull() ?: continue
                val subject = nameMap[subjId] ?: "Предмет $subjId"
                lines.add("$subject: ${parts[3].take(120)}")
            }
        }

        if (lines.isEmpty()) return

        val title = if (lines.size == 1) "Новое в дневнике" else "Новое в дневнике (${lines.size})"
        deliver(ctx, title, lines.joinToString("\n"))
        FileLog.log("NotifierWorker: уведомление о дневнике (${newKeys.size} изменений)")
    }

    // ---- События с сайта ----

    private suspend fun checkEvents(ctx: Context) {
        val events = ApiClient.getEvents()
        if (events.isEmpty()) {
            FileLog.log("NotifierWorker: событий нет или сайт недоступен")
            return
        }

        val currentIds = events.map { it.id }.sorted().joinToString("\n")
        val oldRaw = Prefs.getNotifKnownEvents(ctx)
        val oldIds = if (oldRaw != null) oldRaw.split("\n").filter { it.isNotBlank() }.toSet() else null

        // Первый запуск — запоминаем, без уведомлений
        if (oldIds == null) {
            Prefs.setNotifKnownEvents(ctx, currentIds)
            FileLog.log("NotifierWorker: события — первый опрос (${events.size})")
            return
        }

        val newEvents = events.filter { it.id !in oldIds }
        Prefs.setNotifKnownEvents(ctx, currentIds)

        if (newEvents.isEmpty()) {
            FileLog.log("NotifierWorker: новых событий нет")
            return
        }

        val title = if (newEvents.size == 1) "Новое событие"
        else "Новые события (${newEvents.size})"
        val text = newEvents
            .sortedBy { "${it.date} ${it.time ?: ""}" }
            .joinToString("\n") { ev ->
                val whenText = listOfNotNull(
                    ev.date.takeIf { it.isNotBlank() },
                    ev.time?.takeIf { it.isNotBlank() }
                ).joinToString(" ")
                val suffix = if (whenText.isNotBlank()) " ($whenText)" else ""
                "• ${ev.title}$suffix"
            }
        NotificationHelper.showEvents(ctx, title, text)
        FileLog.log("NotifierWorker: уведомление о событиях (${newEvents.size})")
    }

    // ---- Новая версия приложения ----

    private suspend fun checkVersion(ctx: Context) {
        val result = UpdateChecker.check()
        val update = result.update ?: return

        val lastNotified = Prefs.getNotifLastVersion(ctx)
        if (update.versionName.equals(lastNotified, ignoreCase = true)) {
            FileLog.log("NotifierWorker: о версии ${update.versionName} уже уведомляли")
            return
        }

        Prefs.setNotifLastVersion(ctx, update.versionName)

        val text = buildString {
            append("Доступна версия ${update.versionName}")
            if (update.notes.isNotBlank()) {
                append("\n")
                append(update.notes.take(200))
            }
            append("\nНажмите, чтобы обновить")
        }
        NotificationHelper.showUpdate(ctx, "Доступно обновление", text)
        FileLog.log("NotifierWorker: уведомление о версии ${update.versionName}")
    }

    // ---- Умные уведомления ----

    /** Тихие часы: например, с 22:00 до 08:00. */
    private fun isQuietNow(ctx: Context): Boolean {
        if (!Prefs.isQuietEnabled(ctx)) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val from = Prefs.getQuietFrom(ctx)
        val to = Prefs.getQuietTo(ctx)
        return if (from <= to) hour in from until to
        else hour >= from || hour < to
    }

    /** В тихие часы и в режиме дайджеста копим строки, иначе показываем сразу. */
    private fun deliver(ctx: Context, title: String, text: String) {
        if (Prefs.isDigestEnabled(ctx) || isQuietNow(ctx)) {
            appendPending(ctx, text)
            FileLog.log("NotifierWorker: отложено до дайджеста")
        } else {
            NotificationHelper.showDiary(ctx, title, text)
        }
    }

    private fun appendPending(ctx: Context, text: String) {
        val existing = Prefs.getPendingNotifs(ctx)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val merged = (existing + text.split("\n").filter { it.isNotBlank() })
            .distinct()
            .take(20)
        Prefs.setPendingNotifs(ctx, merged.joinToString("\n"))
    }

    private val testKeywords = listOf(
        "контрольн", "срез", "зачёт", "зачет", "диктант", "тест", "самостоятельн", "проверочн"
    )

    private fun looksLikeTest(text: String): Boolean {
        val t = text.lowercase()
        return testKeywords.any { t.contains(it) }
    }

    /** «Завтра контрольная» — по темам и ДЗ за завтрашний день. */
    private suspend fun checkTomorrowTests(ctx: Context) {
        val login = Prefs.getLogin(ctx) ?: return
        val password = Prefs.getPassword(ctx) ?: return
        val studentId = Prefs.getStudentId(ctx)
        val classId = Prefs.getClassId(ctx)
        if (studentId == -1 || classId == -1) return

        ApiClient.restoreSession(login, password)

        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val start = cal.time
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
        val end = cal.time

        val entries = runCatching { ApiClient.getDairy(classId, studentId, start, end) }
            .getOrNull().orEmpty()
        val tests = entries.filter {
            looksLikeTest(it.theme) || looksLikeTest(it.homework) || looksLikeTest(it.subjectName)
        }
        if (tests.isEmpty()) return

        val text = tests.joinToString("\n") { e ->
            val what = listOf(e.theme, e.homework).firstOrNull { it.isNotBlank() } ?: ""
            if (what.isBlank()) "• ${e.subjectName}" else "• ${e.subjectName} — ${what.take(80)}"
        }
        deliver(ctx, "Завтра контрольная", text)
        FileLog.log("NotifierWorker: найдено контрольных на завтра: ${tests.size}")
    }

    /** Вечером напоминаем про несделанное ДЗ (в дайджест или отдельно). */
    private suspend fun checkHomeworkReminder(ctx: Context) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour < 15) return

        val login = Prefs.getLogin(ctx) ?: return
        val password = Prefs.getPassword(ctx) ?: return
        val studentId = Prefs.getStudentId(ctx)
        val classId = Prefs.getClassId(ctx)
        if (studentId == -1 || classId == -1) return

        val cal = Calendar.getInstance()
        val start = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.time
        val end = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }.time

        val entries = runCatching { ApiClient.getDairy(classId, studentId, start, end) }
            .getOrNull().orEmpty()
            .filter { it.homework.isNotBlank() }
        if (entries.isEmpty()) return

        val undone = entries.filter { !HomeworkChecklist.isDone(ctx, it) }
        if (undone.isEmpty()) return

        val text = undone.take(6).joinToString("\n") { "• ${it.subjectName}" }
        appendPending(ctx, "Не отмечено ДЗ: ${undone.size}\n$text")
        FileLog.log("NotifierWorker: несделанных ДЗ ${undone.size}")
    }

    /** Один дайджест в день вместо потока. */
    private fun sendDigestIfNeeded(ctx: Context) {
        if (!Prefs.isDigestEnabled(ctx)) return

        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) < Prefs.getDigestHour(ctx)) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        if (Prefs.getDigestDate(ctx) == today) return

        val pending = Prefs.getPendingNotifs(ctx)
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()

        val tests = pending.filter { it.contains("контрольн", ignoreCase = true) }
        val homework = pending.filter { it.startsWith("Не отмечено ДЗ") }
        val other = pending - tests.toSet() - homework.toSet()

        val lines = mutableListOf<String>()
        lines += other.take(8)
        lines += tests.take(4)
        lines += homework.take(2)

        if (lines.isEmpty()) {
            Prefs.setDigestDate(ctx, today)
            return
        }

        NotificationHelper.showDigest(ctx, "Итоги дня", lines.joinToString("\n"))
        Prefs.setPendingNotifs(ctx, null)
        Prefs.setDigestDate(ctx, today)
        FileLog.log("NotifierWorker: дайджест отправлен (${lines.size} строк)")
    }
}
