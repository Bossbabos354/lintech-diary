package ru.school28.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Brush
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
import ru.school28.diary.glass.AnimatedNumberText
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassProgressBar
import ru.school28.diary.glass.GlassProgressRing
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassSpinner
import ru.school28.diary.glass.GlassSpinner
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.glass.SectionLabel
import ru.school28.diary.ui.HomeworkChecklist
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Итоги одного дня: оценки, пропуски и домашние задания. */
@Composable
fun DaySummaryScreen(
    onBack: () -> Unit,
    onOpenLessonDetails: (DairyEntry) -> Unit = {},
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var dateLabel by remember { mutableStateOf("") }
    var doneVersion by remember { mutableIntStateOf(0) }

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
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val sunday = cal.time

                val week = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, monday, sunday)
                }
                val todayKey = dayKeyOf(Date())
                entries = week.filter { dayKeyOf(it.date) == todayKey }
                    .sortedWith(compareBy({ it.date }, { it.lessonNumber }))
                dateLabel = SimpleDateFormat("EEEE, d MMMM", Locale("ru")).format(Date())
            }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    val rated = remember(entries) {
        entries.filter { it.mark.removePrefix("!").trim().toIntOrNull() != null }
    }
    val weekMarks = remember(rated) {
        rated.mapNotNull { it.mark.removePrefix("!").trim().toIntOrNull() }
    }
    val avg = remember(weekMarks) {
        if (weekMarks.isEmpty()) null else weekMarks.sum().toDouble() / weekMarks.size
    }
    val absences = remember(entries) {
        entries.count { entry ->
            val m = entry.mark.removePrefix("!").trim().lowercase(Locale("ru"))
            m == "н" || m == "б" || m.contains("отсут")
        }
    }
    val homework = remember(entries) { entries.filter { it.homework.isNotBlank() } }
    val hwDone = remember(homework, doneVersion) { HomeworkChecklist.doneCount(ctx, homework) }
    val gradeColor = when {
        avg == null -> colors.textTertiary
        avg >= 4.5 -> colors.success
        avg >= 3.5 -> colors.warning
        else -> colors.danger
    }

    GlassScreenScaffold(
        title = "Итоги дня",
        subtitle = dateLabel.ifBlank { "Сегодня" },
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
                                    text = "Средний балл за день",
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
                                text = "Сегодня оценок не было",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                if (rated.isNotEmpty()) {
                    item(key = "marksTitle") { SectionLabel("Оценки за день") }
                    items(
                        items = rated,
                        key = { "${it.date.time}_${it.lessonNumber}_${it.subjectId}" },
                    ) { entry ->
                        DayMarkRow(entry = entry, onClick = { onOpenLessonDetails(entry) })
                    }
                }

                if (entries.isNotEmpty()) {
                    item(key = "hwTitle") { SectionLabel("Домашнее задание") }
                    item(key = "hwProgress") {
                        GlassCard(contentPadding = PaddingValues(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Выполнено",
                                    color = colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = "$hwDone из ${homework.size}",
                                    color = if (homework.isNotEmpty() && hwDone >= homework.size) colors.success else colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            GlassProgressBar(
                                progress = if (homework.isEmpty()) 0f else hwDone.toFloat() / homework.size,
                                tint = if (homework.isNotEmpty() && hwDone >= homework.size) colors.success else colors.accent,
                            )
                        }
                    }
                    items(
                        items = entries,
                        key = { "hw_${it.date.time}_${it.lessonNumber}_${it.subjectId}" },
                    ) { entry ->
                        DayHomeworkRow(
                            entry = entry,
                            doneVersion = doneVersion,
                            onToggle = { doneVersion++ },
                            onClick = { onOpenLessonDetails(entry) },
                        )
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
                            text = "Итоги считаются по сегодняшнему дню. Нажми на оценку, чтобы открыть урок в дневнике.",
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
private fun DayMarkRow(entry: DairyEntry, onClick: () -> Unit) {
    val colors = Glass.colors
    GlassCard(onClick = onClick, contentPadding = PaddingValues(12.dp), elevated = false) {
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
                if (entry.theme.isNotBlank()) {
                    Text(
                        text = entry.theme,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun dayKeyOf(date: Date): Int {
    val c = Calendar.getInstance().apply { time = date }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

@Composable
private fun DayHomeworkRow(
    entry: DairyEntry,
    doneVersion: Int,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val isDone = remember(entry, doneVersion) { HomeworkChecklist.isDone(ctx, entry) }
    GlassCard(onClick = onClick, contentPadding = PaddingValues(12.dp), elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(text = entry.subjectName.take(2), subjectId = entry.subjectId, size = 38.dp)
            Spacer(Modifier.width(10.dp))
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
                    text = entry.homework.ifBlank { "Без домашнего задания" },
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            HomeworkCheck(isDone = isDone) {
                HomeworkChecklist.toggle(ctx, entry)
                onToggle()
            }
        }
    }
}

@Composable
private fun HomeworkCheck(isDone: Boolean, onToggle: () -> Unit) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "checkScale",
    )
    val bg by animateColorAsState(
        targetValue = if (isDone) colors.accent.copy(alpha = 0.18f) else colors.textTertiary.copy(alpha = 0.08f),
        animationSpec = tween(220),
        label = "checkBg",
    )
    val fg by animateColorAsState(
        targetValue = if (isDone) colors.accent else colors.textTertiary.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "checkFg",
    )
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = if (isDone) "Домашка сделана" else "Отметить домашку",
            tint = fg,
            modifier = Modifier.size(19.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}
