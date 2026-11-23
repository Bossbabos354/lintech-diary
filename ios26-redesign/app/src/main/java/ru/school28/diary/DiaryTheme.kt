package ru.school28.diary

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ru.school28.diary.glass.GlassTheme

@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val theme = Prefs.getTheme(ctx)
    val dark = when (theme) {
        Prefs.THEME_DARK -> true
        Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> false
        else -> isSystemInDarkTheme()
    }
    GlassTheme(
        isDarkTheme = dark,
        wallpaper = theme == Prefs.THEME_ZHAN,
        dynamic = theme == Prefs.THEME_DYNAMIC,
        content = content,
    )
}
