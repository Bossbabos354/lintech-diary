package ru.school28.diary.ui.screens

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.MainActivity
import ru.school28.diary.QrScannerActivity
import ru.school28.diary.WebViewActivity
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SchoolEvent
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassEmptyState
import ru.school28.diary.glass.GlassErrorState
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassLargeHeader
import ru.school28.diary.glass.GlassSkeletonCard
import ru.school28.diary.glass.StaggeredAppear

@Composable
fun EventsScreen(activity: MainActivity?) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf<List<SchoolEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val seenAppear = remember { mutableSetOf<Any>() }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true
            error = null
            try {
                val loaded = withContext(Dispatchers.IO) { ApiClient.getEvents() }
                events = loaded.sortedBy { "${it.date} ${it.time ?: ""}" }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 132.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            GlassLargeHeader(
                title = "События",
                subtitle = if (events.isEmpty()) "Афиша школы" else "${events.size} событий",
                trailing = {
                    GlassIconButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = "Обновить",
                        onClick = { load() },
                        size = 42.dp,
                    )
                },
            )
        }

        item(key = "scan") {
            GlassButton(
                text = "Сканировать QR-код",
                onClick = {
                    ctx.startActivity(android.content.Intent(ctx, QrScannerActivity::class.java))
                },
                icon = Icons.Rounded.QrCodeScanner,
                variant = GlassButtonVariant.Tinted,
                modifier = Modifier.fillMaxWidth(),
                height = 50.dp,
            )
        }

        when {
            loading -> {
                items(3) { GlassSkeletonCard(height = 170.dp, lines = 3) }
            }

            error != null -> {
                item(key = "error") {
                    GlassCard { GlassErrorState(message = error, onRetry = { load() }) }
                }
            }

            events.isEmpty() -> {
                item(key = "empty") {
                    GlassCard {
                        GlassEmptyState(
                            icon = Icons.Rounded.Event,
                            title = "Событий пока нет",
                            message = "Школьные мероприятия и объявления появятся здесь.",
                        )
                    }
                }
            }

            else -> {
                itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
                    StaggeredAppear(
                            index = index,
                            appearKey = event.id,
                            seen = seenAppear,
                        ) {
                        EventCard(event) {
                            if (!event.link.isNullOrBlank()) {
                                ctx.startActivity(WebViewActivity.intent(ctx, event.link))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: SchoolEvent, onClick: () -> Unit) {
    val colors = Glass.colors
    val hasLink = !event.link.isNullOrBlank()
    val hasImage = !event.image.isNullOrBlank()

    GlassCard(
        onClick = if (hasLink) onClick else null,
        contentPadding = PaddingValues(0.dp),
        elevated = false,
    ) {
        if (hasImage) {
            AsyncImage(
                model = event.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
            )
        }
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent.copy(alpha = 0.14f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = listOfNotNull(
                                event.date.takeIf { it.isNotBlank() },
                                event.time?.takeIf { it.isNotBlank() },
                            ).joinToString(" · "),
                            color = colors.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (hasLink) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowOutward,
                        contentDescription = "Открыть",
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(9.dp))
            Text(
                text = event.title,
                color = colors.text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
            )
            if (event.text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = event.text,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hasLink) {
                Spacer(Modifier.height(9.dp))
                GlassBadge("Подробнее", tint = colors.accent)
            }
        }
    }
}
