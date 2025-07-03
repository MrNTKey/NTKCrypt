package me.wjz.nekocrypt.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.hook.rememberDataStoreState

@Composable
fun CryptoScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var isEncryptMode by remember { mutableStateOf(true) }  //实时判断是加密or解密
    //获取当前密钥，没有就是默认密钥
    val secretKey: String by rememberDataStoreState(CURRENT_KEY,DEFAULT_SECRET_KEY)

    LaunchedEffect(inputText,isEncryptMode) {
        if(inputText.isEmpty()) return@LaunchedEffect
        outputText="嘻嘻哈哈"
    }

    // 模拟加密和解密操作的逻辑
    val encryptMsg = {
        if (inputText.isNotBlank()) {
            outputText = "加密后的密文: ${inputText.reversed()}" // 使用反转字符串来模拟加密
        }
    }
    val decryptMsg = {
        // 在真实应用中，这里会调用 CryptoManager.decrypt()
        if (inputText.isNotBlank()) {
            outputText = "解密后的明文: ${inputText.reversed()}" // 使用反转字符串来模拟解密
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 密钥选择器
        KeySelector(
            selectedKeyName = secretKey,
            onClick = { /* TODO: 在这里处理点击事件，比如弹出一个密钥选择对话框 */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. 输入文本框
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
//                .height(180.dp),这里不设置固定高度
                .verticalScroll(rememberScrollState()),
            minLines = 1,//控制默认的最小行数
            maxLines = 6,//控制最大行数
            label = { Text("输入原文或密文") },
            placeholder = { Text("在此处输入或粘贴文本...") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Notes, contentDescription = "输入图标") },
            // 右方的辅助按钮
            trailingIcon = {
                Row {
                    // 粘贴按钮
                    IconButton(onClick = { /* TODO: 实现粘贴剪贴板内容 */ }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "粘贴")
                    }
                    // 清空按钮，仅在有输入时显示
                    AnimatedVisibility(visible = inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "清空输入")
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. 主要操作按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 加密按钮
            Button(
                onClick = encryptMsg,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "加密图标")
                Spacer(modifier = Modifier.width(8.dp))
                Text("加密", fontWeight = FontWeight.Bold)
            }

            // 交换按钮
            IconButton(onClick = {
                val temp = inputText
                inputText = outputText
                outputText = temp
            }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "交换输入与输出")
            }

            // 解密按钮
            Button(
                onClick = decryptMsg,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LockOpen, contentDescription = "解密图标")
                Spacer(modifier = Modifier.width(8.dp))
                Text("解密", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. 输出结果区域
        // 使用 AnimatedVisibility，当有输出时，这个区域会平滑地淡入
        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            OutlinedTextField(
                value = outputText,
                onValueChange = {}, // 输出框通常是只读的
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth(),
//                    .height(180.dp),
                minLines = 1,
                maxLines = 6,
                label = { Text("结果") },
                // 右下角的复制按钮
                trailingIcon = {
                    IconButton(onClick = { /* TODO: 实现复制输出内容到剪贴板 */ }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制结果")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }
    }
}


/**
 * 一个用于展示和选择密钥的自定义组件。
 */
@Composable
private fun KeySelector(
    selectedKeyName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = "密钥图标",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "当前密钥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                    Text(
                        text = selectedKeyName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "选择密钥",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}