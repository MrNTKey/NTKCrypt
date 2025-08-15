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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCFileType


/**
 *  一个独立的、可复用的解密弹窗 Composable UI
 * 它只关心需要显示什么文本 (text)，以及被关闭时该做什么 (onDismiss)。
 * 它完全不知道什么是无障碍服务，什么是处理器。
 */
@Composable
fun DecryptionPopup(decryptedText: String, durationMills: Long = 3000, onDismiss: () -> Unit) {
    // 增加判断，看需要展示纯文本还是图片or文件。
    val fileProtocol : NCFileProtocol? = NCFileProtocol.fromString(decryptedText)

    if (fileProtocol != null) {
        // --- 情况A：是文件协议，并且成功解析 ---

        // 根据图片和普通文件判断展示逻辑
        when(fileProtocol.type){
            NCFileType.IMAGE -> null
            NCFileType.FILE ->
                DecryptedFilePopupContent(
                fileInfo = fileProtocol,
                onDismiss = onDismiss,
                durationMills = durationMills
            )
        }

    } else {
        // --- 情况B：是普通文本，或者协议解析失败 ---
        DecryptedTextPopupContent(
            text = decryptedText,
            onDismiss = onDismiss,
            durationMills = durationMills
        )
    }
}


/**
 * 负责显示普通文本的弹窗
 */
@Composable
private fun DecryptedTextPopupContent(
    text: String,
    onDismiss: () -> Unit,
    durationMills: Long
) {
    val animationTime = 250
    var isVisible by remember { mutableStateOf(false) }
    val progress = remember { Animatable(1.0f) }

    LaunchedEffect(Unit) {
        isVisible = true // 触发出现
        progress.animateTo(0.0f, animationSpec = tween(durationMills.toInt(), easing = LinearEasing))
        isVisible = false // 倒计时结束后，触发消失
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(animationTime.toLong()) // 等待消失动画播放完毕
            onDismiss() // 动画完全结束后，才真正调用 onDismiss
        }
    }

    NekoCryptTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(animationTime)),
                exit = scaleOut(animationSpec = tween(animationTime)) + fadeOut(
                    animationSpec = tween(animationTime)
                )
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.92f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = text,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(shadow = Shadow(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), offset = Offset(2f, 2f), blurRadius = 4f)),
                            modifier = Modifier.weight(1f, fill = false)
                        )
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
                                isVisible = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "关闭", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * ✨ “文件专员”：一个只负责显示文件信息的UI组件
 */
@Composable
private fun DecryptedFilePopupContent(
    fileInfo: NCFileProtocol,
    onDismiss: () -> Unit,
    durationMills: Long
) {
    val animationTime = 250
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(fileInfo) { // Key是fileInfo，保证每次文件不同时动画重置
        isVisible = true
        delay(durationMills)
        isVisible = false
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(animationTime.toLong())
            onDismiss()
        }
    }

    NekoCryptTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) + fadeIn(tween(animationTime)),
                exit = scaleOut(tween(animationTime)) + fadeOut(tween(animationTime))
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "File Icon",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = fileInfo.url.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${fileInfo.size} - ${fileInfo.type.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(onClick = {
                                isVisible = false // 复制后也触发消失
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy URL",
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("复制链接")
                            }
                        }
                    }
                }
            }
        }
    }
}