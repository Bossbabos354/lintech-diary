package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.ui_components.AppTheme
import ru.school28.diary.ui_components.components.Button
import ru.school28.diary.ui_components.components.textfield.OutlinedTextField
import ru.school28.diary.ui_components.components.Text
import ru.school28.diary.ui_components.gradientHeader

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        super.onCreate(savedInstanceState)

        setContent {
            DiaryTheme {
                LoginScreen()
            }
        }
    }

    @androidx.compose.runtime.Composable
    fun LoginScreen() {
        val ctx = LocalContext.current
        var login by remember { mutableStateOf(Prefs.getLogin(ctx) ?: "") }
        var password by remember { mutableStateOf(Prefs.getPassword(ctx) ?: "") }
        var loading by remember { mutableStateOf(false) }

        val scrollState = rememberScrollState()
        var showUi by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(100)
            showUi = true
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Градиентная шапка
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(gradientHeader())
                    .padding(top = 60.dp, bottom = 32.dp, start = 24.dp, end = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ЛинТех 28",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Электронный дневник",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                if (showUi) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(AppTheme.colors.surface)
                            .padding(20.dp)
                    ) {
                        Text("Вход", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = AppTheme.colors.text)
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = login,
                            onValueChange = { login = it },
                            label = { Text("Логин") },
                            singleLine = true,
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Пароль") },
                            singleLine = true,
                            enabled = !loading,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(
                            text = "Войти",
                            onClick = {
                                if (login.isBlank() || password.isBlank()) {
                                    Toast.makeText(ctx, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                loading = true
                                lifecycleScope.launch {
                                    try {
                                        val personInfo = withContext(Dispatchers.IO) {
                                            ApiClient.login(login.trim(), password.trim())
                                        }
                                        if (personInfo == null) {
                                            Toast.makeText(ctx, "Неверный логин или пароль", Toast.LENGTH_SHORT).show()
                                            loading = false
                                            return@launch
                                        }
                                        Prefs.saveCredentials(ctx, login.trim(), password.trim(), personInfo.studentId)
                                        Prefs.saveUserId(ctx, personInfo.userId)
                                        Prefs.setUchId(ctx, personInfo.uchId)
                                        Prefs.setUchName(ctx, personInfo.uchName)
                                        val year = withContext(Dispatchers.IO) {
                                            ApiClient.getUchYear(personInfo.studentId)
                                        }
                                        Prefs.setUchYear(ctx, year)

                                        if (Prefs.isNotifEnabled(ctx)) {
                                            NotificationScheduler.schedule(ctx)
                                        }

                                        val intent = Intent(ctx, MainActivity::class.java)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    } catch (e: Exception) {
                                        Toast.makeText(ctx, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                                        loading = false
                                    }
                                }
                            },
                            enabled = !loading && login.isNotBlank() && password.isNotBlank(),
                            loading = loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Я не собираю ваши данные и не использую их!\nОни хранятся только на вашем устройстве!",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}