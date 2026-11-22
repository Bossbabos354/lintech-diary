package ru.school28.diary.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.school28.diary.BuildConfig
import ru.school28.diary.api.ApiClient
import ru.school28.diary.LoginActivity
import ru.school28.diary.MainActivity
import ru.school28.diary.NotificationsActivity
import ru.school28.diary.WidgetGalleryActivity
import ru.school28.diary.NotificationStore
import ru.school28.diary.NotificationHelper
import ru.school28.diary.NotificationScheduler
import ru.school28.diary.Prefs
import ru.school28.diary.SplashActivity
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.ButtonVariant
import ru.school28.diary.ui_components.components.textfield.OutlinedTextField
import ru.school28.diary.ui_components.components.Text

@Composable
fun SettingsScreen(activity: MainActivity?, onCheckUpdate: () -> Unit) {
    val ctx = LocalContext.current
    val act = activity ?: return

    var login by remember { mutableStateOf(Prefs.getLogin(ctx) ?: "") }
    var password by remember { mutableStateOf(Prefs.getPassword(ctx) ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(Prefs.getTheme(ctx)) }
    var themeDropdownExpanded by remember { mutableStateOf(false) }
    var notifEnabled by remember { mutableStateOf(Prefs.isNotifEnabled(ctx)) }
    var notifInterval by remember { mutableStateOf(Prefs.getNotifIntervalMin(ctx)) }
    var intervalDropdownExpanded by remember { mutableStateOf(false) }
    var quietEnabled by remember { mutableStateOf(Prefs.isQuietEnabled(ctx)) }
    var quietFrom by remember { mutableStateOf(Prefs.getQuietFrom(ctx)) }
    var quietTo by remember { mutableStateOf(Prefs.getQuietTo(ctx)) }
    var quietDropdown by remember { mutableStateOf<String?>(null) }
    var digestEnabled by remember { mutableStateOf(Prefs.isDigestEnabled(ctx)) }
    var digestHour by remember { mutableStateOf(Prefs.getDigestHour(ctx)) }
    var digestDropdownExpanded by remember { mutableStateOf(false) }
    var checkUpdates by remember { mutableStateOf(Prefs.isCheckUpdatesEnabled(ctx)) }
    var unreadNotifications by remember { mutableStateOf(NotificationStore.unreadCount(ctx)) }
    var classOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedClass by remember { mutableStateOf(Prefs.getSelectedClass(ctx)) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Список классов берём из расписания на сайте
        val schedule = withContext(Dispatchers.IO) { ApiClient.getSchedule() }
        classOptions = schedule?.classes.orEmpty()
    }

    fun hourText(h: Int): String = (if (h < 10) "0$h" else "$h") + ":00"
    val hours = (0..23).map { it to hourText(it) }

    val intervals = listOf(15 to "15 минут", 30 to "30 минут", 60 to "1 час",
        180 to "3 часа", 360 to "6 часов", 720 to "12 часов", 1440 to "24 часа")

    val themeOptions = listOf(
        Prefs.THEME_SYSTEM to "Системная",
        Prefs.THEME_LIGHT to "Светлая",
        Prefs.THEME_DARK to "Тёмная",
        Prefs.THEME_ZHAN to "Альтернативная тема"
    )

    fun themeLabel(t: String): String =
        themeOptions.firstOrNull { it.first == t }?.second ?: "Системная"

    fun intervalLabel(min: Int): String =
        intervals.firstOrNull { it.first == min }?.second ?: "$min минут"

    fun enableNotifications() {
        Prefs.setNotifEnabled(ctx, true)
        notifEnabled = true
        NotificationScheduler.schedule(ctx)
        Toast.makeText(ctx, "Уведомления включены (каждые ${intervalLabel(Prefs.getNotifIntervalMin(ctx))})", Toast.LENGTH_SHORT).show()
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            enableNotifications()
        } else {
            Toast.makeText(ctx, "Нет разрешения на уведомления", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleNotif(enabled: Boolean) {
        if (enabled) {
            if (NotificationHelper.canPost(ctx)) {
                enableNotifications()
            } else {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            Prefs.setNotifEnabled(ctx, false)
            notifEnabled = false
            NotificationScheduler.cancel(ctx)
            Toast.makeText(ctx, "Уведомления выключены", Toast.LENGTH_SHORT).show()
        }
    }

    fun setNotifInterval(min: Int) {
        Prefs.setNotifIntervalMin(ctx, min)
        notifInterval = min
        if (Prefs.isNotifEnabled(ctx)) {
            NotificationScheduler.schedule(ctx)
            Toast.makeText(ctx, "Интервал проверки: ${intervalLabel(min)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, "Интервал сохранён: ${intervalLabel(min)}", Toast.LENGTH_SHORT).show()
        }
    }

    fun save() {
        if (login.isNotBlank() && password.isNotBlank()) {
            Prefs.saveCredentials(ctx, login.trim(), password.trim(), Prefs.getStudentId(ctx))
            Toast.makeText(ctx, "Данные сохранены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(ctx, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
        }
    }

    fun setTheme(t: String) {
        Prefs.setTheme(ctx, t)
        Toast.makeText(ctx, "Тема изменена, приложение перезапускается...", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(ctx, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            ctx.startActivity(intent)
            (ctx as? android.app.Activity)?.finish()
            Runtime.getRuntime().exit(0)
        }, 800)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Настройки", color = AppTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f))

            // Колокольчик — центр уведомлений
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.surface)
                    .clickable {
                        unreadNotifications = 0
                        ctx.startActivity(Intent(ctx, NotificationsActivity::class.java))
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Уведомления",
                    tint = AppTheme.colors.text,
                    modifier = Modifier.size(22.dp)
                )
                if (unreadNotifications > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(AppTheme.colors.error),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadNotifications > 9) "9+" else unreadNotifications.toString(),
                            color = AppTheme.colors.onError,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // Блок логина
        SectionCard {
            Text("Аккаунт", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Скрыть пароль" else "Показать пароль",
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Button(text = "Сохранить", onClick = { save() }, modifier = Modifier.fillMaxWidth().height(48.dp))
        }
        Spacer(Modifier.height(12.dp))

        // Блок темы
        SectionCard {
            Text("Тема оформления", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            DropdownField(
                selectedLabel = themeLabel(theme),
                options = themeOptions,
                expanded = themeDropdownExpanded,
                onExpandedChange = { themeDropdownExpanded = it },
                onSelect = { setTheme(it) },
                contentDescription = "Выбрать тему"
            )
        }
        Spacer(Modifier.height(12.dp))

        // Блок уведомлений
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Уведомления", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Оценки и домашние задания", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                }
                Switch(
                    checked = notifEnabled,
                    onCheckedChange = { toggleNotif(it) }
                )
            }

            if (notifEnabled) {
                Spacer(Modifier.height(12.dp))
                DropdownField(
                    selectedLabel = intervalLabel(notifInterval),
                    options = intervals,
                    expanded = intervalDropdownExpanded,
                    onExpandedChange = { intervalDropdownExpanded = it },
                    onSelect = { setNotifInterval(it) },
                    contentDescription = "Выбрать интервал",
                    caption = "Интервал проверки",
                    verticalPadding = 12.dp
                )

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Тихие часы", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Не беспокоить с ${hourText(quietFrom)} до ${hourText(quietTo)}",
                            color = AppTheme.colors.textSecondary, fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = quietEnabled,
                        onCheckedChange = { on ->
                            quietEnabled = on
                            Prefs.setQuietEnabled(ctx, on)
                        }
                    )
                }
                if (quietEnabled) {
                    Spacer(Modifier.height(10.dp))
                    DropdownField(
                        selectedLabel = "с " + hourText(quietFrom),
                        options = hours,
                        expanded = quietDropdown == "from",
                        onExpandedChange = { if (it) quietDropdown = "from" else if (quietDropdown == "from") quietDropdown = null },
                        onSelect = { h -> quietFrom = h; Prefs.setQuietFrom(ctx, h) },
                        contentDescription = "Начало тихих часов",
                        caption = "Начало",
                        verticalPadding = 12.dp
                    )
                    Spacer(Modifier.height(8.dp))
                    DropdownField(
                        selectedLabel = "до " + hourText(quietTo),
                        options = hours,
                        expanded = quietDropdown == "to",
                        onExpandedChange = { if (it) quietDropdown = "to" else if (quietDropdown == "to") quietDropdown = null },
                        onSelect = { h -> quietTo = h; Prefs.setQuietTo(ctx, h) },
                        contentDescription = "Конец тихих часов",
                        caption = "Конец",
                        verticalPadding = 12.dp
                    )
                }

                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Один дайджест в день", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Оценки, контрольные и ДЗ одним сообщением",
                            color = AppTheme.colors.textSecondary, fontSize = 12.sp
                        )
                    }
                    Switch(
                        checked = digestEnabled,
                        onCheckedChange = { on ->
                            digestEnabled = on
                            Prefs.setDigestEnabled(ctx, on)
                        }
                    )
                }
                if (digestEnabled) {
                    Spacer(Modifier.height(10.dp))
                    DropdownField(
                        selectedLabel = hourText(digestHour),
                        options = hours,
                        expanded = digestDropdownExpanded,
                        onExpandedChange = { digestDropdownExpanded = it },
                        onSelect = { h -> digestHour = h; Prefs.setDigestHour(ctx, h) },
                        contentDescription = "Время дайджеста",
                        caption = "Время дайджеста",
                        verticalPadding = 12.dp
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        // Расписание — свой класс
        SectionCard {
            Text("Расписание", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Класс для показа расписания", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            if (classOptions.isEmpty()) {
                Text("Список классов не загружен (проверь сайт)", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
            } else {
                DropdownField(
                    selectedLabel = selectedClass ?: "Не выбран",
                    options = classOptions.map { it to it },
                    expanded = classDropdownExpanded,
                    onExpandedChange = { classDropdownExpanded = it },
                    onSelect = { cls ->
                        Prefs.setSelectedClass(ctx, cls)
                        selectedClass = cls
                    },
                    contentDescription = "Выбрать класс"
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Виджеты — переход на отдельный экран с предпросмотром
        SectionCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { ctx.startActivity(Intent(ctx, WidgetGalleryActivity::class.java)) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Виджеты", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Предпросмотр, тема и добавление на главный экран",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Открыть виджеты",
                    tint = AppTheme.colors.primary
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Версия и кнопки
        SectionCard {
            Text("Версия ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", color = AppTheme.colors.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Проверять обновления", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Автоматически при запуске", color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                }
                Switch(
                    checked = checkUpdates,
                    onCheckedChange = {
                        checkUpdates = it
                        Prefs.setCheckUpdatesEnabled(ctx, it)
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Button(text = "Проверить обновления", variant = ButtonVariant.Secondary, onClick = { onCheckUpdate() },
                modifier = Modifier.fillMaxWidth().height(48.dp))
            Spacer(Modifier.height(8.dp))
            Button(text = "Выйти из аккаунта", variant = ButtonVariant.Destructive, onClick = {
                Prefs.clearCredentials(ctx)
                val intent = Intent(ctx, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                ctx.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth().height(48.dp))
        }
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppTheme.colors.surface)
            .padding(16.dp)
    ) {
        content()
    }
}

/**
 * Поле с выпадающим списком. Используется и для темы, и для интервала проверки —
 * раньше эти два блока были скопированы друг у друга почти целиком.
 */
@Composable
private fun <T> DropdownField(
    selectedLabel: String,
    options: List<Pair<T, String>>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (T) -> Unit,
    contentDescription: String,
    caption: String? = null,
    verticalPadding: Dp = 14.dp
) {
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppTheme.colors.background)
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 14.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (caption == null) {
                Text(
                    selectedLabel,
                    color = AppTheme.colors.text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(caption, color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    Text(selectedLabel, color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = contentDescription,
                tint = AppTheme.colors.textSecondary
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            containerColor = AppTheme.colors.surface.copy(alpha = 0.98f),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label, fontSize = 15.sp, color = AppTheme.colors.text) },
                    onClick = {
                        onExpandedChange(false)
                        onSelect(value)
                    }
                )
            }
        }
    }
}