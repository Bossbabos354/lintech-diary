package ru.school28.diary.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Палитра в духе ios26-redesign: стеклянные поверхности поверх «авроры». */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val primary: Color,
    val onPrimary: Color,
    val accentSecondary: Color,
    val background: Color,
    val backgroundSecondary: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val outline: Color,
    val cardPurple: Color,
    val markExcellent: Color,
    val markGood: Color,
    val markBad: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val scrim: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val blobD: Color,
)

val LightColors = AppColors(
    isDark = false,
    primary = Color(0xFF5A5AF2),
    onPrimary = Color.White,
    accentSecondary = Color(0xFF9D6BFF),
    background = Color(0xFFF1F2FA),
    backgroundSecondary = Color(0xFFE9ECF7),
    surface = Color.White.copy(alpha = 0.58f),
    surfaceSolid = Color(0xFFFFFFFF),
    text = Color(0xFF111219),
    textSecondary = Color(0xFF646A78),
    textDisabled = Color(0xFF9AA0AF),
    error = Color(0xFFFF5A5A),
    onError = Color.White,
    success = Color(0xFF2EBE72),
    outline = Color.White.copy(alpha = 0.55f),
    cardPurple = Color(0xFFEDEAFB),
    markExcellent = Color(0xFF2EBE72),
    markGood = Color(0xFFFFA23D),
    markBad = Color(0xFFFF5A5A),
    glassTop = Color.White.copy(alpha = 0.78f),
    glassBottom = Color.White.copy(alpha = 0.42f),
    glassBorderTop = Color.White.copy(alpha = 0.95f),
    glassBorderBottom = Color.White.copy(alpha = 0.28f),
    scrim = Color(0xFF10111A).copy(alpha = 0.32f),
    blobA = Color(0xFF9B86FF),
    blobB = Color(0xFF6FD2FF),
    blobC = Color(0xFFFFA8CE),
    blobD = Color(0xFF8FE9D5),
)

val DarkColors = AppColors(
    isDark = true,
    primary = Color(0xFF8585FF),
    onPrimary = Color(0xFF0B0B12),
    accentSecondary = Color(0xFFB98CFF),
    background = Color(0xFF090A10),
    backgroundSecondary = Color(0xFF10111C),
    surface = Color.White.copy(alpha = 0.075f),
    surfaceSolid = Color(0xFF171823),
    text = Color(0xFFF3F4FA),
    textSecondary = Color(0xFF9CA2B2),
    textDisabled = Color(0xFF6C7284),
    error = Color(0xFFFF7070),
    onError = Color(0xFF0B0B12),
    success = Color(0xFF48DB8B),
    outline = Color.White.copy(alpha = 0.14f),
    cardPurple = Color(0xFF1A1B26),
    markExcellent = Color(0xFF48DB8B),
    markGood = Color(0xFFFFB35F),
    markBad = Color(0xFFFF7070),
    glassTop = Color.White.copy(alpha = 0.14f),
    glassBottom = Color.White.copy(alpha = 0.055f),
    glassBorderTop = Color.White.copy(alpha = 0.30f),
    glassBorderBottom = Color.White.copy(alpha = 0.07f),
    scrim = Color.Black.copy(alpha = 0.55f),
    blobA = Color(0xFF6A4DFF),
    blobB = Color(0xFF1F6BFF),
    blobC = Color(0xFFB23A8A),
    blobD = Color(0xFF1B8F7A),
)

val LocalColors = staticCompositionLocalOf { LightColors }

object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalColors.current
}

@Composable
fun DiaryTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalColors provides colors) {
        MaterialTheme(
            colorScheme = if (dark) {
                darkColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    background = colors.background,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    error = colors.error,
                    outline = colors.outline,
                )
            } else {
                lightColorScheme(
                    primary = colors.primary,
                    onPrimary = colors.onPrimary,
                    background = colors.background,
                    onBackground = colors.text,
                    surface = colors.surface,
                    onSurface = colors.text,
                    error = colors.error,
                    outline = colors.outline,
                )
            },
            content = content,
        )
    }
}

/** Акцентный градиент для шапок и крупных чисел. */
fun gradientHeader(): Brush = Brush.linearGradient(
    listOf(Color(0xFF5A5AF2), Color(0xFF9D6BFF), Color(0xFF2FC4C4))
)

fun gradientAverage(): Brush = Brush.horizontalGradient(
    listOf(Color(0xFF9D6BFF), Color(0xFF5A5AF2), Color(0xFF2FC4C4))
)
