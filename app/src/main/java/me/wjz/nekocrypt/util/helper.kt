package me.wjz.nekocrypt.util

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import me.wjz.nekocrypt.NekoCryptApp

// 取反色
fun Color.inverse(): Color {
    return Color(
        red = 1.0f - this.red,
        green = 1.0f - this.green,
        blue = 1.0f - this.blue,
        alpha = this.alpha
    )
}

/**
 * 根据uri查询文件大小。-1表示未知。
 */
fun getFileSize(uri: Uri): Long {
    NekoCryptApp.instance.contentResolver.query(
        uri,
        null,                                       // ④ projection = null → 返回所有列
        null, null, null   // ⑤ selection/args/sortOrder 均不需要
    )?.use {
        if (it.moveToFirst()) {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            return if (it.isNull(sizeIndex)) -1 else it.getLong(sizeIndex)
        }
    }
    return -1
}