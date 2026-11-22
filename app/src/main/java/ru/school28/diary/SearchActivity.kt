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
import ru.school28.diary.databinding.ActivitySearchBinding
import ru.school28.diary.ui.DairyAdapter
import java.text.SimpleDateFormat
import java.util.*

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: DairyAdapter
    private var periodStart: Calendar? = null
    private var periodEnd: Calendar? = null

    private val ruLocale = Locale("ru")
    private val periodFmt = SimpleDateFormat("d MMM", ruLocale)

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
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
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

        // Список и адаптер настраиваем один раз, а не на каждый поиск
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        adapter = DairyAdapter(emptyList(), this)
        binding.rvSearchResults.adapter = adapter

        // По умолчанию — текущая неделя
        setWeek(0)
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

    /** Понедельник недели со сдвигом [weeksAhead] (0 — текущая). */
    private fun weekStart(weeksAhead: Int): Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        val dow = if (get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else get(Calendar.DAY_OF_WEEK) - 1
        add(Calendar.DAY_OF_YEAR, -(dow - 1) + weeksAhead * 7)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }

    private fun setWeek(weeksAhead: Int) {
        periodStart = weekStart(weeksAhead)
        periodEnd = (periodStart!!.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 6) }
    }

    private fun setRange(startMonth: Int, startDay: Int, endMonth: Int, endDay: Int) {
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        c.set(Calendar.MONTH, startMonth); c.set(Calendar.DAY_OF_MONTH, startDay)
        periodStart = c.clone() as Calendar
        c.set(Calendar.MONTH, endMonth); c.set(Calendar.DAY_OF_MONTH, endDay)
        periodEnd = c.clone() as Calendar
    }

    private fun applyPeriod(which: Int) {
        when (which) {
            0 -> setWeek(0)
            1 -> setWeek(1)
            2 -> setRange(Calendar.SEPTEMBER, 14, Calendar.SEPTEMBER, 20)
            3 -> setRange(Calendar.SEPTEMBER, 1, Calendar.OCTOBER, 31)
        }
        updatePeriodTitle()
        if (binding.etQuery.text?.isNotBlank() == true) doSearch()
    }

    private fun updatePeriodTitle() {
        binding.tvPeriodTitle.text = "Период: ${periodFmt.format(periodStart!!.time)} — ${periodFmt.format(periodEnd!!.time)}"
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

                // 1. Загружаем ВСЕ записи дневника за выбранный период
                val allEntries = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, ps.time, pe.time)
                }

                // 2. Фильтруем по запросу (предмет по подстроке ИЛИ день недели)
                val lower = query.lowercase(ruLocale)
                val requestedDay = dayNames[lower]

                // Один Calendar на весь проход вместо аллокации на каждую запись
                val entryCal = Calendar.getInstance()
                val filtered = allEntries.filter { entry ->
                    val subjectMatch = entry.subjectName.lowercase(ruLocale).contains(lower)
                    val dayMatch = if (requestedDay != null) {
                        entryCal.time = entry.date
                        entryCal.get(Calendar.DAY_OF_WEEK) == requestedDay
                    } else false
                    subjectMatch || dayMatch
                }.sortedBy { it.date }

                // 3. Показываем (адаптер уже группирует по дням)
                adapter.submit(filtered)
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