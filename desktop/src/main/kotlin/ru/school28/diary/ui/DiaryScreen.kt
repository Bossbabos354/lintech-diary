package ru.school28.diary.ui

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
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.school28.diary.AppLog
import ru.school28.diary.AppState
import ru.school28.diary.Util
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry

@Composable
fun DiaryScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var weekOffset by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var offlineAt by remember { mutableStateOf<Long?>(null) }
    var weekLabel by remember { mutableStateOf("") }
    var doneVersion by remember { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val (monday, sunday) = Util.weekRange(weekOffset)
                weekLabel = Util.weekLabel(monday, sunday)
                entries = ru.school28.diary.Repo.dairy(monday, sunday).sortedBy { it.date }
                offlineAt = ApiClient.offlineAt("dairy")
            } catch (e: Exception) {
                AppLog.logException("diary", e)
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(weekOffset, reload) { load() }

    val grouped = remember(entries) {
        entries.groupBy { Util.dayKey(it.date) }.toSortedMap()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Дневник", weekLabel)

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = { weekOffset-- }, shape = RoundedCornerShape(12.dp)) { Text("◀") }
            Text(
                weekLabel,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = AppTheme.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
            OutlinedButton(onClick = { weekOffset++ }, shape = RoundedCornerShape(12.dp)) { Text("▶") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { weekOffset = 0; reload++ }, shape = RoundedCornerShape(12.dp)) {
                Text("Сегодня")
            }
        }
        OfflineLabel(offlineAt)

        when {
            loading -> LoadingBox()
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Ошибка: $error", color = AppTheme.colors.error)
            }
            grouped.isEmpty() -> EmptyBox("Нет данных за эту неделю")
            else -> SmoothLazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp,
                ),
            ) {
                grouped.forEach { (_, dayEntries) ->
                    item(key = "day_${dayEntries.first().date.time}") {
                        DayCard(dayEntries, doneVersion, onOpen = { state.lesson = it }) {
                            doneVersion++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCard(
    entries: List<DairyEntry>,
    doneVersion: Int,
    onOpen: (DairyEntry) -> Unit,
    onToggleDone: () -> Unit,
) {
    val hwTotal = entries.count { it.homework.isNotBlank() }
    val hwDone = remember(entries, doneVersion) { HomeworkChecklist.doneCount(entries) }

    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(AppTheme.colors.primary))
            Spacer(Modifier.width(8.dp))
            Text(
                Util.dayLabel(entries.first().date),
                color = AppTheme.colors.text,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
            )
            if (hwTotal > 0) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Д/з $hwDone из $hwTotal",
                    color = if (hwDone == hwTotal) AppTheme.colors.primary else AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        entries.forEach { entry ->
            LessonCard(entry, doneVersion, onOpen, onToggleDone)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LessonCard(
    entry: DairyEntry,
    doneVersion: Int,
    onOpen: (DairyEntry) -> Unit,
    onToggleDone: () -> Unit,
) {
    val isDone = remember(entry, doneVersion) { HomeworkChecklist.isDone(entry) }
    val markColor = if (entry.mark.isNotEmpty()) MarkStyle.color(entry.mark) else null

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (markColor != null) {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(AppTheme.colors.surface, markColor.copy(alpha = 0.10f)),
                    )
                } else {
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(AppTheme.colors.surface, AppTheme.colors.surface),
                    )
                }
            )
            .clickable { onOpen(entry) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubjectAvatar(entry.subjectId, entry.subjectName)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.subjectName, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(entry.theme.ifEmpty { "—" }, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            if (entry.homework.isNotEmpty()) {
                Text(
                    "Д/з: ${entry.homework}",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.teacherFio.isNotEmpty()) {
                Text(entry.teacherFio, color = AppTheme.colors.textDisabled, fontSize = 11.sp)
            }
        }
        if (entry.mark.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            MarkChip(entry.mark, 18)
        }
        if (entry.homework.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDone) AppTheme.colors.primary.copy(alpha = 0.15f) else AppTheme.colors.surface)
                    .clickable {
                        HomeworkChecklist.toggle(entry)
                        onToggleDone()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isDone) "✓" else "○",
                    color = if (isDone) AppTheme.colors.primary else AppTheme.colors.textDisabled,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}
