package ru.school28.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenTitle(title: String, subtitle: String? = null, trailing: @Composable (() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = AppTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) Text(subtitle, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
        }
        trailing?.invoke()
    }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(22.dp), AppTheme.colors)
            .padding(16.dp),
    ) { content() }
}

@Composable
fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AppTheme.colors.primary)
    }
}

@Composable
fun EmptyBox(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = AppTheme.colors.textSecondary)
    }
}

@Composable
fun OfflineLabel(timestamp: Long?) {
    if (timestamp == null) return
    Text(
        "офлайн · данные от " + ru.school28.diary.Util.shortTime(timestamp),
        color = AppTheme.colors.textSecondary,
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 24.dp, top = 4.dp),
    )
}

@Composable
fun SubjectAvatar(subjectId: Int, name: String, size: Int = 40) {
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(subjectColor(subjectId)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(2),
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size / 2.7).sp,
        )
    }
}

@Composable
fun MarkChip(mark: String, fontSize: Int = 16) {
    val color = MarkStyle.color(mark)
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(mark.removePrefix("!"), color = color, fontWeight = FontWeight.ExtraBold, fontSize = fontSize.sp)
    }
}

/**
 * Поле с выпадающим списком: подпись, выбранное значение и меню.
 * options — пары «значение → подпись».
 */
@Composable
fun <T> Picker(
    options: List<Pair<T, String>>,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppTheme.colors.background)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (caption != null) {
                Text(
                    caption,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.width(180.dp),
                )
            }
            Text(
                selectedLabel,
                color = AppTheme.colors.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text("▾", color = AppTheme.colors.textSecondary, fontSize = 16.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, color = AppTheme.colors.text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (selected) AppTheme.colors.primary else AppTheme.colors.background
    val fg = if (selected) AppTheme.colors.onPrimary else AppTheme.colors.text
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = AppTheme.colors.textSecondary, fontSize = 13.sp, modifier = Modifier.width(150.dp))
        Text(value, color = AppTheme.colors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SpacerH(height: Int) {
    Spacer(Modifier.height(height.dp))
}
