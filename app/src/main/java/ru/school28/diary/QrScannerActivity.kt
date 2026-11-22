package ru.school28.diary

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Сканер QR-кодов на базе ZXing.
 * Если код — URL, открывает его во встроенном WebView-экране.
 * Если простой текст — показывает в тосте.
 */
class QrScannerActivity : ComponentActivity() {

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result != null && result.contents.isNotBlank()) {
            val content = result.contents.trim()
            if (content.startsWith("http://") || content.startsWith("https://")) {
                startActivity(WebViewActivity.intent(this, content))
            } else {
                Toast.makeText(
                    this,
                    "Считано: $content",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(this, "Сканирование отменено", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = ScanOptions()
            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            .setPrompt("Наведите камеру на QR-код")
            .setBeepEnabled(true)
            .setBarcodeImageEnabled(false)
        scanLauncher.launch(options)
    }
}