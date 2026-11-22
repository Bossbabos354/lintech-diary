package ru.school28.diary.ui

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.school28.diary.R
import ru.school28.diary.api.SubjectMarks
import java.util.*

class MarksAdapter(
    private val subjects: List<SubjectMarks>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val TYPE_SUBJECT = 0
        const val TYPE_AVG = 1
    }

    class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subjectText: TextView = itemView.findViewById(R.id.tv_subject)
        val teacherText: TextView = itemView.findViewById(R.id.tv_teacher)
        val marksText: TextView = itemView.findViewById(R.id.tv_marks)
        val totalsText: TextView = itemView.findViewById(R.id.tv_totals)
        val avgText: TextView = itemView.findViewById(R.id.tv_avg)
    }

    class AvgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avgText: TextView = itemView.findViewById(R.id.tv_overall_avg)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == subjects.size) TYPE_AVG else TYPE_SUBJECT
    }

    override fun getItemCount(): Int = subjects.size + 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_AVG) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_marks_avg, parent, false)
            AvgViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_marks, parent, false)
            SubjectViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SubjectViewHolder) {
            val subject = subjects[position]
            holder.subjectText.text = subject.subjectName
            holder.teacherText.text = subject.teacherName ?: ""

            val totals = if (subject.totalMarks.isEmpty()) "" else {
                subject.totalMarks.entries
                    .sortedBy { it.key }
                    .joinToString(" · ") { (periodId, mark) ->
                        val periodName = when (periodId) {
                            360000 -> "1 четв."
                            360001 -> "2 четв."
                            360002 -> "3 четв."
                            360003 -> "4 четв."
                            360011 -> "1 полуг."
                            360012 -> "2 полуг."
                            else -> "Пер. $periodId"
                        }
                        "$periodName: $mark"
                    }
            }
            holder.totalsText.text = totals.ifEmpty { "" }
            holder.totalsText.visibility = if (totals.isEmpty()) View.GONE else View.VISIBLE

            val marks = subject.marks
                .filter { it.mark.isNotEmpty() }
                .sortedBy { it.date ?: Date(0) }
                .takeLast(20)
                .joinToString(" ") { it.mark }
            holder.marksText.text = marks.ifEmpty { "—" }

            val numeric = subject.marks
                .mapNotNull { it.mark.toIntOrNull() }
            val avg = if (numeric.isNotEmpty()) {
                String.format(Locale("ru"), "Средний: %.2f", numeric.sum().toDouble() / numeric.size)
            } else {
                ""
            }
            holder.avgText.text = avg
            holder.avgText.visibility = if (avg.isEmpty()) View.GONE else View.VISIBLE
        } else if (holder is AvgViewHolder) {
            val allMarks = subjects.flatMap { it.marks }
                .mapNotNull { it.mark.toIntOrNull() }
            val overall = if (allMarks.isNotEmpty()) {
                String.format(Locale("ru"), "Общий средний балл: %.2f", allMarks.sum().toDouble() / allMarks.size)
            } else {
                "Нет числовых оценок"
            }
            holder.avgText.text = overall
        }
    }
}