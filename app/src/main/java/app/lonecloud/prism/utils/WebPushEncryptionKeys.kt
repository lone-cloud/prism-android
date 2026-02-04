package app.lonecloud.prism.utils

import android.util.Base64
import com.google.crypto.tink.subtle.EllipticCurves
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

object WebPushEncryptionKeys {

    data class EncryptionKeySet(
        val p256dh: String,
        val auth: String,
        val privateKey: ByteArray,
        val authBytes: ByteArray
    )

    fun generateKeySet(): EncryptionKeySet {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKeyBytes = EllipticCurves.pointEncode(
            EllipticCurves.CurveType.NIST_P256,
            EllipticCurves.PointFormatType.UNCOMPRESSED,
            (keyPair.public as ECPublicKey).w
        )

        val authBytes = ByteArray(16)
        SecureRandom().nextBytes(authBytes)

        return EncryptionKeySet(
            p256dh = base64UrlEncode(publicKeyBytes),
            auth = base64UrlEncode(authBytes),
            privateKey = keyPair.private.encoded,
            authBytes = authBytes
        )
    }

    private fun base64UrlEncode(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
