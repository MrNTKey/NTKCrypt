package me.wjz.nekocrypt.util

import androidx.compose.ui.graphics.Color

// 取反色
fun Color.inverse(): Color{
    return Color(
        red = 1.0f - this.red,
        green = 1.0f - this.green,
        blue = 1.0f - this.blue,
        alpha = this.alpha
    )
}