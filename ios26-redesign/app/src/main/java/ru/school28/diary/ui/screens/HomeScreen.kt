package ru.school28.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.glass.Glass
import java.util.Calendar

private data class HomeSection(val title: String, val icon: ImageVector, val tab: Int)

private val SECTIONS = listOf(
    HomeSection("Дневник", Icons.Rounded.MenuBook, 0),
    HomeSection("Расписание", Icons.Rounded.CalendarMonth, 1),
    HomeSection("Оценки", Icons.Rounded.Grade, 2),
    HomeSection("Цели", Icons.Rounded.Flag, 3),
    HomeSection("События", Icons.Rounded.Event, 4),
    HomeSection("Настройки", Icons.Rounded.Settings, 5),
)

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Доброе утро"
        hour in 12..17 -> "Добрый день"
        else -> "Добрый вечер"
    }
}

/**
 * Главный экран: приветствие и разделы кружками.
 * @param badges непрочитанные по номеру вкладки (0..5).
 */
@Composable
fun HomeScreen(
    name: String,
    badges: Map<Int, Int>,
    onOpen: (Int) -> Unit,
) {
    val colors = Glass.colors
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(greeting() + ",", color = colors.textSecondary, fontSize = 15.sp)
        Text(
            name.ifBlank { "друг" },
            color = colors.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
        )
        Spacer(Modifier.height(30.dp))
        SECTIONS.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { section ->
                    HomeCircle(
                        section = section,
                        badge = badges[section.tab] ?: 0,
                        onClick = { onOpen(section.tab) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun HomeCircle(section: HomeSection, badge: Int, onClick: () -> Unit) {
    val colors = Glass.colors
    Column(
        Modifier.width(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(colors.glassFill)
                    .border(1.dp, colors.glassBorderBottom, CircleShape)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = section.icon,
                    contentDescription = section.title,
                    tint = colors.accent,
                    modifier = Modifier.size(32.dp),
                )
            }
            if (badge > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-4).dp, y = 2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF4D4D)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (badge > 9) "9+" else badge.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = section.title,
            color = colors.textSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
