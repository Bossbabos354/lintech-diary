package ru.school28.diary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import ru.school28.diary.api.ApiClient
import ru.school28.diary.ui.AppTheme
import ru.school28.diary.ui.AuroraBackground
import ru.school28.diary.ui.glass
import ru.school28.diary.ui.DiaryScreen
import ru.school28.diary.ui.DiaryTheme
import ru.school28.diary.ui.EventsScreen
import ru.school28.diary.ui.GoalOverlay
import ru.school28.diary.ui.GoalsScreen
import ru.school28.diary.ui.CriticalOverlay
import ru.school28.diary.ui.LessonOverlay
import ru.school28.diary.ui.LoginScreen
import ru.school28.diary.ui.MarksScreen
import ru.school28.diary.ui.ScheduleScreen
import ru.school28.diary.ui.SearchOverlay
import ru.school28.diary.ui.SettingsScreen
import ru.school28.diary.ui.ToastLine
import ru.school28.diary.ui.WeeklyOverlay
import ru.school28.diary.ui.gradientHeader

fun main() {
    AppPaths.ensureDirs()
    AppLog.log("=== APP START ${BuildInfo.VERSION_NAME} ===")
    Settings.init()
    ApiClient.init(AppPaths.cacheDir)
    Settings.login?.let { login ->
        Settings.password?.let { password ->
            ApiClient.restoreSession(login, password, Settings.userId)
        }
    }

    application {
        val state = remember { AppState() }
        state.exitApp = { exitApplication() }
        val windowState = rememberWindowState(
            width = 1240.dp,
            height = 860.dp,
            position = WindowPosition(Alignment.Center),
        )

        Window(
            onCloseRequest = { state.requestClose() },
            state = windowState,
            visible = state.windowVisible,
            title = "ЛинТех 28 — Дневник",
        ) {
            LaunchedEffect(Unit) {
                state.trayAvailable = Tray.install(
                    onOpen = { state.windowVisible = true },
                    onCheckNow = { Background.checkNow() },
                    onExit = state.exitApp,
                )
                Background.start()
            }
            DiaryTheme(dark = state.isDark()) {
                AppRoot(state)
            }
        }
    }
}

@Composable
private fun AppRoot(state: AppState) {
    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        if (state.loggedIn) {
            MainShell(state)
        } else {
            LoginScreen(state)
        }

        if (state.searchOpen) {
            SearchOverlay(
                onClose = { state.searchOpen = false },
                onPick = { state.searchOpen = false; state.lesson = it },
            )
        }
        state.lesson?.let { entry ->
            LessonOverlay(entry) { state.lesson = null }
        }
        state.goal?.let { target ->
            GoalOverlay(
                subject = target.subject,
                initialPeriodKey = target.periodKey,
                onClose = { state.goal = null },
                onSaved = { state.goalsVersion++ },
            )
        }
        if (state.weekly) {
            WeeklyOverlay { state.weekly = false }
        }
        if (state.critical) {
            CriticalOverlay(
                onClose = { state.critical = false },
                onPick = { subject ->
                    state.critical = false
                    state.goal = GoalTarget(subject, "YEAR")
                },
            )
        }
        state.toast?.let { text ->
            ToastLine(text) { state.toast = null }
        }
    }
}

@Composable
private fun MainShell(state: AppState) {
    Column(Modifier.fillMaxSize()) {
        TopBar(state)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (state.tab) {
                0 -> DiaryScreen(state)
                1 -> ScheduleScreen()
                2 -> MarksScreen(state)
                3 -> GoalsScreen(state)
                4 -> EventsScreen()
                else -> SettingsScreen(state)
            }
        }
        BottomNav(state)
    }
}

private data class TabItem(val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabItem("Дневник", Icons.Filled.MenuBook),
    TabItem("Расписание", Icons.Filled.CalendarMonth),
    TabItem("Оценки", Icons.Filled.Grade),
    TabItem("Цели", Icons.Filled.Flag),
    TabItem("События", Icons.Filled.Event),
    TabItem("Настройки", Icons.Filled.Settings),
)

@Composable
private fun TopBar(state: AppState) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(gradientHeader()),
            contentAlignment = Alignment.Center,
        ) {
            Text("Л", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("ЛинТех 28", color = AppTheme.colors.text, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Text("электронный дневник", color = AppTheme.colors.textSecondary, fontSize = 11.sp)
        }
        Spacer(Modifier.weight(1f))
        Icon(
            Icons.Filled.Search,
            contentDescription = "Поиск",
            tint = AppTheme.colors.textSecondary,
            modifier = Modifier.size(22.dp).clip(RoundedCornerShape(10.dp)).clickable { state.searchOpen = true },
        )
        Spacer(Modifier.width(14.dp))
        Text("v${BuildInfo.VERSION_NAME}", color = AppTheme.colors.textDisabled, fontSize = 11.sp)
    }
}

@Composable
private fun BottomNav(state: AppState) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .widthIn(max = 860.dp)
                .fillMaxWidth()
                .glass(RoundedCornerShape(26.dp), AppTheme.colors)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TABS.forEachIndexed { index, item ->
                val selected = state.tab == index
                Row(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) AppTheme.colors.primary else Color.Transparent)
                        .clickable { state.tab = index }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        item.label,
                        color = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}
