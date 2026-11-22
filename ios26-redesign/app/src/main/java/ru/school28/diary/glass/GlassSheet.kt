package ru.school28.diary.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun GlassSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    contentPadding: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Glass.colors
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    val dragOffset = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    fun close() {
        if (closing) return
        closing = true
        scope.launch {
            progress.animateTo(0f, tween(durationMillis = 190))
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 170f),
        )
    }

    BackHandler { close() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()
        val dismissThreshold = 110.dp.value * density

        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim.copy(alpha = colors.scrim.alpha * progress.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { close() },
        )

        val sheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
        Column(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.92f)
                .graphicsLayer {
                    translationY = dragOffset.value + (1f - progress.value) * heightPx
                }
                .clip(sheetShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (colors.isDark) Color(0xFF1B1D2A).copy(alpha = 0.96f) else Color.White.copy(alpha = 0.94f),
                            if (colors.isDark) Color(0xFF12131C).copy(alpha = 0.98f) else Color(0xFFF8F8FC).copy(alpha = 0.97f),
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(colors.glassBorderTop, colors.glassBorderBottom),
                    ),
                    shape = sheetShape,
                )
                .imePadding()
                .navigationBarsPadding(),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, amount ->
                                change.consume()
                                scope.launch {
                                    dragOffset.snapTo((dragOffset.value + amount).coerceAtLeast(0f))
                                }
                            },
                            onDragEnd = {
                                if (dragOffset.value > dismissThreshold) {
                                    close()
                                } else {
                                    scope.launch {
                                        dragOffset.animateTo(0f, spring(stiffness = 500f))
                                    }
                                }
                            },
                            onDragCancel = {
                                scope.launch { dragOffset.animateTo(0f, spring(stiffness = 500f)) }
                            },
                        )
                    }
                    .padding(top = 9.dp, bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .width(38.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(colors.textTertiary.copy(alpha = 0.35f)),
                )
            }

            if (title != null) {
                androidx.compose.foundation.layout.Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = title,
                        color = colors.text,
                        fontSize = 21.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.textTertiary.copy(alpha = 0.12f))
                            .clickable { close() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Закрыть",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            val contentModifier = if (scrollable) {
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            } else {
                Modifier
            }
            Column(
                modifier = contentModifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                content = content,
            )
        }
    }
}
