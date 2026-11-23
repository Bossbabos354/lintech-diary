package ru.school28.diary.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ru.school28.diary.MainActivity
import ru.school28.diary.MainRoute
import ru.school28.diary.NotificationHelper
import ru.school28.diary.R
import java.util.concurrent.TimeUnit

/** Общие помощники для RemoteViews. */
internal object Wv {
    fun click(context: Context, tab: Int): PendingIntent {
        val intent = Intent(context, MainRoute.activity(context)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TAB, tab)
        }
        return PendingIntent.getActivity(
            context, tab, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingFlags()
        )
    }

    fun pendingFlags(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    fun setText(rv: RemoteViews, id: Int, text: String?, color: Int) {
        rv.setTextViewText(id, text ?: "")
        rv.setTextColor(id, color)
    }
}

/** 4×2 — ближайший урок (цветной градиент по макету). */
class NearestLessonWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    companion object {
        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, NearestLessonWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context)) }
        }

        fun build(context: Context): RemoteViews {
            val data = WidgetDataStore.load(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_nearest)
            rv.setInt(R.id.w_root, "setBackgroundResource", WidgetTheme.coloredBackground(context, "nearest"))
            Wv.setText(rv, R.id.w_label, data.nearestLabel ?: "СЛЕДУЮЩИЙ УРОК", 0xFFFFFFFF.toInt())
            Wv.setText(rv, R.id.w_time, data.nearestTime ?: "—", 0xFFFFFFFF.toInt())
            Wv.setText(rv, R.id.w_subject, data.nearestSubject ?: "Нет уроков", 0xFFFFFFFF.toInt())
            Wv.setText(rv, R.id.w_details, data.nearestDetails ?: "", 0xFFFFFFFF.toInt())

            // Живой отсчёт до начала: Chronometer считает сам, без обновлений виджета
            val startAt = data.nearestStartAt
            val nowMs = System.currentTimeMillis()
            if (startAt != null && startAt > nowMs) {
                rv.setViewVisibility(R.id.w_countdown, android.view.View.VISIBLE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val base = SystemClock.elapsedRealtime() + (startAt - nowMs)
                    rv.setChronometer(R.id.w_countdown, base, "до урока %s", true)
                    rv.setChronometerCountDown(R.id.w_countdown, true)
                } else {
                    // Android 5–6: обратный отсчёт не умеет, показываем статичный текст
                    val minutes = ((startAt - nowMs) / 60000).coerceAtLeast(1)
                    val left = if (minutes >= 60) "${minutes / 60} ч ${minutes % 60} мин" else "$minutes мин"
                    rv.setTextViewText(R.id.w_countdown, "до урока $left")
                }
            } else {
                rv.setViewVisibility(R.id.w_countdown, android.view.View.GONE)
            }

            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_SCHEDULE))
            return rv
        }
    }
}

/** 2×2 — средний балл (цветной градиент по макету). */
class AvgMarkWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    companion object {
        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, AvgMarkWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context)) }
        }

        fun build(context: Context): RemoteViews {
            val data = WidgetDataStore.load(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_avg)
            rv.setInt(R.id.w_root, "setBackgroundResource", WidgetTheme.coloredBackground(context, "avg"))
            Wv.setText(rv, R.id.w_avg, data.avgMark ?: "—", 0xFFFFFFFF.toInt())
            Wv.setText(rv, R.id.w_avg_label, "средний балл", 0xFFFFFFFF.toInt())
            val delta = data.avgDelta
            if (delta.isNullOrBlank()) {
                rv.setViewVisibility(R.id.w_avg_delta, android.view.View.GONE)
            } else {
                rv.setViewVisibility(R.id.w_avg_delta, android.view.View.VISIBLE)
                Wv.setText(rv, R.id.w_avg_delta, delta, 0xFFFFFFFF.toInt())
            }
            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_MARKS))
            return rv
        }
    }
}

/** 4×4 — дневник на сегодня (светлый, тема выбирается). */
class DiaryWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    companion object {
        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, DiaryWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context)) }
        }

        private data class RowViews(
            val container: Int,
            val time: Int,
            val subject: Int,
            val homework: Int,
            val chip: Int,
            val mark: Int,
            val separator: Int?
        )

        private val rows = listOf(
            RowViews(R.id.w_row_1, R.id.w_row1_time, R.id.w_row1_subject, R.id.w_row1_hw,
                R.id.w_row1_chip, R.id.w_row1_mark, R.id.w_sep_1),
            RowViews(R.id.w_row_2, R.id.w_row2_time, R.id.w_row2_subject, R.id.w_row2_hw,
                R.id.w_row2_chip, R.id.w_row2_mark, R.id.w_sep_2),
            RowViews(R.id.w_row_3, R.id.w_row3_time, R.id.w_row3_subject, R.id.w_row3_hw,
                R.id.w_row3_chip, R.id.w_row3_mark, null)
        )

        fun build(context: Context): RemoteViews {
            val data = WidgetDataStore.load(context)
            val (primary, secondary) = WidgetTheme.lightTextColors(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_diary)
            rv.setInt(R.id.w_root, "setBackgroundResource", WidgetTheme.backgroundForLightWidget(context))
            Wv.setText(rv, R.id.w_diary_header, data.diaryHeader ?: "Сегодня", primary)
            Wv.setText(rv, R.id.w_diary_more, data.diaryMore ?: "Открыть дневник →", 0xFF955DCD.toInt())

            rows.forEachIndexed { index, row ->
                val item = data.diaryRows.getOrNull(index)
                if (item == null) {
                    rv.setViewVisibility(row.container, android.view.View.GONE)
                    row.separator?.let { rv.setViewVisibility(it, android.view.View.GONE) }
                } else {
                    rv.setViewVisibility(row.container, android.view.View.VISIBLE)
                    Wv.setText(rv, row.time, item.time ?: "—", 0xFF757575.toInt())
                    Wv.setText(rv, row.subject, item.subject ?: "", primary)
                    Wv.setText(rv, row.homework, item.homework ?: "", secondary)

                    val mark = item.mark?.trim().orEmpty()
                    if (mark.isEmpty()) {
                        rv.setViewVisibility(row.chip, android.view.View.INVISIBLE)
                    } else {
                        rv.setViewVisibility(row.chip, android.view.View.VISIBLE)
                        val style = markStyle(mark)
                        rv.setInt(row.chip, "setBackgroundResource", style.background)
                        Wv.setText(rv, row.mark, mark, style.textColor)
                    }
                    val isLast = index == data.diaryRows.lastIndex
                    row.separator?.let {
                        rv.setViewVisibility(it, if (isLast) android.view.View.GONE else android.view.View.VISIBLE)
                    }
                }
            }
            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_DIARY))
            return rv
        }

        private data class MarkStyle(val background: Int, val textColor: Int)

        private fun markStyle(mark: String): MarkStyle {
            val digit = mark.firstOrNull { it.isDigit() }?.digitToIntOrNull()
            return when {
                digit == null -> MarkStyle(R.drawable.wg_chip_lav, 0xFFB39DDB.toInt())
                digit >= 5 -> MarkStyle(R.drawable.wg_chip_green, 0xFF4DB6AC.toInt())
                digit == 4 -> MarkStyle(R.drawable.wg_chip_amber, 0xFFF5A623.toInt())
                else -> MarkStyle(R.drawable.wg_chip_lav, 0xFFB39DDB.toInt())
            }
        }
    }
}

/** 4×2 — цель по предмету (светлый, тема выбирается). */
class GoalWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context)) }
    }

    companion object {
        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, GoalWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context)) }
        }

        fun build(context: Context): RemoteViews {
            val data = WidgetDataStore.load(context)
            val (primary, secondary) = WidgetTheme.lightTextColors(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_goal)
            rv.setInt(R.id.w_root, "setBackgroundResource", WidgetTheme.backgroundForLightWidget(context))
            Wv.setText(rv, R.id.w_goal_title, data.goalTitle ?: "Целей нет", primary)
            Wv.setText(rv, R.id.w_goal_line, data.goalLine ?: "Поставь цель в разделе «Оценки»", secondary)
            rv.setProgressBar(R.id.w_goal_progress, 100, data.goalProgress, false)

            val current = data.goalCurrent
            if (current.isNullOrBlank()) {
                rv.setViewVisibility(R.id.w_goal_current, android.view.View.GONE)
            } else {
                rv.setViewVisibility(R.id.w_goal_current, android.view.View.VISIBLE)
                Wv.setText(rv, R.id.w_goal_current, current, 0xFF9AA0B5.toInt())
            }

            val target = data.goalTarget
            if (target.isNullOrBlank()) {
                rv.setViewVisibility(R.id.w_goal_badge, android.view.View.GONE)
            } else {
                rv.setViewVisibility(R.id.w_goal_badge, android.view.View.VISIBLE)
                Wv.setText(rv, R.id.w_goal_target, target, 0xFFFFFFFF.toInt())
            }
            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_GOALS))
            return rv
        }
    }
}

/** 4×2 — расписание на завтра (цветной градиент по макету). */
class ScheduleWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context, it)) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        mgr: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?
    ) {
        mgr.updateAppWidget(appWidgetId, build(context, appWidgetId))
    }

    companion object {
        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, ScheduleWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context, it)) }
        }

        fun build(context: Context, widgetId: Int): RemoteViews {
            val data = WidgetDataStore.load(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_schedule)
            rv.setInt(R.id.w_root, "setBackgroundResource", WidgetTheme.coloredBackground(context, "schedule"))
            Wv.setText(rv, R.id.w_sched_header, data.scheduleHeader ?: "РАСПИСАНИЕ", 0xFFFFFFFF.toInt())

            // Строки добавляем динамически: сколько влезет в высоту виджета
            rv.removeAllViews(R.id.w_sched_list)
            val lines = data.scheduleLines
            val visible = when {
                lines.isEmpty() -> listOf("Уроков нет")
                else -> lines.take(maxRows(context, widgetId))
            }
            visible.forEach { line ->
                val row = RemoteViews(context.packageName, R.layout.widget_schedule_row)
                row.setTextViewText(R.id.w_row_text, line)
                rv.addView(R.id.w_sched_list, row)
            }

            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_SCHEDULE))
            return rv
        }

        /** Сколько строк помещается при текущей высоте виджета. */
        private fun maxRows(context: Context, widgetId: Int): Int {
            val options = (AppWidgetManager.getInstance(context) ?: return 5).getAppWidgetOptions(widgetId)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val rows = ((heightDp - 62) / 21).coerceIn(3, 12)
            ru.school28.diary.FileLog.log("ScheduleWidget: высота=${heightDp}dp, строк=$rows")
            return rows
        }
    }
}

/** Обновление всех виджетов после изменения данных. */
object WidgetUpdater {
    fun updateAll(context: Context) {
        val mgr = AppWidgetManager.getInstance(context) ?: return
        val ids = mgr.getAppWidgetIds(ComponentName(context, NearestLessonWidget::class.java))
        ru.school28.diary.FileLog.log("WidgetUpdater: id ближайшего урока = ${ids.size}")
        NearestLessonWidget.update(context)
        AvgMarkWidget.update(context)
        DiaryWidget.update(context)
        HomeworkWidget.update(context)
        GoalWidget.update(context)
        ScheduleWidget.update(context)
        ru.school28.diary.FileLog.log("WidgetUpdater: готово")
    }
}

/** Периодическое обновление данных виджетов (каждые 30 минут). */
object WidgetUpdateScheduler {
    private const val WORK_NAME = "widget_data"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetDataWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
