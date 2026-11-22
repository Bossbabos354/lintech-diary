package ru.school28.diary

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Widgets
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
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassChip
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.SectionLabel
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui.HomeworkChecklist
import ru.school28.diary.widgets.AvgMarkWidget
import ru.school28.diary.widgets.DayPeriod
import ru.school28.diary.widgets.DiaryWidget
import ru.school28.diary.widgets.GoalWidget
import ru.school28.diary.widgets.HomeworkWidget
import ru.school28.diary.widgets.NearestLessonWidget
import ru.school28.diary.widgets.ScheduleWidget
import ru.school28.diary.widgets.WidgetDataStore
import ru.school28.diary.widgets.WidgetSync
import ru.school28.diary.widgets.WidgetTheme

class WidgetGalleryActivity : ComponentActivity() {
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
                WidgetGalleryScreen(onBack = { finish() })
            }
        }
    }
}

private val MORNING = listOf(Color(0xFFFFD3A5), Color(0xFFFD9E88))
private val DAY = listOf(Color(0xFFB3E5FC), Color(0xFFB39DDB))
private val EVENING = listOf(Color(0xFFFF9A8B), Color(0xFF6A4FBE))
private val NIGHT = listOf(Color(0xFF2B2D42), Color(0xFF4A3F6B))
private val PASTEL = listOf(Color(0xFFF3E5F5), Color(0xFFE1D5F5))

private val TEXT_DARK = Color(0xFF24272A)
private val TEXT_DARK_SOFT = Color(0xFF5E656B)
private val TEXT_WHITE = Color.White
private val TEXT_WHITE_SOFT = Color(0xE6FFFFFF)

private fun periodBrush(): Brush = when (WidgetTheme.currentPeriod()) {
    DayPeriod.MORNING -> Brush.horizontalGradient(MORNING)
    DayPeriod.DAY -> Brush.horizontalGradient(DAY)
    DayPeriod.EVENING -> Brush.horizontalGradient(EVENING)
    DayPeriod.NIGHT -> Brush.horizontalGradient(NIGHT)
}

private fun coloredBrush(theme: String, kind: String): Brush = when (theme) {
    WidgetTheme.LIGHT -> when (kind) {
        "nearest" -> Brush.verticalGradient(listOf(Color(0xFF955DCD), Color(0xFFB39DDB), Color(0xFFF8BBD0)))
        "avg" -> Brush.verticalGradient(listOf(Color(0xFF4DB6AC), Color(0xFF80CBC4), Color(0xFFB39DDB)))
        else -> Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFFB39DDB)))
    }
    WidgetTheme.PASTEL -> when (kind) {
        "nearest" -> Brush.verticalGradient(listOf(Color(0xFFB9A0E0), Color(0xFFCBB8E8), Color(0xFFF9D7E7)))
        "avg" -> Brush.verticalGradient(listOf(Color(0xFF7FCFC5), Color(0xFFA8DEDA), Color(0xFFCBB8E8)))
        else -> Brush.verticalGradient(listOf(Color(0xFFCDEFFC), Color(0xFFCBB8E8)))
    }
    WidgetTheme.DARK -> when (kind) {
        "nearest" -> Brush.verticalGradient(listOf(Color(0xFF3B2D5C), Color(0xFF4A3B6B), Color(0xFF5C4A78)))
        "avg" -> Brush.verticalGradient(listOf(Color(0xFF24413E), Color(0xFF2F5350), Color(0xFF3B4A63)))
        else -> Brush.verticalGradient(listOf(Color(0xFF263447), Color(0xFF3A2E5A)))
    }
    else -> periodBrush()
}

private fun markTone(mark: String): Pair<Color, Color> {
    val digit = mark.firstOrNull { it.isDigit() }?.digitToIntOrNull()
    return when {
        digit == null -> Color(0xFFB39DDB) to Color(0xFFF1EBFB)
        digit >= 5 -> Color(0xFF4DB6AC) to Color(0xFFE4F4F1)
        digit == 4 -> Color(0xFFF5A623) to Color(0xFFFFF3E0)
        else -> Color(0xFFB39DDB) to Color(0xFFF1EBFB)
    }
}

private fun lightBrush(theme: String): Brush = when (theme) {
    WidgetTheme.LIGHT -> Brush.horizontalGradient(listOf(Color.White, Color.White))
    WidgetTheme.PASTEL -> Brush.horizontalGradient(PASTEL)
    WidgetTheme.DARK -> Brush.horizontalGradient(listOf(Color(0xFF24272A), Color(0xFF24272A)))
    else -> periodBrush()
}

private fun lightText(theme: String): Pair<Color, Color> = when (theme) {
    WidgetTheme.DARK -> TEXT_WHITE to TEXT_WHITE_SOFT
    WidgetTheme.LIGHT, WidgetTheme.PASTEL -> TEXT_DARK to TEXT_DARK_SOFT
    else -> when (WidgetTheme.currentPeriod()) {
        DayPeriod.NIGHT, DayPeriod.EVENING -> TEXT_WHITE to TEXT_WHITE_SOFT
        else -> TEXT_DARK to TEXT_DARK_SOFT
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WidgetGalleryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var data by remember { mutableStateOf(WidgetDataStore.load(ctx)) }
    var theme by remember { mutableStateOf(Prefs.getWidgetTheme(ctx)) }
    val goals = remember { GoalStore.load(ctx) }
    var goalId by remember { mutableStateOf(Prefs.getWidgetGoalId(ctx)) }
    val selectedGoal = goals.firstOrNull { it.id == goalId }
    val (lightPrimary, lightSecondary) = lightText(theme)

    GlassScreenScaffold(
        title = "Виджеты",
        subtitle = "Предпросмотр, тема и добавление на главный экран",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                GlassCard(tint = colors.accent, contentPadding = PaddingValues(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconBubble(
                            icon = Icons.Rounded.Widgets,
                            tint = colors.accent,
                            size = 44.dp,
                            iconSize = 22.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Виджеты ЛинТех 28",
                                color = colors.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "6 виджетов: урок, средний балл, дневник, домашка, цель и расписание",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Добавить: зажми пустое место на главном экране → «Виджеты» → «ЛинТех 28».",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                    )
                }
            }

            item(key = "themeLabel") { SectionLabel("Тема виджетов") }

            item(key = "themes") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    listOf(
                        WidgetTheme.LIGHT to "Светлая",
                        WidgetTheme.PASTEL to "Пастель",
                        WidgetTheme.DARK to "Тёмная",
                        WidgetTheme.AUTO to "Авто",
                    ).forEach { (value, label) ->
                        GlassChip(
                            text = label,
                            selected = theme == value,
                            onClick = {
                                theme = value
                                Prefs.setWidgetTheme(ctx, value)
                                WidgetSync.force(ctx)
                            },
                            compact = true,
                        )
                    }
                }
            }

            item(key = "themeHint") {
                Text(
                    text = "Тема применяется ко всем виджетам. «Авто» — фон по времени суток: рассвет 5–10, день 10–17, закат 17–22, ночь 22–5.",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }

            item(key = "nearest") {
                WidgetCard(
                    title = "Ближайший урок",
                    sizeLabel = "4×2 · тема на выбор",
                    aspectRatio = 250f / 110f,
                    brush = coloredBrush(theme, "nearest"),
                    onAdd = { pin(ctx, NearestLessonWidget::class.java, "Ближайший урок") },
                ) {
                    Text("СЛЕДУЮЩИЙ УРОК", color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(data.nearestTime ?: "—", color = TEXT_WHITE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(data.nearestSubject ?: "Нет уроков", color = TEXT_WHITE, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(data.nearestDetails ?: "", color = TEXT_WHITE_SOFT, fontSize = 10.sp)
                }
            }

            item(key = "avg") {
                WidgetCard(
                    title = "Средний балл",
                    sizeLabel = "2×2 · тема на выбор",
                    aspectRatio = 1f,
                    brush = coloredBrush(theme, "avg"),
                    onAdd = { pin(ctx, AvgMarkWidget::class.java, "Средний балл") },
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(data.avgMark ?: "—", color = TEXT_WHITE, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Text("средний балл", color = TEXT_WHITE_SOFT, fontSize = 10.sp)
                            data.avgDelta?.takeIf { it.isNotBlank() }?.let {
                                Text(it, color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item(key = "diary") {
                WidgetCard(
                    title = "Дневник на сегодня",
                    sizeLabel = "4×4 · светлый, тема на выбор",
                    aspectRatio = 1f,
                    brush = lightBrush(theme),
                    captionColor = Color(0xFF9AA0B5),
                    onAdd = { pin(ctx, DiaryWidget::class.java, "Дневник") },
                ) {
                    Text(data.diaryHeader ?: "Сегодня", color = lightPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    if (data.diaryRows.isEmpty()) {
                        Text("Нет уроков", color = lightSecondary, fontSize = 12.sp)
                    } else {
                        data.diaryRows.take(3).forEach { row ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    row.time ?: "—",
                                    color = Color(0xFF757575),
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(48.dp),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        row.subject ?: "",
                                        color = lightPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                    if (!row.homework.isNullOrBlank()) {
                                        Text(row.homework, color = lightSecondary, fontSize = 10.sp, maxLines = 1)
                                    }
                                }
                                val mark = row.mark.orEmpty()
                                if (mark.isNotBlank()) {
                                    val tone = markTone(mark)
                                    Box(
                                        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tone.second),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(mark, color = tone.first, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                    data.diaryMore?.takeIf { it.isNotBlank() }?.let {
                        Text(it, color = Color(0xFF955DCD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item(key = "homework") {
                WidgetCard(
                    title = "Домашка на сегодня",
                    sizeLabel = "4×3 · галочки прямо на экране",
                    aspectRatio = 250f / 150f,
                    brush = lightBrush(theme),
                    captionColor = Color(0xFF9AA0B5),
                    onAdd = { pin(ctx, HomeworkWidget::class.java, "Домашка") },
                ) {
                    val hw = data.homeworkRows
                    val hwDone = HomeworkChecklist.doneCountKeys(ctx, hw.map { it.key })
                    val hwAll = hw.isNotEmpty() && hwDone >= hw.size
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Домашка на сегодня",
                            color = lightPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (hwAll) Color(0x332EBE72) else Color(0x335A5AF2))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                if (hwAll) "готово" else "$hwDone / ${hw.size}",
                                color = if (hwAll) Color(0xFF2EBE72) else Color(0xFF5A5AF2),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (hw.isEmpty()) {
                        Text("На сегодня домашки нет", color = lightSecondary, fontSize = 10.sp)
                    } else {
                        hw.take(3).forEach { hwRow ->
                            val isDone = HomeworkChecklist.isDone(ctx, hwRow.key)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(26.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isDone) Color(0xFF5A5AF2) else Color(0x14111219)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isDone) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(9.dp),
                                            tint = Color.White,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(7.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        hwRow.subject ?: "",
                                        color = if (isDone) lightPrimary.copy(alpha = 0.66f) else lightPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                    Text(
                                        hwRow.task ?: "",
                                        color = if (isDone) lightSecondary.copy(alpha = 0.5f) else lightSecondary,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (goals.isNotEmpty()) {
                item(key = "goalPickerLabel") { SectionLabel("Цель для виджета") }
                item(key = "goalPicker") {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        goals.forEach { goal ->
                            GlassChip(
                                text = "${goal.subjectName} · «${goal.targetMark}»",
                                selected = goal.id == goalId,
                                onClick = {
                                    goalId = goal.id
                                    Prefs.setWidgetGoalId(ctx, goal.id)
                                    WidgetSync.force(ctx)
                                    data = data.copy(goalTitle = "Цель: ${goal.subjectName}")
                                },
                                compact = true,
                            )
                        }
                    }
                }
            }

            item(key = "goal") {
                WidgetCard(
                    title = "Цель по предмету",
                    sizeLabel = "4×2 · светлый, тема на выбор",
                    aspectRatio = 250f / 110f,
                    brush = lightBrush(theme),
                    captionColor = Color(0xFF9AA0B5),
                    onAdd = { pin(ctx, GoalWidget::class.java, "Цель") },
                ) {
                    Text(
                        selectedGoal?.let { "Цель: ${it.subjectName}" } ?: (data.goalTitle ?: "Целей нет"),
                        color = lightPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        data.goalLine ?: "Поставь цель в разделе «Оценки»",
                        color = lightSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(10.dp))
                    ProgressPreview(data.goalProgress, Color(0xFFB39DDB), Color(0xFFF0EDF7))
                    data.goalCurrent?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Color(0xFF9AA0B5), fontSize = 10.sp)
                    }
                }
            }

            item(key = "schedule") {
                WidgetCard(
                    title = "Расписание на завтра",
                    sizeLabel = "4×2 · тема на выбор",
                    aspectRatio = 250f / 110f,
                    brush = coloredBrush(theme, "schedule"),
                    onAdd = { pin(ctx, ScheduleWidget::class.java, "Расписание") },
                ) {
                    Text("РАСПИСАНИЕ", color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    val lines = data.scheduleLines.ifEmpty { listOf("Уроков нет") }
                    lines.take(6).forEach { line ->
                        Text(line, color = TEXT_WHITE, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(Modifier.height(5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(
    title: String,
    sizeLabel: String,
    aspectRatio: Float,
    brush: Brush,
    onAdd: () -> Unit,
    captionColor: Color = Color.White.copy(alpha = 0.55f),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Glass.colors
    GlassCard(contentPadding = PaddingValues(12.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(brush)
                .padding(16.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, content = content)
            Text(
                text = "Цифровой помощник",
                color = captionColor,
                fontSize = 7.sp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sizeLabel, color = colors.textSecondary, fontSize = 11.sp)
            }
            GlassButton(
                text = "Добавить",
                onClick = onAdd,
                variant = GlassButtonVariant.Tinted,
                height = 38.dp,
            )
        }
    }
}

@Composable
private fun ProgressPreview(progress: Int, primary: Color, secondary: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(secondary.copy(alpha = 0.25f))
    ) {
        Box(
            Modifier
                .fillMaxWidth((progress.coerceIn(0, 100)) / 100f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(primary.copy(alpha = 0.8f))
        )
    }
}

private fun pin(ctx: android.content.Context, provider: Class<*>, label: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mgr = AppWidgetManager.getInstance(ctx)
        if (mgr.isRequestPinAppWidgetSupported) {
            mgr.requestPinAppWidget(ComponentName(ctx, provider), null, null)
            return
        }
    }
    Toast.makeText(ctx, "Добавь виджет «$label» через меню главного экрана", Toast.LENGTH_LONG).show()
}
