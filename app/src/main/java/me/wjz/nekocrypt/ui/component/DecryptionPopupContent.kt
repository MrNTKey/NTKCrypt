package me.wjz.nekocrypt.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.NCFileProtocol


/**
 *  一个独立的、可复用的解密弹窗 Composable UI
 * 它只关心需要显示什么文本 (text)，以及被关闭时该做什么 (onDismiss)。
 * 它完全不知道什么是无障碍服务，什么是处理器。
 */
@Composable
fun DecryptionPopupContent(decryptedText: String, durationMills: Long = 3000, onDismiss: () -> Unit) {
    val animationTime = 250 // 动画时间

    // 创建一个从1.0开始的动画值。
    val progress = remember { Animatable(1.0f) }
    // ✨ 1. 新增一个状态来控制整体的可见性，用于驱动出入场动画
    var isVisible by remember { mutableStateOf(false) }

    // 增加判断，看需要展示纯文本还是图片or文件。
    val fileProtocol : NCFileProtocol? = NCFileProtocol.fromString(decryptedText)

    // ✨ 核心修复：这个LaunchedEffect现在只负责“自动销毁”的计时
    LaunchedEffect(Unit) {
        isVisible = true // 触发出现
        // 启动倒计时
        progress.animateTo(
            0.0f,
            animationSpec = tween(
                durationMills.toInt(),
                easing = LinearEasing
            )
        )
        // 倒计时结束后，触发消失
        isVisible = false
    }

    // ✨ 新增：这个LaunchedEffect专门负责“销毁”流程
    // 无论isVisible是如何变成false的，它都会被触发
    LaunchedEffect(isVisible) {
        if (!isVisible) {
            // 等待消失动画播放完毕
            delay(animationTime.toLong())
            // 动画完全结束后，才真正调用 onDismiss
            onDismiss()
        }
    }

    // 如果不手动指定darkTheme，会出问题。
    NekoCryptTheme(darkTheme = false) {
        // ✨ 关键！在最外层用一个 Box 包裹，并给它加上内边距。
        // 这个内边距就是我们为动画和阴影预留的“安全区”，
        // 确保它们在“弹跳”时不会被最外层的边界裁剪掉。
        Box(modifier = Modifier.padding(16.dp)) {

            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring( // 使用 spring 动画，让效果更Q弹
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(animationTime)),
                // ✨ 消失动画：优雅地缩小并淡出
                exit = scaleOut(animationSpec = tween(animationTime)) + fadeOut(
                    animationSpec = tween(
                        animationTime
                    )
                )
            ) {
                Card(
                    // ✨ 4. 关键！让 Card 的尺寸自适应内容
                    modifier = Modifier
                        .wrapContentSize() // 让卡片包裹内容，而不是撑满
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                            alpha = 0.92f
                        )
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                            end = 8.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (fileProtocol != null) {

                        } else {
                            Text(
                                text = decryptedText,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), // 阴影颜色
                                        offset = Offset(2f, 2f), // 阴影偏移量
                                        blurRadius = 4f // 阴影模糊半径
                                    )
                                ),
                                // ✨ 限制文本的最大宽度，防止一行文本过长导致弹窗撑得太大
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(25.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress.value },
                                modifier = Modifier.size(25.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            IconButton(onClick = {
                                // ✨ 点击关闭按钮时，也应该优雅地播放“消失”动画
                                isVisible = false
                            }) {
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
    }
}