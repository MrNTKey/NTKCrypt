package me.wjz.nekocrypt.service.handler

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.ui.dialog.FilePreviewDialog
import me.wjz.nekocrypt.util.FileDownloader
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCWindowManager

/**
 * 点击文件or图片按钮后的处理类，负责控制悬浮窗的生命周期，并负责下载，展示等逻辑
 */
class FileActionHandler(private val service: NCAccessibilityService) {
    private val tag ="NCFileActionHandler"
    private var dialogManager: NCWindowManager? = null

    private var downloadProgress by mutableStateOf<Int?>(null)

    /**
     * 显示文件预览对话框
     */
    fun show(fileInfo: NCFileProtocol) {
        dismiss() // 先关闭旧的
        downloadProgress = null // 重置进度状态

        dialogManager = NCWindowManager(
            context = service,
            onDismissRequest = { dialogManager = null },
            anchorRect = null
        ) {
            FilePreviewDialog(
                fileInfo = fileInfo,
                downloadProgress = downloadProgress, // ✨ 将进度状态传递给UI
                onDismissRequest = { dismiss() },
                onDownloadRequest = { info ->
                    startDownload(info)
                }
            )
        }
        dialogManager?.show()
    }

    /**
     * 关闭对话框
     */
    fun dismiss() {
        dialogManager?.dismiss()
        dialogManager = null
    }

    /**
     * 启动文件下载
     */
    private fun startDownload(fileInfo: NCFileProtocol) {
        if(downloadProgress != null)  return // 保证健壮性，防止重复点击

        service.serviceScope.launch {
            try{
                downloadProgress =0

                val result = FileDownloader.download(
                    url = fileInfo.url,
                    fileName = fileInfo.name,
                    onProgress = { progress ->
                        // ✨ 在下载过程中，持续更新进度状态
                        downloadProgress = progress
                    }
                )

                if(result.isSuccess){
                    val downloadedFile = result.getOrThrow()
                    Log.d(tag, "文件下载成功: ${downloadedFile.absolutePath}")
                }else{
                    val error = result.exceptionOrNull()?.message ?: "未知错误"
                    Log.e(tag, "文件下载失败: $error")
                    showToast(service.getString(R.string.dialog_download_file_download_failed, error))
                }

            } finally {
                downloadProgress = null // 无论如何恢复状态
            }
        }
    }

    suspend fun showToast(string: String, duration: Int = Toast.LENGTH_SHORT) {
        withContext(Dispatchers.Main) {
            Toast.makeText(service, string, duration).show()
        }
    }
}

