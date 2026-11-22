package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateUiIn()

        val savedLogin = Prefs.getLogin(this)
        val savedPassword = Prefs.getPassword(this)
        if (savedLogin != null) {
            binding.etLogin.setText(savedLogin)
            binding.etPassword.setText(savedPassword)
        }

        binding.btnLogin.setOnClickListener {
            val login = binding.etLogin.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (login.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            doLogin(login, password)
        }
    }

    private fun animateUiIn() {
        val targets = listOf(
            binding.tilLogin, binding.tilPassword, binding.btnLogin
        ).filterNotNull()

        var delay = 100L
        for (v in targets) {
            v.alpha = 0f
            v.translationY = dp(28f)
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(450)
                .setStartDelay(delay)
                .start()
            delay += 120
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun doLogin(login: String, password: String) {
        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val personInfo = withContext(Dispatchers.IO) {
                    ApiClient.login(login, password)
                }
                if (personInfo == null) {
                    Toast.makeText(this@LoginActivity, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                    binding.btnLogin.isEnabled = true
                    binding.progressBar.visibility = android.view.View.GONE
                    return@launch
                }

                Prefs.saveCredentials(this@LoginActivity, login, password, personInfo.studentId)
                Prefs.saveUserId(this@LoginActivity, personInfo.userId)
                Prefs.setUchId(this@LoginActivity, personInfo.uchId)
                Prefs.setUchName(this@LoginActivity, personInfo.uchName)

                val year = withContext(Dispatchers.IO) {
                    ApiClient.getUchYear(personInfo.studentId)
                }
                Prefs.setUchYear(this@LoginActivity, year)

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                binding.btnLogin.isEnabled = true
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
}