package ru.school28.diary.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LessonSheet(entry: DairyEntry, onDismiss: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(durationMillis = 220, easing = FastOutLinearInEasing))
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.8f,
                stiffness = 120f
            )
        )
    }

    BackHandler(onBack = { close() })

    val colors = AppTheme.colors
    val p = progress.value
    val contentAlpha = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val dateSdf = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale("ru")) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim.copy(alpha = colors.scrim.alpha * p))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { close() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .graphicsLayer {
                    rotationX = (1f - p) * 88f
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    cameraDistance = 12f * density
                    alpha = (p * 2.5f).coerceAtMost(1f)
                    scaleX = 0.96f + 0.04f * p
                }
                .shadow(14.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .drawBehind {
                    val fold = 1f - p
                    if (fold > 0.01f) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.30f * fold),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.5f
                            )
                        )
                    }
                }
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Box(Modifier.graphicsLayer { alpha = contentAlpha }) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.subjectName,
                                color = colors.text,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "Урок №${entry.lessonNumber}",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                        if (entry.mark.isNotEmpty()) {
                            val mc = MarkStyle.color(entry.mark)
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(mc.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = entry.mark.removePrefix("!"),
                                    color = mc,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                        }
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.outline.copy(alpha = 0.35f))
                                .clickable { close() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Закрыть",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.outline)
                    )
                    Spacer(Modifier.height(14.dp))

                    SheetField("Дата", dateSdf.format(entry.date), colors.text, colors.textSecondary)
                    if (entry.teacherFio.isNotEmpty()) {
                        SheetField("Учитель", entry.teacherFio, colors.text, colors.textSecondary)
                    }
                    SheetField("Тема урока", entry.theme.ifEmpty { "—" }, colors.text, colors.textSecondary)

                    Spacer(Modifier.height(6.dp))
                    Text("Домашнее задание", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = entry.homework.ifEmpty { "Задано не было" },
                            color = if (entry.homework.isEmpty()) colors.textSecondary else colors.text,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetField(label: String, value: String, ink: Color, inkSoft: Color) {
    Column(Modifier.padding(bottom = 12.dp)) {
        Text(label, color = inkSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = ink, fontSize = 16.sp)
    }
}
