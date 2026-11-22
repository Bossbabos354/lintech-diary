package ru.school28.diary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

    private fun toHttpIfHttps(url: String): String {
        return if (url.startsWith("https://", ignoreCase = true)) {
            "http://" + url.substring(8)
        } else {
            url
        }
    }

    private fun openConnection(urlStr: String): HttpURLConnection {
        val conn = URL(toHttpIfHttps(urlStr)).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 LintTech28-Diary/1.2")
        conn.setRequestProperty("Accept", "*/*")
        return conn
    }

    suspend fun check(): CheckResult = withContext(Dispatchers.IO) {
        for (attempt in 1..3) {
            var conn: HttpURLConnection? = null
            try {
                conn = openConnection(BuildConfig.UPDATE_URL)
                conn.requestMethod = "GET"
                val code = conn.responseCode
                if (code == 403) {
                    Log.w(TAG, "HTTP 403 (UA заблокирован)")
                    return@withContext CheckResult(null, "Сервер вернул 403 Forbidden")
                }
                if (code != 200) {
                    Log.w(TAG, "HTTP $code")
                    if (attempt < 3) { Thread.sleep(1500L); continue }
                    return@withContext CheckResult(null, "Сервер ответил кодом $code")
                }
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(text)

                val serverCode = json.optInt("versionCode", -1)
                val serverName = json.optString("versionName", "")
                FileLog.log("checkUpdate: server code=$serverCode name=$serverName, app code=${BuildConfig.VERSION_CODE} name=${BuildConfig.VERSION_NAME}")

                val appName = BuildConfig.VERSION_NAME.trim()
                val serverNameTrim = serverName.trim()
                val isNewer = serverNameTrim.isNotEmpty() && !serverNameTrim.equals(appName, ignoreCase = true)
                if (!isNewer) {
                    return@withContext CheckResult(null)
                }

                return@withContext CheckResult(
                    UpdateInfo(
                        versionCode = serverCode,
                        versionName = json.optString("versionName", ""),
                        apkUrl = json.optString("apkUrl", ""),
                        notes = json.optString("notes", "")
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "attempt $attempt failed: ${e.message}")
                FileLog.logException("checkUpdate try$attempt", e)
                if (attempt < 3) { Thread.sleep(1500L); continue }
                return@withContext CheckResult(null, e.message ?: "Network error")
            } finally {
                conn?.disconnect()
            }
        }
        CheckResult(null, "Неизвестная ошибка")
    }

    suspend fun download(url: String, context: Context): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "update.apk")
        val conn = openConnection(url)
        conn.requestMethod = "GET"
        conn.connect()
        if (conn.responseCode != 200) {
            throw RuntimeException("HTTP ${conn.responseCode}")
        }
        val input = conn.inputStream
        val output = FileOutputStream(file)
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
        }
        output.flush()
        output.close()
        input.close()
        file
    }

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