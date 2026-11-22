package ru.school28.diary

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.school28.diary.api.ApiClient
import ru.school28.diary.ui.HomeworkChecklist
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/** Планировщик фоновой проверки дневника (замена Android WorkManager). */
object Background {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var scheduler: ScheduledExecutorService? = null

    @Synchronized
    fun start() {
        reschedule()
    }

    @Synchronized
    fun reschedule() {
        scheduler?.shutdownNow()
        scheduler = null
        if (!Settings.notifEnabled || !Settings.isLoggedIn) {
            AppLog.log("Background: выключено")
            return
        }
        val minutes = Settings.notifIntervalMin
        val exec = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "diary-notifier").apply { isDaemon = true }
        }
        exec.scheduleWithFixedDelay({ checkNow() }, 2, minutes.toLong(), TimeUnit.MINUTES)
        scheduler = exec
        AppLog.log("Background: интервал $minutes мин")
    }

    @Synchronized
    fun stop() {
        scheduler?.shutdownNow()
        scheduler = null
        AppLog.log("Background: остановлен")
    }

    fun checkNow() {
        scope.launch {
            try {
                Notifier.runOnce()
            } catch (e: Exception) {
                AppLog.logException("notifier", e)
            }
        }
    }
}

/** Те же проверки, что делал Android-воркер: оценки, ДЗ, события, версия. */
object Notifier {
    @Volatile
    private var running = false

    suspend fun runOnce() {
        if (running) return
        if (!Settings.notifEnabled || !Settings.isLoggedIn) return
        running = true
        try {
            val login = Settings.login ?: return
            val password = Settings.password ?: return
            ApiClient.restoreSession(login, password, Settings.userId)

            safe("diary") { checkDiary() }
            safe("events") { checkEvents() }
            safe("tests") { checkTomorrowTests() }
            safe("homework") { checkHomeworkReminder() }
            safe("digest") { sendDigestIfNeeded() }
            if (Settings.checkUpdates) safe("version") { checkVersion() }
        } finally {
            running = false
        }
    }

    private suspend fun safe(tag: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            AppLog.logException("Notifier/$tag", e)
        }
    }

    private suspend fun checkDiary() {
        val cal = Calendar.getInstance()
        val end = cal.time
        cal.add(Calendar.DAY_OF_YEAR, -3)
        val entries = Repo.dairy(cal.time, end)
        if (entries.isEmpty()) return

        val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val current = mutableSetOf<String>()
        for (e in entries) {
            val date = fmt.format(e.date)
            if (e.mark.isNotBlank()) current.add("M:${e.subjectId}|$date|${e.lessonNumber}|${e.mark}")
            if (e.homework.isNotBlank()) current.add("H:${e.subjectId}|$date|${e.lessonNumber}|${e.homework}")
        }

        val oldRaw = Settings.knownKeys
        val old = oldRaw?.split("\n")?.filter { it.isNotBlank() }?.toSet()
        if (old == null) {
            Settings.knownKeys = current.sorted().joinToString("\n")
            AppLog.log("Notifier: первый опрос, сохранён отпечаток (${current.size})")
            return
        }

        val newKeys = current - old
        Settings.knownKeys = current.sorted().joinToString("\n")
        if (newKeys.isEmpty()) return

        val names = entries.associate { it.subjectId to it.subjectName }
        val lines = mutableListOf<String>()
        for (key in newKeys.sorted()) {
            val parts = key.split("|")
            if (parts.size < 4) continue
            val subjectId = parts[0].removePrefix("M:").removePrefix("H:").toIntOrNull() ?: continue
            val subject = names[subjectId] ?: "Предмет $subjectId"
            if (key.startsWith("M:")) lines.add("$subject: новая оценка ${parts[3]}")
            else lines.add("$subject: ${parts[3].take(120)}")
        }
        if (lines.isEmpty()) return

        val title = if (lines.size == 1) "Новое в дневнике" else "Новое в дневнике (${lines.size})"
        deliver(title, lines.joinToString("\n"))
        AppLog.log("Notifier: уведомление о дневнике (${newKeys.size})")
    }

    private suspend fun checkEvents() {
        val events = ApiClient.getEvents()
        if (events.isEmpty()) return

        val current = events.map { it.id }.sorted()
        val oldRaw = Settings.knownEvents
        val old = oldRaw?.split("\n")?.filter { it.isNotBlank() }?.toSet()
        if (old == null) {
            Settings.knownEvents = current.joinToString("\n")
            return
        }
        Settings.knownEvents = current.joinToString("\n")

        val newEvents = events.filter { it.id !in old }
        if (newEvents.isEmpty()) return

        val title = if (newEvents.size == 1) "Новое событие" else "Новые события (${newEvents.size})"
        val text = newEvents
            .sortedBy { "${it.date} ${it.time ?: ""}" }
            .joinToString("\n") { ev ->
                val whenText = listOfNotNull(ev.date.takeIf { it.isNotBlank() }, ev.time?.takeIf { it.isNotBlank() })
                    .joinToString(" ")
                val suffix = if (whenText.isNotBlank()) " ($whenText)" else ""
                "• ${ev.title}$suffix"
            }
        Tray.notify(title, text)
    }

    private fun isQuietNow(): Boolean {
        if (!Settings.quietEnabled) return false
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val from = Settings.quietFrom
        val to = Settings.quietTo
        return if (from <= to) hour in from until to else hour >= from || hour < to
    }

    private fun deliver(title: String, text: String) {
        if (Settings.digestEnabled || isQuietNow()) {
            appendPending(text)
        } else {
            Tray.notify(title, text)
        }
    }

    private fun appendPending(text: String) {
        val existing = Settings.pendingNotifs
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val merged = (existing + text.split("\n").filter { it.isNotBlank() })
            .distinct()
            .take(20)
        Settings.pendingNotifs = merged.joinToString("\n")
    }

    private val testKeywords = listOf(
        "контрольн", "срез", "зачёт", "зачет", "диктант", "тест", "самостоятельн", "проверочн",
    )

    private fun looksLikeTest(text: String): Boolean {
        val t = text.lowercase()
        return testKeywords.any { t.contains(it) }
    }

    private suspend fun checkTomorrowTests() {
        if (!Settings.notifEnabled) return
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.time
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val entries = runCatching { Repo.dairy(start, cal.time) }.getOrNull().orEmpty()
        val tests = entries.filter {
            looksLikeTest(it.theme) || looksLikeTest(it.homework) || looksLikeTest(it.subjectName)
        }
        if (tests.isEmpty()) return

        val text = tests.joinToString("\n") { e ->
            val what = listOf(e.theme, e.homework).firstOrNull { it.isNotBlank() }.orEmpty()
            if (what.isBlank()) "• ${e.subjectName}" else "• ${e.subjectName} — ${what.take(80)}"
        }
        deliver("Завтра контрольная", text)
    }

    private suspend fun checkHomeworkReminder() {
        if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 15) return
        val cal = Calendar.getInstance()
        val start = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val end = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }.time

        val entries = runCatching { Repo.dairy(start, end) }.getOrNull().orEmpty()
            .filter { it.homework.isNotBlank() }
        if (entries.isEmpty()) return

        val undone = entries.filter { !HomeworkChecklist.isDone(it) }
        if (undone.isEmpty()) return

        val text = undone.take(6).joinToString("\n") { "• ${it.subjectName}" }
        appendPending("Не отмечено ДЗ: ${undone.size}\n$text")
    }

    private fun sendDigestIfNeeded() {
        if (!Settings.digestEnabled) return
        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) < Settings.digestHour) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time)
        if (Settings.digestDate == today) return

        val pending = Settings.pendingNotifs
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
            Settings.digestDate = today
            return
        }
        Tray.notify("Итоги дня", lines.joinToString("\n"))
        Settings.pendingNotifs = null
        Settings.digestDate = today
        AppLog.log("Notifier: дайджест отправлен (${lines.size} строк)")
    }

    private suspend fun checkVersion() {
        val result = UpdateChecker.check()
        val update = result.update ?: return
        if (update.versionName.equals(Settings.lastNotifiedVersion, ignoreCase = true)) return

        Settings.lastNotifiedVersion = update.versionName
        val text = buildString {
            append("Доступна версия ${update.versionName}")
            if (update.notes.isNotBlank()) {
                append("\n")
                append(update.notes.take(200))
            }
            append("\nОбновление — в настройках приложения")
        }
        Tray.notify("Доступно обновление", text)
    }
}
