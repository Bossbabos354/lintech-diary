package ru.school28.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.school28.diary.AppLog
import ru.school28.diary.Repo
import ru.school28.diary.Settings
import ru.school28.diary.Util
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.ScheduleLesson
import ru.school28.diary.api.SchoolSchedule

private data class DayTab(val key: String, val short: String, val full: String)

private val DAYS = listOf(
    DayTab("mon", "Пн", "понедельник"),
    DayTab("tue", "Вт", "вторник"),
    DayTab("wed", "Ср", "среду"),
    DayTab("thu", "Чт", "четверг"),
    DayTab("fri", "Пт", "пятницу"),
    DayTab("sat", "Сб", "субботу"),
)

@Composable
fun ScheduleScreen() {
    val scope = rememberCoroutineScope()
    var schedule by remember { mutableStateOf<SchoolSchedule?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dayKey by remember { mutableStateOf(Util.todayKey()) }
    var className by remember { mutableStateOf(Settings.selectedClass) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            val loaded = Repo.schedule()
            schedule = loaded
            offlineAt = ApiClient.offlineAt("schedule")
            if (Settings.selectedClass == null && !loaded?.classes.isNullOrEmpty()) {
                className = loaded?.classes?.first()
                className?.let { Settings.selectedClass = it }
            }
            if (loaded == null) error = "Сайт с расписанием недоступен"
        } catch (e: Exception) {
            AppLog.logException("schedule", e)
            error = e.message
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Расписание", "Класс: ${className ?: "не выбран (сменить в настройках)"}")
        OfflineLabel(offlineAt)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DAYS.forEach { d ->
                Chip(d.short, selected = d.key == dayKey, modifier = Modifier.weight(1f)) { dayKey = d.key }
            }
        }

        when {
            loading -> LoadingBox()
            error != null && schedule == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error ?: "", color = AppTheme.colors.error)
            }
            className == null -> EmptyBox("Выбери класс в настройках")
            else -> {
                val lessons = schedule?.lessonsFor(className!!, dayKey).orEmpty()
                val bells = schedule?.bellsFor(dayKey).orEmpty()
                val day = DAYS.firstOrNull { it.key == dayKey }?.full ?: ""
                if (lessons.isEmpty()) {
                    EmptyBox("На $day уроков нет")
                } else {
                    SmoothLazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp,
                        ),
                    ) {
                        items(lessons, key = { it.n }) { lesson ->
                            val bell = bells.firstOrNull { it.n == lesson.n }
                            LessonRow(lesson, bell?.start, bell?.end)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: ScheduleLesson, start: String?, end: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                lesson.n.toString(),
                color = AppTheme.colors.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
            )
            if (start != null) {
                Text(start, color = AppTheme.colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            if (end != null) {
                Text(end, color = AppTheme.colors.textDisabled, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(lesson.subject, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            val details = listOfNotNull(
                lesson.teacher?.takeIf { it.isNotBlank() },
                lesson.room?.takeIf { it.isNotBlank() }?.let { "каб. $it" },
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Text(details, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}
