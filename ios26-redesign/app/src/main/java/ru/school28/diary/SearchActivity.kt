package ru.school28.diary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassChip
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassSheet
import ru.school28.diary.glass.GlassSurface
import ru.school28.diary.glass.GlassTextField
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.glass.SectionLabel
import ru.school28.diary.glass.StaggeredAppear
import ru.school28.diary.ui.screens.LessonSheet
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SearchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        enableGlassEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            DiaryTheme {
                SearchScreen(onBack = { finish() })
            }
        }
    }
}

private val SEARCH_DAY_NAMES = mapOf(
    "понедельник" to Calendar.MONDAY,
    "вторник" to Calendar.TUESDAY,
    "среда" to Calendar.WEDNESDAY,
    "четверг" to Calendar.THURSDAY,
    "пятница" to Calendar.FRIDAY,
    "суббота" to Calendar.SATURDAY,
    "воскресенье" to Calendar.SUNDAY,
    "пн" to Calendar.MONDAY,
    "вт" to Calendar.TUESDAY,
    "ср" to Calendar.WEDNESDAY,
    "чт" to Calendar.THURSDAY,
    "пт" to Calendar.FRIDAY,
    "сб" to Calendar.SATURDAY,
    "вс" to Calendar.SUNDAY,
)

private val DOT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
private val MONTH_TITLE: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))

private fun mondayOf(date: LocalDate): LocalDate =
    date.minusDays((date.dayOfWeek.value - 1).toLong())

private fun LocalDate.toStartDate(): Date =
    Date.from(atStartOfDay(ZoneId.systemDefault()).toInstant())

private fun LocalDate.toEndDate(): Date =
    Date.from(atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val ruLocale = remember { Locale("ru") }
    val dayFmt = remember { SimpleDateFormat("EEEE, d MMMM", ruLocale) }

    val today = remember { LocalDate.now() }
    var query by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf(mondayOf(today)) }
    var toDate by remember { mutableStateOf(mondayOf(today).plusDays(6)) }
    var quickIndex by remember { mutableIntStateOf(0) }
    var pickerTarget by remember { mutableStateOf<Int?>(null) }
    var results by remember { mutableStateOf<List<DairyEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }
    var expandedLesson by remember { mutableStateOf<DairyEntry?>(null) }
    val seenAppear = remember { mutableSetOf<Any>() }

    fun doSearch(start: LocalDate = fromDate, end: LocalDate = toDate) {
        val value = query.trim()
        if (value.isEmpty()) {
            Toast.makeText(ctx, "Введите предмет или день недели", Toast.LENGTH_SHORT).show()
            return
        }
        keyboard?.hide()
        val first = minOf(start, end)
        val last = maxOf(start, end)
        scope.launch {
            loading = true
            error = null
            try {
                val studentId = Prefs.getStudentId(ctx)
                val uchYear = Prefs.getUchYear(ctx)
                if (studentId == -1) return@launch

                var classId = Prefs.getClassId(ctx)
                if (classId == -1) {
                    classId = withContext(Dispatchers.IO) { ApiClient.getStudentClass(studentId, uchYear) }
                    Prefs.setClassId(ctx, classId)
                }

                val allEntries = withContext(Dispatchers.IO) {
                    ApiClient.getDairy(classId, studentId, first.toStartDate(), last.toEndDate())
                }

                val lower = value.lowercase(ruLocale)
                val requestedDay = SEARCH_DAY_NAMES[lower]
                val calendar = Calendar.getInstance()
                results = allEntries.filter { entry ->
                    val subjectMatch = entry.subjectName.lowercase(ruLocale).contains(lower)
                    val dayMatch = if (requestedDay != null) {
                        calendar.time = entry.date
                        calendar.get(Calendar.DAY_OF_WEEK) == requestedDay
                    } else {
                        false
                    }
                    subjectMatch || dayMatch
                }.sortedBy { it.date }
                searched = true
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    fun applyQuick(index: Int) {
        val monday = mondayOf(LocalDate.now())
        val start: LocalDate
        val end: LocalDate
        when (index) {
            1 -> {
                start = monday.minusDays(7)
                end = monday.minusDays(1)
            }
            2 -> {
                val now = LocalDate.now()
                start = now.withDayOfMonth(1)
                end = now.withDayOfMonth(now.lengthOfMonth())
            }
            else -> {
                start = monday
                end = monday.plusDays(6)
            }
        }
        fromDate = start
        toDate = end
        quickIndex = index
        pickerTarget = null
        if (query.isNotBlank()) doSearch(start, end)
    }

    val grouped = remember(results) {
        results.groupBy { entry ->
            val c = Calendar.getInstance().apply { time = entry.date }
            c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }.toSortedMap()
    }
    val rangeLabel = remember(fromDate, toDate) {
        "${SHORT_DATE.format(fromDate)} — ${SHORT_DATE.format(toDate)}"
    }

    Box(Modifier.fillMaxSize()) {
    GlassScreenScaffold(
        title = "Поиск",
        subtitle = "по предмету, дню недели или датам",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "query") {
                GlassTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Что ищем",
                    placeholder = "Например: математика или среда",
                    leadingIcon = Icons.Rounded.Search,
                )
            }

            item(key = "button") {
                GlassButton(
                    text = "Найти",
                    onClick = { doSearch() },
                    icon = Icons.Rounded.Search,
                    variant = GlassButtonVariant.Primary,
                    enabled = query.isNotBlank() && !loading,
                    loading = loading,
                    modifier = Modifier.fillMaxWidth(),
                    height = 48.dp,
                )
            }

            item(key = "periodLabel") {
                SectionLabel("Период · $rangeLabel")
            }

            item(key = "quick") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Текущая неделя", "Прошлая", "Месяц").forEachIndexed { index, label ->
                        GlassChip(
                            text = label,
                            selected = quickIndex == index,
                            onClick = { applyQuick(index) },
                            compact = true,
                        )
                    }
                }
            }

            item(key = "dates") {
                GlassCard(contentPadding = PaddingValues(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DateBox(
                            label = "От",
                            date = fromDate,
                            modifier = Modifier.weight(1f),
                            onClick = { pickerTarget = 0 },
                        )
                        Text(
                            text = "—",
                            color = colors.textTertiary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 6.dp),
                        )
                        DateBox(
                            label = "До",
                            date = toDate,
                            modifier = Modifier.weight(1f),
                            onClick = { pickerTarget = 1 },
                        )
                    }
                }
            }

            when {
                error != null -> {
                    item(key = "error") {
                        GlassCard { GlassErrorState(message = error, onRetry = { doSearch() }) }
                    }
                }

                searched && !loading && results.isEmpty() -> {
                    item(key = "empty") {
                        GlassCard {
                            GlassEmptyState(
                                icon = Icons.Rounded.SearchOff,
                                title = "Ничего не нашлось",
                                message = "Попробуй другое название предмета или измени диапазон дат.",
                            )
                        }
                    }
                }

                results.isNotEmpty() -> {
                    item(key = "count") {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            GlassBadge("${results.size} записей", tint = colors.accent)
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    grouped.entries.forEachIndexed { index, (_, dayEntries) ->
                        item(key = "day_${dayEntries.first().date.time}") {
                            StaggeredAppear(
                                index = index,
                                appearKey = "day_${dayEntries.first().date.time}",
                                seen = seenAppear,
                            ) {
                                SearchDay(dayEntries, dayFmt) { expandedLesson = it }
                            }
                        }
                    }
                }
            }
        }
    }

    pickerTarget?.let { target ->
        GlassCalendarSheet(
            initialDate = if (target == 0) fromDate else toDate,
            from = fromDate,
            to = toDate,
            onPick = { picked ->
                var newFrom = fromDate
                var newTo = toDate
                if (target == 0) {
                    newFrom = picked
                    if (newTo < newFrom) newTo = newFrom
                } else {
                    newTo = picked
                    if (newFrom > newTo) newFrom = newTo
                }
                fromDate = newFrom
                toDate = newTo
                quickIndex = -1
                pickerTarget = null
                if (query.isNotBlank()) doSearch(newFrom, newTo)
            },
            onDismiss = { pickerTarget = null },
        )
    }

    expandedLesson?.let { entry ->
        LessonSheet(entry = entry, onDismiss = { expandedLesson = null })
    }
    }
}

@Composable
private fun DateBox(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = Glass.colors
    GlassSurface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        elevated = false,
        interactive = true,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = label.uppercase(),
                    color = colors.textTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(
                text = DOT_DATE.format(date),
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
            )
        }
    }
}

@Composable
private fun GlassCalendarSheet(
    initialDate: LocalDate,
    from: LocalDate,
    to: LocalDate,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = Glass.colors
    var month by remember { mutableStateOf(YearMonth.from(initialDate)) }
    val today = remember { LocalDate.now() }
    val leading = month.atDay(1).dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val title = remember(month) {
        month.atDay(1).format(MONTH_TITLE)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
    }

    GlassSheet(
        onDismiss = onDismiss,
        title = "Выберите дату",
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassIconButton(
                icon = Icons.Rounded.ChevronLeft,
                contentDescription = "Предыдущий месяц",
                onClick = { month = month.minusMonths(1) },
                size = 36.dp,
            )
            Text(
                text = title,
                color = colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            GlassIconButton(
                icon = Icons.Rounded.ChevronRight,
                contentDescription = "Следующий месяц",
                onClick = { month = month.plusMonths(1) },
                size = 36.dp,
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").forEach { day ->
                Text(
                    text = day,
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        repeat(6) { week ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { dow ->
                    val dayNumber = week * 7 + dow - leading + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val isFrom = date == from
                        val isTo = date == to
                        val selected = isFrom || isTo
                        val inRange = date.isAfter(from) && date.isBefore(to)
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            selected -> Brush.linearGradient(
                                                listOf(colors.accent, colors.accentSecondary)
                                            )
                                            inRange -> Brush.linearGradient(
                                                listOf(
                                                    colors.accent.copy(alpha = 0.12f),
                                                    colors.accent.copy(alpha = 0.12f),
                                                )
                                            )
                                            else -> Brush.linearGradient(
                                                listOf(Color.Transparent, Color.Transparent)
                                            )
                                        }
                                    )
                                    .clickable { onPick(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = if (selected) colors.onAccent else colors.text,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                                )
                            }
                            if (date == today && !selected) {
                                Box(
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 4.dp)
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(colors.accent)
                                )
                            }
                        }
                    } else {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassChip(
                text = "Сегодня",
                selected = false,
                onClick = { onPick(today) },
                compact = true,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${DOT_DATE.format(from)} — ${DOT_DATE.format(to)}",
                color = colors.textSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SearchDay(
    entries: List<DairyEntry>,
    dayFmt: SimpleDateFormat,
    onLessonClick: (DairyEntry) -> Unit,
) {
    val colors = Glass.colors
    Column {
        Text(
            text = dayFmt.format(entries.first().date)
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() },
            color = colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp, bottom = 6.dp),
        )
        GlassCard(contentPadding = PaddingValues(0.dp)) {
            entries.forEachIndexed { index, entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onLessonClick(entry) }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GradientAvatar(
                        text = entry.subjectName.take(2),
                        subjectId = entry.subjectId,
                        size = 38.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = entry.subjectName,
                            color = colors.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (entry.theme.isNotBlank()) {
                            Text(
                                text = entry.theme,
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (entry.homework.isNotBlank()) {
                            Text(
                                text = "Д/з: ${entry.homework}",
                                color = colors.textTertiary,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (entry.mark.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        MarkBadge(mark = entry.mark, size = 34.dp)
                    }
                }
                if (index != entries.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 61.dp, end = 13.dp)
                            .height(1.dp)
                            .background(colors.separator)
                    )
                }
            }
        }
    }
}
