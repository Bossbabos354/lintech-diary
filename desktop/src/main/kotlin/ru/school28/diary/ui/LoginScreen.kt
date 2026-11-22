package ru.school28.diary.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.AppLog
import ru.school28.diary.AppState
import ru.school28.diary.Background
import ru.school28.diary.Settings
import ru.school28.diary.api.ApiClient

@Composable
fun LoginScreen(state: AppState) {
    val scope = rememberCoroutineScope()
    var login by remember { mutableStateOf(Settings.login.orEmpty()) }
    var password by remember { mutableStateOf(Settings.password.orEmpty()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(430.dp)
                .glass(RoundedCornerShape(28.dp), AppTheme.colors)
                .padding(28.dp),
        ) {
            Text("ЛинТех 28", color = AppTheme.colors.text, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
            Text("Электронный дневник", color = AppTheme.colors.textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))

            OutlinedTextField(
                value = login,
                onValueChange = { login = it },
                label = { Text("Логин") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error ?: "", color = AppTheme.colors.error, fontSize = 13.sp)
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    if (login.isBlank() || password.isBlank()) {
                        error = "Введите логин и пароль"
                        return@Button
                    }
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val person = withContext(Dispatchers.IO) {
                                ApiClient.login(login.trim(), password.trim())
                            }
                            if (person == null) {
                                error = "Неверный логин или пароль"
                            } else {
                                Settings.saveCredentials(login.trim(), password.trim(), person.studentId, person.userId)
                                Settings.uchId = person.uchId
                                Settings.uchName = person.uchName
                                Settings.uchYear = withContext(Dispatchers.IO) { ApiClient.getUchYear(person.studentId) }
                                Settings.classId = withContext(Dispatchers.IO) {
                                    ApiClient.getStudentClass(person.studentId, Settings.uchYear)
                                }
                                Background.reschedule()
                                state.loggedIn = true
                                state.toast = "Добро пожаловать, ${person.fio}"
                            }
                        } catch (e: Exception) {
                            AppLog.logException("login", e)
                            error = "Ошибка: ${e.message}"
                        }
                        loading = false
                    }
                },
                enabled = !loading && login.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(if (loading) "Вхожу…" else "Войти")
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Логин и пароль хранятся только на этом компьютере и никуда не отправляются, кроме самого журнала.",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp,
            )
        }
    }
}
