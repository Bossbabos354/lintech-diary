package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.school28.diary.api.ApiClient
import java.util.Calendar

private val TERMINAL_BG = Color(0xFF0A0E12)
private val TERMINAL_GREEN = Color(0xFF3DDC84)
private val TERMINAL_DIM = Color(0xFF5E6B63)
private val TERMINAL_ERROR = Color(0xFFFF6B6B)

enum class LineState { RUNNING, OK, ERROR }

data class StatusLine(
    val text: String,
    val state: LineState = LineState.RUNNING,
    val detail: Boolean = false
)

class SplashActivity : ComponentActivity() {

    private val lines = mutableStateListOf<StatusLine>()
    private var progress by mutableFloatStateOf(0f)
    private var loadingDone by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        super.onCreate(savedInstanceState)

        val hasCreds = Prefs.getStudentId(this) != -1 &&
                Prefs.getLogin(this) != null && Prefs.getPassword(this) != null

        setContent {
            Crossfade(
                targetState = loadingDone,
                animationSpec = tween(350),
                label = "splash"
            ) { done ->
                if (done) {
                    LogoOutro(onFinished = { goNext(hasCreds) })
                } else {
                    TerminalSplash(lines = lines, progress = progress)
                }
            }
        }

        if (hasCreds) {
            startLoading()
        } else {
            lines.add(StatusLine("вход в аккаунт"))
            loadingDone = true
        }
    }

    // ---- Реальная загрузка ----

    private fun addLine(text: String): Int {
        lines.add(StatusLine(text, LineState.RUNNING))
        return lines.lastIndex
    }

    /** Строка-счётчик под этапом: мелкая, без префикса «>» и без статуса. */
    private fun addDetail(text: String) {
        lines.add(StatusLine(text, LineState.OK, detail = true))
    }

    private fun markState(index: Int, state: LineState) {
        if (index in lines.indices) lines[index] = lines[index].copy(state = state)
    }

    private fun startLoading() {
        lifecycleScope.launch {
            val finished = withTimeoutOrNull(25_000L) { loadAll() }
            if (finished == null) {
                // Не дождались сервера — помечаем последнюю строку и продолжаем
                val last = lines.lastIndex
                if (last >= 0 && lines[last].state == LineState.RUNNING) {
                    markState(last, LineState.ERROR)
                }
                lines.add(StatusLine("сервер не ответил, продолжаю", LineState.ERROR))
            }
            progress = 1f
            loadingDone = true
        }
    }

    private suspend fun loadAll() {
        val login = Prefs.getLogin(this) ?: return
        val password = Prefs.getPassword(this) ?: return
        val studentId = Prefs.getStudentId(this)

        // 1. Подключение к серверу (восстановление сессии + класс)
        val iConnect = addLine("подключение к серверу")
        try {
            withContext(Dispatchers.IO) {
                ApiClient.restoreSession(login, password)
                if (Prefs.getClassId(this@SplashActivity) == -1) {
                    val uchYear = Prefs.getUchYear(this@SplashActivity)
                    val classId = ApiClient.getStudentClass(studentId, uchYear)
                    Prefs.setClassId(this@SplashActivity, classId)
                }
            }
            markState(iConnect, LineState.OK)
            addDetail("сессия восстановлена")
            progress = 0.3f
        } catch (e: Exception) {
            FileLog.logException("splash/connect", e)
            markState(iConnect, LineState.ERROR)
        }

        val classId = Prefs.getClassId(this)
        val uchYear = Prefs.getUchYear(this)
        if (classId == -1) return

        // 2. Оценки (кладём в кэш ApiClient — экран «Оценки» откроется сразу)
        val iMarks = addLine("загрузка оценок")
        try {
            val subjects = withContext(Dispatchers.IO) {
                ApiClient.getMarks(classId, studentId, uchYear)
            }
            markState(iMarks, LineState.OK)

            val numeric = subjects.flatMap { it.marks }.mapNotNull { it.mark.toIntOrNull() }
            val marksCount = subjects.sumOf { s -> s.marks.count { it.mark.isNotBlank() } }
            val avg = if (numeric.isNotEmpty()) numeric.sum().toDouble() / numeric.size else null
            val avgText = if (avg == null) "—" else String.format(java.util.Locale("ru"), "%.2f", avg)
            addDetail("предметов: ${subjects.size} · оценок: $marksCount")
            addDetail("средний балл: $avgText")

            progress = 0.65f
        } catch (e: Exception) {
            FileLog.logException("splash/marks", e)
            markState(iMarks, LineState.ERROR)
        }

        // 3. Домашние задания (дневник текущей недели — тоже в кэш)
        val iDairy = addLine("загрузка домашних заданий")
        try {
            val (monday, sunday) = currentWeekRange()
            val lessons = withContext(Dispatchers.IO) {
                ApiClient.getDairy(classId, studentId, monday, sunday)
            }
            markState(iDairy, LineState.OK)

            val withHw = lessons.count { it.homework.isNotBlank() }
            val withMark = lessons.count { it.mark.isNotBlank() }
            val days = lessons.map { dayKey(it.date) }.distinct().size
            addDetail("уроков: ${lessons.size} · дней: $days")
            addDetail("с домашним заданием: $withHw · с оценкой: $withMark")

            progress = 0.9f
        } catch (e: Exception) {
            FileLog.logException("splash/dairy", e)
            markState(iDairy, LineState.ERROR)
        }

        addLine("готово").let { markState(it, LineState.OK) }
        progress = 1f
    }

    /** Ключ дня для подсчёта уникальных учебных дней. */
    private fun dayKey(date: java.util.Date): Int {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun currentWeekRange(): Pair<java.util.Date, java.util.Date> {
        val cal = Calendar.getInstance()
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
        return monday to sunday
    }

    private fun goNext(hasCreds: Boolean) {
        if (isFinishing) return
        if (hasCreds) {
            if (Prefs.isNotifEnabled(this)) {
                NotificationScheduler.schedule(this)
            }
        }
        val target = if (hasCreds) Intent(this, MainActivity::class.java)
        else Intent(this, LoginActivity::class.java)
        target.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(target)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

/**
 * Терминал с реальными этапами загрузки: строки приходят из SplashActivity,
 * поэтому текст на экране отражает настоящие запросы к серверу.
 */
@Composable
fun TerminalSplash(lines: List<StatusLine>, progress: Float) {
    val textMeasurer = rememberTextMeasurer()
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now -> time = (now - start) / 1_000_000f }
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .background(TERMINAL_BG)
    ) {
        val left = 20.dp.toPx()
        val lineH = 28.dp.toPx()
        var top = size.height * 0.16f

        lines.forEach { line ->
            if (line.detail) {
                drawTerminalLine(
                    textMeasurer = textMeasurer,
                    text = "  ${line.text}",
                    x = left,
                    y = top,
                    color = TERMINAL_DIM,
                    alpha = 0.9f
                )
            } else {
                drawTerminalLine(
                    textMeasurer = textMeasurer,
                    text = "> ${line.text}${suffix(line.state)}",
                    x = left,
                    y = top,
                    color = when (line.state) {
                        LineState.RUNNING -> TERMINAL_GREEN
                        LineState.OK -> TERMINAL_DIM
                        LineState.ERROR -> TERMINAL_ERROR
                    },
                    alpha = 1f
                )
            }
            top += lineH
        }

        // Мигающий курсор на текущей (последней) строке, пока идёт загрузка
        val lastRunning = lines.lastOrNull()?.state == LineState.RUNNING
        if (lastRunning && (time / 380f).toInt() % 2 == 0) {
            drawRect(
                color = TERMINAL_GREEN,
                topLeft = Offset(left, top - lineH + 2.dp.toPx()),
                size = Size(9.dp.toPx(), 17.dp.toPx())
            )
        }

        // Реальный прогресс-бар
        val barTop = size.height - 64.dp.toPx()
        val barLeft = left
        val barWidth = size.width - left * 2
        val barHeight = 6.dp.toPx()
        drawRoundRect(
            color = TERMINAL_DIM.copy(alpha = 0.35f),
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f)
        )
        val filled = (barWidth * progress.coerceIn(0f, 1f))
        if (filled > 0f) {
            drawRoundRect(
                color = TERMINAL_GREEN,
                topLeft = Offset(barLeft, barTop),
                size = Size(filled, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f)
            )
        }
    }
}

private fun suffix(state: LineState): String = when (state) {
    LineState.RUNNING -> "..."
    LineState.OK -> " ок"
    LineState.ERROR -> " ошибка"
}

/** Короткая финальная анимация: логотип и приветствие, затем переход. */
@Composable
fun LogoOutro(onFinished: () -> Unit) {
    val textMeasurer = rememberTextMeasurer()
    var time by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { now -> time = (now - start) / 1_000_000f }
            if (time >= 1250f) {
                onFinished()
                break
            }
        }
    }

    Canvas(
        Modifier
            .fillMaxSize()
            .background(TERMINAL_BG)
    ) {
        val p = (time / 380f).coerceIn(0f, 1f)
        val alpha = 1f - (1f - p) * (1f - p) * (1f - p)
        val s = density * (0.9f + 0.1f * alpha)
        drawLogo(size.width / 2f, size.height * 0.44f, s, alpha)

        val wp = ((time - 380f) / 360f).coerceIn(0f, 1f)
        if (wp > 0f) {
            drawCenteredText(
                textMeasurer = textMeasurer,
                text = "Добро пожаловать",
                fontSize = 20.sp,
                y = size.height * 0.66f,
                alpha = wp
            )
        }
    }
}

private fun DrawScope.drawTerminalLine(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    alpha: Float
) {
    if (text.isEmpty()) return
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = color,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    )
    drawText(textLayoutResult = layout, topLeft = Offset(x, y), alpha = alpha)
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    fontSize: TextUnit,
    y: Float,
    alpha: Float
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold
        )
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(size.width / 2f - layout.size.width / 2f, y - layout.size.height / 2f),
        alpha = alpha
    )
}

private fun DrawScope.drawLogo(cx: Float, cy: Float, s: Float, alpha: Float) {
    val color = Color.White.copy(alpha = alpha)
    val stroke = Stroke(width = 4f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)

    drawRoundRect(
        color = color,
        topLeft = Offset(cx - 100f * s, cy - 130f * s),
        size = Size(200f * s, 260f * s),
        cornerRadius = CornerRadius(24f * s),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(cx - 118f * s, cy - 100f * s),
        end = Offset(cx - 118f * s, cy - 40f * s),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round
    )
    for (i in 0 until 4) {
        val y = cy - 80f * s + i * 38f * s
        drawLine(
            color = color,
            start = Offset(cx - 74f * s, y),
            end = Offset(cx + 74f * s, y),
            strokeWidth = 4f * s,
            cap = StrokeCap.Round
        )
    }
    val check = Path().apply {
        moveTo(cx - 50f * s, cy + 84f * s)
        lineTo(cx - 20f * s, cy + 114f * s)
        lineTo(cx + 24f * s, cy + 66f * s)
    }
    drawPath(check, color, style = stroke)
}