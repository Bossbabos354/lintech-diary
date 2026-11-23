
package ru.school28.diary

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import ru.school28.diary.ui.GoalStore
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.ButtonVariant
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.widgets.AvgMarkWidget
import ru.school28.diary.widgets.DayPeriod
import ru.school28.diary.widgets.DiaryWidget
import ru.school28.diary.widgets.GoalWidget
import ru.school28.diary.widgets.NearestLessonWidget
import ru.school28.diary.widgets.ScheduleWidget
import ru.school28.diary.widgets.WidgetSync
import ru.school28.diary.widgets.WidgetDataStore
import ru.school28.diary.widgets.WidgetTheme

/**
 * Экран виджетов: предпросмотр, выбор темы и добавление на главный экран.
 */
class WidgetGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
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

/** Градиенты виджетов по выбранной теме — как в макете. */
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

@Composable
private fun WidgetGalleryScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var data by remember { mutableStateOf(WidgetDataStore.load(ctx)) }
    var theme by remember { mutableStateOf(Prefs.getWidgetTheme(ctx)) }
    val goals = remember { GoalStore.load(ctx) }
    var goalId by remember { mutableStateOf(Prefs.getWidgetGoalId(ctx)) }
    val selectedGoal = goals.firstOrNull { it.id == goalId }

    val (lightPrimary, lightSecondary) = lightText(theme)

    Column(Modifier.fillMaxSize().background(AppTheme.colors.background)) {
        // Шапка
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
            Text("Виджеты", color = AppTheme.colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            // Тема светлых виджетов
            Text("Тема виджетов", color = AppTheme.colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeChip("Светлая", theme == WidgetTheme.LIGHT, Modifier.weight(1f)) {
                    theme = WidgetTheme.LIGHT; Prefs.setWidgetTheme(ctx, theme); WidgetSync.force(ctx)
                }
                ThemeChip("Пастель", theme == WidgetTheme.PASTEL, Modifier.weight(1f)) {
                    theme = WidgetTheme.PASTEL; Prefs.setWidgetTheme(ctx, theme); WidgetSync.force(ctx)
                }
                ThemeChip("Тёмная", theme == WidgetTheme.DARK, Modifier.weight(1f)) {
                    theme = WidgetTheme.DARK; Prefs.setWidgetTheme(ctx, theme); WidgetSync.force(ctx)
                }
                ThemeChip("Авто", theme == WidgetTheme.AUTO, Modifier.weight(1f)) {
                    theme = WidgetTheme.AUTO; Prefs.setWidgetTheme(ctx, theme); WidgetSync.force(ctx)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Тема применяется ко всем виджетам. «Авто» — фон по времени суток: рассвет 5–10, день 10–17, закат 17–22, ночь 22–5.",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(16.dp))

            // Ближайший урок
            WidgetCard(
                title = "Ближайший урок",
                sizeLabel = "4×2 · тема на выбор",
                aspectRatio = 250f / 110f,
                brush = coloredBrush(theme, "nearest"),
                onAdd = { pin(ctx, NearestLessonWidget::class.java, "Ближайший урок") }
            ) {
                Text(data.nearestLabel ?: "СЛЕДУЮЩИЙ УРОК", color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(data.nearestTime ?: "—", color = TEXT_WHITE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(data.nearestSubject ?: "Нет уроков", color = TEXT_WHITE, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(data.nearestDetails ?: "", color = TEXT_WHITE_SOFT, fontSize = 10.sp)
            }
            Spacer(Modifier.height(12.dp))

            // Средний балл
            WidgetCard(
                title = "Средний балл",
                sizeLabel = "2×2 · тема на выбор",
                aspectRatio = 1f,
                brush = coloredBrush(theme, "avg"),
                onAdd = { pin(ctx, AvgMarkWidget::class.java, "Средний балл") }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(data.avgMark ?: "—", color = TEXT_WHITE, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                        Text("средний балл", color = TEXT_WHITE_SOFT, fontSize = 10.sp)
                        if (!data.avgDelta.isNullOrBlank()) {
                            Text(data.avgDelta ?: "", color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Дневник
            WidgetCard(
                title = "Дневник на сегодня",
                sizeLabel = "4×4 · светлый, тема на выбор",
                aspectRatio = 1f,
                brush = lightBrush(theme),
                captionColor = Color(0xFF9AA0B5),
                onAdd = { pin(ctx, DiaryWidget::class.java, "Дневник") }
            ) {
                Text(data.diaryHeader ?: "Сегодня", color = lightPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                if (data.diaryRows.isEmpty()) {
                    Text("Нет уроков", color = lightSecondary, fontSize = 12.sp)
                } else {
                    data.diaryRows.take(3).forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(row.time ?: "—", color = Color(0xFF757575), fontSize = 10.sp,
                                modifier = Modifier.width(48.dp))
                            Column(Modifier.weight(1f)) {
                                Text(row.subject ?: "", color = lightPrimary, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold, maxLines = 1)
                                if (!row.homework.isNullOrBlank()) {
                                    Text(row.homework, color = lightSecondary, fontSize = 10.sp, maxLines = 1)
                                }
                            }
                            val mark = row.mark.orEmpty()
                            if (mark.isNotBlank()) {
                                val tone = markTone(mark)
                                Box(
                                    Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(tone.second),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mark, color = tone.first, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                if (!data.diaryMore.isNullOrBlank()) {
                    Text(data.diaryMore ?: "", color = Color(0xFF955DCD), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Цель для виджета
            if (goals.isNotEmpty()) {
                Text(
                    "Цель для виджета",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goals.forEach { g ->
                        ThemeChip(
                            label = "${g.subjectName} · «${g.targetMark}»",
                            selected = g.id == goalId,
                            modifier = Modifier.height(36.dp)
                        ) {
                            goalId = g.id
                            Prefs.setWidgetGoalId(ctx, g.id)
                            WidgetSync.force(ctx)
                            data = data.copy(goalTitle = "Цель: ${g.subjectName}")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Цель
            WidgetCard(
                title = "Цель по предмету",
                sizeLabel = "4×2 · светлый, тема на выбор",
                aspectRatio = 250f / 110f,
                brush = lightBrush(theme),
                captionColor = Color(0xFF9AA0B5),
                onAdd = { pin(ctx, GoalWidget::class.java, "Цель") }
            ) {
                Text(
                    selectedGoal?.let { "Цель: ${it.subjectName}" } ?: (data.goalTitle ?: "Целей нет"),
                    color = lightPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    data.goalLine ?: "Поставь цель в разделе «Оценки»",
                    color = lightSecondary, fontSize = 11.sp, maxLines = 1
                )
                Spacer(Modifier.height(10.dp))
                ProgressPreview(data.goalProgress, Color(0xFFB39DDB), Color(0xFFF0EDF7))
                if (!data.goalCurrent.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(data.goalCurrent ?: "", color = Color(0xFF9AA0B5), fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(12.dp))

            // Расписание
            WidgetCard(
                title = "Расписание на завтра",
                sizeLabel = "4×2 · тема на выбор",
                aspectRatio = 250f / 110f,
                brush = coloredBrush(theme, "schedule"),
                onAdd = { pin(ctx, ScheduleWidget::class.java, "Расписание") }
            ) {
                Text(data.scheduleHeader ?: "РАСПИСАНИЕ", color = TEXT_WHITE_SOFT, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                val lines = data.scheduleLines.ifEmpty { listOf("Уроков нет") }
                lines.take(6).forEach { line ->
                    Text(line, color = TEXT_WHITE, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(5.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) AppTheme.colors.primary else AppTheme.colors.surface)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppTheme.colors.surface)
            .padding(12.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(20.dp))
                .background(brush)
                .padding(16.dp)
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, content = content)
            Text(
                "Цифровой помощник",
                color = captionColor,
                fontSize = 7.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(sizeLabel, color = AppTheme.colors.textSecondary, fontSize = 11.sp)
            }
            Button(
                text = "Добавить",
                variant = ButtonVariant.Primary,
                onClick = onAdd,
                modifier = Modifier.height(40.dp)
            )
        }
    }
}

@Composable
private fun ProgressPreview(progress: Int, primary: Color, secondary: Color) {
    Column(Modifier.fillMaxWidth()) {
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