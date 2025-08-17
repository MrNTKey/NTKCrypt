package me.wjz.nekocrypt.service.handler

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider.getUriForFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.ui.dialog.FilePreviewDialog
import me.wjz.nekocrypt.util.CryptoDownloader
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCWindowManager
import java.io.File

/**
 * 点击文件or图片按钮后的处理类，负责控制悬浮窗的生命周期，并负责下载，展示等逻辑
 */
class FileActionHandler(private val service: NCAccessibilityService) {
    private val tag ="NCFileActionHandler"
    private var dialogManager: NCWindowManager? = null
    private var downloadProgress by mutableStateOf<Int?>(null)
    private var downloadedFileUri by mutableStateOf<Uri?>(null)

    /**
     * 显示文件预览对话框
     */
    fun show(fileInfo: NCFileProtocol) {
        dismiss() // 先关闭旧的

        // 根据文件信息生成本地缓存的唯一路径
        val targetFile = getCacheFileFor(fileInfo)

        // 检查缓存文件是否完整
        if (targetFile.exists() && targetFile.length() == fileInfo.size) {
            Log.d(tag, "文件已在缓存中找到: ${targetFile.path}")
            // ✨ 如果缓存命中，直接为文件生成安全的Uri
            downloadedFileUri = getUriForFile(targetFile)
            downloadProgress = null
        } else {
            Log.d(tag, "文件未缓存或不完整，准备下载。")
            downloadedFileUri = null // 未缓存，重置状态
            downloadProgress = null
        }
        // 创建视图
        dialogManager = NCWindowManager(
            context = service,
            onDismissRequest = { dialogManager = null },
            anchorRect = null
        ) {
            FilePreviewDialog(
                fileInfo = fileInfo,
                downloadProgress = downloadProgress, // ✨ 将进度状态传递给UI
                downloadedFileUri = downloadedFileUri, // nullable
                onDismissRequest = { dismiss() },
                onDownloadRequest = { info ->
                    startDownload(info)
                },
                onOpenRequest = { uri ->
                    openFile(uri) // ✨ 回调现在直接使用 Uri
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
            val targetFile = getCacheFileFor(fileInfo)
            try{
                downloadProgress = 0
                // download会suspend。
                val result = CryptoDownloader.download(
                    fileInfo = fileInfo,
                    targetFile = targetFile,
                    onProgress = { progress -> downloadProgress = progress }
                )

                if(result.isSuccess){
                    val file = result.getOrThrow()
                    // ✨ 下载成功后，为新文件生成安全的Uri并更新状态
                    downloadedFileUri = getUriForFile(file)
                    Log.d(tag, "文件下载成功，Uri: $downloadedFileUri")
                }else{
                    val error = result.exceptionOrNull()?.message ?: "未知错误"
                    Log.e(tag, "文件下载失败: $error")
                    showToast(service.getString(R.string.dialog_download_file_download_failed, error))
                }

            } finally {
                // ✨ 只有在下载失败时才重置进度
                if (downloadedFileUri == null) {
                    downloadProgress = null
                }
            }
        }
    }

    suspend fun showToast(string: String, duration: Int = Toast.LENGTH_SHORT) {
        withContext(Dispatchers.Main) {
            Toast.makeText(service, string, duration).show()
        }
    }

    private fun getCacheFileFor(fileInfo: NCFileProtocol): File{
        // 总是用外部缓存，方便分享之类的操作
        val baseDir = service.externalCacheDir ?: service.cacheDir
        val downloadDir = File(baseDir,"download").apply { mkdirs() }
        // 用唯一文件名，避免重名之类的
        val fileName="${fileInfo.name}-${fileInfo.url.hashCode()}"
        return File(downloadDir,fileName)
    }

    private fun openFile(uri: Uri){
        service.serviceScope.launch {
            try{
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri,service.contentResolver.getType(uri))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                dismiss() // 选择打开文件的话，就要关闭当前的悬浮窗

            } catch (e: Exception) {
                Log.e(tag, "打开文件失败", e)
                showToast(service.getString(R.string.cannot_open_file))
            }
        }
    }

    private fun getUriForFile(file: File):Uri{
        return getUriForFile(service,
            "${service.packageName}.provider",  // 这个地方的authority一定要和manifest里面配置的一样
            file)
    }
}

