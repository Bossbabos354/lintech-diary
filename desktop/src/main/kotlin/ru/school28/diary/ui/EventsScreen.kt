package ru.school28.diary.ui

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
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.school28.diary.AppLog
import ru.school28.diary.Repo
import ru.school28.diary.Settings
import ru.school28.diary.api.SchoolEvent

@Composable
fun EventsScreen() {
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<SchoolEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        try {
            events = Repo.events().sortedBy { "${it.date} ${it.time ?: ""}" }
        } catch (e: Exception) {
            AppLog.logException("events", e)
            error = e.message
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTitle("События", trailing = {
            OutlinedButton(onClick = { reload++ }, shape = RoundedCornerShape(12.dp)) { Text("Обновить") }
        })

        when {
            loading -> LoadingBox()
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Не удалось загрузить события: $error", color = AppTheme.colors.error)
            }
            events.isEmpty() -> EmptyBox("Событий пока нет")
            else -> SmoothLazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp,
                ),
            ) {
                items(events, key = { it.id }) { event -> EventCard(event) }
            }
        }
    }
}

@Composable
private fun EventCard(event: SchoolEvent) {
    val whenText = listOfNotNull(
        event.date.takeIf { it.isNotBlank() },
        event.time?.takeIf { it.isNotBlank() },
    ).joinToString(" · ")

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable {
                val url = event.link
                if (!url.isNullOrBlank()) openInBrowser(url)
            }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.cardPurple),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                event.title,
                color = AppTheme.colors.text,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f),
            )
            if (!event.link.isNullOrBlank()) {
                Icon(
                    Icons.Filled.ArrowForward,
                    contentDescription = "Открыть на сайте",
                    tint = AppTheme.colors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (whenText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(whenText, color = AppTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (event.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                event.text,
                color = AppTheme.colors.textSecondary,
                fontSize = 14.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun openInBrowser(url: String) {
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().browse(java.net.URI(url))
        } else {
            AppLog.log("openInBrowser: Desktop API недоступен, url=$url")
        }
    } catch (e: Exception) {
        AppLog.logException("openInBrowser", e)
    }
}
