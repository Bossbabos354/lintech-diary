package ru.school28.diary

import java.io.File
import java.util.Properties

/**
 * Настройки и сохранённые данные приложения — Properties-файл в каталоге конфигурации.
 * Заменяет Android SharedPreferences; запись синхронная, поэтому настройки
 * и цели переживают аварийное завершение процесса.
 */
object Settings {
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    private const val KEY_LOGIN = "login"
    private const val KEY_PASSWORD = "password"
    private const val KEY_STUDENT_ID = "student_id"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_UCH_ID = "uch_id"
    private const val KEY_UCH_NAME = "uch_name"
    private const val KEY_UCH_YEAR = "uch_year"
    private const val KEY_CLASS_ID = "class_id"
    private const val KEY_THEME = "theme"
    private const val KEY_NOTIF_ENABLED = "notif_enabled"
    private const val KEY_NOTIF_INTERVAL = "notif_interval_min"
    private const val KEY_NOTIF_QUIET = "notif_quiet"
    private const val KEY_NOTIF_QUIET_FROM = "notif_quiet_from"
    private const val KEY_NOTIF_QUIET_TO = "notif_quiet_to"
    private const val KEY_NOTIF_DIGEST = "notif_digest"
    private const val KEY_NOTIF_DIGEST_HOUR = "notif_digest_hour"
    private const val KEY_NOTIF_DIGEST_DATE = "notif_digest_date"
    private const val KEY_NOTIF_PENDING = "notif_pending"
    private const val KEY_NOTIF_KNOWN_KEYS = "notif_known_keys"
    private const val KEY_NOTIF_KNOWN_EVENTS = "notif_known_events"
    private const val KEY_NOTIF_LAST_VERSION = "notif_last_version"
    private const val KEY_GOALS_JSON = "goals_json"
    private const val KEY_SELECTED_CLASS = "selected_class"
    private const val KEY_HOMEWORK_DONE = "homework_done"
    private const val KEY_CHECK_UPDATES = "check_updates"
    private const val KEY_LEGACY_UI = "legacy_ui"

    private val props = Properties()
    private var loaded = false
    private var settingsFile: File? = null

    @Synchronized
    fun init() {
        if (loaded) return
        AppPaths.ensureDirs()
        val file = AppPaths.settingsFile
        settingsFile = file
        if (file.exists()) {
            runCatching { file.inputStream().use(props::load) }
                .onFailure { AppLog.logException("settings/load", it) }
        }
        loaded = true
    }

    /** Перечитывает файл: другой экземпляр приложения мог сохранить цели. */
    @Synchronized
    fun reload() {
        val file = settingsFile ?: return
        if (!file.exists()) return
        runCatching {
            props.clear()
            file.inputStream().use(props::load)
        }.onFailure { AppLog.logException("settings/reload", it) }
    }

    private fun save() {
        try {
            AppPaths.ensureDirs()
            val file = AppPaths.settingsFile
            val tmp = File(file.path + ".tmp")
            tmp.outputStream().use { props.store(it, "Linteh28 diary") }
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
            // На Linux файл с паролем — только для владельца.
            runCatching {
                val perms = java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
                java.nio.file.Files.setPosixFilePermissions(file.toPath(), perms)
            }
        } catch (e: Exception) {
            AppLog.logException("settings/save", e)
        }
    }

    @Synchronized
    fun edit(block: (Properties) -> Unit) {
        block(props)
        save()
    }

    private fun str(key: String, def: String? = null): String? = props.getProperty(key) ?: def
    private fun put(key: String, value: String?) = edit { p ->
        if (value == null) p.remove(key) else p.setProperty(key, value)
    }

    var login: String?
        get() = str(KEY_LOGIN)
        set(v) = put(KEY_LOGIN, v)

    var password: String?
        get() = str(KEY_PASSWORD)
        set(v) = put(KEY_PASSWORD, v)

    var studentId: Int
        get() = str(KEY_STUDENT_ID)?.toIntOrNull() ?: -1
        set(v) = put(KEY_STUDENT_ID, v.toString())

    var userId: Int
        get() = str(KEY_USER_ID)?.toIntOrNull() ?: -1
        set(v) = put(KEY_USER_ID, v.toString())

    var uchId: Int
        get() = str(KEY_UCH_ID)?.toIntOrNull() ?: 1
        set(v) = put(KEY_UCH_ID, v.toString())

    var uchName: String?
        get() = str(KEY_UCH_NAME)
        set(v) = put(KEY_UCH_NAME, v)

    var uchYear: Int
        get() = str(KEY_UCH_YEAR)?.toIntOrNull() ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        set(v) = put(KEY_UCH_YEAR, v.toString())

    var classId: Int
        get() = str(KEY_CLASS_ID)?.toIntOrNull() ?: -1
        set(v) = put(KEY_CLASS_ID, v.toString())

    var theme: String
        get() = str(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(v) = put(KEY_THEME, v)

    var notifEnabled: Boolean
        get() = str(KEY_NOTIF_ENABLED) == "true"
        set(v) = put(KEY_NOTIF_ENABLED, v.toString())

    /** Интервал фоновой проверки, минуты. */
    var notifIntervalMin: Int
        get() = (str(KEY_NOTIF_INTERVAL)?.toIntOrNull() ?: 30).coerceIn(15, 1440)
        set(v) = put(KEY_NOTIF_INTERVAL, v.coerceIn(15, 1440).toString())

    var quietEnabled: Boolean
        get() = str(KEY_NOTIF_QUIET) != "false"
        set(v) = put(KEY_NOTIF_QUIET, v.toString())

    var quietFrom: Int
        get() = (str(KEY_NOTIF_QUIET_FROM)?.toIntOrNull() ?: 22).coerceIn(0, 23)
        set(v) = put(KEY_NOTIF_QUIET_FROM, v.coerceIn(0, 23).toString())

    var quietTo: Int
        get() = (str(KEY_NOTIF_QUIET_TO)?.toIntOrNull() ?: 8).coerceIn(0, 23)
        set(v) = put(KEY_NOTIF_QUIET_TO, v.coerceIn(0, 23).toString())

    var digestEnabled: Boolean
        get() = str(KEY_NOTIF_DIGEST) == "true"
        set(v) = put(KEY_NOTIF_DIGEST, v.toString())

    var digestHour: Int
        get() = (str(KEY_NOTIF_DIGEST_HOUR)?.toIntOrNull() ?: 18).coerceIn(0, 23)
        set(v) = put(KEY_NOTIF_DIGEST_HOUR, v.coerceIn(0, 23).toString())

    var digestDate: String?
        get() = str(KEY_NOTIF_DIGEST_DATE)
        set(v) = put(KEY_NOTIF_DIGEST_DATE, v)

    var pendingNotifs: String?
        get() = str(KEY_NOTIF_PENDING)
        set(v) = put(KEY_NOTIF_PENDING, v)

    var knownKeys: String?
        get() = str(KEY_NOTIF_KNOWN_KEYS)
        set(v) = put(KEY_NOTIF_KNOWN_KEYS, v)

    var knownEvents: String?
        get() = str(KEY_NOTIF_KNOWN_EVENTS)
        set(v) = put(KEY_NOTIF_KNOWN_EVENTS, v)

    var lastNotifiedVersion: String?
        get() = str(KEY_NOTIF_LAST_VERSION)
        set(v) = put(KEY_NOTIF_LAST_VERSION, v)

    var goalsJson: String?
        get() = str(KEY_GOALS_JSON)
        set(v) = put(KEY_GOALS_JSON, v)

    var selectedClass: String?
        get() = str(KEY_SELECTED_CLASS)
        set(v) = put(KEY_SELECTED_CLASS, v)

    var homeworkDoneJson: String?
        get() = str(KEY_HOMEWORK_DONE)
        set(v) = put(KEY_HOMEWORK_DONE, v)

    var checkUpdates: Boolean
        get() = str(KEY_CHECK_UPDATES) != "false"
        set(v) = put(KEY_CHECK_UPDATES, v.toString())

    var legacyUi: Boolean
        get() = str(KEY_LEGACY_UI) == "true"
        set(v) = put(KEY_LEGACY_UI, v.toString())

    fun saveCredentials(login: String, password: String, studentId: Int, userId: Int) = edit { p ->
        p.setProperty(KEY_LOGIN, login)
        p.setProperty(KEY_PASSWORD, password)
        p.setProperty(KEY_STUDENT_ID, studentId.toString())
        p.setProperty(KEY_USER_ID, userId.toString())
    }

    fun clearCredentials() = edit { p ->
        p.remove(KEY_LOGIN)
        p.remove(KEY_PASSWORD)
        p.remove(KEY_STUDENT_ID)
        p.remove(KEY_USER_ID)
        p.remove(KEY_CLASS_ID)
        p.remove(KEY_UCH_YEAR)
        p.remove(KEY_SELECTED_CLASS)
    }

    val isLoggedIn: Boolean get() = studentId != -1 && login != null && password != null
}
