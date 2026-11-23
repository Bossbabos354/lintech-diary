package ru.school28.diary.legacy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.activity.ComponentActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.ScheduleLesson
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import ru.school28.diary.widgets.WidgetSync
import java.util.Calendar

private data class DayTab(val key: String, val short: String, val full: String)

private val DAYS = listOf(
    DayTab("mon", "Пн", "Понедельник"),
    DayTab("tue", "Вт", "Вторник"),
    DayTab("wed", "Ср", "Среда"),
    DayTab("thu", "Чт", "Четверг"),
    DayTab("fri", "Пт", "Пятница"),
    DayTab("sat", "Сб", "Суббота")
)

private fun todayKey(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "mon"
    Calendar.TUESDAY -> "tue"
    Calendar.WEDNESDAY -> "wed"
    Calendar.THURSDAY -> "thu"
    Calendar.FRIDAY -> "fri"
    Calendar.SATURDAY -> "sat"
    else -> "mon"
}

@Composable
fun ScheduleScreen(activity: ComponentActivity?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var schedule by remember { mutableStateOf<SchoolSchedule?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dayKey by remember { mutableStateOf(todayKey()) }
    var className by remember { mutableStateOf(Prefs.getSelectedClass(ctx)) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val loaded = withContext(Dispatchers.IO) { ApiClient.getSchedule() }
                schedule = loaded
                offlineAt = ApiClient.offlineAt("schedule")
                WidgetSync.request(ctx)
                if (loaded == null) error = "Сайт с расписанием недоступен"
                // Если класс не выбран — берём первый из списка
                if (Prefs.getSelectedClass(ctx) == null && !loaded?.classes.isNullOrEmpty()) {
                    className = loaded!!.classes.first()
                    Prefs.setSelectedClass(ctx, className!!)
                }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        // Класс и день
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Класс: ${className ?: "не выбран"}",
                color = AppTheme.colors.text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "сменить в настройках",
                color = AppTheme.colors.textSecondary,
                fontSize = 11.sp
            )
        }

        offlineAt?.let { ts ->
            Text(
                "офлайн · данные от " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ts)),
                color = AppTheme.colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DAYS.forEach { d ->
                val selected = d.key == dayKey
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AppTheme.colors.primary else AppTheme.colors.surface)
                        .clickable { dayKey = d.key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        d.short,
                        color = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = AppTheme.colors.error)
            }
            className == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Выбери класс в настройках", color = AppTheme.colors.textSecondary)
            }
            else -> {
                val sch = schedule
                val lessons = sch?.lessonsFor(className!!, dayKey).orEmpty()
                val bells = sch?.bellsFor(dayKey).orEmpty()
                val fullDay = DAYS.firstOrNull { it.key == dayKey }?.full ?: ""

                if (lessons.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("На $fullDay уроков нет", color = AppTheme.colors.textSecondary)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
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
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Номер и время
        Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                lesson.n.toString(),
                color = AppTheme.colors.primary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
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
                lesson.room?.takeIf { it.isNotBlank() }?.let { "каб. $it" }
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Text(details, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
            }
        }
    }
}