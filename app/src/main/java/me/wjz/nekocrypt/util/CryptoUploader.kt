package me.wjz.nekocrypt.util

import android.util.Base64
import okhttp3.OkHttpClient

/**
 * 封装图片、视频和文件的加密上传逻辑
 */
object CryptoUploader {
    // 这个接口只支持20M内的图片 & mp4
    private const val UPLOAD_URL =
        "https://chatbot.weixin.qq.com/weixinh5/webapp/pfnYYEumBeFN7Yb3TAxwrabYVOa4R9/cos/upload"

    // 从你的JS代码转换过来的1像素GIF的ByteArray
    private val SINGLE_PIXEL_GIF_BUFFER: ByteArray =
        Base64.decode("R0lGODdhAQABAIABAP///wAAACwAAAAAAQABAAACAkQBADs=", Base64.DEFAULT)

    private val client = OkHttpClient()

    /**
     * 上传文件的主函数
     */
    fun upload(
        fileBytes: ByteArray,
        fileName: String,
        encryptionKey: String,
        onProcess: (progress: Int) -> Unit,
        onComplete: (String?) -> Unit,
    ) {

    }
}