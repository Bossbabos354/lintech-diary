package ru.school28.diary.glass

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Слабые устройства: на них отключаем дорогие эффекты (анимация фона,
 * тени карточек, каскадное появление), чтобы интерфейс не тормозил.
 */
object DevicePerf {
    @Volatile
    private var lowEnd: Boolean? = null

    fun isLowEnd(context: Context): Boolean {
        lowEnd?.let { return it }
        val value = runCatching {
            if (Build.VERSION.SDK_INT < 26) return@runCatching true
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (am.isLowRamDevice) return@runCatching true
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            info.totalMem / (1024.0 * 1024.0 * 1024.0) < 2.5
        }.getOrDefault(false)
        lowEnd = value
        return value
    }
}
