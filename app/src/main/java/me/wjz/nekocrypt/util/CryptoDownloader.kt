package me.wjz.nekocrypt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 专门用于下载并解密文件的工具类
 */
object CryptoDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()


    suspend fun download(
        fileInfo: NCFileProtocol,
        targetFile: File, // ✨ 接收一个目标文件
        onProgress: (Int) -> Unit,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = fileInfo.url
                val encryptionKey = fileInfo.encryptionKey
                val totalOriginalSize = fileInfo.size

                // 1. ✨ 使用 OkHttp 创建一个网络请求
                val request = Request.Builder().url(url).build()

                // 2. ✨ 执行请求并获取响应
                val response = client.newCall(request).execute()

                // 3. ✨ 检查响应是否成功，并确保响应体不为空
                if (!response.isSuccessful) throw IOException("下载失败，响应码: ${response.code}")
                val body = response.body

                // 使用我们优化后的 ProgressInputStream
                val progressInputStream =
                    ProgressInputStream(body.byteStream(), body.contentLength())

                // 4. ✨ 在 use 块之前设置好进度监听
                progressInputStream.progressListener = { bytesRead, _ ->
                    // 进度基于原始文件大小计算，对用户更友好
                    val estimatedTotalRead =
                        (bytesRead - CryptoUploader.SINGLE_PIXEL_GIF_BUFFER.size).coerceAtLeast(0)
                    val progress = (estimatedTotalRead * 100 / totalOriginalSize).toInt()
                    onProgress(progress.coerceIn(0, 100))
                }

                // 5. ✨ 使用 try-finally 确保响应体总是被关闭
                try {
                    progressInputStream.use { networkStream ->
                        // 使用循环确保跳过完整的GIF头
                        val skipSize = CryptoUploader.SINGLE_PIXEL_GIF_BUFFER.size.toLong()
                        var skipped = 0L
                        while (skipped < skipSize) {
                            val n = networkStream.skip(skipSize - skipped)
                            if (n <= 0) throw IOException("无法跳过GIF头部，文件可能已损坏。")
                            skipped += n
                        }

                        targetFile.outputStream().use { outputStream ->
                            CryptoManager.decryptStream(networkStream, outputStream, encryptionKey)
                        }
                    }
                } finally {
                    response.close() // 确保响应资源被释放
                }

                targetFile
            }
        }
}

/**
 * 用来追踪 InputStream读取进度的辅助类
 * 它通过包裹一个现有的 InputStream，来监听数据的读取过程。
 */
class ProgressInputStream(
    inStream: InputStream,
    private val totalBytes: Long, // 文件的总大小
) : FilterInputStream(inStream) {

    private var bytesRead: Long = 0 // 已经读取的字节数
    var progressListener: ((bytesRead: Long, totalBytes: Long) -> Unit)? = null

    /**
     * 只重写这一个 read 方法就足够了。
     * 因为单字节的 read() 在内部会自动调用这个方法，
     * 这样可以避免重复计算进度。
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read > 0) {
            bytesRead += read
            // 安全地调用监听器，报告当前的进度
            progressListener?.invoke(bytesRead, totalBytes)
        }
        return read
    }
}