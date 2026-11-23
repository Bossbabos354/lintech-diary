package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
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
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Центр уведомлений: вся история показанных уведомлений.
 * Открывается колокольчиком в настройках.
 */
class NotificationsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
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

    // Снимок непрочитанных до открытия — чтобы подсветить их в списке
    var unreadIds by remember { mutableStateOf(emptySet<Long>()) }
    var items by remember { mutableStateOf(NotificationStore.load(ctx)) }

    LaunchedEffect(Unit) {
        unreadIds = items.filter { !it.read }.map { it.id }.toSet()
        NotificationStore.markAllRead(ctx)
    }

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
            Text("Уведомления", color = AppTheme.colors.onPrimary, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.weight(1f))
            if (items.isNotEmpty()) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            NotificationStore.clear(ctx)
                            items = emptyList()
                        }
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DoneAll,
                        contentDescription = "Очистить",
                        tint = AppTheme.colors.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = AppTheme.colors.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Уведомлений пока нет", color = AppTheme.colors.textSecondary)
                }
            }
        } else {
            val fmt = remember { SimpleDateFormat("dd.MM, HH:mm", Locale.getDefault()) }
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
            ) {
                items(items, key = { it.id }) { n ->
                    NotificationRow(
                        notification = n,
                        isUnread = n.id in unreadIds,
                        timeText = fmt.format(Date(n.time)),
                        onClick = {
                            NotificationStore.markRead(ctx, n.id)
                            val intent = Intent(ctx, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra(MainActivity.EXTRA_TAB, n.tab)
                                if (n.forceUpdate) putExtra(MainActivity.EXTRA_CHECK_UPDATE, true)
                            }
                            ctx.startActivity(intent)
                            onBack()
                        }
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
    onClick: () -> Unit
) {
    val (icon, tint) = iconFor(notification.channel)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isUnread) AppTheme.colors.cardPurple else AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    notification.title,
                    color = AppTheme.colors.text,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                if (isUnread) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AppTheme.colors.primary)
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
            Text(notification.text, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(timeText, color = AppTheme.colors.textDisabled, fontSize = 11.sp)
        }
    }
}

private fun iconFor(channel: String): Pair<ImageVector, Color> = when (channel) {
    NotificationHelper.CHANNEL_EVENTS -> Icons.Filled.Event to Color(0xFF4DB6AC)
    NotificationHelper.CHANNEL_UPDATES -> Icons.Filled.SystemUpdate to Color(0xFF9575CD)
    else -> Icons.Filled.MenuBook to Color(0xFF7E57C2)
}