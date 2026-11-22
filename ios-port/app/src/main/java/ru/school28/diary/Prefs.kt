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
    const val KEY_UCH_ID = "uch_id"
    const val KEY_UCH_NAME = "uch_name"
    const val KEY_UCH_YEAR = "uch_year"
    const val KEY_CLASS_ID = "class_id"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

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

    fun getTheme(context: Context): String = prefs(context).getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(context: Context, theme: String) {
        prefs(context).edit().putString(KEY_THEME, theme).apply()
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
}