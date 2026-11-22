package ru.school28.diary.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.MainActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.ScheduleLesson
import ru.school28.diary.api.SchoolSchedule
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassLargeHeader
import ru.school28.diary.glass.GlassSegmented
import ru.school28.diary.glass.GlassSkeletonCard
import ru.school28.diary.widgets.WidgetSync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class DayTab(val key: String, val short: String, val full: String)

private val SCHEDULE_DAYS = listOf(
    DayTab("mon", "Пн", "Понедельник"),
    DayTab("tue", "Вт", "Вторник"),
    DayTab("wed", "Ср", "Среда"),
    DayTab("thu", "Чт", "Четверг"),
    DayTab("fri", "Пт", "Пятница"),
    DayTab("sat", "Сб", "Суббота"),
)

private fun scheduleTodayKey(): String = when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
    Calendar.MONDAY -> "mon"
    Calendar.TUESDAY -> "tue"
    Calendar.WEDNESDAY -> "wed"
    Calendar.THURSDAY -> "thu"
    Calendar.FRIDAY -> "fri"
    Calendar.SATURDAY -> "sat"
    else -> "mon"
}

@Composable
fun ScheduleScreen(activity: MainActivity?) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()

    var schedule by remember { mutableStateOf<SchoolSchedule?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dayKey by remember { mutableStateOf(scheduleTodayKey()) }
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

    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale("ru")) }
    val selectedIndex = SCHEDULE_DAYS.indexOfFirst { it.key == dayKey }.coerceAtLeast(0)
    val selectedDay = SCHEDULE_DAYS[selectedIndex]

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 132.dp),
    ) {
        item(key = "header") {
            GlassLargeHeader(
                title = "Расписание",
                subtitle = if (className == null) "Класс не выбран" else "Класс $className · ${selectedDay.full}",
                trailing = {
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Обновить",
                        onClick = { load() },
                        size = 42.dp,
                    )
                },
            )
        }

        item(key = "class") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GlassBadge(
                    text = if (className == null) "класс не выбран" else "класс $className",
                    tint = colors.accent,
                )
                offlineAt?.let { ts ->
                    GlassBadge(
                        text = "офлайн · ${timeSdf.format(Date(ts))}",
                        tint = colors.warning,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "сменить в настройках",
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                )
            }
        }

        item(key = "days") {
            GlassSegmented(
                options = SCHEDULE_DAYS.map { it.short },
                selectedIndex = selectedIndex,
                onSelect = { dayKey = SCHEDULE_DAYS[it].key },
            )
        }

        item(key = "content") {
            Box(Modifier.padding(top = 12.dp)) {
                when {
                    loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        repeat(4) {
                            GlassSkeletonCard(height = 78.dp, lines = 2)
                        }
                    }

                    error != null -> GlassCard {
                        GlassErrorState(message = error, onRetry = { load() })
                    }

                    className == null -> GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.CalendarMonth,
                            title = "Класс не выбран",
                            message = "Открой настройки и выбери свой класс — покажем актуальное расписание с сайта школы.",
                        )
                    }

                    else -> {
                        val sch = schedule
                        val lessons = sch?.lessonsFor(className!!, dayKey).orEmpty()
                        val bells = sch?.bellsFor(dayKey).orEmpty()
                        if (lessons.isEmpty()) {
                            GlassCard {
                                GlassEmptyState(
                                    icon = Icons.Rounded.EventBusy,
                                    title = "На ${selectedDay.full.lowercase(Locale("ru"))} уроков нет",
                                    message = "Выбери другой день или обнови расписание.",
                                )
                            }
                        } else {
                            AnimatedContent(
                                targetState = dayKey,
                                transitionSpec = {
                                    (slideInVertically(tween(300)) { it / 8 } + fadeIn(tween(220)))
                                        .togetherWith(slideOutVertically(tween(200)) { -it / 10 } + fadeOut(tween(140)))
                                },
                                label = "dayContent",
                            ) { key ->
                                GlassCard(contentPadding = PaddingValues(0.dp)) {
                                    val dayLessons = sch?.lessonsFor(className!!, key).orEmpty()
                                    val dayBells = sch?.bellsFor(key).orEmpty()
                                    dayLessons.forEachIndexed { index, lesson ->
                                        val bell = dayBells.firstOrNull { it.n == lesson.n }
                                        val isNow = key == scheduleTodayKey() && isLessonNow(bell?.start, bell?.end)
                                        LessonRow(
                                            lesson = lesson,
                                            start = bell?.start,
                                            end = bell?.end,
                                            isNow = isNow,
                                        )
                                        if (index != dayLessons.lastIndex) {
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 72.dp, end = 16.dp)
                                                    .height(1.dp)
                                                    .background(colors.separator)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isLessonNow(start: String?, end: String?): Boolean {
    if (start == null || end == null) return false
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val startMinutes = parseMinutes(start) ?: return false
    val endMinutes = parseMinutes(end) ?: return false
    return nowMinutes in startMinutes..endMinutes
}

private fun parseMinutes(value: String): Int? {
    val parts = value.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].trim().toIntOrNull() ?: return null
    val minute = parts[1].trim().toIntOrNull() ?: return null
    return hour * 60 + minute
}

@Composable
private fun LessonRow(
    lesson: ScheduleLesson,
    start: String?,
    end: String?,
    isNow: Boolean,
) {
    val colors = Glass.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (isNow) Modifier.background(colors.accent.copy(alpha = 0.09f)) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(
                    if (isNow) {
                        Brush.linearGradient(listOf(colors.accent, colors.accentSecondary))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                colors.accent.copy(alpha = 0.16f),
                                colors.accentSecondary.copy(alpha = 0.08f),
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = lesson.n.toString(),
                color = if (isNow) colors.onAccent else colors.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = lesson.subject,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isNow) {
                    Spacer(Modifier.width(7.dp))
                    GlassBadge("сейчас", tint = colors.success)
                }
            }
            val details = listOfNotNull(
                lesson.teacher?.takeIf { it.isNotBlank() },
                lesson.room?.takeIf { it.isNotBlank() }?.let { "каб. $it" },
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Text(
                    text = details,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (start != null || end != null) {
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                if (start != null) {
                    Text(
                        text = start,
                        color = if (isNow) colors.accent else colors.text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (end != null) {
                    Text(
                        text = end,
                        color = colors.textTertiary,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}
