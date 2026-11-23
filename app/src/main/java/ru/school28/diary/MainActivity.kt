        package ru.school28.diary

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.screens.DairyScreen
import ru.school28.diary.ui.screens.EventsScreen
import ru.school28.diary.ui.screens.CriticalSubjectsScreen
import ru.school28.diary.ui.screens.WeeklySummaryScreen
import ru.school28.diary.ui.screens.GoalSheet
import ru.school28.diary.ui.screens.GoalsScreen
import ru.school28.diary.ui.screens.LessonSheet
import ru.school28.diary.ui.screens.MarksScreen
import ru.school28.diary.ui.screens.ScheduleScreen
import ru.school28.diary.ui.screens.SettingsScreen
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.gradientHeader

class MainActivity : ComponentActivity() {

    companion object {
        /** Вкладка, которую открыть при запуске (0..3). */
        const val EXTRA_TAB = "extra_tab"
        /** Принудительно проверить обновления (из уведомления). */
        const val EXTRA_CHECK_UPDATE = "extra_check_update"
    }

    private var updateChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        super.onCreate(savedInstanceState)

        if (Prefs.getStudentId(this) == -1) {
            finish()
            return
        }

        val initialTab = intent?.getIntExtra(EXTRA_TAB, 0) ?: 0
        val forceCheckUpdate = intent?.getBooleanExtra(EXTRA_CHECK_UPDATE, false) ?: false

        setContent {
            DiaryTheme {
                MainScreen(initialTab = initialTab, forceCheckUpdate = forceCheckUpdate)
            }
        }
    }

    @Composable
    fun MainScreen(initialTab: Int = 0, forceCheckUpdate: Boolean = false) {
        var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 5)) }
        var expandedLesson by remember { mutableStateOf<DairyEntry?>(null) }
        var goalSubject by remember { mutableStateOf<SubjectMarks?>(null) }
        var goalPeriodKey by remember { mutableStateOf<String?>(null) }
        var goalsVersion by remember { mutableIntStateOf(0) }
        var showCritical by remember { mutableStateOf(false) }
        var showWeekly by remember { mutableStateOf(false) }
        val prevTab = remember { arrayOf(initialTab.coerceIn(0, 5)) }

        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Мягкая градиентная шапка
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(gradientHeader())
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ЛинТех 28", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Поиск",
                            tint = Color.White,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    startActivity(android.content.Intent(this@MainActivity, SearchActivity::class.java))
                                }
                        )
                    }
                }

                // Контент вкладки с анимацией перехода
                Box(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val forward = targetState > initialState
                            val dir = if (forward) 1 else -1
                            (slideInHorizontally { it * dir } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally { -it * dir } + fadeOut(tween(150)))
                        },
                        label = "tab"
                    ) { currentTab ->
                        when (currentTab) {
                            0 -> DairyScreen(this@MainActivity, onLessonClick = { expandedLesson = it })
                            1 -> ScheduleScreen(this@MainActivity)
                            2 -> MarksScreen(
                                activity = this@MainActivity,
                                onSubjectClick = {
                                    goalPeriodKey = null
                                    goalSubject = it
                                },
                                onOpenWeekly = { showWeekly = true }
                            )
                            3 -> GoalsScreen(
                                activity = this@MainActivity,
                                refreshKey = goalsVersion,
                                onGoalClick = { subject, periodKey ->
                                    goalPeriodKey = periodKey
                                    goalSubject = subject
                                },
                                onOpenCritical = { showCritical = true }
                            )
                            4 -> EventsScreen(this@MainActivity)
                            5 -> SettingsScreen(this@MainActivity, onCheckUpdate = { checkForUpdates(showIfNone = true) })
                        }
                    }
                }

                PlayfulNavBar(
                    tabs = listOf(
                        NavTab("Дневник", Icons.Filled.MenuBook),
                        NavTab("Расписание", Icons.Filled.CalendarMonth),
                        NavTab("Оценки", Icons.Filled.Grade),
                        NavTab("Цели", Icons.Filled.Flag),
                        NavTab("События", Icons.Filled.Event),
                        NavTab("Настройки", Icons.Filled.Settings)
                    ),
                    selected = tab,
                    onSelect = {
                        prevTab[0] = tab
                        tab = it
                    }
                )
            }

            if (showWeekly) {
                WeeklySummaryScreen(onBack = { showWeekly = false })
            }

            if (showCritical) {
                CriticalSubjectsScreen(
                    onBack = { showCritical = false },
                    onPick = { subject ->
                        showCritical = false
                        goalPeriodKey = "YEAR"
                        goalSubject = subject
                    }
                )
            }

            expandedLesson?.let { entry ->
                LessonSheet(entry = entry, onDismiss = { expandedLesson = null })
            }

            // Полноэкранная карточка цели
            goalSubject?.let { subject ->
                GoalSheet(
                    subject = subject,
                    initialPeriodKey = goalPeriodKey,
                    onDismiss = {
                        goalSubject = null
                        goalPeriodKey = null
                        goalsVersion++
                    }
                )
            }
        }

        // Проверка обновлений при старте.
        // forceCheckUpdate = true приходит из уведомления «Доступно обновление» —
        // тогда проверяем даже если авто-проверка выключена в настройках.
        androidx.compose.runtime.LaunchedEffect(Unit) {
            if (forceCheckUpdate) {
                checkForUpdates(showIfNone = false, force = true)
            } else if (Prefs.isCheckUpdatesEnabled(this@MainActivity)) {
                checkForUpdates()
            }
        }
    }

    private fun checkForUpdates(showIfNone: Boolean = false, force: Boolean = false) {
        if (updateChecked && !showIfNone && !force) return
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
            .setPositiveButton("Обновить") { _, _ -> startDownload(update) }
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
}

data class NavTab(val label: String, val icon: ImageVector)

@Composable
fun PlayfulNavBar(
    tabs: List<NavTab>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selected == index
                // Плавное "всплывание" активной таб-пилюли
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.06f else 0.96f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
                    label = "scale"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(if (isSelected) AppTheme.colors.primary else AppTheme.colors.transparent)
                        .clickable { onSelect(index) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1f else 0.88f,
                        animationSpec = spring(stiffness = 400f),
                        label = "iconScale"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.75f)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
                            modifier = Modifier
                                .size(24.dp)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            tab.label,
                            color = if (isSelected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}