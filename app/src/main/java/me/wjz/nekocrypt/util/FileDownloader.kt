package me.wjz.nekocrypt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * 专门用于下载文件的工具类
 */
object FileDownloader {
    suspend fun download(url: String,
                         targetFile: File, // ✨ 接收一个目标文件
                         onProgress: (Int) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 统一放到外部。
                val connection = URL(url).openConnection().apply { connect() }
                // 拿文件总大小
                val totalFileSize = connection.contentLength.toLong()
                var totalBytesRead = 0

                connection.getInputStream().use { inputStream ->
                    targetFile.outputStream().use { outputStream ->
                        val buffer = ByteArray(8 * 1024) // 8KB 缓冲区
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (totalFileSize > 0) {
                                // 计算进度百分比并调用回调
                                val progress = (totalBytesRead * 100 / totalFileSize).toInt()
                                onProgress(progress)
                            }
                        }
                    }
                }
                targetFile
            }
        }
}