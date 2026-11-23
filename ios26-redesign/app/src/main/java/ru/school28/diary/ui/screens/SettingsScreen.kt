package ru.school28.diary.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.BuildConfig
import ru.school28.diary.ExportActivity
import ru.school28.diary.LoginActivity
import ru.school28.diary.MainActivity
import ru.school28.diary.NotificationHelper
import ru.school28.diary.NotificationScheduler
import ru.school28.diary.NotificationStore
import ru.school28.diary.NotificationsActivity
import ru.school28.diary.AppearanceActivity
import ru.school28.diary.Prefs
import ru.school28.diary.SplashActivity
import ru.school28.diary.WidgetGalleryActivity
import ru.school28.diary.api.ApiClient
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassButtonVariant
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassIconBubble
import ru.school28.diary.glass.GlassIconButton
import ru.school28.diary.glass.GlassScreenScaffold
import ru.school28.diary.glass.GlassSettingsRow
import ru.school28.diary.glass.GlassSwitch
import ru.school28.diary.glass.GlassTextField
import ru.school28.diary.glass.SectionLabel

private data class ThemeOption(
    val key: String,
    val label: String,
    val previewDark: Boolean,
    val wallpaper: Boolean = false,
)

private val THEME_OPTIONS = listOf(
    ThemeOption(Prefs.THEME_SYSTEM, "Системная", false),
    ThemeOption(Prefs.THEME_LIGHT, "Светлая", false),
    ThemeOption(Prefs.THEME_DARK, "Тёмная", true),
    ThemeOption(Prefs.THEME_ZHAN, "Книжная", false, wallpaper = true),
)

@Composable
fun SettingsScreen(
    activity: MainActivity?,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val ctx = LocalContext.current
    val colors = Glass.colors

    var login by remember { mutableStateOf(Prefs.getLogin(ctx) ?: "") }
    var password by remember { mutableStateOf(Prefs.getPassword(ctx) ?: "") }
    var legacyUi by remember { mutableStateOf(Prefs.isLegacyUi(ctx)) }
    var appLock by remember { mutableStateOf(Prefs.isAppLockEnabled(ctx)) }
    var notifEnabled by remember { mutableStateOf(Prefs.isNotifEnabled(ctx)) }
    var notifInterval by remember { mutableStateOf(Prefs.getNotifIntervalMin(ctx)) }
    var quietEnabled by remember { mutableStateOf(Prefs.isQuietEnabled(ctx)) }
    var quietFrom by remember { mutableStateOf(Prefs.getQuietFrom(ctx)) }
    var quietTo by remember { mutableStateOf(Prefs.getQuietTo(ctx)) }
    var digestEnabled by remember { mutableStateOf(Prefs.isDigestEnabled(ctx)) }
    var digestHour by remember { mutableStateOf(Prefs.getDigestHour(ctx)) }
    var checkUpdates by remember { mutableStateOf(Prefs.isCheckUpdatesEnabled(ctx)) }
    var unread by remember { mutableStateOf(NotificationStore.unreadCount(ctx)) }
    var classOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedClass by remember { mutableStateOf(Prefs.getSelectedClass(ctx)) }

    LaunchedEffect(Unit) {
        val schedule = withContext(Dispatchers.IO) { ApiClient.getSchedule() }
        classOptions = schedule?.classes.orEmpty()
    }

    fun hourText(h: Int): String = (if (h < 10) "0$h" else "$h") + ":00"
    val hours = remember { (0..23).map { it to hourText(it) } }
    val intervals = remember {
        listOf(
            15 to "15 минут",
            30 to "30 минут",
            60 to "1 час",
            180 to "3 часа",
            360 to "6 часов",
            720 to "12 часов",
            1440 to "24 часа",
        )
    }

    fun intervalLabel(min: Int): String = intervals.firstOrNull { it.first == min }?.second ?: "$min минут"

    fun enableNotifications() {
        Prefs.setNotifEnabled(ctx, true)
        notifEnabled = true
        NotificationScheduler.schedule(ctx)
        Toast.makeText(ctx, "Уведомления включены", Toast.LENGTH_SHORT).show()
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableNotifications() else Toast.makeText(ctx, "Нет разрешения на уведомления", Toast.LENGTH_SHORT).show()
    }

    fun toggleNotif(enabled: Boolean) {
        if (enabled) {
            if (NotificationHelper.canPost(ctx)) enableNotifications()
            else notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Prefs.setNotifEnabled(ctx, false)
            notifEnabled = false
            NotificationScheduler.cancel(ctx)
            Toast.makeText(ctx, "Уведомления выключены", Toast.LENGTH_SHORT).show()
        }
    }

    fun restartApp() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(ctx, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            ctx.startActivity(intent)
            (ctx as? android.app.Activity)?.finish()
            Runtime.getRuntime().exit(0)
        }, 400)
    }

    GlassScreenScaffold(
        title = "Настройки",
        subtitle = "Аккаунт, уведомления и оформление",
        onBack = onBack,
        actions = {
            GlassIconButton(
                icon = Icons.Rounded.Notifications,
                contentDescription = "Центр уведомлений",
                onClick = {
                    unread = 0
                    ctx.startActivity(Intent(ctx, NotificationsActivity::class.java))
                },
                size = 42.dp,
                badge = unread,
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "profile") {
                GlassCard(tint = colors.accent, contentPadding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(listOf(colors.accent, colors.accentSecondary))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = login.take(2).uppercase(),
                                color = colors.onAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = login.ifBlank { "Аккаунт" },
                                color = colors.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Text(
                                text = "Данные хранятся только на устройстве",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = "Логин",
                        placeholder = "Введи логин",
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Пароль",
                        placeholder = "Введи пароль",
                        isPassword = true,
                    )
                    Spacer(Modifier.height(10.dp))
                    GlassButton(
                        text = "Сохранить данные",
                        onClick = {
                            if (login.isBlank() || password.isBlank()) {
                                Toast.makeText(ctx, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                            } else {
                                Prefs.saveCredentials(ctx, login.trim(), password.trim(), Prefs.getStudentId(ctx))
                                Toast.makeText(ctx, "Данные сохранены", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        height = 46.dp,
                    )
                }
            }

            item(key = "appearance") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "Оформление",
                        subtitle = "Тема, иконка и фон приложения",
                        divider = false,
                        onClick = { ctx.startActivity(Intent(ctx, AppearanceActivity::class.java)) },
                    )
                }
            }

            item(key = "security") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    SwitchSettingRow(
                        icon = Icons.Rounded.Lock,
                        title = "Блокировка приложения",
                        subtitle = "Отпечаток или PIN при запуске",
                        checked = appLock,
                        onCheckedChange = { enabled ->
                            appLock = enabled
                            Prefs.setAppLockEnabled(ctx, enabled)
                            Toast.makeText(
                                ctx,
                                if (enabled) "Приложение будет спрашивать отпечаток или PIN"
                                else "Блокировка выключена",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        dividerAfter = false,
                    )
                }
            }

            item(key = "legacyLabel") {
                SectionLabel("Старые устройства")
            }

            item(key = "legacy") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    SwitchSettingRow(
                        icon = Icons.Rounded.PhoneAndroid,
                        title = "Старый интерфейс",
                        subtitle = "Простой интерфейс из стабильной версии — для слабых устройств",
                        checked = legacyUi,
                        onCheckedChange = { enabled ->
                            if (enabled == legacyUi) return@SwitchSettingRow
                            legacyUi = enabled
                            Prefs.setLegacyUi(ctx, enabled)
                            Toast.makeText(
                                ctx,
                                if (enabled) "Включаю старый интерфейс, перезапуск…"
                                else "Возвращаю новый интерфейс, перезапуск…",
                                Toast.LENGTH_SHORT,
                            ).show()
                            restartApp()
                        },
                        dividerAfter = false,
                    )
                }
            }

            item(key = "notificationsLabel") {
                SectionLabel("Уведомления")
            }

            item(key = "notifications") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    SwitchSettingRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Уведомления",
                        subtitle = "Оценки и домашние задания",
                        checked = notifEnabled,
                        onCheckedChange = { toggleNotif(it) },
                    )
                    if (notifEnabled) {
                        MenuSettingRow(
                            icon = Icons.Rounded.Schedule,
                            title = "Интервал проверки",
                            subtitle = "Как часто проверять журнал",
                            options = intervals,
                            selectedLabel = intervalLabel(notifInterval),
                            onSelect = { min ->
                                Prefs.setNotifIntervalMin(ctx, min)
                                notifInterval = min
                                if (Prefs.isNotifEnabled(ctx)) NotificationScheduler.schedule(ctx)
                            },
                        )
                        SwitchSettingRow(
                            icon = Icons.Rounded.DarkMode,
                            title = "Тихие часы",
                            subtitle = "С ${hourText(quietFrom)} до ${hourText(quietTo)}",
                            checked = quietEnabled,
                            onCheckedChange = {
                                quietEnabled = it
                                Prefs.setQuietEnabled(ctx, it)
                            },
                            dividerAfter = quietEnabled,
                        )
                        if (quietEnabled) {
                            MenuSettingRow(
                                icon = Icons.Rounded.DarkMode,
                                title = "Начало",
                                options = hours,
                                selectedLabel = hourText(quietFrom),
                                onSelect = {
                                    quietFrom = it
                                    Prefs.setQuietFrom(ctx, it)
                                },
                            )
                            MenuSettingRow(
                                icon = Icons.Rounded.LightMode,
                                title = "Конец",
                                options = hours,
                                selectedLabel = hourText(quietTo),
                                onSelect = {
                                    quietTo = it
                                    Prefs.setQuietTo(ctx, it)
                                },
                            )
                        }
                        SwitchSettingRow(
                            icon = Icons.Rounded.Notifications,
                            title = "Один дайджест в день",
                            subtitle = "Оценки и ДЗ одним сообщением",
                            checked = digestEnabled,
                            onCheckedChange = {
                                digestEnabled = it
                                Prefs.setDigestEnabled(ctx, it)
                            },
                            dividerAfter = digestEnabled,
                        )
                        if (digestEnabled) {
                            MenuSettingRow(
                                icon = Icons.Rounded.Schedule,
                                title = "Время дайджеста",
                                options = hours,
                                selectedLabel = hourText(digestHour),
                                onSelect = {
                                    digestHour = it
                                    Prefs.setDigestHour(ctx, it)
                                },
                            )
                        }
                    }
                }
            }

            item(key = "schoolLabel") {
                SectionLabel("Школа")
            }

            item(key = "school") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    if (classOptions.isEmpty()) {
                        GlassSettingsRow(
                            icon = Icons.Rounded.SettingsSuggest,
                            title = "Класс для расписания",
                            subtitle = "Список классов не загружен",
                            divider = false,
                        )
                    } else {
                        MenuSettingRow(
                            icon = Icons.Rounded.SettingsSuggest,
                            title = "Класс для расписания",
                            subtitle = "Показывать расписание выбранного класса",
                            options = classOptions.map { it to it },
                            selectedLabel = selectedClass ?: "Не выбран",
                            onSelect = {
                                Prefs.setSelectedClass(ctx, it)
                                selectedClass = it
                            },
                        )
                    }
                }
            }

            item(key = "widgetsLabel") {
                SectionLabel("Виджеты")
            }

            item(key = "widgets") {
                GlassCard(
                    onClick = { ctx.startActivity(Intent(ctx, WidgetGalleryActivity::class.java)) },
                    tint = colors.accent,
                    contentPadding = PaddingValues(15.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassIconBubble(
                            icon = Icons.Rounded.Widgets,
                            tint = colors.accent,
                            size = 44.dp,
                            iconSize = 22.dp,
                        )
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Виджеты",
                                color = colors.text,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "5 виджетов · тема, предпросмотр и добавление",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            Brush.linearGradient(listOf(Color(0xFF955DCD), Color(0xFFF8BBD0))),
                            Brush.linearGradient(listOf(Color(0xFF4DB6AC), Color(0xFF80CBC4))),
                            Brush.linearGradient(listOf(Color(0xFFB3E5FC), Color(0xFFB39DDB))),
                            Brush.linearGradient(listOf(Color(0xFFFFD3A5), Color(0xFFFD9E88))),
                            Brush.linearGradient(listOf(Color(0xFF263447), Color(0xFF3A2E5A))),
                        ).forEach { brush ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(brush)
                            )
                        }
                    }
                }
            }

            item(key = "dataLabel") {
                SectionLabel("Данные")
            }

            item(key = "data") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Rounded.FileDownload,
                        title = "Экспорт данных",
                        subtitle = "Дневник и оценки в PDF/Excel, расписание в .ics",
                        divider = false,
                        onClick = { ctx.startActivity(Intent(ctx, ExportActivity::class.java)) },
                        trailing = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                tint = colors.textTertiary,
                            )
                        },
                    )
                }
            }

            item(key = "aboutLabel") {
                SectionLabel("О приложении")
            }

            item(key = "about") {
                GlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Rounded.Info,
                        title = "Версия ${BuildConfig.VERSION_NAME}",
                        subtitle = "Сборка ${BuildConfig.VERSION_CODE} · iOS 26 design",
                        divider = true,
                    )
                    SwitchSettingRow(
                        icon = Icons.Rounded.Update,
                        title = "Проверять обновления",
                        subtitle = "Автоматически при запуске",
                        checked = checkUpdates,
                        onCheckedChange = {
                            checkUpdates = it
                            Prefs.setCheckUpdatesEnabled(ctx, it)
                        },
                        dividerAfter = true,
                    )
                    Spacer(Modifier.height(6.dp))
                    GlassButton(
                        text = "Проверить обновления",
                        onClick = onCheckUpdate,
                        variant = GlassButtonVariant.Tinted,
                        icon = Icons.Rounded.Update,
                        modifier = Modifier.fillMaxWidth(),
                        height = 46.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        text = "Выйти из аккаунта",
                        onClick = {
                            Prefs.clearCredentials(ctx)
                            val intent = Intent(ctx, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            ctx.startActivity(intent)
                            (ctx as? android.app.Activity)?.finish()
                        },
                        variant = GlassButtonVariant.Destructive,
                        icon = Icons.Rounded.Logout,
                        modifier = Modifier.fillMaxWidth(),
                        height = 46.dp,
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeTile(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Glass.colors
    val previewBrush = when {
        option.wallpaper -> Brush.linearGradient(listOf(Color(0xFFF3E8DC), Color(0xFFE5D4C4), Color(0xFFD8E4EC)))
        option.previewDark -> Brush.linearGradient(listOf(Color(0xFF1B1D2A), Color(0xFF272A3A), Color(0xFF3B2E5A)))
        else -> Brush.linearGradient(listOf(Color(0xFFF6F7FF), Color(0xFFE9ECFF), Color(0xFFDCE8FF)))
    }
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) colors.accent.copy(alpha = 0.12f) else colors.textTertiary.copy(alpha = 0.05f)
            )
            .border(
                1.dp,
                if (selected) colors.accent.copy(alpha = 0.55f) else colors.glassBorderTop.copy(alpha = 0.4f),
                RoundedCornerShape(16.dp),
            )
            .clickable { onClick() }
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(previewBrush),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (option.previewDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.8f))
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.7f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (option.previewDark) Color.White.copy(alpha = 0.35f) else Color(0xFF5A5AF2).copy(alpha = 0.5f))
            )
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✓", color = colors.onAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.label,
            color = if (selected) colors.accent else colors.text,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SwitchSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    dividerAfter: Boolean = true,
) {
    GlassSettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        divider = dividerAfter,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            GlassSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun <T> MenuSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    options: List<Pair<T, String>>,
    selectedLabel: String,
    onSelect: (T) -> Unit,
) {
    val colors = Glass.colors
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        GlassSettingsRow(
            icon = icon,
            title = title,
            subtitle = subtitle,
            divider = true,
            onClick = { expanded = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = selectedLabel,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = if (colors.isDark) Color(0xFF21232F) else Color.White,
            shape = RoundedCornerShape(18.dp),
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (label == selectedLabel) colors.accent else colors.text,
                            fontSize = 14.sp,
                            fontWeight = if (label == selectedLabel) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}
