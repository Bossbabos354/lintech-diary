package ru.school28.diary

import android.app.Application
import ru.school28.diary.widgets.WidgetUpdateScheduler
import ru.school28.diary.api.ApiClient

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        FileLog.init(this)
        ApiClient.init(this)
        // Держим ярлык в соответствии с выбором в «Оформлении» (после обновления/переустановки).
        LauncherIcons.apply(this, Prefs.getAppIcon(this) != Prefs.ICON_OLD)
        NotificationHelper.createChannels(this)
        if (Prefs.isNotifEnabled(this) && Prefs.getStudentId(this) != -1) {
            NotificationScheduler.schedule(this)
        }
        if (Prefs.getStudentId(this) != -1) {
            WidgetUpdateScheduler.schedule(this)
        }
    }
}