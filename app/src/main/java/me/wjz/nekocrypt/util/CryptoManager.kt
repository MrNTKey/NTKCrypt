package me.wjz.nekocrypt.util

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 加密工具类，有相关的加密算法。
 */
object CryptoManager {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_SIZE_BITS = 256 // AES-256
    private const val IV_LENGTH_BYTES = 12  // GCM 推荐的IV长度
    private const val TAG_LENGTH_BITS = 128 // GCM 推荐的认证标签长度

    // --- 隐写编解码所需的常量和映射表 ---
    private const val HEX_ALPHABET = "0123456789abcdef"
    private val STEALTH_ALPHABET = (0xFE00..0xFE0F).map { it.toChar() }.joinToString("")

    // 预先生成映射表，这是最高效的方式。
    // 查询Map的复杂度是O(1)，而每次都用indexOf查询字符串是O(N)。
    private val HEX_TO_STEALTH_MAP = HEX_ALPHABET.zip(STEALTH_ALPHABET).toMap()
    private val STEALTH_TO_HEX_MAP = STEALTH_ALPHABET.zip(HEX_ALPHABET).toMap()


    /**
     * 生成一个符合 AES-256 要求的随机密钥。
     *
     * @return 一个 SecretKey 对象，包含了256位的密钥数据。
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE_BITS)
        return keyGenerator.generateKey()
    }

    /**
     * 将 SecretKey 对象转换为十六进制编码的字符串，方便存储。
     *
     * @param key 要转换的 SecretKey。
     * @return 十六进制编码的密钥字符串。
     */
    fun keyToHex(key: SecretKey): String {
        return bytesToHex(key.encoded)
    }

    fun hexToKey(hexKey: String): SecretKey {
        val decodedKey = hexToBytes(hexKey)
        return SecretKeySpec(decodedKey, 0, decodedKey.size, ALGORITHM)
    }

    /**
     * 加密一个消息，使用给定的密钥，返回的直接是隐写字符串
     */
    fun encrypt(message: String, key: SecretKey): String {
        //先把明文字符串转成字节数组
        val plaintextBytes = message.toByteArray()
        //生成随机的iv
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        //创建加密器
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)//生成GCM所需数据
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)
        //拼接iv和密文
        val combinedBytes = iv + ciphertextBytes
        val hexCiphertext = bytesToHex(combinedBytes)
        //5.转为隐写字符串返回
        return hexToStealth(hexCiphertext)
    }

    //消息解密，智能地从含密文的混合字符串中解密
    fun decrypt(stealthCiphertext: String, key: SecretKey): String? { // 返回值改为可空的 String?
        try {
            val hexCiphertext = stealthToHex(stealthCiphertext)
            // 如果十六进制字符串为空（例如输入了不含任何隐写字符的普通文本），则直接返回null
            if (hexCiphertext.isEmpty()) return null

            val combinedBytes = hexToBytes(hexCiphertext)
            val iv = combinedBytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertextBytes = combinedBytes.copyOfRange(IV_LENGTH_BYTES, combinedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

            // 认证校验在这里隐式发生！
            // 如果密文或IV被篡改，或者密钥错误，doFinal会抛出 AEADBadTagException。
            // 这就是GCM模式的认证功能。
            val decryptedBytes = cipher.doFinal(ciphertextBytes)
            return String(decryptedBytes)
        } catch (e: AEADBadTagException) {
            // 明确捕获认证标签错误的异常。
            // 这意味着数据100%被篡改过，或者使用的密钥是错误的。
            // 在这种情况下，我们不应该让程序崩溃，而是应该返回一个null来表示解密失败。
            println("解密失败：数据认证失败，可能已被篡改或密钥错误。\n" + e.message)
            return null
        } catch (e: Exception) {
            // 捕获其他可能的异常，例如格式错误等。
            println("解密时发生未知错误: ${e.message}")
            return null
        }
    }

    /**
     * 判断给定字符串是否包含密文
     */
    fun containsCiphertext(input: String): Boolean {
        return input.any { STEALTH_TO_HEX_MAP.containsKey(it) }
    }

    // -----------------一些辅助方法---------------------

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hexToBytes(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "十六进制字符串长度必须为偶数" }
        return hex.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }

    /**
     * hexString转为隐写字符串。
     */
    private fun hexToStealth(hexString: String): String {
        // 1. `map` 会遍历字符串中的每一个字符。
        // 2. `HEX_TO_STEALTH_MAP[it]` 会在映射表中查找对应的隐写字符。
        // 3. `joinToString("")` 将所有转换后的字符连接成一个新字符串。
        // 这种写法不仅简洁，而且意图清晰：将每个字符进行映射，然后连接。
        return hexString.lowercase().map { HEX_TO_STEALTH_MAP[it] }.joinToString("")
    }

    /**
     * 将“隐写”字符串解码回原始的十六进制字符串 (函数式实现)。
     */
    private fun stealthToHex(stealthString: String): String {
        // 逻辑同上，只是使用了反向的映射表。
        //这里必须用mapNotNull，否则如果匹配不上return的是null，会被.joinToString("")变为"null"字符串。
        return stealthString
            .mapNotNull { STEALTH_TO_HEX_MAP[it] }
            .joinToString("")
    }
}