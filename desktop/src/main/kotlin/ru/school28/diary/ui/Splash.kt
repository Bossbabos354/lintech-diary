package ru.school28.diary.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import kotlinx.coroutines.isActive

private val TERMINAL_BG = Color(0xFF0A0E12)
private val TERMINAL_GREEN = Color(0xFF3DDC84)
private val TERMINAL_DIM = Color(0xFF5E6B63)
private val TERMINAL_ERROR = Color(0xFFFF6B6B)

enum class LineState { RUNNING, OK, ERROR }

data class StatusLine(
    val text: String,
    val state: LineState = LineState.RUNNING,
    val detail: Boolean = false,
)

/** Оригинальная заставка: терминал с реальными этапами загрузки. */
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

    Canvas(Modifier.fillMaxSize().background(TERMINAL_BG)) {
        val left = 20.dp.toPx()
        val lineH = 28.dp.toPx()
        var top = size.height * 0.16f

        lines.forEach { line ->
            if (line.detail) {
                drawTerminalLine(
                    textMeasurer, "  ${line.text}", left, top, TERMINAL_DIM, 0.9f,
                )
            } else {
                drawTerminalLine(
                    textMeasurer,
                    "> ${line.text}${suffix(line.state)}",
                    left,
                    top,
                    when (line.state) {
                        LineState.RUNNING -> TERMINAL_GREEN
                        LineState.OK -> TERMINAL_DIM
                        LineState.ERROR -> TERMINAL_ERROR
                    },
                    1f,
                )
            }
            top += lineH
        }

        val lastRunning = lines.lastOrNull()?.state == LineState.RUNNING
        if (lastRunning && (time / 380f).toInt() % 2 == 0) {
            drawRect(
                color = TERMINAL_GREEN,
                topLeft = Offset(left, top - lineH + 2.dp.toPx()),
                size = Size(9.dp.toPx(), 17.dp.toPx()),
            )
        }

        val barTop = size.height - 64.dp.toPx()
        val barLeft = left
        val barWidth = size.width - left * 2
        val barHeight = 6.dp.toPx()
        drawRoundRect(
            color = TERMINAL_DIM.copy(alpha = 0.35f),
            topLeft = Offset(barLeft, barTop),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barHeight / 2f),
        )
        val filled = barWidth * progress.coerceIn(0f, 1f)
        if (filled > 0f) {
            drawRoundRect(
                color = TERMINAL_GREEN,
                topLeft = Offset(barLeft, barTop),
                size = Size(filled, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
            )
        }
    }
}

private fun suffix(state: LineState): String = when (state) {
    LineState.RUNNING -> "..."
    LineState.OK -> " ок"
    LineState.ERROR -> " ошибка"
}

/** Финальная анимация: логотип и приветствие, затем переход в приложение. */
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

    Canvas(Modifier.fillMaxSize().background(TERMINAL_BG)) {
        val p = (time / 380f).coerceIn(0f, 1f)
        val alpha = 1f - (1f - p) * (1f - p) * (1f - p)
        val s = density * (0.9f + 0.1f * alpha)
        drawLogo(size.width / 2f, size.height * 0.44f, s, alpha)

        val wp = ((time - 380f) / 360f).coerceIn(0f, 1f)
        if (wp > 0f) {
            drawCenteredText(textMeasurer, "Добро пожаловать", 20.sp, size.height * 0.66f, wp)
        }
    }
}

private fun DrawScope.drawTerminalLine(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    alpha: Float,
) {
    if (text.isEmpty()) return
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
    )
    drawText(textLayoutResult = layout, topLeft = Offset(x, y), alpha = alpha)
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    fontSize: TextUnit,
    y: Float,
    alpha: Float,
) {
    val layout = textMeasurer.measure(
        text = text,
        style = TextStyle(color = Color.White, fontSize = fontSize, fontWeight = FontWeight.ExtraBold),
    )
    drawText(
        textLayoutResult = layout,
        topLeft = Offset(size.width / 2f - layout.size.width / 2f, y - layout.size.height / 2f),
        alpha = alpha,
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
        style = stroke,
    )
    drawLine(
        color = color,
        start = Offset(cx - 118f * s, cy - 100f * s),
        end = Offset(cx - 118f * s, cy - 40f * s),
        strokeWidth = 4f * s,
        cap = StrokeCap.Round,
    )
    for (i in 0 until 4) {
        val y = cy - 80f * s + i * 38f * s
        drawLine(
            color = color,
            start = Offset(cx - 74f * s, y),
            end = Offset(cx + 74f * s, y),
            strokeWidth = 4f * s,
            cap = StrokeCap.Round,
        )
    }
    val check = Path().apply {
        moveTo(cx - 50f * s, cy + 84f * s)
        lineTo(cx - 20f * s, cy + 114f * s)
        lineTo(cx + 24f * s, cy + 66f * s)
    }
    drawPath(check, color, style = stroke)
}
