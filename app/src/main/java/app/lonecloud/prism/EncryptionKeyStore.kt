package app.lonecloud.prism

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit

class EncryptionKeyStore(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun storeKeys(
        channelId: String,
        privateKey: ByteArray,
        authSecret: ByteArray,
        publicKey: String
    ) {
        sharedPreferences.edit {
            putString(keyFor(channelId, KEY_PRIVATE), base64Encode(privateKey))
            putString(keyFor(channelId, KEY_AUTH), base64Encode(authSecret))
            putString(keyFor(channelId, KEY_PUBLIC), publicKey)
        }
    }

    fun getKeys(channelId: String): Triple<ByteArray, ByteArray, String>? {
        val privateKeyB64 = sharedPreferences.getString(keyFor(channelId, KEY_PRIVATE), null)
        val authSecretB64 = sharedPreferences.getString(keyFor(channelId, KEY_AUTH), null)
        val publicKey = sharedPreferences.getString(keyFor(channelId, KEY_PUBLIC), null)

        return if (privateKeyB64 != null && authSecretB64 != null && publicKey != null) {
            Triple(base64Decode(privateKeyB64), base64Decode(authSecretB64), publicKey)
        } else {
            null
        }
    }

    fun deleteKeys(channelId: String) {
        sharedPreferences.edit {
            remove(keyFor(channelId, KEY_PRIVATE))
            remove(keyFor(channelId, KEY_AUTH))
            remove(keyFor(channelId, KEY_PUBLIC))
        }
    }

    fun hasKeys(channelId: String): Boolean = sharedPreferences.contains(keyFor(channelId, KEY_PRIVATE)) &&
        sharedPreferences.contains(keyFor(channelId, KEY_AUTH)) &&
        sharedPreferences.contains(keyFor(channelId, KEY_PUBLIC))

    private fun keyFor(channelId: String, suffix: String): String = "$PREF_PREFIX$channelId$suffix"

    private fun base64Encode(data: ByteArray): String = Base64.encodeToString(data, Base64.NO_WRAP)

    private fun base64Decode(data: String): ByteArray = Base64.decode(data, Base64.NO_WRAP)

    companion object {
        private const val PREF_NAME = "WebPushEncryptionKeys"
        private const val PREF_PREFIX = "channel_"
        private const val KEY_PRIVATE = "_private"
        private const val KEY_AUTH = "_auth"
        private const val KEY_PUBLIC = "_public"
    }
}
