package ru.school28.diary.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.MainActivity
import ru.school28.diary.Prefs
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.ui.HomeworkChecklist
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.ui.subjectColor
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.ButtonVariant
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.widgets.WidgetSync
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun screenTextColor() = AppTheme.colors.text

@Composable
fun DairyScreen(activity: MainActivity?, onLessonClick: (DairyEntry) -> Unit = {}) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var doneVersion by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var weekOffset by remember { mutableStateOf(0) }
    var weekLabel by remember { mutableStateOf("") }
    var offlineAt by remember { mutableStateOf<Long?>(null) }

    val daySdf = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
    val dayShortSdf = SimpleDateFormat("d MMM", Locale("ru"))

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
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                val monday = cal.time
                cal.add(Calendar.DAY_OF_YEAR, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                val sunday = cal.time

                weekLabel = "${dayShortSdf.format(monday)} — ${dayShortSdf.format(sunday)}"

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

    // Группировка по дням
    val grouped = remember(entries) {
        entries.groupBy { entry ->
            val c = Calendar.getInstance().apply { time = entry.date }
            c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }.toSortedMap()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                text = "◀",
                variant = ButtonVariant.SecondaryGhost,
                onClick = { weekOffset -= 7; load() },
                modifier = Modifier.height(40.dp)
            )
            Text(
                text = weekLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Button(
                text = "▶",
                variant = ButtonVariant.SecondaryGhost,
                onClick = { weekOffset += 7; load() },
                modifier = Modifier.height(40.dp)
            )
        }
        Button(
            text = "Сегодня",
            variant = ButtonVariant.Secondary,
            onClick = { weekOffset = 0; load() },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(36.dp)
        )
        Spacer(Modifier.height(4.dp))

        offlineAt?.let { ts ->
            Text(
                "офлайн · данные от " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(ts)),
                color = AppTheme.colors.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(4.dp))
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ошибка: $error", color = AppTheme.colors.error)
                }
            }
            grouped.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет данных за эту неделю", color = AppTheme.colors.textSecondary)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (_, dayEntries) ->
                        item(key = "day_${dayEntries.first().date.time}") {
                            DayCard(dayEntries, daySdf, onLessonClick, doneVersion) { doneVersion++ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    dayEntries: List<DairyEntry>,
    daySdf: SimpleDateFormat,
    onLessonClick: (DairyEntry) -> Unit,
    doneVersion: Int,
    onToggleDone: () -> Unit
) {
    val ctx = LocalContext.current
    val hwTotal = dayEntries.count { it.homework.isNotBlank() }
    val hwDone = remember(dayEntries, doneVersion) { HomeworkChecklist.doneCount(ctx, dayEntries) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppTheme.colors.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = daySdf.format(dayEntries.first().date),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = AppTheme.colors.text
            )
            if (hwTotal > 0) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Д/з $hwDone из $hwTotal",
                    color = if (hwDone == hwTotal) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        dayEntries.forEach { entry ->
            LessonCard(entry, doneVersion, onToggleDone) { onLessonClick(entry) }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LessonCard(
    entry: DairyEntry,
    doneVersion: Int,
    onToggleDone: () -> Unit,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val isDone = remember(entry, doneVersion) { HomeworkChecklist.isDone(ctx, entry) }
    // Плавное появление карточки при первом показе
    val appear by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "appear"
    )

    val hasMark = entry.mark.isNotEmpty()
    val glow = if (hasMark) markGlowColor(entry.mark) else null

    val bgModifier = if (glow != null) {
        Modifier.drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glow.copy(alpha = 0.38f),
                        glow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width, size.height / 2f),
                    radius = size.width * 1.15f
                )
            )
        }
    } else {
        Modifier.background(AppTheme.colors.surface)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = appear
                scaleX = 0.97f + 0.03f * p
                scaleY = 0.97f + 0.03f * p
                alpha = p
                translationY = (1f - p) * 12f
            }
            .clip(RoundedCornerShape(20.dp))
            .then(bgModifier)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Аватар предмета
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(subjectColor(entry.subjectId)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MenuBook,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(entry.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(entry.theme.ifEmpty { "—" }, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            if (entry.homework.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AppTheme.colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Д/з: ${entry.homework}",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 6,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.OpenInFull,
                        contentDescription = "Развернуть",
                        tint = AppTheme.colors.textDisabled,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
            if (entry.teacherFio.isNotEmpty()) {
                Text(entry.teacherFio, color = AppTheme.colors.textDisabled, fontSize = 11.sp)
            }
        }

        if (entry.mark.isNotEmpty()) {
            val raw = entry.mark.removePrefix("!")
            val markColor = MarkStyle.color(entry.mark)
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(markColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = raw,
                    color = markColor,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }

        if (entry.homework.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDone) AppTheme.colors.primary.copy(alpha = 0.15f)
                        else AppTheme.colors.surface
                    )
                    .clickable {
                        HomeworkChecklist.toggle(ctx, entry)
                        onToggleDone()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isDone) "✓" else "○",
                    color = if (isDone) AppTheme.colors.primary else AppTheme.colors.textDisabled,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
private fun markGlowColor(mark: String): Color {
    val v = mark.removePrefix("!").toIntOrNull()
    return when {
        v != null && v >= 5 -> Color(0xFF4DB6AC)   // мятный
        v != null && v == 4 -> Color(0xFFFFD54F)   // жёлтый
        v != null && v == 3 -> Color(0xFFFFB74D)   // оранжевый
        else -> Color(0xFFEF9A9A)                  // розовый-красный
    }
}
