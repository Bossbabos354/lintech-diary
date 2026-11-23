package ru.school28.diary

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.JsonParser
import java.awt.Desktop
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    /** Страница релиза на GitHub — оттуда качаются deb/rpm/msi. */
    val url: String,
    val notes: String,
)

/**
 * Проверка обновлений десктопной сборки по последнему релизу GitHub.
 * Приложение только уведомляет и открывает страницу релиза — само ничего не ставит.
 */
object UpdateChecker {
    private const val UPDATE_URL =
        "https://api.github.com/repos/Bossbabos354/lintech-diary/releases/latest"

    data class CheckResult(val update: UpdateInfo?, val error: String? = null)

    private fun versionParts(v: String): List<Int> =
        v.removePrefix("v").removePrefix("V")
            .split('.', '-', '+')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    /** Тег новее текущей версии (3.0.1 > 3.0.0, суффиксы игнорируются). */
    private fun isNewer(remote: String, local: String): Boolean {
        val a = versionParts(remote)
        val b = versionParts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "Linteh28Diary-Desktop/${BuildInfo.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val code = conn.responseCode
            if (code == 404) {
                return@withContext CheckResult(null, "На GitHub пока нет релизов")
            }
            if (code != 200) {
                return@withContext CheckResult(null, "GitHub ответил кодом $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JsonParser.parseString(text).asJsonObject

            val tag = json.get("tag_name")?.asString?.trim().orEmpty()
            val versionName = tag.removePrefix("v").removePrefix("V")
            val pageUrl = json.get("html_url")?.asString.orEmpty()
            AppLog.log("update/check: tag=$tag, app=${BuildInfo.VERSION_NAME}, url=$pageUrl")

            if (versionName.isEmpty() || pageUrl.isEmpty()) {
                return@withContext CheckResult(null, "В релизе GitHub нет данных о версии")
            }
            if (!isNewer(versionName, BuildInfo.VERSION_NAME)) {
                return@withContext CheckResult(null)
            }
            CheckResult(
                UpdateInfo(
                    versionName = versionName,
                    url = pageUrl,
                    notes = json.get("body")?.asString.orEmpty(),
                )
            )
        } catch (e: Exception) {
            AppLog.logException("update/check", e)
            CheckResult(null, e.message ?: "Не удалось проверить обновления")
        } finally {
            conn?.disconnect()
        }
    }

    fun open(url: String) {
        try {
            if (url.isBlank()) return
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                AppLog.log("update/open: Desktop API недоступен, url=$url")
            }
        } catch (e: Exception) {
            AppLog.logException("update/open", e)
        }
    }
}
