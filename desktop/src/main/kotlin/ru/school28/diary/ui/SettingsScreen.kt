package ru.school28.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import ru.school28.diary.ui.SmoothLazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.AppLog
import ru.school28.diary.AppPaths
import ru.school28.diary.AppState
import ru.school28.diary.Background
import ru.school28.diary.BuildInfo
import ru.school28.diary.Repo
import ru.school28.diary.Settings
import ru.school28.diary.UpdateChecker
import ru.school28.diary.Util
import ru.school28.diary.api.ApiClient
import ru.school28.diary.export.Exporter

@Composable
fun SettingsScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf(Settings.login.orEmpty()) }
    var password by remember { mutableStateOf(Settings.password.orEmpty()) }
    var showPassword by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }

    var notifEnabled by remember { mutableStateOf(Settings.notifEnabled) }
    var interval by remember { mutableStateOf(Settings.notifIntervalMin) }
    var quietEnabled by remember { mutableStateOf(Settings.quietEnabled) }
    var quietFrom by remember { mutableStateOf(Settings.quietFrom) }
    var quietTo by remember { mutableStateOf(Settings.quietTo) }
    var digestEnabled by remember { mutableStateOf(Settings.digestEnabled) }
    var digestHour by remember { mutableStateOf(Settings.digestHour) }
    var checkUpdates by remember { mutableStateOf(Settings.checkUpdates) }
    var selectedClass by remember { mutableStateOf(Settings.selectedClass) }
    var classes by remember { mutableStateOf<List<String>>(emptyList()) }
    var working by remember { mutableStateOf(false) }
    var updateUrl by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        classes = try {
            withContext(Dispatchers.IO) { Repo.schedule()?.classes.orEmpty() }
        } catch (e: Exception) {
            AppLog.logException("settings/classes", e)
            emptyList()
        }
    }

    val intervals = listOf(15, 30, 60, 120, 240, 480, 1440)
    val hours = (0..23).map { it }

    fun saveAccount() {
        if (login.isBlank() || password.isBlank()) {
            accountError = "Введите логин и пароль"
            return
        }
        saving = true
        accountError = null
        scope.launch {
            try {
                val person = withContext(Dispatchers.IO) {
                    ApiClient.login(login.trim(), password.trim())
                }
                if (person == null) {
                    accountError = "Неверный логин или пароль"
                } else {
                    Settings.saveCredentials(login.trim(), password.trim(), person.studentId, person.userId)
                    Settings.uchId = person.uchId
                    Settings.uchName = person.uchName
                    Settings.uchYear = withContext(Dispatchers.IO) { ApiClient.getUchYear(person.studentId) }
                    Settings.classId = withContext(Dispatchers.IO) {
                        ApiClient.getStudentClass(person.studentId, Settings.uchYear)
                    }
                    state.loggedIn = true
                    state.toast = "Данные сохранены"
                }
            } catch (e: Exception) {
                AppLog.logException("settings/login", e)
                accountError = e.message
            }
            saving = false
        }
    }

    SmoothLazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 24.dp, end = 24.dp, top = 18.dp, bottom = 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("Настройки", color = AppTheme.colors.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }

        item {
            CardBox {
                Text("Аккаунт", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = login,
                    onValueChange = { login = it },
                    label = { Text("Логин") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Пароль") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { showPassword = !showPassword }) {
                    Text(if (showPassword) "Скрыть пароль" else "Показать пароль", color = AppTheme.colors.primary)
                }
                if (accountError != null) {
                    Text(accountError ?: "", color = AppTheme.colors.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { saveAccount() },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (saving) "Проверяю…" else "Сохранить и войти")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Логин и пароль хранятся только на этом компьютере, в файле настроек.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                )
            }
        }

        item {
            CardBox {
                Text("Тема оформления", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip("Системная", state.theme == Settings.THEME_SYSTEM, Modifier.weight(1f)) {
                        Settings.theme = Settings.THEME_SYSTEM
                        state.theme = Settings.THEME_SYSTEM
                    }
                    Chip("Светлая", state.theme == Settings.THEME_LIGHT, Modifier.weight(1f)) {
                        Settings.theme = Settings.THEME_LIGHT
                        state.theme = Settings.THEME_LIGHT
                    }
                    Chip("Тёмная", state.theme == Settings.THEME_DARK, Modifier.weight(1f)) {
                        Settings.theme = Settings.THEME_DARK
                        state.theme = Settings.THEME_DARK
                    }
                }
            }
        }

        item {
            CardBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Уведомления", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("Новые оценки и домашние задания", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                    }
                    Switch(
                        checked = notifEnabled,
                        onCheckedChange = {
                            notifEnabled = it
                            Settings.notifEnabled = it
                            if (it) Background.reschedule() else Background.stop()
                        },
                    )
                }
                if (notifEnabled) {
                    Spacer(Modifier.height(12.dp))
                    Picker(
                        options = intervals.map { it to intervalLabel(it) },
                        selectedLabel = intervalLabel(interval),
                        caption = "Интервал проверки",
                        onSelect = {
                            interval = it
                            Settings.notifIntervalMin = it
                            Background.reschedule()
                        },
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Тихие часы", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Не беспокоить с ${Util.hourText(quietFrom)} до ${Util.hourText(quietTo)}",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = quietEnabled,
                            onCheckedChange = {
                                quietEnabled = it
                                Settings.quietEnabled = it
                            },
                        )
                    }
                    if (quietEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Picker(
                            options = hours.map { it to "с " + Util.hourText(it) },
                            selectedLabel = "с " + Util.hourText(quietFrom),
                            onSelect = { quietFrom = it; Settings.quietFrom = it },
                        )
                        Spacer(Modifier.height(8.dp))
                        Picker(
                            options = hours.map { it to "до " + Util.hourText(it) },
                            selectedLabel = "до " + Util.hourText(quietTo),
                            onSelect = { quietTo = it; Settings.quietTo = it },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Один дайджест в день", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "Оценки, контрольные и ДЗ одним сообщением",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = digestEnabled,
                            onCheckedChange = {
                                digestEnabled = it
                                Settings.digestEnabled = it
                            },
                        )
                    }
                    if (digestEnabled) {
                        Spacer(Modifier.height(10.dp))
                        Picker(
                            options = hours.map { it to Util.hourText(it) },
                            selectedLabel = Util.hourText(digestHour),
                            caption = "Время дайджеста",
                            onSelect = { digestHour = it; Settings.digestHour = it },
                        )
                    }
                }
            }
        }

        item {
            CardBox {
                Text("Расписание", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("Класс для показа расписания", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                if (classes.isEmpty()) {
                    Text("Список классов не загружен (проверь сайт)", color = AppTheme.colors.textSecondary, fontSize = 13.sp)
                } else {
                    Picker(
                        options = classes.map { it to it },
                        selectedLabel = selectedClass ?: "Не выбран",
                        onSelect = {
                            selectedClass = it
                            Settings.selectedClass = it
                        },
                    )
                }
            }
        }

        item {
            CardBox {
                Text("Экспорт данных", color = AppTheme.colors.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Дневник и оценки в PDF, оценки в XLSX, расписание в ICS и всё сразу в ZIP",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (exporting) return@Button
                        exporting = true
                        scope.launch {
                            try {
                                val marks = Repo.marks()
                                val (monday, sunday) = Util.weekRange(0)
                                val diary = Repo.dairy(monday, sunday)
                                val schedule = Repo.schedule()
                                val result = withContext(Dispatchers.IO) {
                                    Exporter.exportAll(marks, diary, schedule, Settings.selectedClass)
                                }
                                state.toast = "Экспортировано файлов: ${result.files.size}"
                                java.awt.Desktop.getDesktop().open(result.dir)
                            } catch (e: Exception) {
                                AppLog.logException("export", e)
                                state.toast = "Ошибка экспорта: ${e.message}"
                            } finally {
                                exporting = false
                            }
                        }
                    },
                    enabled = !exporting,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (exporting) "Экспортирую…" else "Экспортировать всё")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        AppPaths.exportDir.mkdirs()
                        runCatching { java.awt.Desktop.getDesktop().open(AppPaths.exportDir) }
                            .onFailure { state.toast = "Папка: ${AppPaths.exportDir}" }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Открыть папку экспорта")
                }
            }
        }

        item {
            CardBox {
                Text(
                    "Версия ${BuildInfo.VERSION_NAME}",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Проверять обновления", color = AppTheme.colors.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("При запуске и в фоне", color = AppTheme.colors.textSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = checkUpdates,
                        onCheckedChange = {
                            checkUpdates = it
                            Settings.checkUpdates = it
                        },
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (!working) {
                            working = true
                            scope.launch {
                                val result = UpdateChecker.check()
                                working = false
                                val update = result.update
                                updateUrl = update?.url
                                state.toast = when {
                                    update != null -> "Доступна версия ${update.versionName}"
                                    result.error != null -> result.error
                                    else -> "Обновлений нет"
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(if (working) "Проверяю…" else "Проверить обновления")
                }
                updateUrl?.let { url ->
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { UpdateChecker.open(url) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("На сайт")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            java.awt.Desktop.getDesktop().open(AppPaths.dataDir)
                        } catch (e: Exception) {
                            AppLog.logException("openDataDir", e)
                            state.toast = "Папка: ${AppPaths.dataDir}"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Открыть папку с логами")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        Settings.clearCredentials()
                        state.loggedIn = false
                        state.tab = 0
                        Background.stop()
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.error,
                        contentColor = AppTheme.colors.onError,
                    ),
                ) {
                    Text("Выйти из аккаунта")
                }
            }
        }
    }
}

private fun intervalLabel(minutes: Int): String = when (minutes) {
    15 -> "каждые 15 минут"
    30 -> "каждые 30 минут"
    60 -> "каждый час"
    120 -> "каждые 2 часа"
    240 -> "каждые 4 часа"
    480 -> "каждые 8 часов"
    1440 -> "раз в день"
    else -> "каждые $minutes мин"
}
