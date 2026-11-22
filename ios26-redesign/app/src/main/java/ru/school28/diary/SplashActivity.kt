package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.school28.diary.api.ApiClient
import ru.school28.diary.glass.AuroraBackground
import ru.school28.diary.glass.AppLogo
import ru.school28.diary.glass.Glass
import ru.school28.diary.glass.GlassCard
import ru.school28.diary.glass.GlassProgressBar
import ru.school28.diary.glass.GlassShape
import ru.school28.diary.ui.GoalMath
import java.util.Calendar
import kotlin.math.hypot

enum class LineState { RUNNING, OK, ERROR }

data class StatusLine(
    val text: String,
    val state: LineState = LineState.RUNNING,
    val detail: Boolean = false,
)

class SplashActivity : ComponentActivity() {

    private val lines = mutableStateListOf<StatusLine>()
    private var progress by mutableFloatStateOf(0f)
    private var loadingDone by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        when (Prefs.getTheme(this)) {
            Prefs.THEME_LIGHT, Prefs.THEME_ZHAN -> setTheme(R.style.Theme_SchoolDiary_Light)
            Prefs.THEME_DARK -> setTheme(R.style.Theme_SchoolDiary_Dark)
            else -> setTheme(R.style.Theme_SchoolDiary)
        }
        enableGlassEdgeToEdge()
        super.onCreate(savedInstanceState)

        val hasCreds = Prefs.getStudentId(this) != -1 &&
            Prefs.getLogin(this) != null && Prefs.getPassword(this) != null

        setContent {
            DiaryTheme {
                Crossfade(
                    targetState = loadingDone,
                    animationSpec = tween(420),
                    label = "splash",
                ) { done ->
                    if (done) {
                        LogoOutro(onFinished = { goNext(hasCreds) })
                    } else {
                        TerminalSplash(lines = lines, progress = progress)
                    }
                }
            }
        }

        if (hasCreds) {
            startLoading()
        } else {
            lines.add(StatusLine("вход в аккаунт"))
            loadingDone = true
        }
    }

    private fun addLine(text: String): Int {
        lines.add(StatusLine(text, LineState.RUNNING))
        return lines.lastIndex
    }

    private fun addDetail(text: String) {
        lines.add(StatusLine(text, LineState.OK, detail = true))
    }

    private fun markState(index: Int, state: LineState) {
        if (index in lines.indices) lines[index] = lines[index].copy(state = state)
    }

    private fun startLoading() {
        lifecycleScope.launch {
            val finished = withTimeoutOrNull(25_000L) { loadAll() }
            if (finished == null) {
                val last = lines.lastIndex
                if (last >= 0 && lines[last].state == LineState.RUNNING) {
                    markState(last, LineState.ERROR)
                }
                lines.add(StatusLine("сервер не ответил, продолжаю", LineState.ERROR))
            }
            progress = 1f
            loadingDone = true
        }
    }

    private suspend fun loadAll() {
        val login = Prefs.getLogin(this) ?: return
        val password = Prefs.getPassword(this) ?: return
        val studentId = Prefs.getStudentId(this)

        val iConnect = addLine("подключение к серверу")
        try {
            withContext(Dispatchers.IO) {
                ApiClient.restoreSession(login, password)
                if (Prefs.getClassId(this@SplashActivity) == -1) {
                    val uchYear = Prefs.getUchYear(this@SplashActivity)
                    val classId = ApiClient.getStudentClass(studentId, uchYear)
                    Prefs.setClassId(this@SplashActivity, classId)
                }
            }
            markState(iConnect, LineState.OK)
            addDetail("сессия восстановлена")
            progress = 0.3f
        } catch (e: Exception) {
            FileLog.logException("splash/connect", e)
            markState(iConnect, LineState.ERROR)
        }

        val classId = Prefs.getClassId(this)
        val uchYear = Prefs.getUchYear(this)
        if (classId == -1) return

        val iMarks = addLine("загрузка оценок")
        val iDairy = addLine("загрузка домашних заданий")

        // Оценки и домашка независимы друг от друга — грузим параллельно,
        // чтобы сплэш не ждал два сетевых запроса подряд.
        coroutineScope {
            val marksJob = async {
                try {
                    val subjects = withContext(Dispatchers.IO) {
                        ApiClient.getMarks(classId, studentId, uchYear)
                    }
                    markState(iMarks, LineState.OK)

                    val periodMarks = subjects.flatMap { GoalMath.currentPeriodLessons(it) }
                    val marksCount = periodMarks.size
                    val avg = GoalMath.weightedAverage(periodMarks)
                    val avgText = if (avg == null) "—" else String.format(java.util.Locale("ru"), "%.2f", avg)
                    addDetail("предметов: ${subjects.size} · оценок: $marksCount")
                    addDetail("средний балл: $avgText")

                    progress = maxOf(progress, 0.65f)
                } catch (e: Exception) {
                    FileLog.logException("splash/marks", e)
                    markState(iMarks, LineState.ERROR)
                }
            }

            val dairyJob = async {
                try {
                    val (monday, sunday) = currentWeekRange()
                    val lessons = withContext(Dispatchers.IO) {
                        ApiClient.getDairy(classId, studentId, monday, sunday)
                    }
                    markState(iDairy, LineState.OK)

                    val withHw = lessons.count { it.homework.isNotBlank() }
                    val withMark = lessons.count { it.mark.isNotBlank() }
                    val days = lessons.map { dayKey(it.date) }.distinct().size
                    addDetail("уроков: ${lessons.size} · дней: $days")
                    addDetail("с домашним заданием: $withHw · с оценкой: $withMark")

                    progress = maxOf(progress, 0.9f)
                } catch (e: Exception) {
                    FileLog.logException("splash/dairy", e)
                    markState(iDairy, LineState.ERROR)
                }
            }

            marksJob.await()
            dairyJob.await()
        }

        addLine("готово").let { markState(it, LineState.OK) }
        progress = 1f
    }

    private fun dayKey(date: java.util.Date): Int {
        val c = Calendar.getInstance().apply { time = date }
        return c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
    }

    private fun currentWeekRange(): Pair<java.util.Date, java.util.Date> {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        cal.add(Calendar.DAY_OF_YEAR, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val monday = cal.time
        cal.add(Calendar.DAY_OF_YEAR, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val sunday = cal.time
        return monday to sunday
    }

    private fun goNext(hasCreds: Boolean) {
        if (isFinishing) return
        if (hasCreds) {
            if (Prefs.isNotifEnabled(this)) {
                NotificationScheduler.schedule(this)
            }
        }
        val target = if (hasCreds) Intent(this, MainRoute.activity(this))
        else Intent(this, LoginActivity::class.java)
        target.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(target)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

@Composable
fun TerminalSplash(lines: List<StatusLine>, progress: Float) {
    val colors = Glass.colors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splashAlpha",
    )
    val rise by animateFloatAsState(
        targetValue = if (visible) 0f else 22f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 140f),
        label = "splashRise",
    )

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.graphicsLayer {
                    this.alpha = alpha
                    translationY = rise
                },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(colors.accent.copy(alpha = 0.24f), Color.Transparent),
                            )
                        )
                )
                AppLogo(size = 108.dp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "ЛинТех 28",
                color = colors.text,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
            Text(
                text = "электронный дневник",
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
            Spacer(Modifier.height(26.dp))

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        this.alpha = alpha
                        translationY = rise
                    },
                shape = GlassShape.screen,
                contentPadding = PaddingValues(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (progress >= 1f && lines.none { it.state == LineState.RUNNING }) "Готово" else "Загрузка",
                        color = colors.text,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${(progress.coerceIn(0f, 1f) * 100).toInt()}%",
                        color = colors.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                GlassProgressBar(progress = progress, height = 7.dp)
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    lines.takeLast(6).forEach { line ->
                        StatusRow(line)
                    }
                }
            }

            Spacer(Modifier.weight(1.25f))
            Text(
                text = "пожалуйста, подождите",
                color = colors.textTertiary,
                fontSize = 10.sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusRow(line: StatusLine) {
    val colors = Glass.colors
    val tint = when (line.state) {
        LineState.RUNNING -> colors.accent
        LineState.OK -> colors.success
        LineState.ERROR -> colors.danger
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(15.dp), contentAlignment = Alignment.Center) {
            when (line.state) {
                LineState.RUNNING -> CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    color = tint,
                    strokeWidth = 1.5.dp,
                )
                LineState.OK -> Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp),
                )
                LineState.ERROR -> Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = line.text,
            color = if (line.detail) colors.textSecondary else colors.text,
            fontSize = if (line.detail) 10.sp else 12.sp,
            fontWeight = if (line.detail) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
fun LogoOutro(onFinished: () -> Unit) {
    val colors = Glass.colors
    var visible by remember { mutableStateOf(false) }
    var flipped by remember { mutableStateOf(false) }
    val wave = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(60)
        visible = true
        delay(220)
        flipped = true
        // 90° проходим примерно на середине переворота — с этого момента пускаем волну.
        delay(360)
        wave.animateTo(1f, tween(durationMillis = 900, easing = LinearOutSlowInEasing))
        delay(450)
        onFinished()
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "outroAlpha",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f),
        label = "outroScale",
    )
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "logoFlip",
    )

    Box(Modifier.fillMaxSize()) {
        AuroraBackground()

        // Волна от перевернувшегося логотипа — расходится по всему экрану.
        val waveProgress = wave.value
        if (waveProgress > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f - 40.dp.toPx())
                val maxRadius = hypot(size.width.toFloat(), size.height.toFloat())
                val radius = maxRadius * waveProgress
                val fade = (1f - waveProgress).coerceIn(0f, 1f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.72f to colors.accent.copy(alpha = 0.10f * fade),
                            0.90f to colors.accent.copy(alpha = 0.30f * fade),
                            1f to Color.Transparent,
                        ),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
                drawCircle(
                    color = colors.accent.copy(alpha = 0.55f * fade),
                    radius = radius * 0.9f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                    rotationY = rotation
                    cameraDistance = 14f * density
                },
                contentAlignment = Alignment.Center,
            ) {
                if (rotation <= 90f) {
                    AppLogo(size = 124.dp)
                } else {
                    Box(
                        Modifier
                            .size(124.dp)
                            .graphicsLayer { rotationY = 180f }
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.lintex_logo),
                            contentDescription = "ЛинТех",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Добро пожаловать",
                color = colors.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
        }
    }
}
