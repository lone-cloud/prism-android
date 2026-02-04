package app.lonecloud.prism.utils

import android.util.Base64
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
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
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(ecSpec)

        val keyPair = keyPairGenerator.generateKeyPair()
        val publicKey = keyPair.public as ECPublicKey
        val privateKey = keyPair.private as ECPrivateKey

        val xCoord = publicKey.w.affineX.toByteArray().trimLeadingZeros()
        val yCoord = publicKey.w.affineY.toByteArray().trimLeadingZeros()

        val publicKeyBytes = ByteArray(65)
        publicKeyBytes[0] = 0x04
        xCoord.copyInto(publicKeyBytes, 1 + (32 - xCoord.size))
        yCoord.copyInto(publicKeyBytes, 33 + (32 - yCoord.size))

        val privateKeyBytes = privateKey.s.toByteArray().trimLeadingZeros()
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
