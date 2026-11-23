package ru.school28.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class ExportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        enableGlassEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DiaryTheme {
                ru.school28.diary.ui.screens.ExportScreen(onBack = { finish() })
            }
        }
    }
}
