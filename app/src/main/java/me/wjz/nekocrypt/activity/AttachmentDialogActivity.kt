package me.wjz.nekocrypt.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.ui.dialog.SendAttachmentDialog
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.ResultRelay

class AttachmentDialogActivity: ComponentActivity() {
    companion object {
        // ✨ 我们用这个Key来把最终的URL结果传回去
        const val EXTRA_RESULT_URL = "result_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            NekoCryptTheme(darkTheme = false) {
                // ✨ 在Activity的上下文里，我们可以安全地使用所有Compose功能
                SendAttachmentDialogHost()
            }
        }
    }

    @Composable
    private fun SendAttachmentDialogHost() {
        // ✨ 1. “猫咪信使”现在活在了它们应该在的地方——Activity里！
        val photoPickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri: Uri? ->
                if (uri != null) {
                    // 2. 拿到结果后，我们直接通过Activity的result机制返回
                    // 这里我们用一个模拟的URL
                    finishWithResult("https://neko.crypt/media_${uri.lastPathSegment}")
                }
            }
        )

        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri: Uri? ->
                if (uri != null) {
                    finishWithResult("https://neko.crypt/file_${uri.lastPathSegment}")
                }
            }
        )

        // ✨ 3. 调用我们的纯UI组件，并把“派遣信使”的命令传给它
        SendAttachmentDialog(
            onDismissRequest = { finish() }, // 用户点取消，直接关闭Activity
            onSendRequest = { url ->
                // 用户点发送，也通过result机制返回
                finishWithResult(url)
            },
            onPickMedia = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onPickFile = {
                filePickerLauncher.launch("*/*")
            }
        )
    }

    private fun finishWithResult(url: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_RESULT_URL, url)
        }
        setResult(RESULT_OK, resultIntent)
        lifecycleScope.launch {
            ResultRelay.send(url)
        }

        finish()
    }
}