package ru.school28.diary.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
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
import ru.school28.diary.MainActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.CriticalSubjects
import ru.school28.diary.ui.Goal
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalPeriods
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.ui.subjectColor
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import ru.school28.diary.ui_components.components.Text

/**
 * Вкладка «Цели» — все поставленные цели со текущим прогрессом.
 */
@Composable
fun GoalsScreen(
    activity: MainActivity?,
    refreshKey: Int,
    onGoalClick: (SubjectMarks, String) -> Unit,
    onOpenCritical: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var goals by remember { mutableStateOf<List<Goal>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Оценки загружаем один раз
    LaunchedEffect(Unit) {
        scope.launch {
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
    }

    // После сохранения/удаления цели — перечитываем список
    LaunchedEffect(refreshKey) {
        goals = GoalStore.load(ctx)
    }

    val byId = remember(subjects) { subjects.associateBy { it.subjectId } }
    val critical = remember(subjects) { CriticalSubjects.find(subjects) }

    Column(Modifier.fillMaxSize()) {
        Text(
            "Мои цели",
            color = AppTheme.colors.text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
        )

        if (critical.isNotEmpty() && !loading) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(AppTheme.colors.error.copy(alpha = 0.10f))
                    .clickable { onOpenCritical() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Критические предметы",
                    color = AppTheme.colors.error,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.colors.error.copy(alpha = 0.18f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${critical.size}", color = AppTheme.colors.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                Text("→", color = AppTheme.colors.error, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(4.dp))
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            goals.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(
                        Icons.Filled.Flag,
                        contentDescription = null,
                        tint = AppTheme.colors.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Целей пока нет", color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Открой «Оценки» → выбери предмет → «Узнать больше» и поставь цель",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        subject = byId[goal.subjectId],
                        onClick = {
                            val s = byId[goal.subjectId]
                            if (s != null) onGoalClick(s, goal.periodKey)
                        },
                        onDelete = {
                            GoalStore.remove(ctx, goal.subjectId, goal.periodKey)
                            goals = GoalStore.load(ctx)
                        }
                    )
                }
            }
        }

        if (error != null) {
            Text(
                "Не удалось загрузить оценки: $error",
                color = AppTheme.colors.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun GoalCard(
    goal: Goal,
    subject: SubjectMarks?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val marks = remember(subject, goal.periodKey) {
        if (subject == null) emptyList() else GoalMath.marksFor(subject, goal.periodKey)
    }
    val hint = remember(marks, goal) {
        if (subject == null) "Нет данных об оценках" else GoalMath.hint(marks, goal)
    }
    val needed = remember(marks, goal) { GoalMath.needed(marks, goal.targetMark, goal.allowedMarks) }
    val achieved = needed == 0

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(subjectColor(goal.subjectId)),
                contentAlignment = Alignment.Center
            ) {
                Text(goal.subjectName.take(2), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(goal.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${GoalPeriods.byKey(goal.periodKey).label} · цель «${goal.targetMark}»",
                    color = AppTheme.colors.textSecondary, fontSize = 12.sp
                )
            }
            // Целевая оценка
            val targetColor = MarkStyle.color(goal.targetMark.toString())
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(targetColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(goal.targetMark.toString(), color = targetColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Удалить цель", tint = AppTheme.colors.textDisabled, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (achieved) AppTheme.colors.success.copy(alpha = 0.12f)
                    else AppTheme.colors.background
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                hint,
                color = if (achieved) AppTheme.colors.success else AppTheme.colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = if (achieved) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text("Узнать больше", color = AppTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

