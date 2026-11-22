package ru.school28.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.api.SubjectMarks
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassSheet
import ru.school28.diary.glass.GlassSurface
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.ui.Goal
import ru.school28.diary.ui.GoalMath
import ru.school28.diary.ui.GoalPeriods
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.MarkStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalSheet(
    subject: SubjectMarks,
    initialPeriodKey: String?,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors

    val existing = remember { GoalStore.find(ctx, subject.subjectId, initialPeriodKey ?: "YEAR") }
    val startPeriod = initialPeriodKey ?: existing?.periodKey ?: "YEAR"

    var periodKey by remember { mutableStateOf(startPeriod) }
    var targetMark by remember { mutableStateOf(existing?.targetMark ?: 5) }
    var allowedMarks by remember {
        mutableStateOf((existing?.allowedMarks ?: listOf(5)).toSet())
    }
    var periodExpanded by remember { mutableStateOf(false) }

    val currentLessons = remember(periodKey, subject) { GoalMath.marksForLessons(subject, periodKey) }
    val currentMarks = remember(currentLessons) { currentLessons.mapNotNull { it.mark.toIntOrNull() } }
    val avg = remember(currentLessons) { GoalMath.weightedAverage(currentLessons) }
    val rounded = GoalMath.roundedMark(avg)
    val needed = remember(currentLessons, targetMark, allowedMarks) {
        GoalMath.needed(currentLessons, targetMark, allowedMarks.toList())
    }

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
        needed == 0 -> colors.success
        needed == null || allowedMarks.isEmpty() -> colors.danger
        else -> colors.accent
    }

    GlassSheet(
        onDismiss = onDismiss,
        title = "Цель по предмету",
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(
                text = subject.subjectName.take(2),
                subjectId = subject.subjectId,
                size = 46.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = subject.subjectName,
                    color = colors.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                subject.teacherName?.takeIf { it.isNotBlank() }?.let {
                    Text(it, color = colors.textSecondary, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        GoalBlock("Текущие оценки") {
            if (currentMarks.isEmpty()) {
                Text("Оценок за этот период нет", color = colors.textSecondary, fontSize = 14.sp)
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    currentMarks.forEach { mark ->
                        val color = MarkStyle.color(mark.toString())
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(color.copy(alpha = 0.15f))
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = mark.toString(),
                                color = color,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                val avgText = if (avg == null) "—" else String.format(Locale("ru"), "%.2f", avg)
                Text(
                    text = "Средний: $avgText" + (rounded?.let { " · тянет на «$it»" } ?: ""),
                    color = colors.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(9.dp))

        GoalBlock("За какой период") {
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(colors.textTertiary.copy(alpha = 0.08f))
                        .clickable { periodExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = GoalPeriods.byKey(periodKey).label,
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Выбрать период",
                        tint = colors.textSecondary,
                    )
                }
                DropdownMenu(
                    expanded = periodExpanded,
                    onDismissRequest = { periodExpanded = false },
                    containerColor = if (colors.isDark) Color(0xFF21232F) else Color.White,
                    shape = RoundedCornerShape(18.dp),
                ) {
                    GoalPeriods.all.forEach { period ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = period.label,
                                    color = colors.text,
                                    fontSize = 15.sp,
                                )
                            },
                            onClick = {
                                periodExpanded = false
                                periodKey = period.key
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        GoalBlock("Какую оценку хочешь") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 4, 3).forEach { mark ->
                    MarkChoice(
                        mark = mark,
                        selected = targetMark == mark,
                        onClick = { targetMark = mark },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        GoalBlock("Какими оценками набирать") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(5, 4, 3, 2).forEach { mark ->
                    MarkChoice(
                        mark = mark,
                        selected = mark in allowedMarks,
                        onClick = {
                            allowedMarks = if (mark in allowedMarks) allowedMarks - mark else allowedMarks + mark
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = "Можно выбрать несколько — посчитаем по лучшей",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(resultColor.copy(alpha = 0.12f))
                .border(1.dp, resultColor.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                .padding(14.dp),
        ) {
            Text(
                text = resultText,
                color = resultColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(14.dp))

        GlassButton(
            text = "Установить цель",
            onClick = {
                if (allowedMarks.isEmpty()) return@GlassButton
                GoalStore.upsert(
                    ctx,
                    Goal(
                        subjectId = subject.subjectId,
                        subjectName = subject.subjectName,
                        periodKey = periodKey,
                        targetMark = targetMark,
                        allowedMarks = allowedMarks.sortedDescending(),
                    ),
                )
                onDismiss()
            },
            enabled = allowedMarks.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp,
        )

        val saved = GoalStore.find(ctx, subject.subjectId, periodKey)
        if (saved != null) {
            Spacer(Modifier.height(8.dp))
            GlassButton(
                text = "Удалить цель",
                onClick = {
                    GoalStore.remove(ctx, subject.subjectId, periodKey)
                    onDismiss()
                },
                variant = GlassButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
                height = 46.dp,
            )
        }
    }
}

@Composable
private fun GoalBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = Glass.colors
    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        elevated = false,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = title.uppercase(),
                color = colors.textTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun MarkChoice(
    mark: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    val color = MarkStyle.color(mark.toString())
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) {
                    Brush.linearGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0.14f)))
                } else {
                    Brush.linearGradient(
                        listOf(colors.glassTop.copy(alpha = 0.4f), colors.glassBottom.copy(alpha = 0.25f))
                    )
                }
            )
            .border(
                1.dp,
                if (selected) color.copy(alpha = 0.5f) else colors.glassBorderTop.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp),
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = mark.toString(),
                color = if (selected) color else colors.text,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private fun plural(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "ку"
        mod10 in 2..4 && mod100 !in 12..14 -> "ки"
        else -> "ок"
    }
}
