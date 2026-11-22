package ru.school28.diary.ui_components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Black: Color = Color(0xFF1B1D1E)
val Gray900: Color = Color(0xFF24272A)
val Gray800: Color = Color(0xFF3A3F44)
val Gray700: Color = Color(0xFF4A5055)
val Gray600: Color = Color(0xFF5E656B)
val Gray500: Color = Color(0xFF7A8288)
val Gray400: Color = Color(0xFFA6AEB5)
val Gray300: Color = Color(0xFFC9D0D6)
val Gray200: Color = Color(0xFFE4E8EB)
val Gray100: Color = Color(0xFFF3F5F7)
val Gray50: Color = Color(0xFFFBFCFC)
val White: Color = Color(0xFFFFFFFF)

// Мягкая пастельная палитра (приглушённые, "уютные" тона вместо кричащих)
val LavenderL: Color = Color(0xFFB39DDB)   // основная (фиолетовая)
val LavenderDark: Color = Color(0xFF9575CD)
val BlushL: Color = Color(0xFFF8BBD0)      // розовая (вторичная)
val MintL: Color = Color(0xFFB2DFDB)       // акцентная (мятная)
val PeachL: Color = Color(0xFFFFCCBC)
val SkyL: Color = Color(0xFFB3E5FC)
val LemonL: Color = Color(0xFFFFF9C4)

val SoftBg: Color = Color(0xFFF6F3FA)      // тёплый светло-лиловый фон
val SoftSurface: Color = Color(0xFFFFFFFF)

// Яркие акценты для оценок (контраст на пастельном фоне)
val MintAccent: Color = Color(0xFF4DB6AC)   // 5
val AmberAccent: Color = Color(0xFFFFB74D)  // 4
val CoralAccent: Color = Color(0xFFFF7043)  // 3
val RoseAccent: Color = Color(0xFFEF5350)   // <3

val Red900: Color = Color(0xFF8E2A22)
val Red800: Color = Color(0xFFB43B33)
val Red700: Color = Color(0xFFCF4A41)
val Red600: Color = Color(0xFFE0574D)
val Red500: Color = Color(0xFFEF6358)
val Red400: Color = Color(0xFFF48A80)
val Red300: Color = Color(0xFFF9B3AC)
val Red200: Color = Color(0xFFFCD6D2)
val Red100: Color = Color(0xFFFEE8E5)
val Red50: Color = Color(0xFFFFF4F3)

val Blue900: Color = Color(0xFF2E6BD6)
val Blue800: Color = Color(0xFF3F7DF2)
val Blue700: Color = Color(0xFF5790F4)
val Blue600: Color = Color(0xFF70A3F6)
val Blue500: Color = Color(0xFF88B6F8)
val Blue400: Color = Color(0xFFA1C9FA)
val Blue300: Color = Color(0xFFBBDCFC)
val Blue200: Color = Color(0xFFD4EDFD)
val Blue100: Color = Color(0xFFEAF6FE)
val Blue50: Color = Color(0xFFF5FBFF)

val Green950: Color = Color(0xFF1F5E4E)
val Green900: Color = Color(0xFF257360)
val Green800: Color = Color(0xFF2B826D)
val Green700: Color = Color(0xFF31947C)
val Green600: Color = Color(0xFF38A68B)
val Green500: Color = Color(0xFF40B89A)
val Green400: Color = Color(0xFF67CAB0)
val Green300: Color = Color(0xFF8FD8C5)
val Green200: Color = Color(0xFFB7E6DA)
val Green100: Color = Color(0xFFDFF3EE)
val Green50: Color = Color(0xFFEFF9F6)

val Orange500: Color = Color(0xFFFFA726)
val Orange400: Color = Color(0xFFFFB74D)

val Pink500: Color = Color(0xFFF06292)
val Pink400: Color = Color(0xFFF48FB1)

val Purple500: Color = Color(0xFF9575CD)
val Purple400: Color = Color(0xFFB39DDB)

val Teal500: Color = Color(0xFF4DB6AC)
val Teal400: Color = Color(0xFF80CBC4)

// Мягкий градиент шапки (лиловый → розовый, плавный и спокойный)
fun gradientHeader(): Brush = Brush.linearGradient(
    colors = listOf(LavenderDark, LavenderL, BlushL)
)

fun gradientCardAccent(): Brush = Brush.linearGradient(
    colors = listOf(LavenderL, Purple400),
    start = Offset(0f, 0f), end = Offset(Float.MAX_VALUE, 0f)
)

fun gradientAverage(): Brush = Brush.horizontalGradient(
    colors = listOf(CoralAccent, AmberAccent, MintAccent)
)

@Immutable
data class Colors(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val disabled: Color,
    val onDisabled: Color,
    val surface: Color,
    val onSurface: Color,
    val background: Color,
    val onBackground: Color,
    val outline: Color,
    val transparent: Color = Color.Transparent,
    val white: Color = White,
    val black: Color = Black,
    val text: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val scrim: Color,
    val elevation: Color,
    val cardPink: Color = Color(0xFFFDE8F0),
    val cardPurple: Color = Color(0xFFF1EBFB),
    val cardTeal: Color = Color(0xFFE4F4F1),
    val cardOrange: Color = Color(0xFFFFF0E0),
    val markExcellent: Color = MintAccent,
    val markGood: Color = AmberAccent,
    val markBad: Color = RoseAccent,
    val navBarIndicator: Color = LavenderL,
)

internal val LightColors =
    Colors(
        primary = LavenderL,
        onPrimary = White,
        secondary = BlushL,
        onSecondary = White,
        tertiary = MintL,
        onTertiary = Black,
        surface = SoftSurface,
        onSurface = Black,
        error = RoseAccent,
        onError = White,
        success = MintAccent,
        onSuccess = White,
        disabled = Gray200,
        onDisabled = Gray400,
        background = SoftBg,
        onBackground = Gray900,
        outline = Gray200,
        transparent = Color.Transparent,
        white = White,
        black = Black,
        text = Gray900,
        textSecondary = Gray600,
        textDisabled = Gray400,
        scrim = Color.Black.copy(alpha = 0.28f),
        elevation = Gray300,
    )

internal val DarkColors =
    Colors(
        primary = LavenderDark,
        onPrimary = White,
        secondary = Color(0xFF8E5B72),
        onSecondary = White,
        tertiary = Color(0xFF4E7A6F),
        onTertiary = White,
        surface = Gray800,
        onSurface = Color(0xFFEEF1F4),
        error = Color(0xFFF48A80),
        onError = Black,
        success = Color(0xFF67CAB0),
        onSuccess = Black,
        disabled = Gray700,
        onDisabled = Gray500,
        background = Color(0xFF1A1C20),
        onBackground = Color(0xFFE8ECF0),
        outline = Gray700,
        transparent = Color.Transparent,
        white = White,
        black = Black,
        text = Color(0xFFE8ECF0),
        textSecondary = Gray400,
        textDisabled = Gray600,
        scrim = Color.Black.copy(alpha = 0.6f),
        elevation = Gray400,
        cardPink = Color(0xFF3C2830),
        cardPurple = Color(0xFF2E2936),
        cardTeal = Color(0xFF233331),
        cardOrange = Color(0xFF36291F),
        markExcellent = Color(0xFF67CAB0),
        markGood = Color(0xFFFFB74D),
        markBad = Color(0xFFF48A80),
        navBarIndicator = Color(0xFF9575CD),
    )

val LocalColors = staticCompositionLocalOf { LightColors }
val LocalContentColor = compositionLocalOf { Color.Black }
val LocalContentAlpha = compositionLocalOf { 1f }

fun Colors.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary -> onPrimary
        secondary -> onSecondary
        tertiary -> onTertiary
        surface -> onSurface
        error -> onError
        success -> onSuccess
        disabled -> onDisabled
        background -> onBackground
        else -> Color.Unspecified
    }
}