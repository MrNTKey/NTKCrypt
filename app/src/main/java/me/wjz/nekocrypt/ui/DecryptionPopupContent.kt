package me.wjz.nekocrypt.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

/**
 *  一个独立的、可复用的解密弹窗 Composable UI
 * 它只关心需要显示什么文本 (text)，以及被关闭时该做什么 (onDismiss)。
 * 它完全不知道什么是无障碍服务，什么是处理器。
 */
@Composable
fun DecryptionPopupContent(text: String, durationMills: Int = 5000, onDismiss: () -> Unit) {
    // 不手动指定darkTheme，会出问题。

    // 创建一个从1.0开始的动画值。
    val progress = remember { Animatable(1.0f) }

    LaunchedEffect(text) {  // tween就是in-between的缩写，意思就是从开始到结束的中间怎么绘制。
        progress.animateTo(0.0f, animationSpec = tween(durationMills, easing = LinearEasing))
        onDismiss()//动画结束时自动调用onDismiss
    }

    NekoCryptTheme(darkTheme = false) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                // ✨ 3. 创建一个盒子，把倒计时圆圈和关闭按钮叠在一起
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp) // 给盒子一个合适的尺寸
                ) {
                    // 这是我们的倒计时圆圈
                    CircularProgressIndicator(
                        // 它的进度直接绑定到我们的动画值上
                        progress = { progress.value },
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary, // 圆圈的颜色
                        strokeWidth = 2.dp // 圆圈的粗细
                    )
                    // 这是原来的关闭按钮，放在圆圈的上面
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
