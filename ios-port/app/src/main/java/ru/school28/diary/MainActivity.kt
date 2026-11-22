package ru.school28.diary

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import ru.school28.diary.databinding.ActivityMainBinding
import ru.school28.diary.ui.MainPagerAdapter
import ru.school28.diary.ui.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pagerAdapter: MainPagerAdapter
    private var updateChecked = false

    companion object {
        const val TAB_DAIRY = 0
        const val TAB_MARKS = 1
        const val TAB_SETTINGS = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.toolbar.alpha = 0f
        binding.viewPager.alpha = 0f
        binding.tabLayout.alpha = 0f
        binding.viewPager.animate().alpha(1f).setDuration(500).start()
        binding.toolbar.animate().alpha(1f).setDuration(400).start()
        binding.tabLayout.animate().alpha(1f).setDuration(600).start()

        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.offscreenPageLimit = 2

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                TAB_DAIRY -> tab.text = "Дневник"
                TAB_MARKS -> tab.text = "Оценки"
                TAB_SETTINGS -> tab.text = "Настройки"
            }
        }.attach()

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    TAB_DAIRY -> (pagerAdapter.fragments[TAB_DAIRY] as? DairyFragment)?.loadDairy()
                    TAB_MARKS -> (pagerAdapter.fragments[TAB_MARKS] as? MarksFragment)?.loadMarks()
                }
            }
        })

        if (Prefs.getStudentId(this) == -1) {
            finish()
            return
        }

        (pagerAdapter.fragments[TAB_SETTINGS] as? SettingsFragment)?.apply {
            setOnLogoutListener {
                Prefs.clearCredentials(this@MainActivity)
                val intent = android.content.Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            setOnCheckUpdateListener { checkForUpdates(showIfNone = true) }
        }

        binding.viewPager.post {
            (pagerAdapter.fragments[TAB_DAIRY] as? DairyFragment)?.loadDairy()
        }

        binding.viewPager.postDelayed({
            checkForUpdates()
        }, 3000)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkForUpdates(showIfNone: Boolean = false) {
        updateChecked = true

        lifecycleScope.launch {
            val result = try {
                UpdateChecker.check()
            } catch (e: Exception) {
                UpdateChecker.CheckResult(null, e.message ?: "Неизвестная ошибка")
            }
            if (result.update != null) {
                showUpdateDialog(result.update)
            } else if (showIfNone) {
                val msg = result.error ?: "Обновлений нет. У вас актуальная версия"
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showUpdateDialog(update: UpdateInfo) {
        val message = buildString {
            append("Доступна новая версия: ")
            append(update.versionName)
            if (update.notes.isNotBlank()) {
                append("\n\nЧто нового:\n")
                append(update.notes)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Обновление")
            .setMessage(message)
            .setPositiveButton("Обновить") { _, _ ->
                startDownload(update)
            }
            .setNegativeButton("Позже", null)
            .create()
        dialog.show()
    }

    private fun startDownload(update: UpdateInfo) {
        val progress = AlertDialog.Builder(this)
            .setTitle("Скачивание обновления")
            .setMessage("Загружаю версию ${update.versionName}...")
            .setCancelable(false)
            .create()
        progress.show()

        lifecycleScope.launch {
            try {
                val file = UpdateChecker.download(update.apkUrl, this@MainActivity)
                progress.dismiss()
                UpdateChecker.install(this@MainActivity, file)
            } catch (e: Exception) {
                progress.dismiss()
                Toast.makeText(this@MainActivity, "Не удалось скачать обновление: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}