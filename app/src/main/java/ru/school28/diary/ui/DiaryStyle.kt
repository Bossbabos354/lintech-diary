package ru.school28.diary.ui

import androidx.compose.ui.graphics.Color

/**
 * Единые правила оформления оценок и предметов.
 *
 * Раньше правило «оценка → цвет» было скопировано в четырёх местах
 * (DairyScreen, MarksScreen, LessonSheet, DairyAdapter), причём Compose-экраны
 * брали цвета из темы, а адаптер — из XML-ресурсов, и значения уже расходились.
 * Здесь один источник: ARGB-константы, из которых выводится и Color, и int.
 */
object MarkStyle {
    private const val EXCELLENT = 0xFF66BB6A.toInt()
    private const val GOOD = 0xFFFFB74D.toInt()
    private const val BAD = 0xFFEF5350.toInt()

    private fun valueOf(mark: String): Int? = mark.removePrefix("!").toIntOrNull()

    /** Цвет оценки в виде ARGB — для View-адаптера. */
    fun argb(mark: String): Int = when {
        mark.startsWith("!") -> BAD
        (valueOf(mark) ?: 0) >= 4 -> EXCELLENT
        valueOf(mark) == 3 -> GOOD
        else -> BAD
    }

    /** Цвет оценки для Compose. */
    fun color(mark: String): Color = Color(argb(mark))
}

private val SUBJECT_PALETTE = listOf(
    Color(0xFF9575CD), Color(0xFFF48FB1), Color(0xFF80CBC4),
    Color(0xFFFFB74D), Color(0xFF81C9FA), Color(0xFFA5D6A7),
    Color(0xFFCE93D8), Color(0xFFEF9A9A), Color(0xFFA2E3F0)
)

/** Стабильный цвет-аватар предмета (одинаковый для одного subjectId). */
fun subjectColor(subjectId: Int): Color =
    SUBJECT_PALETTE[Math.floorMod(subjectId, SUBJECT_PALETTE.size)]
