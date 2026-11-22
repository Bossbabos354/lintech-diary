package ru.school28.diary

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private const val PREF_NAME = "school_diary_prefs"
    const val KEY_LOGIN = "login"
    const val KEY_PASSWORD = "password"
    const val KEY_STUDENT_ID = "student_id"
    const val KEY_USER_ID = "user_id"
    const val KEY_THEME = "theme"
    const val KEY_LEGACY_UI = "legacy_ui"
    const val KEY_UCH_ID = "uch_id"
    const val KEY_UCH_NAME = "uch_name"
    const val KEY_UCH_YEAR = "uch_year"
    const val KEY_CLASS_ID = "class_id"
    const val KEY_NOTIF_ENABLED = "notif_enabled"
    const val KEY_NOTIF_KNOWN_KEYS = "notif_known_keys"
    const val KEY_NOTIF_INTERVAL_MIN = "notif_interval_min"
    const val KEY_NOTIF_KNOWN_EVENTS = "notif_known_events"
    const val KEY_NOTIF_LAST_VERSION = "notif_last_version"
    const val KEY_NOTIF_QUIET = "notif_quiet"
    const val KEY_NOTIF_QUIET_FROM = "notif_quiet_from"
    const val KEY_NOTIF_QUIET_TO = "notif_quiet_to"
    const val KEY_NOTIF_DIGEST = "notif_digest"
    const val KEY_NOTIF_DIGEST_HOUR = "notif_digest_hour"
    const val KEY_NOTIF_DIGEST_DATE = "notif_digest_date"
    const val KEY_NOTIF_PENDING = "notif_pending"
    const val KEY_GOALS_JSON = "goals_json"
    const val KEY_NOTIFICATIONS_JSON = "notifications_json"
    const val KEY_SELECTED_CLASS = "selected_class"
    const val KEY_WIDGET_DATA = "widget_data_json"
    const val KEY_WIDGET_THEME = "widget_theme"
    const val KEY_WIDGET_GOAL = "widget_goal_id"
    const val KEY_HOMEWORK_DONE = "homework_done"
    const val KEY_CHECK_UPDATES = "check_updates"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_ZHAN = "zhan"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveCredentials(context: Context, login: String, password: String, studentId: Int) {
        prefs(context).edit()
            .putString(KEY_LOGIN, login)
            .putString(KEY_PASSWORD, password)
            .putInt(KEY_STUDENT_ID, studentId)
            .apply()
    }

    fun saveUserId(context: Context, userId: Int) {
        prefs(context).edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): Int = prefs(context).getInt(KEY_USER_ID, -1)

    fun getLogin(context: Context): String? = prefs(context).getString(KEY_LOGIN, null)
    fun getPassword(context: Context): String? = prefs(context).getString(KEY_PASSWORD, null)
    fun getStudentId(context: Context): Int = prefs(context).getInt(KEY_STUDENT_ID, -1)

    fun clearCredentials(context: Context) {
        prefs(context).edit()
            .remove(KEY_LOGIN)
            .remove(KEY_PASSWORD)
            .remove(KEY_STUDENT_ID)
            .apply()
    }

    // ---- Оформление: иконка и фон ----
    const val ICON_NEW = "new"
    const val ICON_OLD = "old"
    const val BG_AURORA = "aurora"
    const val BG_PHOTO = "photo"

    fun getAppIcon(context: Context): String =
        prefs(context).getString("app_icon", ICON_NEW) ?: ICON_NEW

    fun setAppIcon(context: Context, value: String) {
        prefs(context).edit().putString("app_icon", value).commit()
    }

    fun getAppBg(context: Context): String =
        prefs(context).getString("app_bg", BG_AURORA) ?: BG_AURORA

    fun setAppBg(context: Context, value: String) {
        prefs(context).edit().putString("app_bg", value).commit()
    }

    fun getTheme(context: Context): String = prefs(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
    }

    /** Своя подпись «за что оценка» (когда учитель её не оставил). */
    fun getMarkNote(context: Context, subjectId: Int, lessonId: Int): String? =
        prefs(context).getString("mark_note_${subjectId}_$lessonId", null)

    fun setMarkNote(context: Context, subjectId: Int, lessonId: Int, text: String) {
        val key = "mark_note_${subjectId}_$lessonId"
        val editor = prefs(context).edit()
        if (text.isBlank()) editor.remove(key) else editor.putString(key, text.trim())
        editor.apply()
    }

    /** Старый интерфейс (как в стабильной версии) — для слабых устройств. */
    fun isLegacyUi(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LEGACY_UI, false)

    fun setLegacyUi(context: Context, enabled: Boolean) {
        // commit(), а не apply(): сразу после этого процесс перезапускается,
        // и асинхронная запись может не успеть сохраниться.
        prefs(context).edit().putBoolean(KEY_LEGACY_UI, enabled).commit()
    }

    fun setUchId(context: Context, id: Int) {
        prefs(context).edit().putInt(KEY_UCH_ID, id).apply()
    }

    fun getUchId(context: Context): Int = prefs(context).getInt(KEY_UCH_ID, -1)

    fun setUchName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_UCH_NAME, name).apply()
    }

    fun getUchName(context: Context): String? = prefs(context).getString(KEY_UCH_NAME, null)

    fun setUchYear(context: Context, year: Int) {
        prefs(context).edit().putInt(KEY_UCH_YEAR, year).apply()
    }

    fun getUchYear(context: Context): Int = prefs(context).getInt(KEY_UCH_YEAR, -1)

    fun setClassId(context: Context, id: Int) {
        prefs(context).edit().putInt(KEY_CLASS_ID, id).apply()
    }

    fun getClassId(context: Context): Int = prefs(context).getInt(KEY_CLASS_ID, -1)

    fun isNotifEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_NOTIF_ENABLED, false)

    fun setNotifEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    fun getNotifKnownKeys(context: Context): String? = prefs(context).getString(KEY_NOTIF_KNOWN_KEYS, null)

    fun setNotifKnownKeys(context: Context, keys: String) {
        prefs(context).edit().putString(KEY_NOTIF_KNOWN_KEYS, keys).apply()
    }

    fun getNotifIntervalMin(context: Context): Int = prefs(context).getInt(KEY_NOTIF_INTERVAL_MIN, 30)

    fun setNotifIntervalMin(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_INTERVAL_MIN, minutes).apply()
    }

    fun isCheckUpdatesEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_CHECK_UPDATES, true)

    fun setCheckUpdatesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_CHECK_UPDATES, enabled).apply()
    }

    // ---- Уведомления о событиях ----

    /** Известные id событий (по одному на строку), null — ещё ни разу не проверяли. */
    fun getNotifKnownEvents(context: Context): String? =
        prefs(context).getString(KEY_NOTIF_KNOWN_EVENTS, null)

    fun setNotifKnownEvents(context: Context, ids: String) {
        prefs(context).edit().putString(KEY_NOTIF_KNOWN_EVENTS, ids).apply()
    }

    // ---- Уведомление о версии ----

    /** versionName, о котором уже уведомили (чтобы не повторяться). */
    fun getNotifLastVersion(context: Context): String? =
        prefs(context).getString(KEY_NOTIF_LAST_VERSION, null)

    fun setNotifLastVersion(context: Context, version: String) {
        prefs(context).edit().putString(KEY_NOTIF_LAST_VERSION, version).apply()
    }

    // ---- Цели по предметам ----

    fun getGoalsJson(context: Context): String? =
        prefs(context).getString(KEY_GOALS_JSON, null)

    fun setGoalsJson(context: Context, json: String) {
        // commit(), а не apply(): при обновлении приложения процесс может быть
        // убит сразу после записи, и асинхронный apply() не успеет сохранить цели.
        prefs(context).edit().putString(KEY_GOALS_JSON, json).commit()
    }

    // ---- История уведомлений ----

    fun getNotificationsJson(context: Context): String? =
        prefs(context).getString(KEY_NOTIFICATIONS_JSON, null)

    fun setNotificationsJson(context: Context, json: String) {
        prefs(context).edit().putString(KEY_NOTIFICATIONS_JSON, json).commit()
    }

    // ---- Класс для расписания ----

    fun getSelectedClass(context: Context): String? =
        prefs(context).getString(KEY_SELECTED_CLASS, null)

    fun setSelectedClass(context: Context, className: String) {
        prefs(context).edit().putString(KEY_SELECTED_CLASS, className).apply()
    }

    // ---- Виджеты ----

    fun getWidgetDataJson(context: Context): String? =
        prefs(context).getString(KEY_WIDGET_DATA, null)

    fun setWidgetDataJson(context: Context, json: String) {
        prefs(context).edit().putString(KEY_WIDGET_DATA, json).commit()
    }

    fun getWidgetTheme(context: Context): String =
        prefs(context).getString(KEY_WIDGET_THEME, "auto") ?: "auto"

    fun setWidgetTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_WIDGET_THEME, theme).apply()
    }

    /** Цель, которую показывает виджет. null — первая по порядку. */
    fun getWidgetGoalId(context: Context): String? =
        prefs(context).getString(KEY_WIDGET_GOAL, null)

    fun setWidgetGoalId(context: Context, goalId: String?) {
        prefs(context).edit().putString(KEY_WIDGET_GOAL, goalId).apply()
    }

    /** Отметки «сделал» по домашним заданиям (JSON). */
    fun getHomeworkDone(context: Context): String? =
        prefs(context).getString(KEY_HOMEWORK_DONE, null)

    fun setHomeworkDone(context: Context, json: String) {
        prefs(context).edit().putString(KEY_HOMEWORK_DONE, json).commit()
    }

    /** Какая половина списка домашки показана в виджете — для анимации смены. */
    fun getHomeworkFlip(context: Context): Int =
        prefs(context).getInt("widget_hw_flip", 0)

    fun setHomeworkFlip(context: Context, value: Int) {
        prefs(context).edit().putInt("widget_hw_flip", value).apply()
    }

    /** Отпечаток содержимого домашки: анимируем только реальные изменения. */
    fun getHomeworkSig(context: Context): String? =
        prefs(context).getString("widget_hw_sig", null)

    fun setHomeworkSig(context: Context, value: String) {
        prefs(context).edit().putString("widget_hw_sig", value).apply()
    }

    // ---- Тихие часы и дайджест ----

    fun isQuietEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIF_QUIET, true)

    fun setQuietEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_QUIET, enabled).apply()
    }

    /** Час начала тишины (0–23). */
    fun getQuietFrom(context: Context): Int =
        prefs(context).getInt(KEY_NOTIF_QUIET_FROM, 22)

    fun setQuietFrom(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_QUIET_FROM, hour.coerceIn(0, 23)).apply()
    }

    /** Час окончания тишины (0–23). */
    fun getQuietTo(context: Context): Int =
        prefs(context).getInt(KEY_NOTIF_QUIET_TO, 8)

    fun setQuietTo(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_QUIET_TO, hour.coerceIn(0, 23)).apply()
    }

    fun isDigestEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_NOTIF_DIGEST, false)

    fun setDigestEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_NOTIF_DIGEST, enabled).apply()
    }

    fun getDigestHour(context: Context): Int =
        prefs(context).getInt(KEY_NOTIF_DIGEST_HOUR, 18)

    fun setDigestHour(context: Context, hour: Int) {
        prefs(context).edit().putInt(KEY_NOTIF_DIGEST_HOUR, hour.coerceIn(0, 23)).apply()
    }

    /** Дата (yyyy-MM-dd) последнего отправленного дайджеста. */
    fun getDigestDate(context: Context): String? =
        prefs(context).getString(KEY_NOTIF_DIGEST_DATE, null)

    fun setDigestDate(context: Context, date: String) {
        prefs(context).edit().putString(KEY_NOTIF_DIGEST_DATE, date).apply()
    }

    /** Отложенные строки для дайджеста (новые оценки, ДЗ и т. п.). */
    fun getPendingNotifs(context: Context): String? =
        prefs(context).getString(KEY_NOTIF_PENDING, null)

    fun setPendingNotifs(context: Context, lines: String?) {
        prefs(context).edit().putString(KEY_NOTIF_PENDING, lines).apply()
    }
}
