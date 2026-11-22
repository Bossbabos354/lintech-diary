package ru.school28.diary

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.school28.diary.api.DairyEntry
import ru.school28.diary.api.SubjectMarks

/** Предмет и период, для которых открыт редактор цели. */
data class GoalTarget(val subject: SubjectMarks, val periodKey: String?)

/** Состояние интерфейса: вкладка, тема, открытые карточки. */
class AppState {
    var windowVisible by mutableStateOf(true)
    var loggedIn by mutableStateOf(Settings.isLoggedIn)
    var theme by mutableStateOf(Settings.theme)
    var tab by mutableIntStateOf(0)
    var lesson by mutableStateOf<DairyEntry?>(null)
    var goal by mutableStateOf<GoalTarget?>(null)
    var searchOpen by mutableStateOf(false)
    var weekly by mutableStateOf(false)
    var critical by mutableStateOf(false)
    var goalsVersion by mutableIntStateOf(0)
    var toast by mutableStateOf<String?>(null)
    var trayAvailable by mutableStateOf(false)

    /** Выход из приложения; задаётся в application{}, где доступен exitApplication(). */
    var exitApp: () -> Unit = {}

    @Composable
    fun isDark(): Boolean = when (theme) {
        Settings.THEME_DARK -> true
        Settings.THEME_LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    /** Закрытие окна: в трей, если он есть, иначе выход. */
    fun requestClose() {
        if (trayAvailable) {
            windowVisible = false
            Tray.notifyMinimized()
        } else {
            exitApp()
        }
    }
}
