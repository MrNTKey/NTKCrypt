package me.wjz.nekocrypt.util

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class FilePickerActivity : ComponentActivity() {
    companion object {
        const val KEY_PICK_TYPE = "pick_type"
        const val TYPE_MEDIA = "media"
        const val TYPE_FILE = "file"
    }

    private lateinit var photoPickerLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val onResult = { uri: Uri? ->
            if (uri != null) {
                // 当拿到结果时，通过“内部对讲机”发送回去
                lifecycleScope.launch {
                    ResultRelay.send(uri)
                }
            }
            // 无论是否有结果，完成任务后都立刻关闭自己
            finish()
        }

        // ✨ 核心修正 3：在 onCreate 中“训练”好我们的两个信使
        photoPickerLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia(),
            onResult)
        filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent(),
            onResult)

        // ✨ 核心修正 4：根据启动类型，派遣正确的信使
        val pickType = intent.getStringExtra(KEY_PICK_TYPE)
        if (pickType == TYPE_MEDIA) {
            photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        } else {
            filePickerLauncher.launch("*/*")
        }
    }
}