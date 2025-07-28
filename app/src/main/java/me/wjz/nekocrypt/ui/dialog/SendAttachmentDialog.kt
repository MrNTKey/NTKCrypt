package me.wjz.nekocrypt.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

/**
 * ✨ [最终精致版] 发送附件的对话框UI内容
 * 带有动画、进度反馈，并合并了图片/视频选项。
 */
@Composable
fun SendAttachmentDialog(
    onDismissRequest: () -> Unit,
    onSendRequest: (String) -> Unit,
) {
    var selectedUrl by remember { mutableStateOf("") }
    var uploadProgress by remember { mutableStateOf<Float?>(null) }
    val isUploading = uploadProgress != null
    val coroutineScope = rememberCoroutineScope()

    fun startMockUpload(resultUrl: String) {
        coroutineScope.launch {
            uploadProgress = 0f
            while ((uploadProgress ?: 0f) < 1f) {
                delay(100)
                uploadProgress = ((uploadProgress ?: 0f) + 0.1f).coerceAtMost(1.0f)
            }
            selectedUrl = resultUrl
            uploadProgress = null
        }
    }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    fun dismissWithAnimation() {
        coroutineScope.launch {
            isVisible = false
            delay(300)
            onDismissRequest()
        }
    }

    NekoCryptTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300))
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "发送加密内容",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                SendOptionItem(
                                    icon = Icons.Outlined.Collections,
                                    label = "图片或视频",
                                    enabled = !isUploading,
                                    onClick = { startMockUpload("https://neko.crypt/media_mock.png") }
                                )
                                SendOptionItem(
                                    icon = Icons.Outlined.AttachFile,
                                    label = "文件",
                                    enabled = !isUploading,
                                    onClick = { startMockUpload("https://neko.crypt/file_mock.zip") }
                                )
                            }

                            Row(horizontalArrangement = Arrangement.Center) {
                                AnimatedVisibility(
                                    visible = isUploading,
                                    enter = fadeIn(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                                        animationSpec = tween(
                                            200
                                        )
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            progress = { uploadProgress ?: 0f }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "正在加密上传...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        AnimatedVisibility(
                            visible = selectedUrl.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            OutlinedTextField(
                                value = selectedUrl,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("内容链接") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { dismissWithAnimation() },
                                enabled = !isUploading
                            ) {
                                Text("取消")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onSendRequest(selectedUrl) },
                                enabled = selectedUrl.isNotEmpty() && !isUploading
                            ) {
                                Text("发送")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 对话框里可点击的选项按钮的UI封装
 */
@Composable
private fun RowScope.SendOptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.5f, label = "")
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .weight(1f)
            .clip(shape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * alpha),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f * alpha))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
        }
    }
}
