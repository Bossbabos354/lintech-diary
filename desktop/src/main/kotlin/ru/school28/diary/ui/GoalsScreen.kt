package ru.school28.diary.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.AppLog
import ru.school28.diary.AppState
import ru.school28.diary.GoalTarget
import ru.school28.diary.Repo
import ru.school28.diary.api.SubjectMarks

@Composable
fun GoalsScreen(state: AppState) {
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var goals by remember { mutableStateOf(GoalStore.load()) }

    LaunchedEffect(state.goalsVersion) {
        goals = GoalStore.load()
    }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            subjects = Repo.marks()
        } catch (e: Exception) {
            AppLog.logException("goals/marks", e)
            error = e.message
        }
        loading = false
    }

    val byId = remember(subjects) { subjects.associateBy { it.subjectId } }
    val critical = remember(subjects) { CriticalSubjects.find(subjects) }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Мои цели", if (goals.isEmpty()) null else "поставлено: ${goals.size}")

        if (critical.isNotEmpty() && !loading) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.error.copy(alpha = 0.10f))
                    .clickable { state.critical = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Критические предметы",
                    color = AppTheme.colors.error,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.colors.error.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("${critical.size}", color = AppTheme.colors.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text("→", color = AppTheme.colors.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        when {
            loading -> LoadingBox()
            goals.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = AppTheme.colors.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Целей пока нет", color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Открой «Оценки» → выбери предмет → «Узнать больше» и поставь цель",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            else -> SmoothLazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp,
                ),
            ) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        subject = byId[goal.subjectId],
                        onClick = {
                            byId[goal.subjectId]?.let { state.goal = GoalTarget(it, goal.periodKey) }
                        },
                        onDelete = {
                            GoalStore.remove(goal.subjectId, goal.periodKey)
                            goals = GoalStore.load()
                            state.goalsVersion++
                        },
                    )
                }
            }
        }

        if (error != null) {
            Text(
                "Не удалось загрузить оценки: $error",
                color = AppTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
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
    val marks = remember(subject, goal.periodKey) {
        if (subject == null) emptyList() else GoalMath.marksFor(subject, goal.periodKey)
    }
    val hint = remember(marks, goal) {
        if (subject == null) "Нет данных об оценках" else GoalMath.hint(marks, goal)
    }
    val needed = remember(marks, goal) { GoalMath.needed(marks, goal.targetMark, goal.allowedMarks) }
    val achieved = needed == 0
    val targetColor = MarkStyle.color(goal.targetMark.toString())

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SubjectAvatar(goal.subjectId, goal.subjectName)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${GoalPeriods.byKey(goal.periodKey).label} · цель «${goal.targetMark}»",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(targetColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    goal.targetMark.toString(),
                    color = targetColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Удалить цель",
                    tint = AppTheme.colors.textDisabled,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (achieved) AppTheme.colors.success.copy(alpha = 0.12f) else AppTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                hint,
                color = if (achieved) AppTheme.colors.success else AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
            )
            Text("Узнать больше", color = AppTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
