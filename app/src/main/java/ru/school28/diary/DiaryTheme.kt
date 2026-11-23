package ru.school28.diary

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import ru.school28.diary.ui_components.AppTheme

/**
 * Обёртка темы приложения.
 * Берёт выбранную в настройках тему из Prefs:
 *  - system / light / dark — стандартные темы Lumo
 *  - zhan ("Альтернативная тема") — светлые цвета + фоновая картинка с книгами
 */
@Composable
fun DiaryTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val theme = Prefs.getTheme(ctx)

    when (theme) {
        Prefs.THEME_DARK -> AppTheme(isDarkTheme = true) { content() }
        Prefs.THEME_LIGHT -> AppTheme(isDarkTheme = false) { content() }
        Prefs.THEME_ZHAN -> AppTheme(isDarkTheme = false) {
            Box(Modifier.fillMaxSize()) {
                // Фоновая картинка (узор с книгами)
                Image(
                    painter = painterResource(R.drawable.bg_zhan),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Лёгкая полупрозрачная подложка, чтобы контент читался
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.35f))
                )
                content()
            }
        }
        else -> AppTheme() { content() } // system
    }
}