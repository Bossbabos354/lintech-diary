package ru.school28.diary.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassLargeHeader
import ru.school28.diary.glass.GlassProgressBar
import ru.school28.diary.glass.GlassProgressRing
import ru.school28.diary.glass.GlassSkeletonCard
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.StaggeredAppear
import ru.school28.diary.ui.CriticalSubjects
import ru.school28.diary.ui.Goal
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalPeriods
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.MarkStyle

@Composable
fun GoalsScreen(
    activity: MainActivity?,
    refreshKey: Int,
    onGoalClick: (SubjectMarks, String) -> Unit,
    onOpenCritical: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()

    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var goals by remember { mutableStateOf<List<Goal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val seenAppear = remember { mutableSetOf<Any>() }
    var error by remember { mutableStateOf<String?>(null) }
    var localRefresh by remember { mutableStateOf(0) }

    LaunchedEffect(localRefresh) {
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
                subjects = withContext(Dispatchers.IO) {
                    ApiClient.getMarks(classId, studentId, uchYear)
                }
            }
        } catch (e: Exception) {
            error = e.message
        }
        goals = GoalStore.load(ctx)
        loading = false
    }

    LaunchedEffect(refreshKey) {
        goals = GoalStore.load(ctx)
    }

    val byId = remember(subjects) { subjects.associateBy { it.subjectId } }
    val critical = remember(subjects) { CriticalSubjects.find(subjects) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            GlassLargeHeader(
                title = "Цели",
                subtitle = if (goals.isEmpty()) "Личные цели по предметам" else "${goals.size} целей · следи за прогрессом",
                trailing = {
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Обновить",
                        onClick = { localRefresh++ },
                        size = 42.dp,
                    )
                },
            )
        }

        if (critical.isNotEmpty() && !loading) {
            item(key = "critical") {
                GlassCard(
                    onClick = onOpenCritical,
                    tint = colors.danger,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconBubble(
                            icon = Icons.Rounded.WarningAmber,
                            tint = colors.danger,
                            size = 38.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Критические предметы",
                                color = colors.text,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Низкий средний балл или много двоек",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        GlassBadge(critical.size.toString(), tint = colors.danger)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        when {
            loading -> {
                items(3) { GlassSkeletonCard(height = 132.dp, lines = 4) }
            }

            error != null && goals.isEmpty() -> {
                item(key = "error") {
                    GlassCard { GlassErrorState(message = error, onRetry = { localRefresh++ }) }
                }
            }

            goals.isEmpty() -> {
                item(key = "empty") {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.Flag,
                            title = "Целей пока нет",
                            message = "Открой «Оценки», выбери предмет и поставь цель — покажем, сколько оценок нужно до неё.",
                        )
                    }
                }
            }

            else -> {
                itemsIndexed(goals, key = { _, goal -> goal.id }) { index, goal ->
                    StaggeredAppear(
                            index = index,
                            appearKey = goal.id,
                            seen = seenAppear,
                        ) {
                        GoalCard(
                            goal = goal,
                            subject = byId[goal.subjectId],
                            onClick = {
                                val subject = byId[goal.subjectId]
                                if (subject != null) onGoalClick(subject, goal.periodKey)
                            },
                            onDelete = {
                                GoalStore.remove(ctx, goal.subjectId, goal.periodKey)
                                goals = GoalStore.load(ctx)
                            },
                        )
                    }
                }
            }
        }

        if (error != null && goals.isNotEmpty()) {
            item(key = "errorFooter") {
                Text(
                    text = "Оценки не загрузились: $error",
                    color = colors.textTertiary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    subject: SubjectMarks?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val marks = remember(subject, goal.periodKey) {
        if (subject == null) emptyList() else GoalMath.marksForLessons(subject, goal.periodKey)
    }
    val hint = remember(marks, goal) {
        if (subject == null) "Нет данных об оценках" else GoalMath.hint(marks, goal)
    }
    val needed = remember(marks, goal) { GoalMath.needed(marks, goal.targetMark, goal.allowedMarks) }
    val achieved = needed == 0
    val avg = remember(marks) { GoalMath.weightedAverage(marks) }
    val threshold = GoalMath.threshold(goal.targetMark)
    val progress = if (achieved) 1f else if (avg == null) 0f else (avg / threshold).toFloat().coerceIn(0f, 0.99f)
    val accent = if (achieved) colors.success else colors.accent

    GlassCard(onClick = onClick, contentPadding = PaddingValues(15.dp), elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(
                text = goal.subjectName.take(2),
                subjectId = goal.subjectId,
                size = 42.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = goal.subjectName,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${GoalPeriods.byKey(goal.periodKey).label} · цель «${goal.targetMark}»",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MarkStyle.color(goal.targetMark.toString()).copy(alpha = 0.15f))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Удалить цель",
                    tint = MarkStyle.color(goal.targetMark.toString()),
                    modifier = Modifier.size(19.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                GlassProgressRing(
                    progress = progress,
                    size = 52.dp,
                    strokeWidth = 5.dp,
                    tint = accent,
                )
                Text(
                    text = if (needed == null) "—" else if (achieved) "✓" else "${(progress * 100).toInt()}",
                    color = accent,
                    fontSize = if (achieved) 20.sp else 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = hint,
                    color = if (achieved) colors.success else colors.text,
                    fontSize = 12.sp,
                    fontWeight = if (achieved) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                GlassProgressBar(
                    progress = progress,
                    height = 5.dp,
                    tint = accent,
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
