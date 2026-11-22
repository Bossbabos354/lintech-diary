package ru.school28.diary.ui
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.school28.diary.AppLog
import ru.school28.diary.Repo
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks
import java.text.SimpleDateFormat
import java.util.Locale

/** Затемнение + центрированная карточка; клик по фону закрывает. */
@Composable
private fun Overlay(onClose: () -> Unit, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val scale by animateFloatAsState(
        if (shown) 1f else 0.94f,
        spring(dampingRatio = 0.8f, stiffness = 500f),
        label = "overlayScale",
    )
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(160), label = "overlayAlpha")
    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.scrim.copy(alpha = AppTheme.colors.scrim.alpha * alpha))
            .clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .clickable(enabled = false) {},
        ) { content() }
    }
}

@Composable
fun LessonOverlay(entry: DairyEntry, onClose: () -> Unit) {
    Overlay(onClose) {
        Column(
            Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(AppTheme.colors.surfaceSolid)
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SubjectAvatar(entry.subjectId, entry.subjectName, 44)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.subjectName, color = AppTheme.colors.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    val whenText = SimpleDateFormat("d MMMM", Locale("ru")).format(entry.date) +
                        " · урок ${entry.lessonNumber}"
                    Text(whenText, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                }
                if (entry.mark.isNotEmpty()) MarkChip(entry.mark, 20)
            }
            Spacer(Modifier.height(16.dp))
            InfoRow("Тема", entry.theme.ifEmpty { "—" })
            InfoRow("Домашнее задание", entry.homework.ifEmpty { "не задано" })
            if (entry.teacherFio.isNotEmpty()) InfoRow("Учитель", entry.teacherFio)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
fun GoalOverlay(
    subject: SubjectMarks,
    initialPeriodKey: String?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val existing = remember { GoalStore.find(subject.subjectId, initialPeriodKey ?: "YEAR") }
    var periodKey by remember { mutableStateOf(initialPeriodKey ?: existing?.periodKey ?: "YEAR") }
    var targetMark by remember { mutableStateOf(existing?.targetMark ?: 5) }
    var allowedMarks by remember { mutableStateOf((existing?.allowedMarks ?: listOf(5)).toSet()) }

    val currentMarks = remember(periodKey, subject) { GoalMath.marksFor(subject, periodKey) }
    val avg = remember(currentMarks) { GoalMath.average(currentMarks) }
    val needed = remember(currentMarks, targetMark, allowedMarks) {
        GoalMath.needed(currentMarks, targetMark, allowedMarks.toList())
    }

    Overlay(onClose) {
        Column(
            Modifier
                .width(560.dp)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)).background(AppTheme.colors.surfaceSolid)
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Цель по предмету", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(subject.subjectName, color = AppTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                }
                Text(
                    "✕",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 20.sp,
                    modifier = Modifier.clickable { onClose() }.padding(8.dp),
                )
            }
            Spacer(Modifier.height(14.dp))

            CardBox {
                Text("Текущие оценки", color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (currentMarks.isEmpty()) {
                    Text("Оценок за этот период нет", color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        currentMarks.forEach { MarkChip(it.toString(), 14) }
                    }
                    Spacer(Modifier.height(8.dp))
                    val avgText = if (avg == null) "—" else String.format(Locale("ru"), "%.2f", avg)
                    Text(
                        "Средний: $avgText" + (GoalMath.roundedMark(avg)?.let { " · тянет на «$it»" } ?: ""),
                        color = AppTheme.colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            CardBox {
                Text("За какой период", color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Picker(
                    options = GoalPeriods.all.map { it.key to it.label },
                    selectedLabel = GoalPeriods.byKey(periodKey).label,
                    onSelect = { periodKey = it },
                )
            }
            Spacer(Modifier.height(12.dp))

            CardBox {
                Text("Какую оценку хочешь", color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 4, 3).forEach { mark ->
                        Chip(mark.toString(), targetMark == mark, Modifier.weight(1f)) { targetMark = mark }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            CardBox {
                Text("Какими оценками набирать", color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 4, 3, 2).forEach { mark ->
                        Chip(mark.toString(), mark in allowedMarks, Modifier.weight(1f)) {
                            allowedMarks = if (mark in allowedMarks) allowedMarks - mark else allowedMarks + mark
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Можно выбрать несколько — посчитаем по лучшей из них",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.height(14.dp))

            val resultText = when {
                allowedMarks.isEmpty() -> "Выбери хотя бы одну оценку"
                needed == null -> "С такими оценками цель недостижима"
                needed == 0 -> "Цель уже достигнута — средний балл достаточно высокий"
                else -> {
                    val best = allowedMarks.max()
                    "Нужно ещё $needed ${ru.school28.diary.Util.marksWord(needed)} «$best», чтобы выйти на «$targetMark»"
                }
            }
            val resultColor = when {
                needed == 0 -> AppTheme.colors.success
                needed == null || allowedMarks.isEmpty() -> AppTheme.colors.error
                else -> AppTheme.colors.primary
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(resultColor.copy(alpha = 0.12f))
                    .padding(16.dp),
            ) {
                Text(resultText, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (allowedMarks.isEmpty()) return@Button
                    GoalStore.upsert(
                        Goal(
                            subjectId = subject.subjectId,
                            subjectName = subject.subjectName,
                            periodKey = periodKey,
                            targetMark = targetMark,
                            allowedMarks = allowedMarks.sortedDescending(),
                        )
                    )
                    onSaved()
                    onClose()
                },
                enabled = allowedMarks.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Установить цель")
            }
            if (GoalStore.find(subject.subjectId, periodKey) != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        GoalStore.remove(subject.subjectId, periodKey)
                        onSaved()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Удалить цель", color = AppTheme.colors.error)
                }
            }
        }
    }
}

@Composable
fun WeeklyOverlay(onClose: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var weekMarks by remember { mutableStateOf<List<Int>>(emptyList()) }
    var absences by remember { mutableStateOf(0) }
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var period by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loading = true
        try {
            val (monday, sunday) = ru.school28.diary.Util.weekRange(0)
            period = ru.school28.diary.Util.weekLabel(monday, sunday)
            val week = Repo.dairy(monday, sunday)
            weekMarks = week.mapNotNull { it.mark.removePrefix("!").trim().toIntOrNull() }
            absences = week.count { entry ->
                val m = entry.mark.removePrefix("!").trim().lowercase(Locale("ru"))
                m == "н" || m == "б" || m.contains("отсут")
            }
            subjects = Repo.marks()
        } catch (e: Exception) {
            AppLog.logException("weekly", e)
            error = e.message
        }
        loading = false
    }

    val avg = remember(weekMarks) {
        if (weekMarks.isEmpty()) null else weekMarks.sum().toDouble() / weekMarks.size
    }
    val rated = remember(subjects) {
        subjects.mapNotNull { s ->
            val values = s.marks.mapNotNull { it.mark.toIntOrNull() }
            if (values.size < 2) null else s to (values.sum().toDouble() / values.size)
        }
    }
    val best = remember(rated) { rated.maxByOrNull { it.second } }
    val worst = remember(rated) { rated.minByOrNull { it.second } }

    Overlay(onClose) {
        Column(
            Modifier
                .width(600.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)).background(AppTheme.colors.surfaceSolid)
                .verticalScroll(rememberScrollState())
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Итоги недели", color = AppTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text("✕", color = AppTheme.colors.textSecondary, fontSize = 20.sp, modifier = Modifier.clickable { onClose() }.padding(8.dp))
            }
            Text(period, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(14.dp))

            if (loading) {
                LoadingBox()
            } else {
                CardBox {
                    Text("Средний балл за неделю", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(
                        avg?.let { String.format(Locale("ru"), "%.2f", it) } ?: "—",
                        color = AppTheme.colors.text,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        if (weekMarks.isEmpty()) "На этой неделе оценок не было"
                        else "Оценок: ${weekMarks.size} · пропусков: $absences",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(
                        "Лучший предмет",
                        best?.first?.subjectName,
                        best?.let { String.format(Locale("ru"), "%.2f", it.second) },
                        Color(0xFF4DB6AC),
                        Modifier.weight(1f),
                    )
                    SummaryTile(
                        "Просел",
                        worst?.first?.subjectName,
                        worst?.let { String.format(Locale("ru"), "%.2f", it.second) },
                        Color(0xFFF5A623),
                        Modifier.weight(1f),
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("Не удалось загрузить данные: $error", color = AppTheme.colors.error, fontSize = 12.sp)
                }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun SummaryTile(title: String, name: String?, value: String?, tint: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surfaceSolid)
            .padding(16.dp),
    ) {
        Text(title, color = AppTheme.colors.textSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(name ?: "Нет данных", color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(tint))
            Spacer(Modifier.width(6.dp))
            Text(value ?: "—", color = AppTheme.colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun CriticalOverlay(onClose: () -> Unit, onPick: (SubjectMarks) -> Unit) {
    var subjects by remember { mutableStateOf<List<SubjectMarks>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            subjects = Repo.marks()
        } catch (e: Exception) {
            AppLog.logException("critical", e)
            error = e.message
        }
        loading = false
    }

    val critical = remember(subjects) { CriticalSubjects.find(subjects) }
    val byId = remember(subjects) { subjects.associateBy { it.subjectId } }

    Overlay(onClose) {
        Column(
            Modifier
                .width(600.dp)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)).background(AppTheme.colors.surfaceSolid)
                .padding(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Критические предметы",
                    color = AppTheme.colors.text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                )
                Text("✕", color = AppTheme.colors.textSecondary, fontSize = 20.sp, modifier = Modifier.clickable { onClose() }.padding(8.dp))
            }
            Spacer(Modifier.height(8.dp))

            when {
                loading -> LoadingBox()
                critical.isEmpty() -> EmptyBox("Критических предметов нет")
                else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        "Стоит взяться в первую очередь: средний балл ниже 3,5 или много «2».",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    critical.forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppTheme.colors.surfaceSolid)
                                .clickable { byId[item.subjectId]?.let(onPick) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SubjectAvatar(item.subjectId, item.subjectName, 36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    item.reasons.joinToString(" · "),
                                    color = AppTheme.colors.error,
                                    fontSize = 12.sp,
                                )
                                Text(
                                    "всего оценок: ${item.totalMarks}",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                            Text("Цель →", color = AppTheme.colors.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    if (error != null) {
                        Text("Не удалось загрузить оценки: $error", color = AppTheme.colors.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ToastLine(text: String, onDone: () -> Unit) {
    LaunchedEffect(text) {
        delay(2800)
        onDone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .padding(bottom = 26.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xE624272A))
                .padding(horizontal = 18.dp, vertical = 11.dp),
        ) {
            Text(text, color = Color.White, fontSize = 14.sp)
        }
    }
}
