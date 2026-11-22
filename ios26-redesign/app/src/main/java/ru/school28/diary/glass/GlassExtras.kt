package ru.school28.diary.glass

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.school28.diary.ui.MarkStyle
import ru.school28.diary.ui.subjectColor

@Composable
fun GlassLargeHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Glass.colors
    val type = Glass.type
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = type.large,
                color = colors.text,
                maxLines = 1,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = type.caption,
                    color = colors.textSecondary,
                    maxLines = 2,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Glass.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.uppercase(),
            color = colors.textTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.7.sp,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

@Composable
fun GlassIconBubble(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    iconSize: Dp = 19.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.24f), tint.copy(alpha = 0.12f)),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun GradientAvatar(
    text: String,
    subjectId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val brush = remember(subjectId) { avatarBrush(subjectId) }
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(brush),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = (size.value * 0.31f).sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
        )
    }
}

@Composable
fun MarkBadge(
    mark: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val color = MarkStyle.color(mark)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = mark.removePrefix("!"),
            color = color,
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = Glass.colors
    val color = tint ?: colors.accent
    Box(
        modifier = modifier
            .clip(GlassShape.pill)
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        )
    }
}

@Composable
fun GlassSettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tint: Color? = null,
    divider: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Glass.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable { onClick() } else Modifier
                )
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconBubble(
                icon = icon,
                tint = tint ?: colors.accent,
                size = 34.dp,
                iconSize = 17.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
        if (divider) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 45.dp)
                    .height(1.dp)
                    .background(colors.separator),
            )
        }
    }
}

@Composable
fun GlassEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = Glass.colors
    val type = Glass.type
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.accent.copy(alpha = 0.22f),
                            colors.accentSecondary.copy(alpha = 0.12f),
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = type.title, color = colors.text, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = type.body,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(18.dp))
            GlassButton(
                text = actionText,
                onClick = onAction,
                variant = GlassButtonVariant.Tinted,
                height = 44.dp,
            )
        }
    }
}

@Composable
fun GlassErrorState(
    message: String?,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Не удалось загрузить",
            color = colors.text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = message ?: "Проверьте подключение к интернету",
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(16.dp))
            GlassButton(
                text = "Повторить",
                onClick = onRetry,
                variant = GlassButtonVariant.Tinted,
                height = 42.dp,
            )
        }
    }
}

/**
 * Появление элемента списка с каскадной задержкой.
 *
 * Анимируется только один раз: ключи уже показанных элементов складываются
 * в [seen], поэтому при прокрутке вверх и обратно карточки не «всплывают» заново.
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    appearKey: Any? = null,
    seen: MutableSet<Any>? = null,
    content: @Composable () -> Unit,
) {
    // Анимации появления отключены: интерфейс прогружается сразу,
    // и при прокрутке карточки не «всплывают» повторно.
    Box(modifier) {
        content()
    }
}

@Composable
fun GlassScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    BackHandler(enabled = onBack != null) { onBack?.invoke() }
    Box(modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        onClick = onBack,
                        size = 42.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Glass.colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.6).sp,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Glass.colors.textSecondary,
                            fontSize = 12.sp,
                        )
                    }
                }
                actions?.invoke()
            }
            Box(Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@Composable
fun GlassSkeletonCard(
    modifier: Modifier = Modifier,
    lines: Int = 3,
    height: Dp = 108.dp,
) {
    val colors = Glass.colors
    Column(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(GlassShape.card)
            .background(colors.skeleton.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        repeat(lines) { index ->
            Box(
                Modifier
                    .fillMaxWidth(if (index == 0) 0.55f else if (index == lines - 1) 0.35f else 0.85f)
                    .height(if (index == 0) 12.dp else 9.dp)
                    .clip(GlassShape.pill)
                    .background(colors.textTertiary.copy(alpha = 0.14f)),
            )
            if (index != lines - 1) Spacer(Modifier.height(9.dp))
        }
    }
}


// Градиенты аватаров кэшируем: цвет зависит только от предмета.
private val avatarBrushCache = HashMap<Int, Brush>()

private fun avatarBrush(subjectId: Int): Brush = synchronized(avatarBrushCache) {
    avatarBrushCache.getOrPut(subjectId) {
        val base = subjectColor(subjectId)
        Brush.linearGradient(
            listOf(base, base.copy(alpha = 0.65f), Color.White.copy(alpha = 0.3f)),
        )
    }
}
