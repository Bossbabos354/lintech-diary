package ru.school28.diary

import java.util.Properties

/**
 * Версия приложения из единственного источника — version в build.gradle.kts.
 * При сборке Gradle подставляет её в version.properties.
 */
object BuildInfo {
    val VERSION_NAME: String = readVersion()

    private fun readVersion(): String {
        val props = Properties()
        runCatching {
            BuildInfo::class.java.getResourceAsStream("/version.properties")?.use { props.load(it) }
        }
        return props.getProperty("version")?.trim().orEmpty().ifEmpty { "0.0.0" }
    }
}
