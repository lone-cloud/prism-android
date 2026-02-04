package app.lonecloud.prism.utils

import com.google.crypto.tink.subtle.EllipticCurves
import com.google.crypto.tink.subtle.Hkdf
import java.nio.ByteBuffer
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * WebPush message decryption (RFC 8291 - aes128gcm).
 * Uses Tink for all cryptographic primitives (ECDH, HKDF, AES-GCM).
 */
object WebPushDecryptor {

    private const val SALT_SIZE = 16
    private const val RECORD_SIZE_LEN = 4
    private const val PUBLIC_KEY_SIZE_LEN = 1
    private const val PUBLIC_KEY_SIZE = 65
    private const val CONTENT_CODING_HEADER_SIZE = SALT_SIZE + RECORD_SIZE_LEN + PUBLIC_KEY_SIZE_LEN + PUBLIC_KEY_SIZE
    private const val TAG_SIZE = 16
    private const val CIPHERTEXT_OVERHEAD = CONTENT_CODING_HEADER_SIZE + 1 + TAG_SIZE
    private const val AUTH_SECRET_SIZE = 16

    /**
     * Decrypt a WebPush message.
     *
     * @param ciphertext The encrypted message
     * @param privateKeyBytes The recipient's private key (PKCS8 encoded)
     * @param publicKeyBytes The recipient's public key (uncompressed P-256 point, 65 bytes)
     * @param authSecret The auth secret (16 bytes)
     * @return Decrypted plaintext or null on failure
     */
    fun decrypt(
        ciphertext: ByteArray,
        privateKeyBytes: ByteArray,
        publicKeyBytes: ByteArray,
        authSecret: ByteArray
    ): ByteArray? {
        try {
            if (ciphertext.size < CIPHERTEXT_OVERHEAD) return null
            if (publicKeyBytes.size != PUBLIC_KEY_SIZE) return null
            if (authSecret.size != AUTH_SECRET_SIZE) return null

            val record = ByteBuffer.wrap(ciphertext)
            val salt = ByteArray(SALT_SIZE)
            record.get(salt)

            val recordSize = record.int
            val publicKeySize = record.get().toInt()
            if (publicKeySize != PUBLIC_KEY_SIZE) return null

            val ephemeralPublicKeyBytes = ByteArray(PUBLIC_KEY_SIZE)
            record.get(ephemeralPublicKeyBytes)

            val payload = ByteArray(ciphertext.size - CONTENT_CODING_HEADER_SIZE)
            record.get(payload)

            val keyFactory = KeyFactory.getInstance("EC")
            val privateKeySpec = PKCS8EncodedKeySpec(privateKeyBytes)
            val privateKey = keyFactory.generatePrivate(privateKeySpec) as ECPrivateKey

            val ephemeralPublicPoint = EllipticCurves.pointDecode(
                EllipticCurves.CurveType.NIST_P256,
                EllipticCurves.PointFormatType.UNCOMPRESSED,
                ephemeralPublicKeyBytes
            )

            val ecdhSecret = EllipticCurves.computeSharedSecret(privateKey, ephemeralPublicPoint)

            val ikm = computeIkm(ecdhSecret, authSecret, publicKeyBytes, ephemeralPublicKeyBytes)
            val cek = computeCek(ikm, salt)
            val nonce = computeNonce(ikm, salt)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val params = GCMParameterSpec(8 * TAG_SIZE, nonce)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(cek, "AES"), params)
            val plaintext = cipher.doFinal(payload)

            var index = plaintext.size - 1
            while (index > 0 && plaintext[index] == 0.toByte()) {
                index--
            }

            if (plaintext[index] != 0x02.toByte()) return null

            return plaintext.copyOf(index)
        } catch (e: Exception) {
            return null
        }
    }

    private fun computeIkm(
        ecdhSecret: ByteArray,
        authSecret: ByteArray,
        uaPublic: ByteArray,
        asPublic: ByteArray
    ): ByteArray {
        val ikmInfo = "WebPush: info\u0000".toByteArray(Charsets.UTF_8)
        val keyInfo = ikmInfo + uaPublic + asPublic
        return Hkdf.computeHkdf("HMACSHA256", ecdhSecret, authSecret, keyInfo, 32)
    }

    private fun computeCek(ikm: ByteArray, salt: ByteArray): ByteArray {
        val cekInfo = "Content-Encoding: aes128gcm\u0000".toByteArray(Charsets.UTF_8)
        return Hkdf.computeHkdf("HMACSHA256", ikm, salt, cekInfo, 16)
    }

    private fun computeNonce(ikm: ByteArray, salt: ByteArray): ByteArray {
        val nonceInfo = "Content-Encoding: nonce\u0000".toByteArray(Charsets.UTF_8)
        return Hkdf.computeHkdf("HMACSHA256", ikm, salt, nonceInfo, 12)
    }
}
