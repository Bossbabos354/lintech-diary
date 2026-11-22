package ru.school28.diary

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * Встроенный браузер (WebView) для открытия ссылок из QR-кодов и событий.
 * Умеет показывать прогресс загрузки и имеет кнопку "назад".
 */
class WebViewActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "url"

        fun intent(context: Context, url: String): Intent {
            val i = Intent(context, WebViewActivity::class.java)
            i.putExtra(EXTRA_URL, url)
            return i
        }
    }

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        if (url.isEmpty()) {
            finish()
            return
        }

        // Контейнер: LinearLayout с WebView и верхней панелью
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Верхняя панель (кнопка назад)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            background = ColorDrawable(0xFFB39DDB.toInt())
        }
        val backBtn = ImageButton(this).apply {
            background = null
            setOnClickListener { finish() }
        }
        backBtn.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
        backBtn.imageTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
        val title = TextView(this).apply {
            text = "Просмотр"
            textSize = 18f
            setTextColor(Color.WHITE)
        }
        header.addView(backBtn, LinearLayout.LayoutParams(dp(48), dp(48)))
        header.addView(title)
        root.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        // WebView
        val wv = WebView(this)
        wv.settings.javaScriptEnabled = true
        wv.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        wv.settings.cacheMode = WebSettings.LOAD_DEFAULT
        wv.webViewClient = object : WebViewClient() {}
        wv.webChromeClient = WebChromeClient()
        root.addView(wv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        setContentView(root)

        webView = wv
        wv.loadUrl(url)

        // Обработка системной кнопки "назад": сначала навигация в WebView
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val wv = webView
                if (wv != null && wv.canGoBack()) {
                    wv.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()
}