package ru.school28.diary.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import ru.school28.diary.FileLog
import ru.school28.diary.NotificationHelper
import ru.school28.diary.R
import ru.school28.diary.ui.HomeworkChecklist

/**
 * «Домашка на сегодня» с галочками прямо на рабочем столе.
 * Строк столько, сколько помещается в высоту виджета; если заданий больше —
 * под списком появляется строка «ещё N», а счётчик всё равно показывает все.
 */
class HomeworkWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, build(context, it)) }
        // Виджет только что добавили — тянем снимок без троттлинга.
        if (WidgetDataStore.load(context).homeworkLoaded) WidgetSync.request(context)
        else WidgetSync.force(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        mgr: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        mgr.updateAppWidget(appWidgetId, build(context, appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_TOGGLE) {
            val key = intent.getStringExtra(EXTRA_KEY)
            if (!key.isNullOrBlank()) {
                val done = HomeworkChecklist.toggleKey(context, key)
                FileLog.log("HomeworkWidget: отметка $key -> $done")
                update(context)
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_TOGGLE = "ru.school28.diary.widgets.HOMEWORK_TOGGLE"
        private const val EXTRA_KEY = "hw_key"
        private const val MIN_ROW_DP = 28
        private const val MORE_HEIGHT_DP = 18
        private const val OVERHEAD_DP = 61
        private const val MAX_ROWS = 8

        fun update(context: Context) {
            val mgr = AppWidgetManager.getInstance(context) ?: return
            mgr.getAppWidgetIds(ComponentName(context, HomeworkWidget::class.java))
                .forEach { mgr.updateAppWidget(it, build(context, it)) }
        }

        fun build(context: Context, widgetId: Int): RemoteViews {
            val data = WidgetDataStore.load(context)
            val dark = isDark(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_homework)

            rv.setInt(
                R.id.w_root,
                "setBackgroundResource",
                if (dark) R.drawable.widget_homework_bg_dark else R.drawable.widget_homework_bg_light
            )

            val accent = if (dark) 0xFF8585FF.toInt() else 0xFF5A5AF2.toInt()
            val success = if (dark) 0xFF48DB8B.toInt() else 0xFF2EBE72.toInt()
            val primary = if (dark) 0xFFF3F4FA.toInt() else 0xFF111219.toInt()
            val secondary = if (dark) 0xFF9CA2B2.toInt() else 0xFF646A78.toInt()
            val dimPrimary = if (dark) 0xA8F3F4FA.toInt() else 0xA8111219.toInt()
            val dimSecondary = if (dark) 0x809CA2B2.toInt() else 0x80646A78.toInt()

            val items = data.homeworkRows
            val total = items.size
            val done = HomeworkChecklist.doneCountKeys(context, items.map { it.key })
            val allDone = total > 0 && done >= total

            Wv.setText(rv, R.id.w_hw_title, "Домашка на сегодня", primary)

            rv.removeAllViews(R.id.w_hw_rows)

            if (total == 0) {
                rv.setViewVisibility(R.id.w_hw_pill, View.GONE)
                rv.setViewVisibility(R.id.w_hw_progress_box, View.GONE)
                rv.setViewVisibility(R.id.w_hw_empty, View.VISIBLE)
                val emptyText = if (data.homeworkLoaded) "На сегодня домашки нет"
                else "Нет данных · открой приложение"
                Wv.setText(rv, R.id.w_hw_empty, emptyText, secondary)
                rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_DIARY))
                return rv
            }

            rv.setViewVisibility(R.id.w_hw_pill, View.VISIBLE)
            rv.setViewVisibility(R.id.w_hw_progress_box, View.VISIBLE)
            rv.setViewVisibility(R.id.w_hw_empty, View.GONE)

            Wv.setText(rv, R.id.w_hw_pill, if (allDone) "готово" else "$done / $total", if (allDone) success else accent)
            rv.setInt(
                R.id.w_hw_pill,
                "setBackgroundResource",
                if (allDone) R.drawable.wg_hw_pill_success else R.drawable.wg_hw_pill_accent
            )

            val percent = (done * 100 / total).coerceIn(0, 100)
            val bars = intArrayOf(
                R.id.w_hw_progress,
                R.id.w_hw_progress_done,
                R.id.w_hw_progress_light,
                R.id.w_hw_progress_light_done
            )
            bars.forEach { rv.setViewVisibility(it, View.GONE) }
            val active = when {
                dark && allDone -> R.id.w_hw_progress_done
                dark -> R.id.w_hw_progress
                allDone -> R.id.w_hw_progress_light_done
                else -> R.id.w_hw_progress_light
            }
            rv.setViewVisibility(active, View.VISIBLE)
            rv.setProgressBar(active, 100, percent, false)

            val cap = capacity(context, widgetId)
            var shown = minOf(total, cap.rows)
            if (total > shown) {
                // оставляем место под строку «ещё N»
                val withFooter = ((cap.heightDp - MORE_HEIGHT_DP) / MIN_ROW_DP).coerceIn(1, MAX_ROWS)
                shown = minOf(shown, withFooter, total)
            }

            items.take(shown).forEach { item ->
                rv.addView(R.id.w_hw_rows, rowView(context, item, dark, primary, secondary, dimPrimary, dimSecondary))
            }

            val hidden = total - shown
            if (hidden > 0) {
                val more = RemoteViews(context.packageName, R.layout.widget_homework_more)
                more.setTextViewText(R.id.w_hw_more, "ещё $hidden ${plural(hidden)} →")
                more.setTextColor(R.id.w_hw_more, accent)
                rv.addView(R.id.w_hw_rows, more)
            }

            rv.setOnClickPendingIntent(R.id.w_root, Wv.click(context, NotificationHelper.TAB_DIARY))
            return rv
        }

        private data class Capacity(val heightDp: Int, val rows: Int)

        private fun capacity(context: Context, widgetId: Int): Capacity {
            val options = (AppWidgetManager.getInstance(context) ?: return Capacity(240, 5)).getAppWidgetOptions(widgetId)
            val heightDp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 240)
            val available = (heightDp - OVERHEAD_DP).coerceAtLeast(MIN_ROW_DP * 2)
            val count = (available / MIN_ROW_DP).coerceIn(2, MAX_ROWS)
            FileLog.log("HomeworkWidget: высота=${heightDp}dp, строк=$count")
            return Capacity(available, count)
        }

        private fun rowView(
            context: Context,
            item: HomeworkRow,
            dark: Boolean,
            primary: Int,
            secondary: Int,
            dimPrimary: Int,
            dimSecondary: Int
        ): RemoteViews {
            val isDone = HomeworkChecklist.isDone(context, item.key)
            val row = RemoteViews(context.packageName, R.layout.widget_homework_row)
            row.setInt(
                R.id.w_row_box,
                "setBackgroundResource",
                when {
                    isDone && dark -> R.drawable.wg_hw_box_on_dark
                    isDone -> R.drawable.wg_hw_box_on_light
                    dark -> R.drawable.wg_hw_box_off_dark
                    else -> R.drawable.wg_hw_box_off_light
                }
            )
            row.setViewVisibility(R.id.w_row_check, if (isDone) View.VISIBLE else View.INVISIBLE)
            Wv.setText(row, R.id.w_row_subject, item.subject ?: "", if (isDone) dimPrimary else primary)
            Wv.setText(row, R.id.w_row_task, item.task ?: "", if (isDone) dimSecondary else secondary)
            row.setOnClickPendingIntent(R.id.w_row_root, toggleIntent(context, item.key))
            return row
        }

        private fun plural(n: Int): String {
            val tail = n % 100
            if (tail in 11..14) return "заданий"
            return when (n % 10) {
                1 -> "задание"
                2, 3, 4 -> "задания"
                else -> "заданий"
            }
        }

        private fun toggleIntent(context: Context, key: String): PendingIntent {
            val intent = Intent(context, HomeworkWidget::class.java).apply {
                action = ACTION_TOGGLE
                putExtra(EXTRA_KEY, key)
            }
            return PendingIntent.getBroadcast(
                context, key.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or Wv.pendingFlags()
            )
        }

        private fun isDark(context: Context): Boolean {
            val (primary, _) = WidgetTheme.lightTextColors(context)
            return primary == 0xFFFFFFFF.toInt()
        }
    }
}
