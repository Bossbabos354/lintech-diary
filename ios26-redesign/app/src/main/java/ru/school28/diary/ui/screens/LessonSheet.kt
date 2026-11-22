package ru.school28.diary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassBadge
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassSheet
import ru.school28.diary.glass.GlassSurface
import ru.school28.diary.glass.GradientAvatar
import ru.school28.diary.glass.MarkBadge
import ru.school28.diary.ui.MarkStyle
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LessonSheet(entry: DairyEntry, onDismiss: () -> Unit) {
    val colors = Glass.colors
    val dateSdf = remember { SimpleDateFormat("EEEE, d MMMM", Locale("ru")) }
    GlassSheet(
        onDismiss = onDismiss,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GradientAvatar(
                text = entry.subjectName.take(2),
                subjectId = entry.subjectId,
                size = 54.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry.subjectName,
                    color = colors.text,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    text = "Урок ${entry.lessonNumber} · ${dateSdf.format(entry.date)}",
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                )
                if (entry.teacherFio.isNotBlank()) {
                    Text(
                        text = entry.teacherFio,
                        color = colors.textTertiary,
                        fontSize = 11.sp,
                    )
                }
            }
            if (entry.mark.isNotEmpty()) {
                MarkBadge(mark = entry.mark, size = 44.dp)
                Spacer(Modifier.width(8.dp))
            }
            GlassIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Закрыть",
                onClick = onDismiss,
                size = 34.dp,
            )
        }

        Spacer(Modifier.height(18.dp))

        DetailBlock(
            label = "Тема урока",
            value = entry.theme.ifBlank { "Тема не указана" },
        )

        Spacer(Modifier.height(10.dp))

        GlassSurface(
            shape = RoundedCornerShape(20.dp),
            tint = if (entry.homework.isNotBlank()) colors.accent else null,
            elevated = false,
            contentPadding = PaddingValues(15.dp),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ДОМАШНЕЕ ЗАДАНИЕ",
                        color = if (entry.homework.isNotBlank()) colors.accent else colors.textTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    if (entry.homework.isNotBlank()) {
                        GlassBadge("задано", tint = colors.accent)
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = entry.homework.ifBlank { "Задано не было" },
                    color = if (entry.homework.isNotBlank()) colors.text else colors.textSecondary,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (entry.mark.isNotEmpty()) {
                val color = MarkStyle.color(entry.mark)
                GlassSurface(
                    shape = RoundedCornerShape(14.dp),
                    tint = color,
                    elevated = false,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Оценка за урок",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = entry.mark.removePrefix("!"),
                            color = color,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }

        if (!entry.markComment.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "За что: ${entry.markComment}",
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun DetailBlock(label: String, value: String) {
    val colors = Glass.colors
    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        elevated = false,
        contentPadding = PaddingValues(15.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                text = label.uppercase(),
                color = colors.textTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                color = colors.text,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
