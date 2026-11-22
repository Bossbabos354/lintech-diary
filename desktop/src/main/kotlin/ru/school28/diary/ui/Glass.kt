package ru.school28.diary.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Живой градиентный фон с цветными пятнами — замена Android-«авроре». */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )

    Canvas(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.background, colors.backgroundSecondary))),
    ) {
        fun blob(cx: Float, cy: Float, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx, cy),
            )
        }
        val w = size.width
        val h = size.height
        blob(w * (0.14f + 0.08f * phase), h * (0.12f + 0.06f * phase), w * 0.48f, colors.blobA)
        blob(w * (0.86f - 0.06f * phase), h * (0.24f + 0.07f * phase), w * 0.42f, colors.blobB)
        blob(w * (0.76f + 0.05f * phase), h * (0.86f - 0.06f * phase), w * 0.46f, colors.blobC)
        blob(w * (0.20f - 0.04f * phase), h * (0.82f + 0.04f * phase), w * 0.38f, colors.blobD)
    }
}

/** Стеклянная поверхность: мягкая тень, полупрозрачный градиент и светлая рамка. */
fun Modifier.glass(shape: Shape, colors: AppColors): Modifier =
    this
        .shadow(16.dp, shape, clip = false)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(colors.glassTop, colors.glassBottom)))
        .border(
            1.dp,
            Brush.verticalGradient(listOf(colors.glassBorderTop, colors.glassBorderBottom)),
            shape,
        )
