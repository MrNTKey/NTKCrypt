package me.wjz.nekocrypt.util

import android.util.Base64
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException

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
        val encryptedBytes = CryptoManager.encrypt(fileBytes, encryptionKey)
        // 加密后的文件藏在后面
        val payload = SINGLE_PIXEL_GIF_BUFFER + encryptedBytes
        // 创建一个我们的requestBody
        val requestBody = ProcessRequestBody(payload, "image/gif".toMediaTypeOrNull(), onProcess)
        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("media", "encrypted.gif", requestBody).build()
        val request = Request.Builder().url(UPLOAD_URL).post(multipartBody).build()
        // 发送我们的请求
        client.newCall(request).enqueue(object : Callback{
            override fun onFailure(call: Call, e: IOException) {
                onComplete(null)
            }
            override fun onResponse(call: Call, response: Response) {
                response.use { onComplete(it.body.string()) }
            }
        })
    }
}

private class ProcessRequestBody(
    private val data: ByteArray,
    private val contentType: MediaType?,
    private val onProcess: (Int) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType
    override fun contentLength(): Long = data.size.toLong()
    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var bytesWritten = 0L
        val bufferSize = 4 * 1024 // 每次读取4KB
        // 用use方法，可以自动关闭，类似python open with
        data.inputStream().use { inputStream ->
            val buffer = ByteArray(bufferSize) // 创建临时缓冲区
            var read: Int
            /**
             * 逻辑：read放入一个数组，stream流会自动更新放入的这个buffer，接着，它的返回值是本次读取到的值，刚开始会是buffer的
             * 大小，我们这里是4096，读取完了再读取，就会返回-1，刚好结束循环。
             */
            while (inputStream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                bytesWritten += read
                val progress = (100 * bytesWritten / totalBytes).toInt()
                onProcess(progress)
            }
        }
    }
}