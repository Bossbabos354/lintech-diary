package ru.school28.diary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val notes: String
)

object UpdateChecker {

    private const val TAG = "UpdateChecker"

    data class CheckResult(
        val update: UpdateInfo?,
        val error: String? = null
    )

    // Временно проверяем обновления в релизах GitHub, пока сайт не работает.
    // Формат ответа — GitHub Releases API (tag_name, assets, body).
    private fun openConnection(urlStr: String): HttpURLConnection {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("User-Agent", "LintTech28-Diary")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        return conn
    }

    private fun versionParts(v: String): List<Int> =
        v.removePrefix("v").removePrefix("V")
            .split('.', '-', '+')
            .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    /** Версия из тега новее текущей (3.0.0 > 2.6.18, суффиксы игнорируются). */
    private fun isNewer(remote: String, local: String): Boolean {
        val a = versionParts(remote)
        val b = versionParts(local)
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
            if (diff != 0) return diff > 0
        }
        return false
    }

    /**
     * Проверяет наличие обновления в последнем релизе GitHub.
     * Возвращает CheckResult: update != null когда есть новая версия,
     * error != null когда GitHub недоступен.
     */
    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        if (BuildConfig.UPDATE_URL.isBlank()) return@withContext CheckResult(null)
        var conn: HttpURLConnection? = null
        try {
            conn = openConnection(BuildConfig.UPDATE_URL)
            conn.requestMethod = "GET"
            val code = conn.responseCode
            if (code != 200) {
                Log.w(TAG, "HTTP $code")
                return@withContext CheckResult(null, "GitHub ответил кодом $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(text)

            val tag = json.optString("tag_name").trim()
            val versionName = tag.removePrefix("v").removePrefix("V")
            val assets = json.optJSONArray("assets")
            val apk = (0 until (assets?.length() ?: 0))
                .map { assets!!.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            val apkUrl = apk?.optString("browser_download_url").orEmpty()
            FileLog.log("checkUpdate: github tag=$tag, app=${BuildConfig.VERSION_NAME}, apk=$apkUrl")

            if (versionName.isEmpty() || apkUrl.isEmpty()) {
                return@withContext CheckResult(null, "В релизе GitHub нет APK")
            }
            if (!isNewer(versionName, BuildConfig.VERSION_NAME)) {
                return@withContext CheckResult(null)
            }
            CheckResult(
                UpdateInfo(
                    versionCode = -1,
                    versionName = versionName,
                    apkUrl = apkUrl,
                    notes = json.optString("body", "")
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "github check failed: ${e.message}")
            FileLog.logException("checkUpdate", e)
            CheckResult(null, e.message ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }

    /**
     * Скачивает APK во внутреннее хранилище.
     * @return путь к файлу
     */
    suspend fun download(url: String, context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "update.apk")
        val conn = openConnection(url)
        try {
            conn.requestMethod = "GET"
            conn.connect()
            if (conn.responseCode != 200) {
                throw RuntimeException("HTTP ${conn.responseCode}")
            }
            conn.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
        } finally {
            conn.disconnect()
        }
        file
    }

    /**
     * Инициирует установку скачанного APK.
     */
    fun install(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}