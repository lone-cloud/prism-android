package app.lonecloud.prism

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import java.nio.charset.StandardCharsets
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStringPreferences(
    context: Context,
    private val prefName: String,
    private val keyAlias: String
) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    private val secretKey: SecretKey by lazy { getOrCreateSecretKey(keyAlias) }

    fun putString(key: String, value: String?) {
        if (value == null) {
            remove(key)
            return
        }
        sharedPreferences.edit(commit = true) { putString(key, encrypt(value)) }
    }

    fun getString(key: String, defaultValue: String? = null): String? {
        val encrypted = sharedPreferences.getString(key, null) ?: return defaultValue
        return decrypt(encrypted) ?: defaultValue
    }

    fun remove(key: String) {
        sharedPreferences.edit(commit = true) { remove(key) }
    }

    fun contains(key: String): Boolean = sharedPreferences.contains(key)

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? {
        val payload = try {
            Base64.decode(value, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            return null
        }
        if (payload.size <= IV_SIZE_BYTES) return null

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val encrypted = payload.copyOfRange(IV_SIZE_BYTES, payload.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        return try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_SIZE_BITS, iv))
            val plain = cipher.doFinal(encrypted)
            String(plain, StandardCharsets.UTF_8)
        } catch (_: GeneralSecurityException) {
            null
        }
    }

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keySpec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setKeySize(KEY_SIZE_BITS)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE_BYTES = 12
        private const val GCM_TAG_SIZE_BITS = 128
        private const val KEY_SIZE_BITS = 256
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    }
}
