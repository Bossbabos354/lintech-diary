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
import ru.school28.diary.databinding.FragmentMarksBinding
import ru.school28.diary.ui.MarksAdapter

class MarksFragment : Fragment() {

    private var _binding: FragmentMarksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.srlRefresh.setOnRefreshListener { loadMarks() }
    }

    fun loadMarks() {
        lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.srlRefresh.isRefreshing = true

            try {
                val studentId = Prefs.getStudentId(requireContext())
                val uchYear = Prefs.getUchYear(requireContext())
                if (studentId == -1) return@launch

                var classId = Prefs.getClassId(requireContext())
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) {
                        ApiClient.getStudentClass(studentId, uchYear)
                    }
                    Prefs.setClassId(requireContext(), classId)
                }

                val subjects = withContext(Dispatchers.IO) {
                    ApiClient.getMarks(classId, studentId, uchYear)
                }
                binding.rvMarks.layoutManager = LinearLayoutManager(requireContext())
                binding.rvMarks.adapter = MarksAdapter(subjects, requireContext())
                binding.tvEmpty.visibility = if (subjects.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                (activity as? MainActivity)?.showError("Ошибка: ${e.message}")
            }
            binding.progressBar.visibility = View.GONE
            binding.srlRefresh.isRefreshing = false
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}