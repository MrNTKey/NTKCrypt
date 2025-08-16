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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCFileType
import me.wjz.nekocrypt.util.formatFileSize

/**
 * ✨ 全新改造的文件详情对话框 UI
 */
@Composable
fun FilePreviewDialog(
    fileInfo: NCFileProtocol,
    downloadProgress: Int?,
    onDismissRequest: () -> Unit,
    onDownloadRequest: (NCFileProtocol) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    val isDownloading = downloadProgress != null // ✨ 判断当前是否正在下载

    // 出现动画
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // 带动画的关闭逻辑
    fun dismissWithAnimation() {
        coroutineScope.launch {
            isVisible = false
            delay(300) // 等待动画播放完毕
            onDismissRequest()
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
                ) + fadeIn(animationSpec = tween(200)),
                exit = scaleOut(animationSpec = tween(300)) + fadeOut(
                    animationSpec = tween(300)
                )
            ) {
                Card(
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(24.dp).width(300.dp)) {
                        // --- 标题和关闭按钮 ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.dialog_download_file_download_failed),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { dismissWithAnimation() }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- 文件图标和名称 ---
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val icon = when (fileInfo.type) {
                                NCFileType.IMAGE -> Icons.Default.Image
                                NCFileType.FILE -> Icons.Default.FilePresent
                            }
                            Icon(
                                imageVector = icon,
                                contentDescription = "文件类型",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = fileInfo.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- 文件大小和URL ---
                        InfoRow(label = stringResource(R.string.file_size), value = fileInfo.size.formatFileSize())
                        InfoRow(label = stringResource(R.string.url), value = fileInfo.url)

                        Spacer(modifier = Modifier.height(24.dp))

                        // --- ✨ 下载进度条 ---
                        AnimatedVisibility(visible = isDownloading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                val animatedProgress by animateFloatAsState(
                                    targetValue = (downloadProgress ?: 0) / 100f,
                                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                                    label = "download_progress_animation"
                                )
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "下载中... ${downloadProgress ?: 0}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // --- 操作按钮 ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { dismissWithAnimation() }) {
                                Text(stringResource(R.string.cancel))
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(onClick = { onDownloadRequest(fileInfo) }, enabled = downloadProgress == null) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "下载",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.download))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "$label: ",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
