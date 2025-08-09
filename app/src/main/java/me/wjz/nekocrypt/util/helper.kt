package me.wjz.nekocrypt.util

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.Color
import me.wjz.nekocrypt.NekoCryptApp
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

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

/**
 * 获取文件名
 */
fun getFileName(uri: Uri): String {
    var fileName = "unknown"
    NekoCryptApp.instance.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayNameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameColumn != -1) { // 检查 DISPLAY_NAME 列是否存在
                fileName = cursor.getString(displayNameColumn)
            }
        }
    }
    return fileName
}

/**
 * 检查给定的包名是否属于系统应用。
 * @param packageName 需要检查的应用包名。
 * @return 如果是系统应用或核心应用，则返回 true，否则返回 false。
 */
fun isSystemApp(packageName: String?): Boolean {
    if (packageName.isNullOrBlank()) {
        return false
    }
    // 相册属于前者，文件选择器属于后者。
    return packageName.startsWith("com.android.providers") || packageName.startsWith("com.google.android")
}

/**
 * 格式化文件大小，入参单位为bytes
 */
fun formatFileSize(sizeBytes:Long):String{
    if(sizeBytes<=0) return "0 B"
    val units=arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(sizeBytes.toDouble()) / log10(1024.0)).toInt()
    return String.format(Locale.US,"%.1f %s", sizeBytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}