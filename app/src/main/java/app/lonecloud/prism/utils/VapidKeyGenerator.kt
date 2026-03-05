package app.lonecloud.prism.utils

import android.util.Base64
import com.google.crypto.tink.subtle.EllipticCurves
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

/**
 * VAPID key generation for Web Push (RFC 8292).
 * Uses NIST P-256 (secp256r1) elliptic curve.
 */
object VapidKeyGenerator {

    data class VapidKeyPair(val publicKey: String, val privateKey: String)

    fun generateKeyPair(): VapidKeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = keyPairGenerator.generateKeyPair()

        val publicKeyBytes = EllipticCurves.pointEncode(
            EllipticCurves.CurveType.NIST_P256,
            EllipticCurves.PointFormatType.UNCOMPRESSED,
            (keyPair.public as ECPublicKey).w
        )

        val privateKeyBytes = (keyPair.private as java.security.interfaces.ECPrivateKey)
            .s.toByteArray().trimLeadingZeros()
        val privateKeyPadded = ByteArray(32)
        privateKeyBytes.copyInto(privateKeyPadded, 32 - privateKeyBytes.size)

        return VapidKeyPair(
            publicKey = base64UrlEncode(publicKeyBytes),
            privateKey = base64UrlEncode(privateKeyPadded)
        )
    }

    private fun base64UrlEncode(data: ByteArray): String =
        Base64.encodeToString(data, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private fun ByteArray.trimLeadingZeros(): ByteArray {
        var i = 0
        while (i < size && this[i] == 0.toByte()) {
            i++
        }
        return copyOfRange(i, size)
    }
}
