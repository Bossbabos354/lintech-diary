package ru.school28.diary

import java.io.File

/**
 * Каталоги приложения для Windows и Linux (XDG).
 * Настройки — в config, кэш данных журнала — в cache, лог и бэкапы — в data.
 */
object AppPaths {
    private val isWindows = System.getProperty("os.name").lowercase().contains("win")
    private val home: File = File(System.getProperty("user.home"))

    private fun env(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }

    private fun dir(envName: String, fallback: File): File =
        File(env(envName) ?: fallback.path)

    val configDir: File by lazy {
        if (isWindows) dir("APPDATA", File(home, "AppData/Roaming")).resolve("Linteh28")
        else dir("XDG_CONFIG_HOME", File(home, ".config")).resolve("lintech28")
    }

    val cacheDir: File by lazy {
        if (isWindows) dir("LOCALAPPDATA", File(home, "AppData/Local")).resolve("Linteh28/cache")
        else dir("XDG_CACHE_HOME", File(home, ".cache")).resolve("lintech28")
    }

    val dataDir: File by lazy {
        if (isWindows) dir("LOCALAPPDATA", File(home, "AppData/Local")).resolve("Linteh28/data")
        else dir("XDG_DATA_HOME", File(home, ".local/share")).resolve("lintech28")
    }

    val logFile: File get() = File(dataDir, "diary.log")
    val exportDir: File get() = File(dataDir, "exports")
    val settingsFile: File get() = File(configDir, "settings.properties")

    fun ensureDirs() {
        configDir.mkdirs()
        cacheDir.mkdirs()
        dataDir.mkdirs()
    }
}
