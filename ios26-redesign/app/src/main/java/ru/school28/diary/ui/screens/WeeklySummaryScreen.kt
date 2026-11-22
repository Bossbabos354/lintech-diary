package ru.school28.diary.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.AnimatedNumberText
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassProgressRing
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassSpinner
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.glass.SectionLabel
import ru.school28.diary.ui.GoalMath
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun WeeklySummaryScreen(
    onBack: () -> Unit,
    onOpenLesson: (DairyEntry) -> Unit = {},
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var weekMarks by remember { mutableStateOf<List<Int>>(emptyList()) }
    var weekRated by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var absences by remember { mutableStateOf(0) }
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var period by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val studentId = Prefs.getStudentId(ctx)
            val uchYear = Prefs.getUchYear(ctx)
            if (studentId != -1) {
                var classId = Prefs.getClassId(ctx)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) { ApiClient.getStudentClass(studentId, uchYear) }
                    Prefs.setClassId(ctx, classId)
                }

                val cal = Calendar.getInstance()
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                cal.add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val monday = cal.time
                val fmt = SimpleDateFormat("d MMM", Locale("ru"))
                cal.add(Calendar.DAY_OF_YEAR, 6)
                period = fmt.format(monday) + " — " + fmt.format(cal.time)
                val sunday = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                }.time

                val week: List<DairyEntry> = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, monday, sunday)
                }
                weekMarks = week.mapNotNull { it.mark.removePrefix("!").trim().toIntOrNull() }
                weekRated = week
                    .filter { it.mark.removePrefix("!").trim().toIntOrNull() != null }
                    .sortedWith(compareBy({ it.date }, { it.lessonNumber }))
                absences = week.count { entry ->
                    val m = entry.mark.removePrefix("!").trim().lowercase(Locale("ru"))
                    m == "н" || m == "б" || m.contains("отсут")
                }
                subjects = withContext(Dispatchers.IO) {
                    ApiClient.getMarks(classId, studentId, uchYear)
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    val avg = remember(weekMarks) {
        if (weekMarks.isEmpty()) null else weekMarks.sum().toDouble() / weekMarks.size
    }
    val rated = remember(subjects) {
        subjects.mapNotNull { subject ->
            val lessons = GoalMath.currentPeriodLessons(subject)
            val value = GoalMath.weightedAverage(lessons) ?: return@mapNotNull null
            if (lessons.size < 2) null else subject to value
        }
    }
    val best = remember(rated) { rated.maxByOrNull { it.second } }
    val worst = remember(rated) { rated.minByOrNull { it.second } }
    val gradeColor = when {
        avg == null -> colors.textTertiary
        avg >= 4.5 -> colors.success
        avg >= 3.5 -> colors.warning
        else -> colors.danger
    }

    GlassScreenScaffold(
        title = "Итоги недели",
        subtitle = if (period.isBlank()) "С понедельника по воскресенье" else period,
        onBack = onBack,
    ) {
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassSpinner()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "hero") {
                    GlassCard(tint = colors.accent, contentPadding = PaddingValues(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Средний балл за неделю",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(Modifier.height(2.dp))
                                AnimatedNumberText(
                                    value = avg,
                                    fontSize = 50.sp,
                                    color = colors.text,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    GlassBadge("${weekMarks.size} оценок", tint = colors.textSecondary)
                                    GlassBadge("$absences пропусков", tint = colors.textSecondary)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Box(contentAlignment = Alignment.Center) {
                                GlassProgressRing(
                                    progress = if (avg == null) 0f else ((avg - 2.0) / 3.0).toFloat(),
                                    size = 90.dp,
                                    strokeWidth = 9.dp,
                                    tint = gradeColor,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (avg == null) "—" else String.format(Locale("ru"), "%.2f", avg),
                                        color = gradeColor,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                    Text(
                                        text = "из 5",
                                        color = colors.textTertiary,
                                        fontSize = 9.sp,
                                    )
                                }
                            }
                        }
                        if (weekMarks.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "На этой неделе оценок не было",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                item(key = "titles") { SectionLabel("Предметы недели") }

                item(key = "tiles") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.EmojiEvents,
                            title = "Лучший предмет",
                            name = best?.first?.subjectName,
                            value = best?.let { String.format(Locale("ru"), "%.2f", it.second) },
                            tint = colors.success,
                        )
                        SummaryTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.TrendingDown,
                            title = "Просел",
                            name = worst?.first?.subjectName,
                            value = worst?.let { String.format(Locale("ru"), "%.2f", it.second) },
                            tint = colors.danger,
                        )
                    }
                }

                if (weekRated.isNotEmpty()) {
                    item(key = "marksTitle") { SectionLabel("Оценки за неделю") }
                    items(
                        items = weekRated,
                        key = { "${it.date.time}_${it.lessonNumber}_${it.subjectId}" },
                    ) { entry ->
                        WeekMarkRow(entry = entry, onClick = { onOpenLesson(entry) })
                    }
                }

                if (error != null) {
                    item(key = "error") {
                        GlassCard {
                            Text(
                                text = "Не удалось загрузить данные: $error",
                                color = colors.danger,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                item(key = "note") {
                    GlassCard(contentPadding = PaddingValues(14.dp)) {
                        Text(
                            text = "Неделя считается с понедельника по воскресенье. Отметки «сделал» в дневнике сюда не входят.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    name: String?,
    value: String?,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    GlassCard(modifier = modifier, contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconBubble(icon = icon, tint = tint, size = 32.dp, iconSize = 16.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = name ?: "Нет данных",
            color = colors.text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Brush.linearGradient(listOf(tint, tint.copy(alpha = 0.6f))))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = value ?: "—",
                color = tint,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

/** Строка «какая оценка и по какому предмету» в итогах недели. */
@Composable
private fun WeekMarkRow(entry: DairyEntry, onClick: () -> Unit) {
    val colors = Glass.colors
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale("ru")) }
    GlassCard(onClick = onClick, contentPadding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkBadge(mark = entry.mark, size = 40.dp)
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.subjectName,
                    color = colors.text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dayFmt.format(entry.date) + " · " + entry.lessonNumber + "-й урок",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
