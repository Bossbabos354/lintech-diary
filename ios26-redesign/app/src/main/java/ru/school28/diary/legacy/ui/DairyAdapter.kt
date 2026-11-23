package ru.school28.diary.legacy.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.school28.diary.R
import ru.school28.diary.api.DairyEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Адаптер дневника: группирует записи по дням.
 * Каждый день — одна карточка (заголовок дня + записи, разделённые пунктиром).
 */
class DairyAdapter(
    entries: List<DairyEntry>,
    private val context: Context
) : RecyclerView.Adapter<DairyAdapter.DayViewHolder>() {

    data class Day(
        val date: Date,
        val entries: List<DairyEntry>
    )

    private var days: List<Day> = buildDays(entries)

    /** Обновляет данные без пересоздания адаптера и layoutManager. */
    fun submit(entries: List<DairyEntry>) {
        days = buildDays(entries)
        notifyDataSetChanged()
    }

    private fun buildDays(entries: List<DairyEntry>): List<Day> = entries
        .sortedBy { it.date }
        .groupBy { cal(it.date) }
        .map { (key, list) -> Day(dateOf(key), list) }

    private val daySdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layoutEntries: ViewGroup = itemView.findViewById(R.id.layout_entries)
        val dateText: TextView = itemView.findViewById(R.id.tv_date)
    }

    private fun cal(d: Date): Calendar = Calendar.getInstance().apply {
        time = d
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun dateOf(c: Calendar): Date = c.time

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dairy_day, parent, false)
        return DayViewHolder(view)
    }

    override fun getItemCount(): Int = days.size

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.layoutEntries.removeAllViews()

        val inflater = LayoutInflater.from(context)

        // Заголовок дня
        holder.dateText.text = daySdf.format(day.date)

        // Записи дня
        for ((i, entry) in day.entries.withIndex()) {
            val entryView = inflater.inflate(R.layout.item_dairy_entry, holder.layoutEntries, false)

            val subject: TextView = entryView.findViewById(R.id.tv_subject)
            val theme: TextView = entryView.findViewById(R.id.tv_theme)
            val homework: TextView = entryView.findViewById(R.id.tv_homework)
            val mark: TextView = entryView.findViewById(R.id.tv_mark)
            val teacher: TextView = entryView.findViewById(R.id.tv_teacher)
            val divider: View = entryView.findViewById(R.id.divider)

            // Пунктир между записями (не перед первой)
            divider.visibility = if (i > 0) View.VISIBLE else View.GONE

            subject.text = entry.subjectName
            theme.text = entry.theme.ifEmpty { "—" }
            homework.text = if (entry.homework.isEmpty()) {
                "Д/з: нет"
            } else {
                "Д/з: ${entry.homework}"
            }
            mark.text = if (entry.mark.isEmpty()) "·" else entry.mark

            if (entry.mark.isNotEmpty()) {
                mark.setTextColor(MarkStyle.argb(entry.mark))
                mark.text = entry.mark.removePrefix("!")
            }

            teacher.text = entry.teacherFio.ifEmpty { "" }
            teacher.visibility = if (entry.teacherFio.isEmpty()) View.GONE else View.VISIBLE

            holder.layoutEntries.addView(entryView)
        }
    }
}