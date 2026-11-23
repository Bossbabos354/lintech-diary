package ru.school28.diary.glass

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.school28.diary.Prefs
import ru.school28.diary.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    wallpaper: Boolean = false,
) {
    val colors = Glass.colors
    val context = LocalContext.current

    // Пользователь выбрал фотографию вместо градиента «Аврора».
    if (Prefs.getAppBg(context) == Prefs.BG_PHOTO) {
        Box(modifier.fillMaxSize().clipToBounds()) {
            Image(
                painter = painterResource(R.drawable.app_bg_new),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(colors.background.copy(alpha = if (colors.isDark) 0.42f else 0.30f))
            )
        }
        return
    }

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val heightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)
        val bitmap = remember(widthPx, heightPx, colors) {
            createAuroraBitmap(widthPx, heightPx, colors)
        }
        // На слабых устройствах фон статичный: анимация полноэкранного слоя
        // каждый кадр заметно греет телефон и роняет FPS.
        val lowEnd = DevicePerf.isLowEnd(LocalContext.current)
        val phase by if (lowEnd) {
            remember { mutableStateOf(0f) }
        } else {
            rememberInfiniteTransition(label = "aurora").animateFloat(
                initialValue = 0f,
                targetValue = (2f * PI).toFloat(),
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 26000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "phase",
            )
        }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                    translationX = sin(phase) * size.width * 0.018f
                    translationY = cos(phase * 0.8f) * size.height * 0.014f
                },
        )

        if (wallpaper) {
            Image(
                painter = painterResource(R.drawable.bg_zhan),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.5f,
            )
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.18f)))
        }
    }
}

private fun createAuroraBitmap(widthPx: Int, heightPx: Int, colors: GlassColors): ImageBitmap {
    val width = (widthPx / 2).coerceIn(180, 720)
    val height = (heightPx / 2).coerceIn(260, 1280)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    paint.shader = LinearGradient(
        0f,
        0f,
        width.toFloat(),
        height.toFloat(),
        colors.background.toArgb(),
        colors.backgroundSecondary.toArgb(),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val minSide = min(width, height).toFloat()
    fun blob(cx: Float, cy: Float, radius: Float, color: Color) {
        paint.shader = RadialGradient(
            cx,
            cy,
            radius,
            intArrayOf(
                color.toArgb(),
                color.copy(alpha = color.alpha * 0.45f).toArgb(),
                Color.Transparent.toArgb(),
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(cx, cy, radius, paint)
    }

    val alpha = if (colors.isDark) 0.5f else 0.55f
    blob(width * 0.16f, height * 0.18f, minSide * 0.72f, colors.blobA.copy(alpha = alpha))
    blob(width * 0.95f, height * 0.1f, minSide * 0.62f, colors.blobB.copy(alpha = alpha * 0.9f))
    blob(width * 0.86f, height * 0.72f, minSide * 0.7f, colors.blobC.copy(alpha = alpha * 0.8f))
    blob(width * 0.1f, height * 0.88f, minSide * 0.66f, colors.blobD.copy(alpha = alpha * 0.72f))

    paint.shader = LinearGradient(
        0f,
        0f,
        0f,
        height.toFloat(),
        intArrayOf(
            if (colors.isDark) 0x06FFFFFF else 0x14FFFFFF,
            0x00000000,
            colors.background.copy(alpha = if (colors.isDark) 0.35f else 0.18f).toArgb(),
        ),
        floatArrayOf(0f, 0.45f, 1f),
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    return bitmap.asImageBitmap()
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    size: Dp = 34.dp,
    iconSize: Dp = 19.dp,
) {
    val colors = Glass.colors
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(size * 0.32f),
                clip = false,
                ambientColor = colors.accent.copy(alpha = 0.35f),
                spotColor = colors.accent.copy(alpha = 0.45f),
            )
            .clip(RoundedCornerShape(size * 0.32f))
            .background(
                Brush.linearGradient(
                    colors = listOf(colors.accent, colors.accentSecondary),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.MenuBook,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun AppWallpaperEnabled(): Boolean = Glass.colors.wallpaper

@Composable
fun wallpaperImageEnabled(): Boolean = LocalContext.current != null && Glass.colors.wallpaper

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
) {
    val colors = Glass.colors
    // Иконка на заставке и в шапке та же, что выбрана в «Оформлении».
    val icon = if (Prefs.getAppIcon(LocalContext.current) == Prefs.ICON_OLD) {
        R.drawable.ic_launcher_photo
    } else {
        R.drawable.app_icon_new
    }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.radialGradient(
                        listOf(colors.accent.copy(alpha = 0.18f), Color.Transparent),
                    )
                )
        )
        Image(
            painter = painterResource(icon),
            contentDescription = "ЛинТех 28",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(size * 0.22f)),
        )
    }
}
