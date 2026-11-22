package ru.school28.diary.api

import ru.school28.diary.AppLog
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Доверие к journal.school28-kirov.ru: системные корни + встроенные ISRG/Let's Encrypt.
 * На JVM сертификаты лежат в resources/certs вместо Android res/raw.
 */
object AppTls {
    private val certificates = listOf(
        "/certs/isrg_root_x1.pem",
        "/certs/isrg_root_x2.pem",
        "/certs/le_root_ye.pem",
        "/certs/le_ye1.pem",
    )

    fun trustManager(): X509TrustManager {
        val system = systemTrustManager()
        val custom = runCatching { customTrustManager() }
            .onFailure { AppLog.logException("tls/custom", it) }
            .getOrNull() ?: return system
        return CompositeTrustManager(system, custom)
    }

    fun socketFactory(trustManager: X509TrustManager): SSLSocketFactory {
        val context = SSLContext.getInstance("TLS")
        context.init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        return context.socketFactory
    }

    private fun systemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private fun customTrustManager(): X509TrustManager {
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
        val certificateFactory = CertificateFactory.getInstance("X.509")
        certificates.forEachIndexed { index, path ->
            AppTls::class.java.getResourceAsStream(path)?.use { input ->
                val certificate = certificateFactory.generateCertificate(input) as X509Certificate
                keyStore.setCertificateEntry("lintech_ca_$index", certificate)
            }
        }
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
    }

    private class CompositeTrustManager(
        private val system: X509TrustManager,
        private val custom: X509TrustManager,
    ) : X509TrustManager {

        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                system.checkClientTrusted(chain, authType)
            } catch (first: CertificateException) {
                try {
                    custom.checkClientTrusted(chain, authType)
                } catch (second: CertificateException) {
                    second.addSuppressed(first)
                    throw second
                }
            }
        }

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            try {
                system.checkServerTrusted(chain, authType)
            } catch (first: CertificateException) {
                try {
                    custom.checkServerTrusted(chain, authType)
                } catch (second: CertificateException) {
                    second.addSuppressed(first)
                    throw second
                }
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> =
            system.acceptedIssuers + custom.acceptedIssuers
    }
}
