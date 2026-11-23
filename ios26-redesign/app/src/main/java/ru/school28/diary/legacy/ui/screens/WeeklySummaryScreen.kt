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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.legacy.ui.GoalMath
import ru.school28.diary.legacy.ui.MarkStyle
import ru.school28.diary.legacy.ui.subjectColor
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * «Итоги недели»: оценки за неделю, средний балл, пропуски,
 * лучший и проблемный предметы. Доступно из «Оценок».
 */
@Composable
fun WeeklySummaryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
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
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val monday = cal.time
                val fmt = SimpleDateFormat("d MMM", Locale("ru"))
                cal.add(Calendar.DAY_OF_YEAR, 6)
                period = fmt.format(monday) + " — " + fmt.format(cal.time)
                val sunday = (cal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
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
        subjects.mapNotNull { s ->
            val lessons = GoalMath.currentPeriodLessons(s)
            val value = GoalMath.weightedAverage(lessons) ?: return@mapNotNull null
            if (lessons.size < 2) null else s to value
        }
    }
    val best = remember(rated) { rated.maxByOrNull { it.second } }
    val worst = remember(rated) { rated.minByOrNull { it.second } }

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(AppTheme.colors.primary)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = AppTheme.colors.onPrimary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onBack() }
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Итоги недели",
                color = AppTheme.colors.onPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(period, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))

                // Средний балл за неделю
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(AppTheme.colors.surface)
                        .padding(18.dp)
                ) {
                    Text("Средний балл за неделю", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(
                        avg?.let { String.format(Locale("ru"), "%.2f", it) } ?: "—",
                        color = AppTheme.colors.text,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (weekMarks.isEmpty()) "На этой неделе оценок не было"
                        else "Оценок: ${weekMarks.size} · пропусков: $absences",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Лучший и проблемный предметы
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(
                        title = "Лучший предмет",
                        name = best?.first?.subjectName,
                        value = best?.let { String.format(Locale("ru"), "%.2f", it.second) },
                        tint = Color(0xFF4DB6AC),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        title = "Просел",
                        name = worst?.first?.subjectName,
                        value = worst?.let { String.format(Locale("ru"), "%.2f", it.second) },
                        tint = Color(0xFFF5A623),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (weekRated.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Оценки за неделю",
                        color = AppTheme.colors.text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    weekRated.forEach { entry ->
                        LegacyWeekMarkRow(entry)
                        Spacer(Modifier.height(6.dp))
                    }
                }

                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Не удалось загрузить данные: $error",
                        color = AppTheme.colors.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "Неделя считается с понедельника по воскресенье. Отметки «сделал» в дневнике сюда не входят.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun SummaryTile(
    title: String,
    name: String?,
    value: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surface)
            .padding(14.dp)
    ) {
        Text(title, color = AppTheme.colors.textSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            name ?: "Нет данных",
            color = AppTheme.colors.text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 2
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(tint))
            Spacer(Modifier.width(6.dp))
            Text(value ?: "—", color = AppTheme.colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

/** Строка «какая оценка и по какому предмету» в старых итогах недели. */
@Composable
private fun LegacyWeekMarkRow(entry: DairyEntry) {
    val dayFmt = remember { SimpleDateFormat("d MMM", Locale("ru")) }
    val markColor = MarkStyle.color(entry.mark)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AppTheme.colors.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(markColor.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                entry.mark.removePrefix("!"),
                color = markColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.subjectName,
                color = AppTheme.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Text(
                dayFmt.format(entry.date) + " · " + entry.lessonNumber + "-й урок",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}
