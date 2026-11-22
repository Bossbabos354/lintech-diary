package ru.school28.diary.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GlassTopBar(
    onSearch: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    unread: Int,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppLogo(size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = "ЛинТех 28",
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
            )
            Text(
                text = "электронный дневник",
                color = colors.textSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp,
            )
        }
        Spacer(Modifier.weight(1f))
        GlassIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = "Поиск",
            onClick = onSearch,
            size = 41.dp,
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = Icons.Rounded.Notifications,
            contentDescription = "Уведомления",
            onClick = onNotifications,
            size = 41.dp,
            badge = unread,
        )
        Spacer(Modifier.width(8.dp))
        GlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Настройки",
            onClick = onSettings,
            size = 41.dp,
        )
    }
}

data class GlassTabItem(
    val label: String,
    val icon: ImageVector,
)

@Composable
fun GlassTabBar(
    items: List<GlassTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    val haptic = LocalHapticFeedback.current
    val indicator by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 300f),
        label = "tabIndicator",
    )

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(6.dp),
        blurred = true,
        elevated = true,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / items.size
            Box(
                Modifier
                    .offset(x = itemWidth * indicator)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(23.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(colors.accent.copy(alpha = 0.22f), colors.accent.copy(alpha = 0.1f)),
                        )
                    )
                    .border(
                        1.dp,
                        colors.accent.copy(alpha = 0.28f),
                        RoundedCornerShape(23.dp),
                    )
            )
            Row(Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 0.92f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 420f),
                        label = "tabIcon",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(23.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(index)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) colors.accent else colors.textSecondary,
                            modifier = Modifier
                                .size(23.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = if (selected) colors.accent else colors.textSecondary,
                            fontSize = 9.5.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
