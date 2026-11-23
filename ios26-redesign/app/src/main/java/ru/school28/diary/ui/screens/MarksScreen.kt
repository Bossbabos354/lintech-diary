package ru.school28.diary.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.AnimatedNumberText
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassLargeHeader
import ru.school28.diary.glass.GlassProgressRing
import ru.school28.diary.glass.GlassShape
import ru.school28.diary.glass.GlassSkeletonCard
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.SectionLabel
import ru.school28.diary.glass.StaggeredAppear
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.widgets.WidgetSync
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarksScreen(
    activity: MainActivity?,
    onSubjectClick: (SubjectMarks) -> Unit = {},
    onOpenWeekly: () -> Unit = {},
    onOpenDay: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()

    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    val seenAppear = remember { mutableSetOf<Any>() }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val studentId = Prefs.getStudentId(ctx)
                val uchYear = Prefs.getUchYear(ctx)
                if (studentId == -1) {
                    loading = false
                    return@launch
                }

                var classId = Prefs.getClassId(ctx)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) { ApiClient.getStudentClass(studentId, uchYear) }
                    Prefs.setClassId(ctx, classId)
                }

                subjects = withContext(Dispatchers.IO) { ApiClient.getMarks(classId, studentId, uchYear) }
                offlineAt = ApiClient.offlineAt("marks")
                WidgetSync.request(ctx)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val periodMarks = remember(subjects) {
        subjects.flatMap { GoalMath.currentPeriodLessons(it) }
    }
    val marksCount = remember(periodMarks) { periodMarks.size }
    val overallAvg = remember(periodMarks) { GoalMath.weightedAverage(periodMarks) }
    val sortedSubjects = remember(subjects) { subjects.sortedBy { it.subjectName } }
    val timeSdf = remember { java.text.SimpleDateFormat("HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            GlassLargeHeader(
                title = "Оценки",
                subtitle = if (subjects.isEmpty()) "Загрузка…" else "${subjects.size} предметов · $marksCount оценок",
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

        offlineAt?.let { ts ->
            item(key = "offline") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    GlassBadge("офлайн · данные от ${timeSdf.format(Date(ts))}", tint = colors.warning)
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        when {
            loading -> {
                item(key = "heroSkeleton") { GlassSkeletonCard(height = 176.dp, lines = 4) }
                items(3) { GlassSkeletonCard(height = 150.dp, lines = 4) }
            }

            error != null -> {
                item(key = "error") {
                    GlassCard { GlassErrorState(message = error, onRetry = { load() }) }
                }
            }

            subjects.isEmpty() -> {
                item(key = "empty") {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.Grade,
                            title = "Оценок пока нет",
                            message = "Как только учителя выставят оценки, они появятся здесь.",
                        )
                    }
                }
            }

            else -> {
                item(key = "hero") {
                    AverageHero(
                        average = overallAvg,
                        marksCount = marksCount,
                        subjectsCount = subjects.size,
                    )
                }

                item(key = "summaries") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SummaryButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Schedule,
                            title = "Итоги дня",
                            subtitle = "сегодня",
                            onClick = onOpenDay,
                        )
                        SummaryButton(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Insights,
                            title = "Итоги недели",
                            subtitle = "за неделю",
                            onClick = onOpenWeekly,
                        )
                    }
                }

                item(key = "subjectsLabel") {
                    SectionLabel("Предметы") {
                        Text(
                            text = "${sortedSubjects.size}",
                            color = colors.textTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                itemsIndexed(sortedSubjects, key = { _, subject -> subject.subjectId }) { index, subject ->
                    StaggeredAppear(
                            index = index,
                            appearKey = subject.subjectId,
                            seen = seenAppear,
                        ) {
                        SubjectCard(subject = subject, onClick = { onSubjectClick(subject) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AverageHero(
    average: Double?,
    marksCount: Int,
    subjectsCount: Int,
) {
    val colors = Glass.colors
    val gradeColor = when {
        average == null -> colors.textTertiary
        average >= 4.5 -> colors.success
        average >= 3.5 -> colors.warning
        else -> colors.danger
    }
    val label = when {
        average == null -> "нет оценок"
        average >= 4.5 -> "отличный результат"
        average >= 3.5 -> "хороший результат"
        average >= 2.5 -> "нужно подтянуть"
        else -> "требуется внимание"
    }

    GlassCard(
        shape = GlassShape.screen,
        tint = colors.accent,
        contentPadding = PaddingValues(20.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accent.copy(alpha = 0.3f), Color.Transparent),
                        center = Offset(size.width * 0.92f, -size.height * 0.1f),
                        radius = size.width * 0.7f,
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(size.width * 0.92f, -size.height * 0.1f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.accentSecondary.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 1.05f),
                        radius = size.width * 0.6f,
                    ),
                    radius = size.width * 0.6f,
                    center = Offset(size.width * 0.05f, size.height * 1.05f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Средний балл",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    AnimatedNumberText(
                        value = average,
                        fontSize = 52.sp,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(gradeColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = label,
                            color = gradeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        GlassBadge("$marksCount оценок", tint = colors.textSecondary)
                        GlassBadge("$subjectsCount предметов", tint = colors.textSecondary)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(contentAlignment = Alignment.Center) {
                    GlassProgressRing(
                        progress = if (average == null) 0f else ((average - 2.0) / 3.0).toFloat(),
                        size = 94.dp,
                        strokeWidth = 9.dp,
                        tint = gradeColor,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when {
                                average == null -> "—"
                                average >= 4.5 -> "5"
                                average >= 3.5 -> "4"
                                average >= 2.5 -> "3"
                                else -> "2"
                            },
                            color = gradeColor,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "из 5",
                            color = colors.textTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(subject: SubjectMarks, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val goal = remember(subject.subjectId) { GoalStore.find(ctx, subject.subjectId, "YEAR") }
    val goalHint = remember(goal, subject) {
        if (goal == null) null else GoalMath.hint(GoalMath.marksForLessons(subject, goal.periodKey), goal)
    }
    val marks = remember(subject.marks) {
        subject.marks.filter { it.mark.isNotEmpty() }.sortedBy { it.date }.takeLast(20)
    }
    val avg = remember(subject.marks) { GoalMath.currentPeriodAverage(subject) }
    val avgColor = when {
        avg == null -> colors.textTertiary
        avg >= 4.5 -> colors.success
        avg >= 3.5 -> colors.warning
        else -> colors.danger
    }

    GlassCard(
        onClick = onClick,
        contentPadding = PaddingValues(15.dp),
        elevated = false,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(
                text = subject.subjectName.take(2),
                subjectId = subject.subjectId,
                size = 42.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = subject.subjectName,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val teacher = subject.teacherName
                if (!teacher.isNullOrBlank()) {
                    Text(
                        text = teacher,
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (avg != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(avgColor.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = String.format(Locale("ru"), "%.2f", avg),
                        color = avgColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }

        if (marks.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                marks.forEach { mark ->
                    val markColor = MarkStyle.color(mark.mark)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(markColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = mark.mark.removePrefix("!"),
                            color = markColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        val totals = remember(subject.totalMarks) {
            if (subject.totalMarks.isEmpty()) "" else subject.totalMarks.entries.sortedBy { it.key }
                .joinToString(" · ") { (period, mark) ->
                    val name = when (period) {
                        360000 -> "1 четв."
                        360001 -> "2 четв."
                        360002 -> "3 четв."
                        360003 -> "4 четв."
                        360011 -> "1 полуг."
                        360012 -> "2 полуг."
                        else -> "Пер. $period"
                    }
                    "$name: $mark"
                }
        }
        if (totals.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = totals,
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(9.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.textTertiary.copy(alpha = 0.06f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Flag,
                contentDescription = null,
                tint = if (goalHint != null) colors.accent else colors.textTertiary,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = goalHint ?: "Поставь цель по предмету",
                color = if (goalHint != null) colors.text else colors.textSecondary,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}

@Composable
private fun SummaryButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    GlassCard(
        modifier = modifier,
        onClick = onClick,
        tint = colors.accent,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconBubble(icon = icon, tint = colors.accent, size = 32.dp, iconSize = 16.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
