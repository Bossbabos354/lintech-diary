package ru.school28.diary

import android.content.Context
import ru.school28.diary.legacy.LegacyMainActivity

/** Какой главный экран открывать: новый (glass) или старый интерфейс. */
object MainRoute {
    fun activity(ctx: Context): Class<*> =
        if (Prefs.isLegacyUi(ctx)) LegacyMainActivity::class.java else MainActivity::class.java
}
