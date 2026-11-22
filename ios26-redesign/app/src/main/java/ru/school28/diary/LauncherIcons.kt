package ru.school28.diary

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Переключение ярлыка на главном экране между новой и старой иконкой.
 * Оба ярлыка — activity-alias на SplashActivity; активен ровно один.
 */
object LauncherIcons {
    private const val ALIAS_NEW = ".LauncherNew"
    private const val ALIAS_OLD = ".LauncherOld"

    fun apply(context: Context, newIcon: Boolean) {
        val pkg = context.packageName
        val pm = context.packageManager
        val enable = if (newIcon) ALIAS_NEW else ALIAS_OLD
        val disable = if (newIcon) ALIAS_OLD else ALIAS_NEW
        // Сначала включаем нужный ярлык, потом убираем прежний —
        // иначе иконка на секунду исчезает с рабочего стола.
        runCatching {
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + enable),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                ComponentName(pkg, pkg + disable),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
