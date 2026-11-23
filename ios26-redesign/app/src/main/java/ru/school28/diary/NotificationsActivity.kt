package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassScreenScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsActivity : ComponentActivity() {

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
                NotificationsScreen(onBack = { finish() })
            }
        }
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors

    var unreadIds by remember { mutableStateOf(emptySet<Long>()) }
    var items by remember { mutableStateOf(NotificationStore.load(ctx)) }

    LaunchedEffect(Unit) {
        unreadIds = items.filter { !it.read }.map { it.id }.toSet()
        NotificationStore.markAllRead(ctx)
    }

    GlassScreenScaffold(
        title = "Уведомления",
        subtitle = if (items.isEmpty()) "Здесь появятся важные события" else "${items.size} записей",
        onBack = onBack,
        actions = {
            if (items.isNotEmpty()) {
                GlassIconButton(
                    icon = Icons.Rounded.DoneAll,
                    contentDescription = "Очистить историю",
                    onClick = {
                        NotificationStore.clear(ctx)
                        items = emptyList()
                    },
                    size = 42.dp,
                )
            }
        },
    ) {
        if (items.isEmpty()) {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                item {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.Notifications,
                            title = "Уведомлений пока нет",
                            message = "Оценки, домашние задания и школьные события будут появляться здесь.",
                        )
                    }
                }
            }
        } else {
            val fmt = remember { SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items, key = { it.id }) { notification ->
                    NotificationRow(
                        notification = notification,
                        isUnread = notification.id in unreadIds,
                        timeText = fmt.format(Date(notification.time)),
                        onClick = {
                            NotificationStore.markRead(ctx, notification.id)
                            val intent = Intent(ctx, MainRoute.activity(ctx)).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_TAB, notification.tab)
                                if (notification.forceUpdate) putExtra(MainActivity.EXTRA_CHECK_UPDATE, true)
                            }
                            ctx.startActivity(intent)
                            onBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    isUnread: Boolean,
    timeText: String,
    onClick: () -> Unit,
) {
    val colors = Glass.colors
    val (icon, tint) = iconFor(notification.channel)

    GlassCard(
        onClick = onClick,
        tint = if (isUnread) tint else null,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            GlassIconBubble(
                icon = icon,
                tint = tint,
                size = 38.dp,
            )
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        color = colors.text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (isUnread) {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(tint)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = notification.text,
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = timeText,
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

private fun iconFor(channel: String): Pair<ImageVector, Color> = when (channel) {
    NotificationHelper.CHANNEL_EVENTS -> Icons.Rounded.Event to Color(0xFF2FC4B4)
    NotificationHelper.CHANNEL_UPDATES -> Icons.Rounded.SystemUpdate to Color(0xFF9D6BFF)
    else -> Icons.Rounded.MenuBook to Color(0xFF5A5AF2)
}
