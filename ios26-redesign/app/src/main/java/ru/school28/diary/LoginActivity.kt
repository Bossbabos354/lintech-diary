package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.school28.diary.api.ApiClient
import ru.school28.diary.glass.AuroraBackground
import ru.school28.diary.glass.AppLogo
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassButton
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassShape
import ru.school28.diary.glass.GlassSurface
import ru.school28.diary.glass.GlassTextField

class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        enableGlassEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            DiaryTheme {
                LoginScreen()
            }
        }
    }

    @Composable
    fun LoginScreen() {
        val ctx = LocalContext.current
        val colors = Glass.colors
        var login by remember { mutableStateOf(Prefs.getLogin(ctx) ?: "") }
        var password by remember { mutableStateOf(Prefs.getPassword(ctx) ?: "") }
        var loading by remember { mutableStateOf(false) }
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            delay(90)
            visible = true
        }
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(520),
            label = "loginAlpha",
        )
        val offset by animateDpAsState(
            targetValue = if (visible) 0.dp else 26.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 160f),
            label = "loginOffset",
        )

        Box(Modifier.fillMaxSize()) {
            AuroraBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(46.dp))
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(128.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(colors.accent.copy(alpha = 0.22f), androidx.compose.ui.graphics.Color.Transparent),
                                )
                            )
                    )
                    AppLogo(size = 96.dp)
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "ЛинТех 28",
                    color = colors.text,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = "электронный дневник",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.3.sp,
                )
                Spacer(Modifier.height(28.dp))

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            this.alpha = alpha
                            translationY = offset.toPx()
                        },
                    shape = GlassShape.screen,
                    contentPadding = PaddingValues(20.dp),
                ) {
                    Text(
                        text = "Вход",
                        color = colors.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Войди в аккаунт электронного журнала",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    GlassTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = "Логин",
                        placeholder = "Введи логин",
                        enabled = !loading,
                    )
                    Spacer(Modifier.height(9.dp))
                    GlassTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Пароль",
                        placeholder = "Введи пароль",
                        isPassword = true,
                        keyboardType = KeyboardType.Password,
                        enabled = !loading,
                    )
                    Spacer(Modifier.height(18.dp))
                    GlassButton(
                        text = "Войти",
                        onClick = {
                            if (login.isBlank() || password.isBlank()) {
                                Toast.makeText(ctx, "Введите логин и пароль", Toast.LENGTH_SHORT).show()
                                return@GlassButton
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
                                    Prefs.setFio(ctx, personInfo.fio)
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

                                    val intent = Intent(ctx, MainRoute.activity(ctx))
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
                        modifier = Modifier.fillMaxWidth(),
                        height = 52.dp,
                    )
                }

                Spacer(Modifier.height(16.dp))

                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlassShape.small,
                    elevated = false,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = "Логин и пароль хранятся только на этом устройстве и никуда не отправляются.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    text = "версия ${BuildConfig.VERSION_NAME}",
                    color = colors.textTertiary,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}
