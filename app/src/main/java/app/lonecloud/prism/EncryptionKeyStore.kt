package app.lonecloud.prism

import android.content.Context
import android.util.Base64

class EncryptionKeyStore(context: Context) {

    data class EncryptionKeys(
        val privateKey: ByteArray,
        val authSecret: ByteArray,
        val p256dh: String
    )

    private val securePreferences = SecureStringPreferences(
        context = context,
        prefName = PREF_NAME,
        keyAlias = KEY_ALIAS
    )

    fun storeKeys(
        channelId: String,
        privateKey: ByteArray,
        authSecret: ByteArray,
        publicKey: String
    ) {
        securePreferences.putString(keyFor(channelId, KEY_PRIVATE), base64Encode(privateKey))
        securePreferences.putString(keyFor(channelId, KEY_AUTH), base64Encode(authSecret))
        securePreferences.putString(keyFor(channelId, KEY_PUBLIC), publicKey)
    }

    fun getKeys(channelId: String): EncryptionKeys? {
        val privateKeyB64 = securePreferences.getString(keyFor(channelId, KEY_PRIVATE), null)
        val authSecretB64 = securePreferences.getString(keyFor(channelId, KEY_AUTH), null)
        val publicKey = securePreferences.getString(keyFor(channelId, KEY_PUBLIC), null)

        if (privateKeyB64 == null || authSecretB64 == null || publicKey == null) return null

        return try {
            EncryptionKeys(base64Decode(privateKeyB64), base64Decode(authSecretB64), publicKey)
        } catch (_: IllegalArgumentException) {
            deleteKeys(channelId)
            null
        }
    }

    fun deleteKeys(channelId: String) {
        securePreferences.remove(keyFor(channelId, KEY_PRIVATE))
        securePreferences.remove(keyFor(channelId, KEY_AUTH))
        securePreferences.remove(keyFor(channelId, KEY_PUBLIC))
    }

    fun hasKeys(channelId: String): Boolean = securePreferences.contains(keyFor(channelId, KEY_PRIVATE)) &&
        securePreferences.contains(keyFor(channelId, KEY_AUTH)) &&
        securePreferences.contains(keyFor(channelId, KEY_PUBLIC))

    private fun keyFor(channelId: String, suffix: String): String = "$PREF_PREFIX$channelId$suffix"

    private fun base64Encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    private fun base64Decode(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)

    companion object {
        private const val PREF_NAME = "WebPushEncryptionKeys"
        private const val KEY_ALIAS = "prism_webpush_encryption_keys"
        private const val PREF_PREFIX = "channel_"
        private const val KEY_PRIVATE = "_private"
        private const val KEY_AUTH = "_auth"
        private const val KEY_PUBLIC = "_public"
    }
}
