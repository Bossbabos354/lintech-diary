package ru.school28.diary.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape.card,
    tint: Color? = null,
    elevated: Boolean = true,
    blurred: Boolean = false,
    interactive: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && interactive) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 420f),
        label = "glassScale",
    )

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (elevated && !DevicePerf.isLowEnd(LocalContext.current)) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = if (colors.isDark) 0.28f else 0.09f),
                        spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.38f else 0.14f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .drawWithCache {
                val top = tint?.copy(alpha = if (colors.isDark) 0.22f else 0.26f) ?: colors.glassTop
                val bottom = tint?.copy(alpha = if (colors.isDark) 0.08f else 0.12f) ?: colors.glassBottom
                val fill = Brush.linearGradient(listOf(top, bottom))
                val sheen = Brush.linearGradient(
                    colors = listOf(
                        (tint ?: colors.glassHighlight).copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.Transparent,
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width * 0.9f, size.height * 0.75f),
                )
                val highlight = colors.glassHighlight.copy(alpha = if (colors.isDark) 0.16f else 0.5f)
                onDrawBehind {
                    drawRect(fill)
                    drawRect(sheen)
                    drawLine(
                        color = highlight,
                        start = Offset(size.width * 0.14f, 1.dp.toPx()),
                        end = Offset(size.width * 0.86f, 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(colors.glassBorderTop, colors.glassBorderBottom),
                ),
                shape = shape,
            )
            .then(clickableModifier)
    ) {
        Box(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShape.card,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        shape = shape,
        tint = tint,
        elevated = elevated,
        interactive = onClick != null,
        onClick = onClick,
        contentPadding = contentPadding,
    ) {
        Column(content = content)
    }
}

enum class GlassButtonVariant { Primary, Tinted, Plain, Destructive, Ghost }

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassButtonVariant = GlassButtonVariant.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    loading: Boolean = false,
    height: Dp = 52.dp,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.972f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 460f),
        label = "buttonScale",
    )

    val contentColor = when (variant) {
        GlassButtonVariant.Primary -> colors.onAccent
        GlassButtonVariant.Tinted -> colors.accent
        GlassButtonVariant.Plain -> colors.text
        GlassButtonVariant.Destructive -> colors.danger
        GlassButtonVariant.Ghost -> colors.accent
    }

    val backgroundBrush = when (variant) {
        GlassButtonVariant.Primary -> Brush.linearGradient(
            listOf(colors.accent, colors.accentSecondary),
        )
        GlassButtonVariant.Tinted -> Brush.linearGradient(
            listOf(colors.accent.copy(alpha = 0.2f), colors.accentSecondary.copy(alpha = 0.12f)),
        )
        GlassButtonVariant.Destructive -> Brush.linearGradient(
            listOf(colors.danger.copy(alpha = 0.18f), colors.danger.copy(alpha = 0.1f)),
        )
        GlassButtonVariant.Plain -> Brush.linearGradient(listOf(colors.glassTop, colors.glassBottom))
        GlassButtonVariant.Ghost -> Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .then(
                if (variant == GlassButtonVariant.Primary && !DevicePerf.isLowEnd(LocalContext.current)) {
                    Modifier.shadow(
                        elevation = 16.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = colors.accent.copy(alpha = 0.4f),
                        spotColor = colors.accent.copy(alpha = 0.55f),
                    )
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(backgroundBrush)
            .then(
                if (variant == GlassButtonVariant.Primary || variant == GlassButtonVariant.Ghost) {
                    Modifier
                } else {
                    Modifier.border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                colors.glassBorderTop.copy(alpha = 0.9f),
                                colors.glassBorderBottom,
                            )
                        ),
                        shape,
                    )
                }
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !loading,
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (variant == GlassButtonVariant.Primary) {
            androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.9f),
                    )
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.08f, 1.dp.toPx()),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.92f, 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 18.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                }
                Text(
                    text = text,
                    color = contentColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    tint: Color? = null,
    badge: Int = 0,
    enabled: Boolean = true,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    GlassSurface(
        modifier = modifier.size(size),
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        interactive = true,
        onClick = {
            if (enabled) onClick()
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint ?: colors.text,
                modifier = Modifier.size(size * 0.46f),
            )
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = size * 0.09f, end = size * 0.09f)
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(colors.danger),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
fun GlassChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    compact: Boolean = false,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.glassTop.copy(alpha = 0.5f),
        animationSpec = tween(260),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.textSecondary,
        animationSpec = tween(260),
        label = "chipFg",
    )
    Row(
        modifier = modifier
            .height(if (compact) 32.dp else 38.dp)
            .clip(GlassShape.pill)
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else colors.glassBorderTop.copy(alpha = 0.6f),
                GlassShape.pill,
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = if (compact) 12.dp else 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = fg,
            fontSize = if (compact) 12.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun GlassSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val selectedFloat by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
        label = "segment",
    )
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(4.dp),
        elevated = false,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(height - 8.dp)) {
            val itemWidth = maxWidth / options.size
            Box(
                Modifier
                    .offset(x = itemWidth * selectedFloat)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false,
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.18f),
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.glassStrong)
                    .border(
                        1.dp,
                        colors.glassBorderTop.copy(alpha = 0.8f),
                        RoundedCornerShape(12.dp),
                    )
            )
            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { index, label ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (index == selectedIndex) colors.text else colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val track by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.textTertiary.copy(alpha = 0.32f),
        animationSpec = tween(240),
        label = "switchTrack",
    )
    val thumbX by animateDpAsState(
        targetValue = if (checked) 23.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "switchThumb",
    )
    Box(
        modifier = modifier
            .width(52.dp)
            .height(31.dp)
            .clip(GlassShape.pill)
            .background(track)
            .clickable(enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = thumbX)
                .size(27.dp)
                .shadow(4.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
) {
    val colors = Glass.colors
    val type = Glass.type
    var passwordVisible by remember { mutableStateOf(false) }

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 11.dp),
        elevated = false,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 26.dp),
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = type.headline.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = colors.textTertiary,
                                    fontSize = 16.sp,
                                )
                            }
                            inner()
                        }
                    },
                )
                if (isPassword) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                        tint = colors.textTertiary,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { passwordVisible = !passwordVisible },
                    )
                }
            }
        }
    }
}

@Composable
fun GlassProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    tint: Color? = null,
) {
    val colors = Glass.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f),
        label = "bar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(GlassShape.pill)
            .background(colors.textTertiary.copy(alpha = 0.18f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(animated)
                .fillMaxHeight()
                .clip(GlassShape.pill)
                .background(
                    Brush.horizontalGradient(
                        listOf(tint ?: colors.accent, tint?.copy(alpha = 0.75f) ?: colors.accentSecondary),
                    )
                )
        )
    }
}

@Composable
fun GlassProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 8.dp,
    size: Dp = 64.dp,
    tint: Color? = null,
) {
    val colors = Glass.colors
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 90f),
        label = "ring",
    )
    androidx.compose.foundation.Canvas(modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        drawArc(
            color = colors.textTertiary.copy(alpha = 0.16f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(
                listOf(tint ?: colors.accent, tint?.copy(alpha = 0.65f) ?: colors.accentSecondary, tint ?: colors.accent),
            ),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
        )
    }
}

@Composable
fun GlassSpinner(modifier: Modifier = Modifier, color: Color? = null) {
    val colors = Glass.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = color ?: colors.accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(34.dp),
        )
    }
}

@Composable
fun GlassScrollFade(
    modifier: Modifier = Modifier,
    height: Dp = 24.dp,
) {
    val colors = Glass.colors
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    listOf(colors.background.copy(alpha = 0.9f), Color.Transparent),
                )
            )
    )
}

@Composable
fun AnimatedNumberText(
    value: Double?,
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 54.sp,
    color: Color? = null,
    format: (Double) -> String = { String.format(java.util.Locale("ru"), "%.2f", it) },
) {
    val colors = Glass.colors
    val target = value ?: 0.0
    val animated = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(target) {
        animated.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = 900, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        )
    }
    Text(
        text = if (value == null) "—" else format(animated.value.toDouble()),
        modifier = modifier,
        color = color ?: colors.text,
        fontSize = fontSize,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-2).sp,
    )
}
