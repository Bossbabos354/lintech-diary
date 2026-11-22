package ru.school28.diary.ui

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

class DairyAdapter(
    private val entries: List<DairyEntry>,
    private val context: Context
) : RecyclerView.Adapter<DairyAdapter.DairyViewHolder>() {

    class DairyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tv_date)
        val subjectText: TextView = itemView.findViewById(R.id.tv_subject)
        val themeText: TextView = itemView.findViewById(R.id.tv_theme)
        val homeworkText: TextView = itemView.findViewById(R.id.tv_homework)
        val markText: TextView = itemView.findViewById(R.id.tv_mark)
        val teacherText: TextView = itemView.findViewById(R.id.tv_teacher)
    }

    private val daySdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
    private val dateOnly = Calendar.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DairyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dairy, parent, false)
        return DairyViewHolder(view)
    }

    override fun getItemCount(): Int = entries.size

    private fun isNewDay(pos: Int): Boolean {
        if (pos == 0) return true
        val c = entries[pos].date
        val p = entries[pos - 1].date
        dateOnly.time = c
        val cy = dateOnly.get(Calendar.YEAR)
        val cm = dateOnly.get(Calendar.DAY_OF_YEAR)
        dateOnly.time = p
        return cy != dateOnly.get(Calendar.YEAR) || cm != dateOnly.get(Calendar.DAY_OF_YEAR)
    }

    override fun onBindViewHolder(holder: DairyViewHolder, position: Int) {
        val entry = entries[position]

        if (isNewDay(position)) {
            holder.dateText.text = daySdf.format(entry.date)
            holder.dateText.visibility = View.VISIBLE
            val lp = holder.dateText.layoutParams as ViewGroup.MarginLayoutParams
            lp.topMargin = if (position == 0) 0 else dp(16)
            holder.dateText.layoutParams = lp
        } else {
            holder.dateText.visibility = View.GONE
        }

        holder.subjectText.text = entry.subjectName
        holder.themeText.text = entry.theme.ifEmpty { "—" }
        holder.homeworkText.text = if (entry.homework.isEmpty()) {
            "Д/з: нет"
        } else {
            "Д/з: ${entry.homework}"
        }
        holder.markText.text = if (entry.mark.isEmpty()) "·" else entry.mark

        if (entry.mark.isNotEmpty()) {
            val color = when {
                entry.mark.startsWith("!") -> context.getColor(R.color.mark_bad)
                entry.mark.toIntOrNull() != null && entry.mark.toInt() >= 4 -> context.getColor(R.color.mark_good)
                entry.mark.toIntOrNull() != null && entry.mark.toInt() == 3 -> context.getColor(R.color.mark_mid)
                else -> context.getColor(R.color.mark_bad)
            }
            holder.markText.setTextColor(color)
            holder.markText.text = entry.mark.removePrefix("!")
        }

        holder.teacherText.text = entry.teacherFio.ifEmpty { "" }
        holder.teacherText.visibility = if (entry.teacherFio.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun dp(v: Int): Int =
        (v * context.resources.displayMetrics.density).toInt()
}