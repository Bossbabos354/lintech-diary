package ru.school28.diary.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.school28.diary.Repo
import ru.school28.diary.Util
import ru.school28.diary.api.DairyEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Поиск по дневнику: текущая неделя или последний месяц. */
@Composable
fun SearchOverlay(onClose: () -> Unit, onPick: (DairyEntry) -> Unit) {
    var query by remember { mutableStateOf("") }
    var month by remember { mutableStateOf(false) }
    var entries by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, month) {
        val q = query.trim()
        if (q.length < 2) {
            entries = emptyList()
            loading = false
            return@LaunchedEffect
        }
        delay(250)
        loading = true
        error = null
        try {
            val range = if (month) {
                val end = Date()
                Date(end.time - 30L * 24 * 3600 * 1000) to end
            } else {
                Util.weekRange(0)
            }
            entries = Repo.dairy(range.first, range.second)
                .filter {
                    it.subjectName.contains(q, true) ||
                        it.theme.contains(q, true) ||
                        it.homework.contains(q, true)
                }
                .sortedByDescending { it.date }
        } catch (e: Exception) {
            error = e.message
        }
        loading = false
    }

    Box(
        Modifier.fillMaxSize().background(AppTheme.colors.scrim).clickable { onClose() },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.clickable(enabled = false) {}) {
            Column(
                Modifier
                    .width(720.dp)
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(AppTheme.colors.surfaceSolid)
                    .padding(22.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Поиск",
                        color = AppTheme.colors.text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "✕",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 20.sp,
                        modifier = Modifier.clickable { onClose() }.padding(8.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Предмет, тема или домашнее задание") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Неделя", !month) { month = false }
                    Chip("Месяц", month) { month = true }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                    error != null -> Text("Ошибка: $error", color = AppTheme.colors.error, fontSize = 13.sp)
                    query.trim().length < 2 -> Text(
                        "Введите минимум 2 символа",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp,
                    )
                    entries.isEmpty() -> Text("Ничего не найдено", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    else -> SmoothLazyColumn(Modifier.fillMaxSize()) {
                        items(entries) { entry ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AppTheme.colors.background)
                                    .clickable { onPick(entry) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${SimpleDateFormat("dd.MM", Locale("ru")).format(entry.date)} · " +
                                            "${entry.lessonNumber}. ${entry.subjectName}",
                                        color = AppTheme.colors.text,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                    )
                                    if (entry.theme.isNotBlank()) {
                                        Text(entry.theme, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                                    }
                                    if (entry.homework.isNotBlank()) {
                                        Text("Д/з: ${entry.homework}", color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                                    }
                                }
                                if (entry.mark.isNotBlank()) MarkChip(entry.mark, 16)
                            }
                        }
                    }
                }
            }
        }
    }
}
