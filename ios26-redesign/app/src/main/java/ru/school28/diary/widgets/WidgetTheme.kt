package ru.school28.diary.widgets

import android.content.Context
import ru.school28.diary.Prefs
import ru.school28.diary.R
import java.util.Calendar

/** Периоды суток для динамического фона цветных виджетов. */
enum class DayPeriod { MORNING, DAY, EVENING, NIGHT }

object WidgetTheme {
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val PASTEL = "pastel"
    const val DARK = "dark"

    private const val TEXT_DARK = 0xFF24272A.toInt()
    private const val TEXT_DARK_SOFT = 0xFF5E656B.toInt()
    private const val TEXT_WHITE = 0xFFFFFFFF.toInt()
    private const val TEXT_WHITE_SOFT = 0xD9FFFFFF.toInt()

    fun periodForHour(hour: Int): DayPeriod = when (hour) {
        in 5..9 -> DayPeriod.MORNING
        in 10..16 -> DayPeriod.DAY
        in 17..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }

    fun currentPeriod(): DayPeriod =
        periodForHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))

    /** Фон для светлых виджетов: зависит от выбранной темы; «авто» = время суток. */
    fun backgroundForLightWidget(context: Context): Int =
        when (Prefs.getWidgetTheme(context)) {
            LIGHT -> R.drawable.widget_bg_light
            PASTEL -> R.drawable.widget_bg_pastel
            DARK -> R.drawable.widget_bg_dark
            else -> dynamicBackground()
        }

    /** Динамический фон цветных виджетов: рассвет / день / закат / ночь. */
    fun dynamicBackground(): Int = when (currentPeriod()) {
        DayPeriod.MORNING -> R.drawable.widget_bg_morning
        DayPeriod.DAY -> R.drawable.widget_bg_day
        DayPeriod.EVENING -> R.drawable.widget_bg_evening
        DayPeriod.NIGHT -> R.drawable.widget_bg_night
    }

    /**
     * Фон цветного виджета по выбранной теме.
     * @param kind "nearest", "avg" или "schedule"
     */
    fun coloredBackground(context: Context, kind: String): Int =
        when (Prefs.getWidgetTheme(context)) {
            LIGHT -> when (kind) {
                "nearest" -> R.drawable.wg_grad_nearest
                "avg" -> R.drawable.wg_grad_avg
                else -> R.drawable.wg_grad_schedule
            }
            PASTEL -> when (kind) {
                "nearest" -> R.drawable.wg_grad_nearest_pastel
                "avg" -> R.drawable.wg_grad_avg_pastel
                else -> R.drawable.wg_grad_schedule_pastel
            }
            DARK -> when (kind) {
                "nearest" -> R.drawable.wg_grad_nearest_dark
                "avg" -> R.drawable.wg_grad_avg_dark
                else -> R.drawable.wg_grad_schedule_dark
            }
            else -> dynamicBackground()
        }

    /**
     * Цвета текста для светлых виджетов.
     * @return пара (основной, вторичный)
     */
    fun lightTextColors(context: Context): Pair<Int, Int> {
        val theme = Prefs.getWidgetTheme(context)
        return when (theme) {
            DARK -> TEXT_WHITE to TEXT_WHITE_SOFT
            LIGHT, PASTEL -> TEXT_DARK to TEXT_DARK_SOFT
            else -> {
                // «авто»: ночью и на закате — белый, днём/утром — тёмный
                when (currentPeriod()) {
                    DayPeriod.NIGHT, DayPeriod.EVENING -> TEXT_WHITE to TEXT_WHITE_SOFT
                    else -> TEXT_DARK to TEXT_DARK_SOFT
                }
            }
        }
    }

    /** Цвета текста для цветных виджетов — всегда белые. */
    fun coloredTextColors(): Pair<Int, Int> = TEXT_WHITE to TEXT_WHITE_SOFT
}