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
        val enable = if (newIcon) ALIAS_NEW else ALIAS_OLD
        val disable = if (newIcon) ALIAS_OLD else ALIAS_NEW
        // Сначала включаем нужный ярлык, потом убираем прежний —
        // иначе иконка на секунду исчезает с рабочего стола.
        runCatching {
            setEnabled(context, pkg + enable, true)
            setEnabled(context, pkg + disable, false)
        }
    }

    /** Повторный вызов на старте не дёргает лаунчер: трогаем только расходящееся состояние. */
    private fun setEnabled(context: Context, name: String, enabled: Boolean) {
        val pm = context.packageManager
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        val component = ComponentName(context.packageName, name)
        if (pm.getComponentEnabledSetting(component) != state) {
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
        }
    }
}
