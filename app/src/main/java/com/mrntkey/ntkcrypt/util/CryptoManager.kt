package com.mrntkey.ntkcrypt.util

import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
// import kotlin.random.Random // Random 可能不再需要，除非你有其他地方用

/**
 * 加密工具类，有相关的加密算法。
 */
object CryptoManager {

    private const val ALGORITHM = "AES"
    const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256 // AES-256
    const val IV_LENGTH_BYTES = 16
    const val TAG_LENGTH_BITS = 128

    private val STEALTH_ALPHABET = (0xFE00..0xFE0F).map { it.toChar() }.joinToString("")
    private val STEALTH_CHAR_TO_INDEX_MAP =
        STEALTH_ALPHABET.withIndex().associate { (index, char) -> char to index }

    // --- 移除所有 NEKO_SOUNDS, NEKO_INNER_THOUGHTS, NEKO_KAOMOJI 的定义 ---

    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE_BITS)
        return keyGenerator.generateKey()
    }

    /**
     * 为字符串附加用户直接输入的“猫语”文本。
     * 可以选择在加密文本的中间、前面或后面附加。
     *
     * @receiver 调用此函数的原始字符串（通常是已加密的文本）。
     * @param nekoTalk 用户在文本框中输入的单个猫语字符串。
     * @return 附加了猫语的新字符串。如果 nekoTalk 为空或 null，则返回原始字符串。
     */
    fun String.appendNTKCryptTalk(nekoTalk: String?): String {
        if (nekoTalk.isNullOrBlank()) {
            return this // 如果用户没有输入猫语，直接返回原始加密文本
        }

        // 简单地将猫语文本拆分，一半放在前面，一半放在后面，加密文本在中间
        // 你可以根据需要调整附加逻辑，例如只加后缀，或只加前缀
        val middleIndex = nekoTalk.length / 2
        val prefix = nekoTalk.substring(0, middleIndex)
        val suffix = nekoTalk.substring(middleIndex)

        return prefix + this + suffix
    }

    // --- 其他加密、解密、baseN 方法保持不变 ---

    fun encrypt(message: String, key: String): String {
        val plaintextBytes = message.toByteArray(Charsets.UTF_8)
        val encryptedBytes = encryptBytes(plaintextBytes, key)
        return baseNEncode(encryptedBytes)
    }

    fun encrypt(data: ByteArray, key: String): ByteArray {
        return encryptBytes(data, key)
    }

    fun decrypt(stealthCiphertext: String, key: String): String? {
        val combinedBytes = baseNDecode(stealthCiphertext)
        val decryptedBytes = decryptBytes(combinedBytes, key)
        return decryptedBytes?.toString(Charsets.UTF_8)
    }

    fun decrypt(data: ByteArray, key: String): ByteArray? {
        return decryptBytes(data, key)
    }

    private fun encryptBytes(plaintextBytes: ByteArray, key: String): ByteArray {
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKeyFromString(key), parameterSpec)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)
        return iv + ciphertextBytes
    }

    private fun decryptBytes(combinedBytes: ByteArray, key: String): ByteArray? {
        try {
            if (combinedBytes.size < IV_LENGTH_BYTES) return null
            val iv = combinedBytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertextBytes = combinedBytes.copyOfRange(IV_LENGTH_BYTES, combinedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, deriveKeyFromString(key), parameterSpec)
            return cipher.doFinal(ciphertextBytes)
        } catch (e: AEADBadTagException) {
            println("解密失败：数据认证失败，可能已被篡改或密钥错误。\n" + e.message)
            return null
        } catch (e: Exception) {
            println("解密时发生未知错误: ${e.message}")
            return null
        }
    }

    fun containsCiphertext(input: String): Boolean {
        return input.any { STEALTH_CHAR_TO_INDEX_MAP.containsKey(it) }
    }

    fun deriveKeyFromString(keyString: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(keyString.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    private fun baseNEncode(data: ByteArray): String {
        if (data.isEmpty()) return ""
        var bigInt = BigInteger(1, data)
        val base = BigInteger.valueOf(STEALTH_ALPHABET.length.toLong())
        val builder = StringBuilder()
        while (bigInt > BigInteger.ZERO) {
            val (quotient, remainder) = bigInt.divideAndRemainder(base)
            bigInt = quotient
            builder.append(STEALTH_ALPHABET[remainder.toInt()])
        }
        return builder.reverse().toString()
    }

    private fun baseNDecode(encodedString: String): ByteArray {
        var bigInt = BigInteger.ZERO
        val base = BigInteger.valueOf(STEALTH_ALPHABET.length.toLong())
        encodedString.forEach { char ->
            val index = STEALTH_CHAR_TO_INDEX_MAP[char]
            if (index != null) {
                bigInt = bigInt.multiply(base).add(BigInteger.valueOf(index.toLong()))
            }
        }
        if (bigInt == BigInteger.ZERO) return ByteArray(0)
        val bytes = bigInt.toByteArray()
        return if (bytes.isNotEmpty() && bytes[0].toInt() == 0) {
            bytes.copyOfRange(1, bytes.size)
        } else {
            bytes
        }
    }

    fun decryptStream(inputStream: InputStream, outputStream: OutputStream, key: String) {
        val iv = ByteArray(IV_LENGTH_BYTES)
        require(inputStream.read(iv) == IV_LENGTH_BYTES) {
            "输入流太短，无法读取IV。"
        }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            val spec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            init(Cipher.DECRYPT_MODE, deriveKeyFromString(key), spec)
        }
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = inputStream.read(buffer)
            if (read == -1) break
            cipher.update(buffer, 0, read)?.let { outputStream.write(it) }
        }
        try {
            cipher.doFinal()?.let { outputStream.write(it) }
        } catch (e: AEADBadTagException) {
            throw SecurityException("解密失败，数据可能被篡改或密钥错误", e)
        }
    }
}

