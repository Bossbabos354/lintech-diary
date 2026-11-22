package ru.school28.diary.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.MainActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.glass.AnimatedNumberText
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassChip
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassLargeHeader
import ru.school28.diary.glass.GlassSkeletonCard
import ru.school28.diary.glass.GlassSurface
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.glass.StaggeredAppear
import ru.school28.diary.ui.HomeworkChecklist
import ru.school28.diary.widgets.WidgetSync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DairyScreen(
    activity: MainActivity?,
    onLessonClick: (DairyEntry) -> Unit = {},
    focusEntry: DairyEntry? = null,
    focusVersion: Int = 0,
    onFocusConsumed: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val type = Glass.type
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var highlightKey by remember { mutableStateOf<String?>(null) }
    val seenAppear = remember { mutableSetOf<Any>() }

    var entries by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var doneVersion by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var weekOffset by remember { mutableIntStateOf(0) }
    var weekLabel by remember { mutableStateOf("") }
    var offlineAt by remember { mutableStateOf<Long?>(null) }

    val daySdf = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ru")) }
    val shortSdf = remember { SimpleDateFormat("d MMM", Locale("ru")) }
    val weekdaySdf = remember { SimpleDateFormat("EE", Locale("ru")) }
    val dayNumberSdf = remember { SimpleDateFormat("d", Locale("ru")) }
    val timeSdf = remember { SimpleDateFormat("HH:mm", Locale("ru")) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val studentId = Prefs.getStudentId(ctx)
                val uchYear = Prefs.getUchYear(ctx)
                if (studentId == -1) return@launch

                if (Prefs.getClassId(ctx) == -1) {
                    val classId = withContext(Dispatchers.IO) {
                        ApiClient.getStudentClass(studentId, uchYear)
                    }
                    Prefs.setClassId(ctx, classId)
                }
                val classId = Prefs.getClassId(ctx)
                if (classId == -1) return@launch

                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, weekOffset)
                val dow = cal.get(Calendar.DAY_OF_WEEK)
                cal.add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val monday = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                val sunday = cal.time

                weekLabel = "${shortSdf.format(monday)} — ${shortSdf.format(sunday)}"

                val loaded = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, monday, sunday)
                }
                entries = loaded.sortedBy { it.date }
                offlineAt = ApiClient.offlineAt("dairy")
                WidgetSync.request(ctx)
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    val grouped = remember(entries) {
        entries.groupBy { dayKeyOf(it.date) }.toSortedMap()
    }

    // Переход из «Итогов недели»: докручиваем до нужного дня и подсвечиваем урок.
    LaunchedEffect(focusVersion, grouped, loading, offlineAt) {
        val target = focusEntry ?: return@LaunchedEffect
        if (loading) return@LaunchedEffect
        val dayIndex = grouped.keys.toList().indexOf(dayKeyOf(target.date))
        if (dayIndex >= 0) {
            val base = if (offlineAt != null) 3 else 2
            listState.animateScrollToItem(base + dayIndex)
        }
        highlightKey = lessonKey(target)
        delay(4000)
        highlightKey = null
        onFocusConsumed()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            GlassLargeHeader(
                title = "Дневник",
                subtitle = if (weekLabel.isEmpty()) "Загрузка недели…" else weekLabel,
                modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
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

        item(key = "week") {
            WeekSwitcher(
                weekLabel = weekLabel,
                weekOffset = weekOffset,
                onPrev = { weekOffset -= 7; load() },
                onNext = { weekOffset += 7; load() },
                onToday = { weekOffset = 0; load() },
            )
        }

        offlineAt?.let { ts ->
            item(key = "offline") {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassBadge("офлайн · данные от ${timeSdf.format(Date(ts))}", tint = colors.warning)
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        when {
            loading -> {
                items(3) { index ->
                    GlassSkeletonCard(
                        modifier = Modifier.padding(top = if (index == 0) 2.dp else 0.dp),
                        height = 150.dp,
                        lines = 4,
                    )
                }
            }

            error != null -> {
                item(key = "error") {
                    GlassCard {
                        GlassErrorState(message = error, onRetry = { load() })
                    }
                }
            }

            grouped.isEmpty() -> {
                item(key = "empty") {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.EventBusy,
                            title = "На этой неделе уроков нет",
                            message = "Переключи неделю или загляни позже — расписание обновляется с сайта школы.",
                        )
                    }
                }
            }

            else -> {
                grouped.entries.forEachIndexed { index, (_, dayEntries) ->
                    item(key = "day_${dayEntries.first().date.time}") {
                        StaggeredAppear(
                            index = index,
                            appearKey = "day_${dayEntries.first().date.time}",
                            seen = seenAppear,
                        ) {
                            DaySection(
                                dayEntries = dayEntries,
                                daySdf = daySdf,
                                weekdaySdf = weekdaySdf,
                                dayNumberSdf = dayNumberSdf,
                                doneVersion = doneVersion,
                                onToggleDone = { doneVersion++ },
                                onLessonClick = onLessonClick,
                                highlightKey = highlightKey,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekSwitcher(
    weekLabel: String,
    weekOffset: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val colors = Glass.colors
    GlassCard(contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = "Предыдущая неделя",
                onClick = onPrev,
                size = 38.dp,
            )
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = weekLabel.ifEmpty { "…" },
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Text(
                    text = if (weekOffset == 0) "текущая неделя" else "учебная неделя",
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                )
            }
            GlassIconButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = "Следующая неделя",
                onClick = onNext,
                size = 38.dp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            GlassChip(
                text = "Сегодня",
                selected = weekOffset == 0,
                onClick = onToday,
                compact = true,
            )
        }
    }
}

@Composable
private fun DaySection(
    dayEntries: List<DairyEntry>,
    daySdf: SimpleDateFormat,
    weekdaySdf: SimpleDateFormat,
    dayNumberSdf: SimpleDateFormat,
    doneVersion: Int,
    onToggleDone: () -> Unit,
    onLessonClick: (DairyEntry) -> Unit,
    highlightKey: String?,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val first = dayEntries.first().date
    val hwTotal = dayEntries.count { it.homework.isNotBlank() }
    val hwDone = remember(dayEntries, doneVersion) { HomeworkChecklist.doneCount(ctx, dayEntries) }
    val isToday = remember(first) {
        val a = Calendar.getInstance()
        val b = Calendar.getInstance().apply { time = first }
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }
    val progress = if (hwTotal == 0) 0f else hwDone.toFloat() / hwTotal

    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isToday) {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(colors.accent, colors.accentSecondary)
                            )
                        } else {
                            androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(colors.glassTop, colors.glassBottom)
                            )
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = weekdaySdf.format(first).uppercase(Locale("ru")),
                        color = if (isToday) colors.onAccent else colors.textSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        text = dayNumberSdf.format(first),
                        color = if (isToday) colors.onAccent else colors.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = daySdf.format(first)
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() },
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (hwTotal > 0) {
                    Text(
                        text = "Домашки: $hwDone из $hwTotal",
                        color = if (hwDone == hwTotal) colors.success else colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            if (hwTotal > 0) {
                Box(Modifier.width(38.dp), contentAlignment = Alignment.Center) {
                    GlassProgressRingSmall(progress = progress)
                }
            }
        }

        GlassCard(elevated = false, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
            dayEntries.forEachIndexed { index, entry ->
                LessonRow(
                    entry = entry,
                    doneVersion = doneVersion,
                    onToggleDone = onToggleDone,
                    onClick = { onLessonClick(entry) },
                    hapticTap = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    highlighted = highlightKey == lessonKey(entry),
                )
                if (index != dayEntries.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 70.dp, end = 16.dp)
                            .height(1.dp)
                            .background(colors.separator)
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassProgressRingSmall(progress: Float) {
    val colors = Glass.colors
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "dayProgress",
    )
    androidx.compose.foundation.Canvas(Modifier.size(28.dp)) {
        val stroke = 3.dp.toPx()
        drawArc(
            color = colors.textTertiary.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        drawArc(
            color = if (progress >= 1f) colors.success else colors.accent,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
private fun LessonRow(
    entry: DairyEntry,
    doneVersion: Int,
    onToggleDone: () -> Unit,
    onClick: () -> Unit,
    hapticTap: () -> Unit,
    highlighted: Boolean,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val isDone = remember(entry, doneVersion) { HomeworkChecklist.isDone(ctx, entry) }

    // Мигающая зелёная рамка вокруг урока, к которому пришли из «Итогов недели».
    val highlightPulse = if (highlighted) {
        val transition = rememberInfiniteTransition(label = "lessonHighlight")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(560), RepeatMode.Reverse),
            label = "highlightAlpha",
        )
        value
    } else 0f

    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                if (highlightPulse > 0f) {
                    val inset = 3.dp.toPx()
                    drawRoundRect(
                        color = colors.success.copy(alpha = highlightPulse),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            .clickable {
                hapticTap()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GradientAvatar(
            text = entry.subjectName.take(2),
            subjectId = entry.subjectId,
            size = 44.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.subjectName,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${entry.lessonNumber} урок",
                    color = colors.textTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (entry.theme.isNotBlank()) {
                Text(
                    text = entry.theme,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.homework.isNotBlank()) {
                Row(Modifier.padding(top = 3.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        tint = colors.accent.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp).padding(top = 2.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = entry.homework,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (entry.teacherFio.isNotBlank()) {
                Text(
                    text = entry.teacherFio,
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        if (entry.mark.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            MarkBadge(mark = entry.mark, size = 40.dp)
        }

        if (entry.homework.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            HomeworkCheck(isDone = isDone) {
                HomeworkChecklist.toggle(ctx, entry)
                onToggleDone()
            }
        }
    }
}

@Composable
private fun HomeworkCheck(isDone: Boolean, onToggle: () -> Unit) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 420f),
        label = "checkScale",
    )
    val bg by animateColorAsState(
        targetValue = if (isDone) colors.accent.copy(alpha = 0.18f) else colors.textTertiary.copy(alpha = 0.08f),
        animationSpec = tween(220),
        label = "checkBg",
    )
    val fg by animateColorAsState(
        targetValue = if (isDone) colors.accent else colors.textTertiary.copy(alpha = 0.55f),
        animationSpec = tween(220),
        label = "checkFg",
    )
    Box(
        Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bg)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = if (isDone) "Домашка сделана" else "Отметить домашку",
            tint = fg,
            modifier = Modifier.size(19.dp).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        )
    }
}

/** Ключ дня: год * 1000 + номер дня в году. */
private fun dayKeyOf(date: Date): Int {
    val c = Calendar.getInstance().apply { time = date }
    return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
}

/** Ключ урока для подсветки — совпадает с ключом элемента списка. */
private fun lessonKey(entry: DairyEntry): String =
    "${entry.date.time}|${entry.lessonNumber}|${entry.subjectId}"
