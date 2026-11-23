package ru.school28.diary.legacy.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.legacy.ui.Goal
import ru.school28.diary.legacy.ui.GoalMath
import ru.school28.diary.legacy.ui.GoalPeriods
import ru.school28.diary.legacy.ui.GoalStore
import ru.school28.diary.legacy.ui.MarkStyle
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.ButtonVariant
import ru.school28.diary.ui_components.components.Text
import java.util.Locale

/**
 * Полноэкранная карточка цели по предмету.
 * Позволяет выбрать период, целевой балл и набор оценок, показывает,
 * сколько ещё оценок нужно, и сохраняет цель.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalSheet(
    subject: SubjectMarks,
    initialPeriodKey: String?,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current

    val existing = remember { GoalStore.find(ctx, subject.subjectId, initialPeriodKey ?: "YEAR") }
    val startPeriod = initialPeriodKey
        ?: existing?.periodKey
        ?: "YEAR"

    var periodKey by remember { mutableStateOf(startPeriod) }
    var targetMark by remember { mutableStateOf(existing?.targetMark ?: 5) }
    var allowedMarks by remember {
        mutableStateOf((existing?.allowedMarks ?: listOf(5)).toSet())
    }
    var periodExpanded by remember { mutableStateOf(false) }

    BackHandler(onBack = onDismiss)

    val currentLessons = remember(periodKey, subject) {
        GoalMath.marksForLessons(subject, periodKey)
    }
    val currentMarks = remember(currentLessons) { currentLessons.mapNotNull { it.mark.toIntOrNull() } }
    val avg = remember(currentLessons) { GoalMath.weightedAverage(currentLessons) }
    val rounded = GoalMath.roundedMark(avg)
    val needed = remember(currentLessons, targetMark, allowedMarks) {
        GoalMath.needed(currentLessons, targetMark, allowedMarks.toList())
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Цель по предмету", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(subject.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.outline.copy(alpha = 0.4f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Текущие оценки
            SectionBlock("Текущие оценки") {
                if (currentMarks.isEmpty()) {
                    Text("Оценок за этот период нет", color = AppTheme.colors.textSecondary, fontSize = 14.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        currentMarks.forEach { m ->
                            val c = MarkStyle.color(m.toString())
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(c.copy(alpha = 0.15f))
                                    .padding(horizontal = 9.dp, vertical = 3.dp)
                            ) {
                                Text(m.toString(), color = c, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val avgText = if (avg == null) "—" else String.format(Locale("ru"), "%.2f", avg)
                    Text(
                        "Средний: $avgText" + (rounded?.let { " · тянет на «$it»" } ?: ""),
                        color = AppTheme.colors.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Период
            SectionBlock("За какой период") {
                Box {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.background)
                            .clickable { periodExpanded = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            GoalPeriods.byKey(periodKey).label,
                            color = AppTheme.colors.text,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Выбрать период", tint = AppTheme.colors.textSecondary)
                    }
                    DropdownMenu(
                        expanded = periodExpanded,
                        onDismissRequest = { periodExpanded = false },
                        containerColor = AppTheme.colors.surface
                    ) {
                        GoalPeriods.all.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.label, color = AppTheme.colors.text, fontSize = 15.sp) },
                                onClick = {
                                    periodExpanded = false
                                    periodKey = p.key
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Целевая оценка
            SectionBlock("Какую оценку хочешь") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 4, 3).forEach { mark ->
                        ChoiceChip(
                            text = mark.toString(),
                            selected = targetMark == mark,
                            onClick = { targetMark = mark },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Набор оценок
            SectionBlock("Какими оценками набирать") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 4, 3, 2).forEach { mark ->
                        ChoiceChip(
                            text = mark.toString(),
                            selected = mark in allowedMarks,
                            onClick = {
                                allowedMarks = if (mark in allowedMarks) allowedMarks - mark else allowedMarks + mark
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Можно выбрать несколько — посчитаем по лучшей из них",
                    color = AppTheme.colors.textSecondary, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(14.dp))

            // Результат расчёта
            val resultText = remember(needed, targetMark, allowedMarks, avg, currentMarks) {
                when {
                    allowedMarks.isEmpty() -> "Выбери хотя бы одну оценку"
                    needed == null -> "С такими оценками цель недостижима"
                    needed == 0 -> "Цель уже достигнута — средний балл достаточно высокий"
                    else -> {
                        val best = allowedMarks.max()
                        "Нужно ещё $needed оцен${plural(needed)} «$best», чтобы выйти на «$targetMark»"
                    }
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
                    .padding(16.dp)
            ) {
                Text(resultText, color = resultColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(18.dp))

            Button(
                text = "Установить цель",
                onClick = {
                    if (allowedMarks.isEmpty()) return@Button
                    GoalStore.upsert(
                        ctx,
                        Goal(
                            subjectId = subject.subjectId,
                            subjectName = subject.subjectName,
                            periodKey = periodKey,
                            targetMark = targetMark,
                            allowedMarks = allowedMarks.sortedDescending()
                        )
                    )
                    onDismiss()
                },
                enabled = allowedMarks.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            )

            val saved = GoalStore.find(ctx, subject.subjectId, periodKey)
            if (saved != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    text = "Удалить цель",
                    variant = ButtonVariant.Destructive,
                    onClick = {
                        GoalStore.remove(ctx, subject.subjectId, periodKey)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(AppTheme.colors.surface)
            .padding(14.dp)
    ) {
        Text(title, color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) AppTheme.colors.primary else AppTheme.colors.background
    val fg = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.text
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, color = fg, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
        }
    }
}

/** Русское склонение слова «оценка»: 1 оценку, 2 оценки, 5 оценок. */
private fun plural(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "ку"
        mod10 in 2..4 && mod100 !in 12..14 -> "ки"
        else -> "ок"
    }
}