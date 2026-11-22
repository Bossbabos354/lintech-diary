package ru.school28.diary.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import ru.school28.diary.Prefs
import ru.school28.diary.R
import ru.school28.diary.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var onLogoutListener: (() -> Unit)? = null
    private var onCheckUpdateListener: (() -> Unit)? = null

    fun setOnLogoutListener(listener: () -> Unit) {
        onLogoutListener = listener
    }

    fun setOnCheckUpdateListener(listener: () -> Unit) {
        onCheckUpdateListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etLogin.setText(Prefs.getLogin(requireContext()))
        binding.etPassword.setText(Prefs.getPassword(requireContext()))

        val savedTheme = Prefs.getTheme(requireContext())
        when (savedTheme) {
            Prefs.THEME_LIGHT -> binding.rgTheme.check(R.id.rb_light)
            Prefs.THEME_DARK -> binding.rgTheme.check(R.id.rb_dark)
            else -> binding.rgTheme.check(R.id.rb_system)
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.rb_light -> Prefs.THEME_LIGHT
                R.id.rb_dark -> Prefs.THEME_DARK
                else -> Prefs.THEME_SYSTEM
            }
            Prefs.setTheme(requireContext(), theme)
            Toast.makeText(requireContext(), "Тема применена при следующем запуске", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (login.isNotEmpty() && password.isNotEmpty()) {
                Prefs.saveCredentials(requireContext(), login, password, Prefs.getStudentId(requireContext()))
                Toast.makeText(requireContext(), "Данные сохранены", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Введите логин и пароль", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            onLogoutListener?.invoke()
        }

        binding.btnCheckUpdate.setOnClickListener {
            onCheckUpdateListener?.invoke()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}