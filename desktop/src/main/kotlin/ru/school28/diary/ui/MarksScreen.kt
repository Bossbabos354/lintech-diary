package ru.school28.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.school28.diary.AppLog
import ru.school28.diary.AppState
import ru.school28.diary.GoalTarget
import ru.school28.diary.Repo
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SubjectMarks
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarksScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            subjects = Repo.marks()
            offlineAt = ApiClient.offlineAt("marks")
        } catch (e: Exception) {
            AppLog.logException("marks", e)
            error = e.message
        }
        loading = false
    }

    val periodLessons = remember(subjects) { subjects.flatMap { GoalMath.currentPeriodLessons(it) } }
    val overallAvg = remember(periodLessons) { GoalMath.weightedAverage(periodLessons) }
    val sorted = remember(subjects) { subjects.sortedBy { it.subjectName } }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Оценки", "${subjects.size} предметов")
        OfflineLabel(offlineAt)

        when {
            loading -> LoadingBox()
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ошибка: $error", color = AppTheme.colors.error)
            }
            else -> SmoothLazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp,
                ),
            ) {
                item(key = "overall") { OverallCard(overallAvg) }
                item(key = "weekly") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.surface)
                            .clickable { state.weekly = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Итоги недели",
                            color = AppTheme.colors.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text("→", color = AppTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                if (sorted.isEmpty()) {
                    item { EmptyBox("Нет данных об оценках") }
                }
                items(sorted, key = { it.subjectId }) { subject ->
                    SubjectCard(subject) { state.goal = GoalTarget(subject, "YEAR") }
                }
            }
        }
    }
}

@Composable
private fun OverallCard(avg: Double?) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(gradientAverage())
            .padding(18.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Общий средний балл", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            if (avg != null) {
                val gradeColor = when {
                    avg >= 4.5 -> Color(0xFF4DB6AC)
                    avg >= 3.5 -> Color(0xFFFFD54F)
                    else -> Color(0xFFFFB74D)
                }
                Text(
                    String.format(Locale("ru"), "%.2f", avg),
                    color = gradeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp,
                )
            } else {
                Text("—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(subject: SubjectMarks, onClick: () -> Unit) {
    val goal = remember(subject.subjectId) { GoalStore.find(subject.subjectId, "YEAR") }
    val goalHint = remember(goal, subject) {
        goal?.let { GoalMath.hint(GoalMath.marksForLessons(subject, it.periodKey), it) }
    }
    val marks = remember(subject) {
        subject.marks.filter { it.mark.isNotEmpty() }.sortedBy { it.date?.time ?: 0L }.takeLast(20)
    }
    val totals = remember(subject) {
        if (subject.totalMarks.isEmpty()) "" else subject.totalMarks.entries
            .sortedBy { it.key }
            .joinToString(" · ") { (p, m) ->
                val name = when (p) {
                    360000 -> "1 четв."
                    360001 -> "2 четв."
                    360002 -> "3 четв."
                    360003 -> "4 четв."
                    360011 -> "1 полуг."
                    360012 -> "2 полуг."
                    else -> "Пер. $p"
                }
                "$name: $m"
            }
    }
    val avg = remember(subject) { GoalMath.currentPeriodAverage(subject) }

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
            SubjectAvatar(subject.subjectId, subject.subjectName)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(subject.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                subject.teacherName?.let {
                    Text(it, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                }
            }
            if (avg != null) {
                Text(
                    String.format(Locale("ru"), "%.2f", avg),
                    color = AppTheme.colors.text,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
            }
        }

        if (marks.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                marks.forEach { MarkChip(it.mark, 14) }
            }
        }

        if (totals.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(totals, color = AppTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                goalHint ?: "Поставь цель по предмету",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            Text("Узнать больше →", color = AppTheme.colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
