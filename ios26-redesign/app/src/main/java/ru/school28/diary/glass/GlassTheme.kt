package ru.school28.diary.glass

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Immutable
data class GlassColors(
    val isDark: Boolean,
    val wallpaper: Boolean = false,
    val background: Color,
    val backgroundSecondary: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val accentThird: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassStrong: Color,
    val glassBorderTop: Color,
    val glassBorderBottom: Color,
    val glassHighlight: Color,
    val blobA: Color,
    val blobB: Color,
    val blobC: Color,
    val blobD: Color,
    val scrim: Color,
    val skeleton: Color,
    val separator: Color,
) {
    val glassFill: Color get() = if (isDark) Color.White.copy(alpha = 0.075f) else Color.White.copy(alpha = if (wallpaper) 0.74f else 0.56f)
    val tabBarFill: Color get() = if (isDark) Color(0xFF161823).copy(alpha = 0.82f) else Color.White.copy(alpha = if (wallpaper) 0.86f else 0.72f)
}

val LightGlassColors = GlassColors(
    isDark = false,
    background = Color(0xFFF1F2FA),
    backgroundSecondary = Color(0xFFE9ECF7),
    text = Color(0xFF111219),
    textSecondary = Color(0xFF646A78),
    textTertiary = Color(0xFF9AA0AF),
    accent = Color(0xFF5A5AF2),
    accentSecondary = Color(0xFF9D6BFF),
    accentThird = Color(0xFF2FC4C4),
    onAccent = Color.White,
    success = Color(0xFF2EBE72),
    warning = Color(0xFFFFA23D),
    danger = Color(0xFFFF5A5A),
    glassTop = Color.White.copy(alpha = 0.78f),
    glassBottom = Color.White.copy(alpha = 0.42f),
    glassStrong = Color.White.copy(alpha = 0.9f),
    glassBorderTop = Color.White.copy(alpha = 0.95f),
    glassBorderBottom = Color.White.copy(alpha = 0.28f),
    glassHighlight = Color.White.copy(alpha = 0.65f),
    blobA = Color(0xFF9B86FF),
    blobB = Color(0xFF6FD2FF),
    blobC = Color(0xFFFFA8CE),
    blobD = Color(0xFF8FE9D5),
    scrim = Color(0xFF10111A).copy(alpha = 0.32f),
    skeleton = Color.White.copy(alpha = 0.6f),
    separator = Color(0xFF111219).copy(alpha = 0.07f),
)

val DarkGlassColors = GlassColors(
    isDark = true,
    background = Color(0xFF090A10),
    backgroundSecondary = Color(0xFF10111C),
    text = Color(0xFFF3F4FA),
    textSecondary = Color(0xFF9CA2B2),
    textTertiary = Color(0xFF6C7284),
    accent = Color(0xFF8585FF),
    accentSecondary = Color(0xFFB98CFF),
    accentThird = Color(0xFF4EE0D4),
    onAccent = Color(0xFF0B0B12),
    success = Color(0xFF48DB8B),
    warning = Color(0xFFFFB35F),
    danger = Color(0xFFFF7070),
    glassTop = Color.White.copy(alpha = 0.14f),
    glassBottom = Color.White.copy(alpha = 0.055f),
    glassStrong = Color.White.copy(alpha = 0.2f),
    glassBorderTop = Color.White.copy(alpha = 0.3f),
    glassBorderBottom = Color.White.copy(alpha = 0.07f),
    glassHighlight = Color.White.copy(alpha = 0.18f),
    blobA = Color(0xFF6A4DFF),
    blobB = Color(0xFF1F6BFF),
    blobC = Color(0xFFB23A8A),
    blobD = Color(0xFF1B8F7A),
    scrim = Color.Black.copy(alpha = 0.55f),
    skeleton = Color.White.copy(alpha = 0.12f),
    separator = Color.White.copy(alpha = 0.08f),
)

@Immutable
data class GlassType(
    val hero: TextStyle,
    val large: TextStyle,
    val title: TextStyle,
    val headline: TextStyle,
    val body: TextStyle,
    val callout: TextStyle,
    val caption: TextStyle,
    val micro: TextStyle,
)

val DefaultGlassType = GlassType(
    hero = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 54.sp,
        lineHeight = 58.sp,
        letterSpacing = (-2).sp,
    ),
    large = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.8).sp,
    ),
    title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.4).sp,
    ),
    headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp,
    ),
    body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp,
    ),
    callout = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp,
    ),
    micro = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.5.sp,
    ),
)

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }
val LocalGlassType = staticCompositionLocalOf { DefaultGlassType }

object Glass {
    val colors: GlassColors
        @ReadOnlyComposable
        @Composable
        get() = LocalGlassColors.current

    val type: GlassType
        @ReadOnlyComposable
        @Composable
        get() = LocalGlassType.current
}

object GlassShape {
    val screen = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
    val card = androidx.compose.foundation.shape.RoundedCornerShape(26.dp)
    val small = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
    val pill = androidx.compose.foundation.shape.RoundedCornerShape(100.dp)
    val sheet = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
}

@Composable
fun GlassTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    wallpaper: Boolean = false,
    content: @Composable () -> Unit,
) {
    val base = if (isDarkTheme) DarkGlassColors else LightGlassColors
    val colors = if (wallpaper) {
        base.copy(
            wallpaper = true,
            glassTop = Color.White.copy(alpha = 0.86f),
            glassBottom = Color.White.copy(alpha = 0.62f),
            text = Color(0xFF15161D),
            textSecondary = Color(0xFF5C6270),
            textTertiary = Color(0xFF8C92A0),
            glassBorderTop = Color.White.copy(alpha = 0.98f),
            glassBorderBottom = Color.White.copy(alpha = 0.4f),
        )
    } else {
        base
    }

    CompositionLocalProvider(
        LocalGlassColors provides colors,
        LocalGlassType provides DefaultGlassType,
        content = content,
    )
}
