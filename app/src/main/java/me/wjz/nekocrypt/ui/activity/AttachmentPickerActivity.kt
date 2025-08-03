package me.wjz.nekocrypt.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.util.ResultRelay

class AttachmentPickerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PICK_TYPE = "pick_type"
        const val TYPE_MEDIA = "media"   // 图+视频
        const val TYPE_FILE = "file"    // 任意文件
    }

    private val mediaPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        lifecycleScope.launch {
            uri?.let { ResultRelay.send(it) }
            finish()
        }
    }

    private val filePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        lifecycleScope.launch {
            uri?.let { ResultRelay.send(it) }
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (intent.getStringExtra(EXTRA_PICK_TYPE)) {
            TYPE_MEDIA -> mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
            TYPE_FILE -> filePicker.launch("*/*")
        }
    }
}