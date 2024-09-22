package com.minseok.tel

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

object EncryptionUtil {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"

    // 고정된 키 (예: 32바이트)
    private val fixedKey = "12345678901234567890123456789012".toByteArray(Charsets.UTF_8)

    // SecretKey 생성
    private val secretKey: SecretKey = SecretKeySpec(fixedKey, ALGORITHM)

    fun encrypt(data: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
    }

    fun decrypt(encryptedData: String, key: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key)
        val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
