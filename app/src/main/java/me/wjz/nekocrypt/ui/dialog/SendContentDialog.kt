package me.wjz.nekocrypt.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 可以发送图片、视频、文件的对话框
 */
@Composable
fun SendContentDialog(
    onDismissRequest: () -> Unit,
    onSendRequest: (String) -> Unit
){
    var selectedUrl by remember { mutableStateOf("") }

    // 使用高度定制化的Dialog组件
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "发送加密内容",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- 第一个分组：图片或视频 ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SendOptionItem(
                            icon = Icons.Outlined.Image,
                            label = "图片",
                            onClick = {
                                // ✨ 模拟选择图片并获取URL
                                selectedUrl = "https://neko.crypt/image_mock.jpg"
                            }
                        )
                        SendOptionItem(
                            icon = Icons.Outlined.Videocam,
                            label = "视频",
                            onClick = {
                                // ✨ 模拟选择视频并获取URL
                                selectedUrl = "https://neko.crypt/video_mock.mp4"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 第二个分组：文件 ---
                Row(modifier = Modifier.fillMaxWidth()) {
                    SendOptionItem(
                        icon = Icons.Outlined.AttachFile,
                        label = "文件",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // ✨ 模拟选择文件并获取URL
                            selectedUrl = "https://neko.crypt/file_mock.zip"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- URL显示区域 ---
                // 当有URL时，带动画地显示出来
                AnimatedVisibility(
                    visible = selectedUrl.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    OutlinedTextField(
                        value = selectedUrl,
                        onValueChange = {}, // 用户不可编辑
                        readOnly = true,
                        label = { Text("内容链接") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 底部按钮 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("取消")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSendRequest(selectedUrl) },
                        // ✨ 只有当selectedUrl不为空时，发送按钮才可用
                        enabled = selectedUrl.isNotEmpty()
                    ) {
                        Text("发送")
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
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .weight(1f)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}