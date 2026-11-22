package ru.school28.diary.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.MainActivity
import ru.school28.diary.QrScannerActivity
import ru.school28.diary.WebViewActivity
import ru.school28.diary.api.ApiClient
import ru.school28.diary.api.SchoolEvent
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.ButtonVariant
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.components.progressindicators.CircularProgressIndicator
import coil.compose.AsyncImage

@Composable
fun EventsScreen(activity: MainActivity?) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var events by remember { mutableStateOf<List<SchoolEvent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            loading = true; error = null
            try {
                val loaded = withContext(Dispatchers.IO) { ApiClient.getEvents() }
                // Сортируем по дате, а внутри дня — по времени
                events = loaded.sortedBy { "${it.date} ${it.time ?: ""}" }
            } catch (e: Exception) {
                error = e.message
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize()) {
        // Кнопка сканирования QR
        Button(
            text = "Сканировать QR-код",
            variant = ButtonVariant.Secondary,
            onClick = {
                ctx.startActivity(android.content.Intent(ctx, QrScannerActivity::class.java))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(46.dp)
        )

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Не удалось загрузить события", color = AppTheme.colors.error)
                    Text(error ?: "", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                }
            }
            events.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Событий пока нет", color = AppTheme.colors.textSecondary)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(events, key = { it.id }) { ev ->
                    EventCard(ev) {
                        if (!ev.link.isNullOrBlank()) {
                            ctx.startActivity(WebViewActivity.intent(ctx, ev.link))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: SchoolEvent, onClick: () -> Unit) {
    // Картинка грузится и рендерится только когда реально доступна —
    // при битом/пустом URL не оставляем пустой блок высотой 160dp.
    val hasImageUrl = !event.image.isNullOrBlank()
    var imageLoaded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.cardPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = AppTheme.colors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(10.dp))
            Text(event.title, color = AppTheme.colors.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (event.link != null) {
                Icon(Icons.Filled.ArrowForward, contentDescription = "Открыть", tint = AppTheme.colors.primary, modifier = Modifier.size(18.dp))
            }
        }
        val whenText = listOfNotNull(
            event.date.takeIf { it.isNotBlank() },
            event.time?.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (whenText.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(whenText, color = AppTheme.colors.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        if (hasImageUrl && imageLoaded) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = event.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
        if (event.text.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(event.text, color = AppTheme.colors.textSecondary, fontSize = 14.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
        }
    }

    // Когда URL есть, но картинка ещё не отрисована — запускаем загрузку
    // невидимым AsyncImage, чтобы не оставлять пустого места в карточке.
    if (hasImageUrl && !imageLoaded) {
        AsyncImage(
            model = event.image,
            contentDescription = null,
            onState = { state ->
                if (state is coil.compose.AsyncImagePainter.State.Success) imageLoaded = true
            },
            modifier = Modifier.size(0.dp)
        )
    }
}