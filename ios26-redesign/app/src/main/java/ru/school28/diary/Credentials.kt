package ru.school28.diary

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Логин и пароль журнала шифруются ключом Android Keystore: ключ не покидает
 * устройство, поэтому данные бесполезны в бэкапе или при копировании prefs.
 * На Android 5–6 (API 21–22) Keystore AES не умеет — там остаётся открытый текст.
 */
object Credentials {
    private const val ALIAS = "lintech_credentials"
    private const val PREFIX = "enc1:"

    private fun supported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    fun encrypt(value: String): String {
        if (!supported()) return value
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun isEncrypted(value: String) = value.startsWith(PREFIX)

    fun decrypt(value: String): String? = runCatching {
        val payload = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key(),
            GCMParameterSpec(128, payload.copyOfRange(0, 12)),
        )
        String(cipher.doFinal(payload.copyOfRange(12, payload.size)), Charsets.UTF_8)
    }.getOrNull()
}
