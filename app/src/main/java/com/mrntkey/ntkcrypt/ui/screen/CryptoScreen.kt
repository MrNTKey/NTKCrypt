package com.mrntkey.ntkcrypt.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.edit
import com.mrntkey.ntkcrypt.Constant
import com.mrntkey.ntkcrypt.Constant.DEFAULT_SECRET_KEY
import com.mrntkey.ntkcrypt.R
import com.mrntkey.ntkcrypt.SettingKeys
import com.mrntkey.ntkcrypt.SettingKeys.CURRENT_KEY
import com.mrntkey.ntkcrypt.SettingKeys.KEY_HISTORY_LIST
import com.mrntkey.ntkcrypt.SettingKeys.USER_NEKO_TALK
import com.mrntkey.ntkcrypt.data.dataStore
import com.mrntkey.ntkcrypt.hook.rememberDataStoreState
import com.mrntkey.ntkcrypt.util.CryptoManager
import com.mrntkey.ntkcrypt.util.CryptoManager.appendNTKCryptTalk
// kotlinx.coroutines.FlowPreview 和 kotlinx.coroutines.flow.debounce 不再需要
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CryptoScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    // intermediateOutputText 存储不含猫语的原始加/解密结果
    var intermediateOutputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isEncryptMode by remember { mutableStateOf(true) }

    val secretKey: String by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    var userNekoTalk: String by rememberDataStoreState(USER_NEKO_TALK, "喵~")

    val decryptFailed = stringResource(id = R.string.crypto_decrypt_fail)
    var isDecryptFailed by remember { mutableStateOf(false) }
    var charCount by remember { mutableStateOf(0) } // 将由 finalOutputToDisplay 更新
    var elapsedTime by remember { mutableStateOf(0L) }

    var showKeySelectionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // ✨ 主要的 LaunchedEffect，现在只依赖 inputText 和 secretKey ✨
    LaunchedEffect(inputText, secretKey) {
        if (inputText.isEmpty()) {
            intermediateOutputText = ""
            // outputText (最终显示) 也会在下面被清空
            // charCount 和 elapsedTime 也会在 finalOutputToDisplay 中重置
            isDecryptFailed = false // 重置解密失败状态
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis()
        val resultMsg: String? // 可以为 null

        if (CryptoManager.containsCiphertext(inputText)) {
            isEncryptMode = false
            // intermediateOutputText 将是解密结果或 null
            resultMsg = CryptoManager.decrypt(inputText, secretKey)
            intermediateOutputText = resultMsg ?: "" // 如果解密失败，intermediateOutputText 为空串
        } else {
            isEncryptMode = true
            // intermediateOutputText 将是纯加密结果 (不含猫语)
            resultMsg = CryptoManager.encrypt(inputText, secretKey)
            intermediateOutputText = resultMsg // encrypt 不会返回 null (假设)
        }

        val endTime = System.currentTimeMillis()
        elapsedTime = endTime - startTime

        isDecryptFailed = resultMsg == null && !isEncryptMode // 只有在解密模式且结果为 null 时才算失败
        // charCount 会在 finalOutputToDisplay 中更新
    }

    // ✨ 使用 remember 计算最终要显示的 outputText ✨
    // 这个 remember 块会在 intermediateOutputText, userNekoTalk, isEncryptMode, isDecryptFailed 变化时重新计算
    val finalOutputToDisplay = remember(intermediateOutputText, userNekoTalk, isEncryptMode, isDecryptFailed, decryptFailed) {
        if (intermediateOutputText.isEmpty() && inputText.isEmpty()) { // 同时检查 inputText，确保初始状态为空
            charCount = 0
            elapsedTime = 0L // 重置时间
            ""
        } else if (isDecryptFailed) { // 解密失败 (由 LaunchedEffect 设置)
            charCount = decryptFailed.length
            decryptFailed
        } else if (isEncryptMode) {
            if (intermediateOutputText.isNotEmpty()) { // 确保有基础加密文本
                val finalText = intermediateOutputText.appendNTKCryptTalk(userNekoTalk)
                charCount = finalText.length
                finalText
            } else {
                charCount = 0 // 如果基础加密文本为空（不应该发生如果inputText非空）
                ""
            }
        } else { // 解密成功
            charCount = intermediateOutputText.length
            intermediateOutputText
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        KeySelector(
            selectedKeyName = secretKey,
            onClick = {
                showKeySelectionDialog = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            // ... (其他 inputText OutlinedTextField 属性)
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            minLines = 1,
            maxLines = 6,
            label = { Text(stringResource(id = R.string.crypto_input_label)) },
            placeholder = { Text(stringResource(id = R.string.crypto_input_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = stringResource(id = R.string.crypto_input_icon_desc)
                )
            },
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { inputText += it }
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_pasted_from_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = stringResource(id = R.string.crypto_paste_icon_desc)
                        )
                    }
                    AnimatedVisibility(visible = inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.crypto_clear_input_icon_desc)
                            )
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

        Spacer(modifier = Modifier.height(12.dp))

        // 猫语输入框
        OutlinedTextField(
            value = userNekoTalk,
            onValueChange = { newUserNekoTalk ->
                userNekoTalk = newUserNekoTalk // 这会触发 finalOutputToDisplay 的重新计算
            },
            // ... (其他 userNekoTalk OutlinedTextField 属性)
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.crypto_neko_talk_label)) },
            placeholder = { Text(stringResource(R.string.crypto_neko_talk_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Pets,
                    contentDescription = stringResource(R.string.crypto_neko_talk_icon_desc)
                )
            },
            trailingIcon = {
                AnimatedVisibility(visible = userNekoTalk.isNotEmpty()) {
                    IconButton(onClick = { userNekoTalk = "" }) {
                        Icon(
                            Icons.Rounded.Clear,
                            contentDescription = stringResource(id = R.string.crypto_clear_input_icon_desc)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 输出框现在使用 finalOutputToDisplay
        AnimatedVisibility(
            visible = finalOutputToDisplay.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            OutlinedTextField(
                value = finalOutputToDisplay, // <-- 使用计算后的最终文本
                onValueChange = {},
                readOnly = true,
                // ... (其他输出框 OutlinedTextField 属性)
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                minLines = 1,
                maxLines = 6,
                isError = isDecryptFailed, // isDecryptFailed 仍然由 LaunchedEffect 更新
                label = { Text(stringResource(if (isEncryptMode) R.string.crypto_result_label_encrypted else R.string.crypto_result_label_decrypted)) },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(finalOutputToDisplay))
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_copied_to_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(id = R.string.crypto_copy_result_icon_desc)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = finalOutputToDisplay.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CryptoStats(
                charCount = charCount, // charCount 由 finalOutputToDisplay 更新
                elapsedTime = elapsedTime
            )
        }
    }

    if (showKeySelectionDialog) {
        KeySelectionDialog(
            currentSelectedKey = secretKey,
            onDismissRequest = { showKeySelectionDialog = false },
            onKeySelected = { newKey ->
                scope.launch {
                    context.dataStore.edit { preferences ->
                        preferences[CURRENT_KEY] = newKey
                        val history = preferences[KEY_HISTORY_LIST] ?: emptySet()
                        if (!history.contains(newKey) && newKey.isNotBlank()) {
                            preferences[KEY_HISTORY_LIST] = history + newKey
                        }
                    }
                }
                showKeySelectionDialog = false
                Toast.makeText(context, context.getString(R.string.key_selection_dialog_key_updated, newKey), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// ----------------------------------------------------
// Helper Composable Functions (KeySelector, KeySelectionDialog, CryptoStats)
// 这些函数的定义与前一个版本相同，这里省略以保持简洁
// 确保它们在此文件或已正确导入
// ----------------------------------------------------

@Composable
private fun KeySelector(
    selectedKeyName: String,
    onClick: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    imageVector = Icons.Default.Key,
                    contentDescription = stringResource(id = R.string.crypto_key_icon_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.crypto_current_key_label),
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(id = R.string.crypto_select_key_icon_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KeySelectionDialog(
    currentSelectedKey: String,
    onDismissRequest: () -> Unit,
    onKeySelected: (String) -> Unit
) {
    val context = LocalContext.current
    var newKeyInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val keyHistoryFlow = remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[KEY_HISTORY_LIST] ?: emptySet()
        }
    }
    val keyHistory by keyHistoryFlow.collectAsState(initial = emptySet())

    val filteredHistory = remember(keyHistory, searchQuery) {
        if (searchQuery.isBlank()) {
            keyHistory.toList().sortedDescending()
        } else {
            keyHistory.filter { it.contains(searchQuery, ignoreCase = true) }
                .sortedDescending()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.key_selection_dialog_title),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = newKeyInput,
                    onValueChange = { newKeyInput = it },
                    label = { Text(stringResource(R.string.key_selection_dialog_input_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (newKeyInput.isNotBlank()) {
                            onKeySelected(newKeyInput)
                            newKeyInput = ""
                        } else {
                            Toast.makeText(context, context.getString(R.string.key_selection_dialog_empty_input_error), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.key_selection_dialog_set_button))
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onKeySelected(Constant.DEFAULT_SECRET_KEY)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text(stringResource(R.string.key_selection_dialog_restore_default_button))
                }

                if (keyHistory.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.key_selection_dialog_history_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.key_selection_dialog_search_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.key_selection_dialog_search_icon_desc)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.key_selection_dialog_clear_search_desc)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (filteredHistory.isNotEmpty()) {
                        filteredHistory.forEach { key ->
                            TextButton(
                                onClick = { onKeySelected(key) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = key,
                                    color = if (key == currentSelectedKey) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            Divider()
                        }
                    } else if (searchQuery.isNotEmpty()) {
                        Text(
                            stringResource(R.string.key_selection_dialog_no_search_results),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun CryptoStats(
    charCount: Int,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_char_count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = charCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_elapsed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_ms, elapsedTime),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

