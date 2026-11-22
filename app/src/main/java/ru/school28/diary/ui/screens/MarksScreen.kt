package ru.school28.diary.ui.screens

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
import ru.school28.diary.MainActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.ui.subjectColor
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.gradientAverage
import ru.school28.diary.widgets.WidgetSync
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarksScreen(
    activity: MainActivity?,
    onSubjectClick: (SubjectMarks) -> Unit = {},
    onOpenWeekly: () -> Unit = {}
) {
    val ctx = activity ?: return
    val scope = rememberCoroutineScope()
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val studentId = Prefs.getStudentId(ctx)
                val uchYear = Prefs.getUchYear(ctx)
                if (studentId == -1) { loading = false; return@launch }

                var classId = Prefs.getClassId(ctx)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) { ApiClient.getStudentClass(studentId, uchYear) }
                    Prefs.setClassId(ctx, classId)
                }

                subjects = withContext(Dispatchers.IO) { ApiClient.getMarks(classId, studentId, uchYear) }
                offlineAt = ApiClient.offlineAt("marks")
                WidgetSync.request(ctx)
            } catch (e: Exception) { error = e.message }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    // Тяжёлые вычисления — только при смене данных, а не на каждую рекомпозицию
    val numericMarks = remember(subjects) {
        subjects.flatMap { it.marks }.mapNotNull { it.mark.toIntOrNull() }
    }
    val overallAvg = remember(numericMarks) {
        if (numericMarks.isNotEmpty()) numericMarks.sum().toDouble() / numericMarks.size else null
    }
    val sortedSubjects = remember(subjects) { subjects.sortedBy { it.subjectName } }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
    ) {
        offlineAt?.let { ts ->
            item(key = "offline") {
                Text(
                    "офлайн · данные от " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(ts)),
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp)
                )
            }
        }

        // Общий средний балл — градиентная карточка
        item(key = "overall_header") {
            OverallCard(overallAvg)
        }

        item(key = "weekly_link") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(AppTheme.colors.surface)
                    .clickable { onOpenWeekly() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Итоги недели",
                    color = AppTheme.colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text("→", color = AppTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        items(sortedSubjects) { s ->
            SubjectCard(s) { onSubjectClick(s) }
        }

        if (!loading && error == null && subjects.isEmpty()) {
            item {
                Box(Modifier.fillMaxSize().padding(top = 100.dp), contentAlignment = Alignment.Center) {
                    Text("Нет данных об оценках", color = AppTheme.colors.textSecondary)
                }
            }
        }
    }

    // Loading / Error поверх
    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
    if (error != null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ошибка: $error", color = AppTheme.colors.error)
        }
    }
}

@Composable
private fun OverallCard(avg: Double?) {
    // Плавное "нарастание" среднего балла при загрузке
    var shownAvg by remember(avg) {
        mutableStateOf(avg ?: 0.0)
    }
    LaunchedEffect(avg) {
        if (avg == null) return@LaunchedEffect
        val target = avg
        val steps = 24
        for (i in 1..steps) {
            shownAvg = target * i / steps
            kotlinx.coroutines.delay(16L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(gradientAverage())
            .padding(18.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Общий средний балл", color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
            if (avg != null) {
                val gradeColor = when {
                    avg >= 4.5 -> Color(0xFF4DB6AC)
                    avg >= 3.5 -> Color(0xFFFFD54F)
                    else -> Color(0xFFFFB74D)
                }
                Text(
                    text = String.format(Locale("ru"), "%.2f", shownAvg),
                    color = gradeColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 34.sp
                )
            } else {
                Text("—", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectCard(s: SubjectMarks, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val goal = remember(s.subjectId) { GoalStore.find(ctx, s.subjectId, "YEAR") }
    val goalHint = remember(goal, s) {
        if (goal == null) null else GoalMath.hint(GoalMath.marksFor(s, goal.periodKey), goal)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(subjectColor(s.subjectId)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.subjectName.take(2),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(s.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (s.teacherName != null) Text(s.teacherName!!, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))

        val marks = remember(s.marks) {
            s.marks.filter { it.mark.isNotEmpty() }.sortedBy { it.date }.takeLast(20)
        }
        if (marks.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                marks.forEach { m ->
                    val markColor = MarkStyle.color(m.mark)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(markColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(m.mark, color = markColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }

        val totals = remember(s.totalMarks) {
            if (s.totalMarks.isEmpty()) ""
            else s.totalMarks.entries.sortedBy { it.key }.joinToString(" · ") { (p, m) ->
                val n = when (p) { 360000 -> "1 четв."; 360001 -> "2 четв."; 360002 -> "3 четв."; 360003 -> "4 четв."; 360011 -> "1 полуг."; 360012 -> "2 полуг."; else -> "Пер. $p" }
                "$n: $m"
            }
        }
        if (totals.isNotEmpty()) {
            Text(totals, color = AppTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                modifier = Modifier.padding(top = 6.dp))
        }

        val numeric = remember(s.marks) { s.marks.mapNotNull { it.mark.toIntOrNull() } }
        if (numeric.isNotEmpty()) {
            val avg = String.format(Locale("ru"), "%.2f", numeric.sum().toDouble() / numeric.size)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                Text("Средний: ", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                Text(avg, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Подсказка: цель по предмету / «Узнать больше»
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = goalHint ?: "Поставь цель по предмету",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "Узнать больше →",
                color = AppTheme.colors.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}