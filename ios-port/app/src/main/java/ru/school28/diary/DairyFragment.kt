package ru.school28.diary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.databinding.FragmentDairyBinding
import ru.school28.diary.ui.DairyAdapter
import java.util.*

class DairyFragment : Fragment() {

    private var _binding: FragmentDairyBinding? = null
    private val binding get() = _binding!!
    private var currentWeekOffset = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDairyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvDairy.layoutManager = LinearLayoutManager(requireContext())
        binding.srlRefresh.setOnRefreshListener { loadDairy() }
        binding.btnPrevWeek.setOnClickListener { currentWeekOffset -= 7; loadDairy() }
        binding.btnNextWeek.setOnClickListener { currentWeekOffset += 7; loadDairy() }
        binding.btnCurrentWeek.setOnClickListener { currentWeekOffset = 0; loadDairy() }
    }

    fun loadDairy() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.srlRefresh.isRefreshing = true

            try {
                val studentId = Prefs.getStudentId(requireContext())
                val uchYear = Prefs.getUchYear(requireContext())
                if (studentId == -1) return@launch

                if (Prefs.getClassId(requireContext()) == -1) {
                    val classId = withContext(Dispatchers.IO) {
                        ApiClient.getStudentClass(studentId, uchYear)
                    }
                    Prefs.setClassId(requireContext(), classId)
                }
                val classId = Prefs.getClassId(requireContext())
                if (classId == -1) return@launch

                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, currentWeekOffset)
                with(cal) {
                    val dow = get(Calendar.DAY_OF_WEEK)
                    add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val monday = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                val sunday = cal.time

                binding.tvWeek.text = java.text.SimpleDateFormat("d MMM", Locale("ru")).format(monday) +
                        " — " + java.text.SimpleDateFormat("d MMM", Locale("ru")).format(sunday)

                val entries = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, monday, sunday)
                }
                binding.rvDairy.adapter = DairyAdapter(entries, requireContext())
                binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                (activity as? MainActivity)?.showError("Ошибка: ${e.message}")
            }
            binding.progressBar.visibility = View.GONE
            binding.srlRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}