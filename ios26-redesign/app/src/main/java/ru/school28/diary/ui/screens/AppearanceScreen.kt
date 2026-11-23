package ru.school28.diary.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.school28.diary.LauncherIcons
import ru.school28.diary.Prefs
import ru.school28.diary.R
import ru.school28.diary.SplashActivity
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassChip
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.SectionLabel

private fun themes(): List<Pair<String, String>> {
    val list = mutableListOf(
        Prefs.THEME_SYSTEM to "Системная",
        Prefs.THEME_LIGHT to "Светлая",
        Prefs.THEME_DARK to "Тёмная",
        Prefs.THEME_ZHAN to "Книжная",
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        list.add(Prefs.THEME_DYNAMIC to "Material You")
    }
    return list
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppearanceScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val colors = Glass.colors
    var theme by remember { mutableStateOf(Prefs.getTheme(ctx)) }
    var iconNew by remember { mutableStateOf(Prefs.getAppIcon(ctx) != Prefs.ICON_OLD) }
    var bg by remember { mutableStateOf(Prefs.getAppBg(ctx)) }

    fun restartApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(ctx, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            ctx.startActivity(intent)
            (ctx as? Activity)?.finish()
            Runtime.getRuntime().exit(0)
        }, 500)
    }

    GlassScreenScaffold(
        title = "Оформление",
        subtitle = "Тема, иконка и фон приложения",
        onBack = onBack,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "themeLabel") { SectionLabel("Тема") }

            item(key = "theme") {
                GlassCard(contentPadding = PaddingValues(12.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        themes().forEach { (key, label) ->
                            GlassChip(
                                text = label,
                                selected = theme == key,
                                onClick = {
                                    if (theme != key) {
                                        theme = key
                                        Prefs.setTheme(ctx, key)
                                        Toast.makeText(ctx, "Тема изменена, перезапуск…", Toast.LENGTH_SHORT).show()
                                        restartApp()
                                    }
                                },
                                compact = true,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Смена темы перезапускает приложение.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                    )
                }
            }

            item(key = "iconLabel") { SectionLabel("Иконка приложения") }

            item(key = "icon") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceCard(
                        title = "Новая",
                        selected = iconNew,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (!iconNew) {
                                iconNew = true
                                Prefs.setAppIcon(ctx, Prefs.ICON_NEW)
                                LauncherIcons.apply(ctx, true)
                                Toast.makeText(ctx, "Иконка обновлена", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_icon_new),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    ChoiceCard(
                        title = "Старая",
                        selected = !iconNew,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (iconNew) {
                                iconNew = false
                                Prefs.setAppIcon(ctx, Prefs.ICON_OLD)
                                LauncherIcons.apply(ctx, false)
                                Toast.makeText(ctx, "Иконка обновлена", Toast.LENGTH_SHORT).show()
                            }
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_photo),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            item(key = "bgLabel") { SectionLabel("Фон приложения") }

            item(key = "bg") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ChoiceCard(
                        title = "Аврора",
                        selected = bg == Prefs.BG_AURORA,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (bg != Prefs.BG_AURORA) {
                                bg = Prefs.BG_AURORA
                                Prefs.setAppBg(ctx, Prefs.BG_AURORA)
                                restartApp()
                            }
                        },
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(colors.blobA, colors.blobB, colors.blobC, colors.blobD)
                                    )
                                )
                        )
                    }
                    ChoiceCard(
                        title = "Новый фон",
                        selected = bg == Prefs.BG_PHOTO,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (bg != Prefs.BG_PHOTO) {
                                bg = Prefs.BG_PHOTO
                                Prefs.setAppBg(ctx, Prefs.BG_PHOTO)
                                restartApp()
                            }
                        },
                    ) {
                        Image(
                            painter = painterResource(R.drawable.app_bg_new),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val colors = Glass.colors
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.glassFill)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.accent else colors.glassBorderTop,
                shape = RoundedCornerShape(18.dp),
            )
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                preview()
            }
            Spacer(Modifier.height(7.dp))
            Text(
                text = title,
                color = colors.text,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp),
            )
        }
    }
}
