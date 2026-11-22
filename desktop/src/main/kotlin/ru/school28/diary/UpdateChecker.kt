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
    val url: String,
    val notes: String,
)

/**
 * Проверка обновлений десктопной сборки.
 * Ожидаемый формат файла: { "versionName": "1.1.0", "url": "https://…/lintech28.deb", "notes": "…" }
 * Пока файла нет, сервер отвечает 404 — это нормально для первой версии.
 */
object UpdateChecker {
    private const val UPDATE_URL = "http://d91950hv.beget.tech/desktop/update.json"

    data class CheckResult(val update: UpdateInfo?, val error: String? = null)

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(UPDATE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("User-Agent", "Linteh28Diary-Desktop/${BuildInfo.VERSION_NAME}")
            }
            val code = conn.responseCode
            if (code == 404) {
                return@withContext CheckResult(null, "Канал обновлений для десктопа ещё не настроен")
            }
            if (code != 200) {
                return@withContext CheckResult(null, "Сервер ответил кодом $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JsonParser.parseString(text).asJsonObject
            val name = json.get("versionName")?.asString?.trim().orEmpty()
            if (name.isEmpty() || name.equals(BuildInfo.VERSION_NAME, ignoreCase = true)) {
                return@withContext CheckResult(null)
            }
            CheckResult(
                UpdateInfo(
                    versionName = name,
                    url = json.get("url")?.asString.orEmpty(),
                    notes = json.get("notes")?.asString.orEmpty(),
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
