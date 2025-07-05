package me.wjz.nekocrypt.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

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

// --- 猫语短语库 (分类版) ---

    /**
     * 猫娘的内心活动，用括号包裹，显得很可爱。
     */
    private val NEKO_INNER_THOUGHTS = listOf(
        "(今天也要开心喵！)",
        "(想吃小鱼干了...)",
        "(那是什么，好想玩！)",
        "(最喜欢你啦~)",
        "(要抱抱才能好起来...)",
        "(偷偷看你一眼。)",
        "(今天也要努力加密哦！)",
        "(这个bug好难喵...)",
        "(打个哈欠~)"
    )

    /**
     * 可爱的颜文字 (Kaomoji) 列表。
     */
    private val NEKO_KAOMOJI = listOf(
        "ฅ●ω●ฅ",
        "(>^ω^<)",
        "(=´ω`=)",
        "(/ω＼)",
        "(´・ω・`)",
        "(Ф∀Ф)",
        "(๑•̀ㅂ•́)و✧"
    )

    /**
     * 纯粹的猫咪叫声和拟声词。
     */
    private val NEKO_SOUNDS = listOf(
        "喵~",
        "喵呜！",
        "嗷呜！",
        "嗷呜~",
        "咪~",
        "喵！",
        "喵？",
        "呼噜噜...",
        "咕噜咕噜~",
        "蹭蹭~",
        "喵喵~"
    )

    /**
     * 将所有类型的猫语列表聚合到一个主列表中，方便随机选择类型。
     */
    private val ALL_NEKO_PHRASE_TYPES = listOf(
        NEKO_INNER_THOUGHTS,
        NEKO_KAOMOJI,
        NEKO_SOUNDS
    )


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
     * 为字符串追加一段结构化、有规则的可爱“猫咪话语”后缀。
     *
     * 生成规则：
     * 1. 必须包含固定数量的【猫咪叫声】。
     * 2. 随机包含 0-2 个不重复的【内心活动】，并保证它们不会出现在开头，且被【猫咪叫声】隔开。
     * 3. 随机决定是否在末尾追加一个【颜文字】。
     *
     * @receiver 调用此函数的原始字符串。
     * @return 附加了猫咪话语的新字符串。
     */
    fun String.appendNekoTalk(): String {
        // --- 1. 决定本次生成的组件数量 ---
        val soundCount = 2 // 写死包含2个叫声
        val thoughtCount = Random.nextInt(0, 3) // 随机包含 0, 1, 或 2 个内心活动

        // --- 2. 从库中随机挑选出本次要使用的具体短语 ---
        val soundsToUse = (1..soundCount).map { NEKO_SOUNDS.random() }.toMutableList()
        val thoughtsToUse = NEKO_INNER_THOUGHTS.shuffled().take(thoughtCount)


        // --- 3. 核心逻辑：将【内心活动】插入到【叫声】的间隙中 ---
        // 【关键修改】为了避免内心活动出现在开头，插入点从 1 开始 (即第一个叫声之后)。
        // 这样就保证了第一个元素永远是“叫声”。
        val availableSlots = (1..soundsToUse.size).toMutableList()
        availableSlots.shuffle()

        thoughtsToUse.forEach { thought ->
            if (availableSlots.isNotEmpty()) {
                val insertionIndex = availableSlots.removeAt(0)
                soundsToUse.add(insertionIndex, thought)
            }
        }
        val middleParts = soundsToUse


        // --- 4. 构建最终的后缀字符串 ---
        val nekoTalkSuffix = buildString {
            middleParts.forEach { part ->
                append(part)
            }

            // 【关键修改】随机决定是否在末尾追加颜文字 (这里设置为 60% 概率)。
            if (Random.nextInt(1, 11) <= 6) {
                val kaomojiToEnd = NEKO_KAOMOJI.random()
                append(kaomojiToEnd)
            }
        }

        return this + nekoTalkSuffix
    }

    /**
     * 加密一个消息，使用给定的密钥，返回的直接是隐写字符串
     */
    fun encrypt(message: String, key: String): String {
        //先把明文字符串转成字节数组
        val plaintextBytes = message.toByteArray()
        //生成随机的iv
        val iv = ByteArray(IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)
        //创建加密器
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)//生成GCM所需数据
        cipher.init(Cipher.ENCRYPT_MODE, deriveKeyFromString(key), parameterSpec)
        val ciphertextBytes = cipher.doFinal(plaintextBytes)
        //拼接iv和密文
        val combinedBytes = iv + ciphertextBytes
        val hexCiphertext = bytesToHex(combinedBytes)
        //5.转为隐写字符串返回
        return hexToStealth(hexCiphertext)
    }

    //消息解密，智能地从含密文的混合字符串中解密
    fun decrypt(stealthCiphertext: String, key: String): String? { // 返回值改为可空的 String?
        try {
            val hexCiphertext = stealthToHex(stealthCiphertext)
            // 如果十六进制字符串为空（例如输入了不含任何隐写字符的普通文本），则直接返回null
            if (hexCiphertext.isEmpty()) return null

            val combinedBytes = hexToBytes(hexCiphertext)
            val iv = combinedBytes.copyOfRange(0, IV_LENGTH_BYTES)
            val ciphertextBytes = combinedBytes.copyOfRange(IV_LENGTH_BYTES, combinedBytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, deriveKeyFromString(key), parameterSpec)

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

    private fun deriveKeyFromString(keyString: String): SecretKey {
        val digest = MessageDigest.getInstance("SHA-256")
        val keyBytes = digest.digest(keyString.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(keyBytes, ALGORITHM)
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