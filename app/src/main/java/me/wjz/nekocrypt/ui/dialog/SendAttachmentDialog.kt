package me.wjz.nekocrypt.ui.dialog

import android.content.Intent
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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.activity.AttachmentPickerActivity
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

/**
 * ✨ [最终精致版] 发送附件的对话框UI内容
 * 带有动画、进度反馈，并合并了图片/视频选项。
 */
@Composable
fun SendAttachmentDialog(
    onDismissRequest: () -> Unit,
    onSendRequest: (String) -> Unit,
    // --- ✨ 新增/修改的参数 ---
    uploadProgress: Float?,      // 从外部接收上传进度
    resultUrl: String,            // 从外部接收最终的URL
) {
    val isUploading = uploadProgress != null
    val coroutineScope = rememberCoroutineScope()

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
        Box(modifier = Modifier.padding(8.dp)) {
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
                            text = stringResource(R.string.crypto_attachment_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center) {
                            // 封装好的组件
                            AttachmentOptions(
                                isUploading = isUploading,
                            )

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

                        // ✨ 链接输入框的可见性和内容，现在由外部传入的 resultUrl 决定
                        AnimatedVisibility(
                            visible = resultUrl.isNotEmpty(),
                            enter = fadeIn(animationSpec = tween(300)),
                            exit = fadeOut(animationSpec = tween(300))
                        ) {
                            OutlinedTextField(
                                value = resultUrl,
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
                                // ✨ 发送按钮的可用性也由外部状态决定
                                onClick = { onSendRequest(resultUrl) },
                                enabled = resultUrl.isNotEmpty() && !isUploading
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

@Composable
private fun AttachmentOptions(
    isUploading: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SendOptionItem(
            icon = Icons.Outlined.Collections,
            label = stringResource(R.string.crypto_attachment_media),
            enabled = !isUploading,
            onClick = {
//                val intent = Intent(Intent.ACTION_PICK).apply {
//                    type = "image/* video/*" // 同时选择图片和视频
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//                context.startActivity(intent)

                // 这里体现了两种intent的创建方式，上面的是先弹出一个弹窗，让用户选择其一，再具体拉出对应弹窗，适合service上下文

                // 下面我们这里指定一个activity，就方便很多。
                val intent = Intent(context, AttachmentPickerActivity::class.java).apply {
                    // 这里放个extra，activity内部就根据这个额外的kv判断具体拉起逻辑
                    putExtra(AttachmentPickerActivity.EXTRA_PICK_TYPE, AttachmentPickerActivity.TYPE_MEDIA)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
        SendOptionItem(
            icon = Icons.Outlined.AttachFile,
            label = stringResource(R.string.crypto_attachment_file),
            enabled = !isUploading,
            onClick = {
                val intent = Intent(context, AttachmentPickerActivity::class.java).apply {
                    putExtra(AttachmentPickerActivity.EXTRA_PICK_TYPE, AttachmentPickerActivity.TYPE_FILE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        )
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