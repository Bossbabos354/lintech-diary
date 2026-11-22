package ru.school28.diary

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import ru.school28.diary.api.ApiClient
import ru.school28.diary.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvLoadingText.alpha = 0f
        binding.progress.alpha = 0f

        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvLoadingText.animate().alpha(1f).setDuration(500).start()
            binding.progress.animate().alpha(1f).setDuration(600).start()
        }, 400)

        Handler(Looper.getMainLooper()).postDelayed({
            goNext()
        }, 2600)
    }

    private fun goNext() {
        val studentId = Prefs.getStudentId(this)
        val login = Prefs.getLogin(this)
        val password = Prefs.getPassword(this)

        val target = if (studentId != -1 && login != null && password != null) {
            ApiClient.restoreSession(login, password)
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        target.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        binding.root.animate()
            .alpha(0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                startActivity(target)
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            .start()
    }
}