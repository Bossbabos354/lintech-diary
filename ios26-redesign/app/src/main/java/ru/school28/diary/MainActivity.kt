package ru.school28.diary

import android.app.AlertDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.AuroraBackground
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassTabBar
import ru.school28.diary.glass.GlassTabItem
import ru.school28.diary.glass.GlassTopBar
import ru.school28.diary.glass.PullWordmark
import ru.school28.diary.ui.screens.CriticalSubjectsScreen
import ru.school28.diary.ui.screens.DairyScreen
import ru.school28.diary.ui.screens.DaySummaryScreen
import ru.school28.diary.ui.screens.EventsScreen
import ru.school28.diary.ui.screens.GoalSheet
import ru.school28.diary.ui.screens.GoalsScreen
import ru.school28.diary.ui.screens.HomeScreen
import ru.school28.diary.ui.screens.LessonSheet
import ru.school28.diary.ui.screens.MarksScreen
import ru.school28.diary.ui.screens.ScheduleScreen
import ru.school28.diary.ui.screens.SettingsScreen
import ru.school28.diary.ui.screens.SubjectMarksScreen

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_TAB = "extra_tab"
        const val EXTRA_CHECK_UPDATE = "extra_check_update"
    }

    private var updateChecked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        enableGlassEdgeToEdge()
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
        val ctx = LocalContext.current
        val colors = Glass.colors
        val lifecycleOwner = LocalLifecycleOwner.current

        var tab by remember { mutableIntStateOf(initialTab.coerceIn(0, 4)) }
        var settingsOpen by remember { mutableStateOf(initialTab == 5) }
        var unread by remember { mutableIntStateOf(0) }

        var expandedLesson by remember { mutableStateOf<DairyEntry?>(null) }
        var goalSubject by remember { mutableStateOf<SubjectMarks?>(null) }
        var goalPeriodKey by remember { mutableStateOf<String?>(null) }
        var goalsVersion by remember { mutableIntStateOf(0) }
        var showCritical by remember { mutableStateOf(false) }
        var showWeekly by remember { mutableStateOf(false) }
        var showDay by remember { mutableStateOf(false) }
        var focusEntry by remember { mutableStateOf<DairyEntry?>(null) }
        var focusVersion by remember { mutableIntStateOf(0) }
        var marksSubject by remember { mutableStateOf<SubjectMarks?>(null) }
        // Протяжка дневника: 0 — свёрнуто, 1 — обновление запущено.
        // Прогресс приходит покадрово из DairyScreen, своей анимации не нужно.
        var pullProgress by remember { mutableFloatStateOf(0f) }
        // Главная открывается при старте; из неё уходим в разделы кружками.
        var showHome by remember { mutableStateOf(true) }
        val homeName = remember { Prefs.getFio(ctx).orEmpty() }
        val badges = remember(unread) {
            NotificationStore.load(ctx).filter { !it.read }.groupBy { it.tab }.mapValues { it.value.size }
        }

        // Сохраняем состояние экранов (скролл, фильтры) между переключениями вкладок.
        val screenState = rememberSaveableStateHolder()

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    unread = NotificationStore.unreadCount(ctx)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        Box(Modifier.fillMaxSize()) {
            AuroraBackground()

            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                if (!showHome) {
                // Полоса, которая открывается, когда панель уезжает вниз.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp * pullProgress)
                        .clipToBounds()
                ) {
                    PullWordmark(progress = pullProgress, modifier = Modifier.fillMaxSize())
                }
                GlassTopBar(
                    onSearch = {
                        startActivity(android.content.Intent(this@MainActivity, SearchActivity::class.java))
                    },
                    onNotifications = {
                        startActivity(android.content.Intent(this@MainActivity, NotificationsActivity::class.java))
                    },
                    onSettings = { settingsOpen = true },
                    unread = unread,
                    onHome = { showHome = true },
                )
                }

                Box(Modifier.weight(1f)) {
                    if (showHome) {
                        HomeScreen(
                            name = homeName,
                            badges = badges,
                            onOpen = {
                                tab = it
                                showHome = false
                            },
                        )
                    } else {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val direction = if (targetState > initialState) 1 else -1
                            (
                                slideInHorizontally(
                                    tween(330, easing = FastOutSlowInEasing)
                                ) { it / 6 * direction } +
                                    fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                                    scaleIn(tween(330, easing = FastOutSlowInEasing), initialScale = 0.975f)
                                ).togetherWith(
                                slideOutHorizontally(
                                    tween(240, easing = FastOutLinearInEasing)
                                ) { -it / 9 * direction } +
                                    fadeOut(tween(170, easing = FastOutLinearInEasing)) +
                                    scaleOut(tween(240, easing = FastOutLinearInEasing), targetScale = 0.99f)
                            )
                        },
                        label = "tab",
                    ) { current ->
                        screenState.SaveableStateProvider(current) {
                            when (current) {
                                0 -> DairyScreen(
                                    activity = this@MainActivity,
                                    onLessonClick = { expandedLesson = it },
                                    onPull = { pullProgress = it },
                                    focusEntry = focusEntry,
                                    focusVersion = focusVersion,
                                    onFocusConsumed = { focusEntry = null },
                                )
                                1 -> ScheduleScreen(this@MainActivity)
                                2 -> MarksScreen(
                                    activity = this@MainActivity,
                                    onSubjectClick = { marksSubject = it },
                                    onOpenWeekly = { showWeekly = true },
                                    onOpenDay = { showDay = true },
                                )
                                3 -> GoalsScreen(
                                    activity = this@MainActivity,
                                    refreshKey = goalsVersion,
                                    onGoalClick = { subject, periodKey ->
                                        goalPeriodKey = periodKey
                                        goalSubject = subject
                                    },
                                    onOpenCritical = { showCritical = true },
                                )
                                else -> EventsScreen(this@MainActivity)
                            }
                        }
                    }
                    }
                }
            }

            AnimatedVisibility(
                visible = !settingsOpen && !showHome,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    colors.background.copy(alpha = 0.88f),
                                    colors.background,
                                )
                            )
                        )
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 34.dp, bottom = 10.dp)
                ) {
                    GlassTabBar(
                        items = listOf(
                            GlassTabItem("Дневник", Icons.Rounded.MenuBook),
                            GlassTabItem("Расписание", Icons.Rounded.CalendarMonth),
                            GlassTabItem("Оценки", Icons.Rounded.Grade),
                            GlassTabItem("Цели", Icons.Rounded.Flag),
                            GlassTabItem("События", Icons.Rounded.Event),
                        ),
                        selectedIndex = tab,
                        onSelect = { tab = it },
                    )
                }
            }

            AnimatedVisibility(
                visible = settingsOpen,
                enter = screenEnter(),
                exit = screenExit(),
            ) {
                SettingsScreen(
                    activity = this@MainActivity,
                    onBack = { settingsOpen = false },
                    onCheckUpdate = { checkForUpdates(showIfNone = true) },
                )
            }

            AnimatedVisibility(
                visible = showDay,
                enter = screenEnter(),
                exit = screenExit(),
            ) {
                DaySummaryScreen(
                    onBack = { showDay = false },
                    onOpenLessonDetails = { entry -> expandedLesson = entry },
                )
            }

            AnimatedVisibility(
                visible = showWeekly,
                enter = screenEnter(),
                exit = screenExit(),
            ) {
                ru.school28.diary.ui.screens.WeeklySummaryScreen(
                    onBack = { showWeekly = false },
                    onOpenLesson = { entry ->
                        showWeekly = false
                        focusEntry = entry
                        focusVersion++
                        tab = 0
                    },
                )
            }

            AnimatedVisibility(
                visible = showCritical,
                enter = screenEnter(),
                exit = screenExit(),
            ) {
                CriticalSubjectsScreen(
                    onBack = { showCritical = false },
                    onPick = { subject ->
                        showCritical = false
                        goalPeriodKey = "YEAR"
                        goalSubject = subject
                    },
                )
            }

            expandedLesson?.let { entry ->
                LessonSheet(entry = entry, onDismiss = { expandedLesson = null })
            }

            val subjectScreen = marksSubject
            var lastSubject by remember { mutableStateOf<SubjectMarks?>(null) }
            LaunchedEffect(subjectScreen) {
                if (subjectScreen != null) lastSubject = subjectScreen
            }
            AnimatedVisibility(
                visible = subjectScreen != null,
                enter = screenEnter(),
                exit = screenExit(),
            ) {
                lastSubject?.let { subject ->
                    Box(Modifier.fillMaxSize()) {
                        SubjectMarksScreen(
                            subject = subject,
                            onBack = { marksSubject = null },
                            onOpenGoal = { picked ->
                                marksSubject = null
                                goalPeriodKey = null
                                goalSubject = picked
                            },
                        )
                    }
                }
            }

            goalSubject?.let { subject ->
                GoalSheet(
                    subject = subject,
                    initialPeriodKey = goalPeriodKey,
                    onDismiss = {
                        goalSubject = null
                        goalPeriodKey = null
                        goalsVersion++
                    },
                )
            }
        }

        LaunchedEffect(Unit) {
            unread = NotificationStore.unreadCount(ctx)
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

/** Единый вход экрана: мягкий слайд справа и проявление. */
private fun screenEnter(): EnterTransition =
    slideInHorizontally(tween(330, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(220, easing = FastOutSlowInEasing))

/** Выход экрана: короткий слайд и затухание. */
private fun screenExit(): ExitTransition =
    slideOutHorizontally(tween(260, easing = FastOutLinearInEasing)) { it / 4 } +
        fadeOut(tween(170, easing = FastOutLinearInEasing))
