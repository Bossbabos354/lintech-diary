package ru.school28.diary

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.databinding.ActivitySearchBinding
import ru.school28.diary.ui.DairyAdapter
import java.text.SimpleDateFormat
import java.util.*

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private var periodStart: Calendar? = null
    private var periodEnd: Calendar? = null

    private val dayNames = mapOf(
        "понедельник" to Calendar.MONDAY,
        "вторник" to Calendar.TUESDAY,
        "среда" to Calendar.WEDNESDAY,
        "четверг" to Calendar.THURSDAY,
        "пятница" to Calendar.FRIDAY,
        "суббота" to Calendar.SATURDAY,
        "воскресенье" to Calendar.SUNDAY,
        "пн" to Calendar.MONDAY, "вт" to Calendar.TUESDAY, "ср" to Calendar.WEDNESDAY,
        "чт" to Calendar.THURSDAY, "пт" to Calendar.FRIDAY, "сб" to Calendar.SATURDAY,
        "вс" to Calendar.SUNDAY
    )

    private val periodOptions = listOf(
        "Текущая неделя",
        "Следующая неделя",
        "14–20 сентября",
        "Вся четверть (сентябрь–октябрь)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvPeriodTitle.setOnClickListener { showPeriodDialog() }
        binding.btnSearch.setOnClickListener { doSearch() }
        binding.etQuery.setOnEditorActionListener { _, _, _ ->
            doSearch()
            true
        }

        periodStart = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val dow = if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else get(Calendar.DAY_OF_WEEK) - 1
            add(Calendar.DAY_OF_YEAR, -(dow - 1))
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        periodEnd = (periodStart!!.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
        updatePeriodTitle()
    }

    private fun showPeriodDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Выбор периода дней")
            .setItems(periodOptions.toTypedArray()) { _, which ->
                applyPeriod(which)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun applyPeriod(which: Int) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY

        when (which) {
            0 -> {
                val dow = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1
                cal.add(Calendar.DAY_OF_YEAR, -(dow - 1))
                periodStart = cal.clone() as Calendar
                (periodStart!!.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }.also { periodEnd = it }
            }
            1 -> {
                val dow = if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else cal.get(Calendar.DAY_OF_WEEK) - 1
                cal.add(Calendar.DAY_OF_YEAR, -(dow - 1) + 7)
                periodStart = cal.clone() as Calendar
                (periodStart!!.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }.also { periodEnd = it }
            }
            2 -> {
                val c = Calendar.getInstance()
                c.set(Calendar.MONTH, Calendar.SEPTEMBER); c.set(Calendar.DAY_OF_MONTH, 14)
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
                periodStart = c.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, 20)
                periodEnd = c.clone() as Calendar
            }
            3 -> {
                val c = Calendar.getInstance()
                c.set(Calendar.MONTH, Calendar.SEPTEMBER); c.set(Calendar.DAY_OF_MONTH, 1)
                c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
                periodStart = c.clone() as Calendar
                c.set(Calendar.MONTH, Calendar.OCTOBER); c.set(Calendar.DAY_OF_MONTH, 31)
                periodEnd = c.clone() as Calendar
            }
        }
        updatePeriodTitle()
        if (binding.etQuery.text?.isNotBlank() == true) doSearch()
    }

    private fun updatePeriodTitle() {
        val fmt = SimpleDateFormat("d MMM", Locale("ru"))
        binding.tvPeriodTitle.text = "Период: ${fmt.format(periodStart!!.time)} — ${fmt.format(periodEnd!!.time)}"
    }

    private fun doSearch() {
        val query = binding.etQuery.text.toString().trim()
        if (query.isEmpty()) {
            toast("Введите предмет или день недели")
            return
        }
        val ps = periodStart ?: return
        val pe = periodEnd ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.rvSearchResults.visibility = View.GONE
        binding.tvSearchEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val studentId = Prefs.getStudentId(this@SearchActivity)
                val uchYear = Prefs.getUchYear(this@SearchActivity)
                if (studentId == -1) return@launch

                var classId = Prefs.getClassId(this@SearchActivity)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) { ApiClient.getStudentClass(studentId, uchYear) }
                    Prefs.setClassId(this@SearchActivity, classId)
                }

                val allEntries = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, ps.time, pe.time)
                }

                val lower = query.lowercase(Locale("ru"))
                val requestedDay = dayNames[lower]

                val filtered = allEntries.filter { entry ->
                    val subjectMatch = entry.subjectName.lowercase(Locale("ru")).contains(lower)
                    val dayMatch = requestedDay?.let { day ->
                        val cal = Calendar.getInstance().apply { time = entry.date }
                        cal.get(Calendar.DAY_OF_WEEK) == day
                    } ?: false
                    subjectMatch || dayMatch
                }.sortedBy { it.date }

                binding.rvSearchResults.layoutManager = LinearLayoutManager(this@SearchActivity)
                binding.rvSearchResults.adapter = DairyAdapter(filtered, this@SearchActivity)
                binding.rvSearchResults.visibility = View.VISIBLE
                binding.tvSearchEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                toast("Ошибка: ${e.message}")
            }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}