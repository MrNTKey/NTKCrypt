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
import androidx.compose.ui.window.Dialog // 确保导入 Dialog
import androidx.datastore.preferences.core.edit
import com.mrntkey.ntkcrypt.Constant
import com.mrntkey.ntkcrypt.Constant.DEFAULT_SECRET_KEY
import com.mrntkey.ntkcrypt.R
import com.mrntkey.ntkcrypt.SettingKeys
import com.mrntkey.ntkcrypt.SettingKeys.CURRENT_KEY
import com.mrntkey.ntkcrypt.data.dataStore
import com.mrntkey.ntkcrypt.hook.rememberDataStoreState
import com.mrntkey.ntkcrypt.util.CryptoManager
import com.mrntkey.ntkcrypt.util.CryptoManager.appendNTKCryptTalk
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Composable
fun CryptoScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isEncryptMode by remember { mutableStateOf(true) }
    val secretKey: String by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    val decryptFailed = stringResource(id = R.string.crypto_decrypt_fail)
    var isDecryptFailed by remember { mutableStateOf(false) }
    var charCount by remember { mutableStateOf(0) }
    var elapsedTime by remember { mutableStateOf(0L) }

    var showKeySelectionDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(inputText, secretKey) {
        if (inputText.isEmpty()) {
            outputText = ""
            charCount = 0
            elapsedTime = 0L
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis()
        var ciphertextCharCount = 0

        val resultMsg = if (CryptoManager.containsCiphertext(inputText)) {
            isEncryptMode = false
            ciphertextCharCount = inputText.length
            CryptoManager.decrypt(inputText, secretKey)
        } else {
            isEncryptMode = true
            val ciphertext = CryptoManager.encrypt(inputText, secretKey).appendNTKCryptTalk()
            ciphertextCharCount = ciphertext.length
            ciphertext
        }

        val endTime = System.currentTimeMillis()
        elapsedTime = endTime - startTime

        isDecryptFailed = resultMsg == null
        outputText = resultMsg ?: decryptFailed
        charCount = ciphertextCharCount
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

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
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

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            OutlinedTextField(
                value = outputText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // 如果输出也可能很长，添加滚动
                minLines = 1,
                maxLines = 6,
                isError = isDecryptFailed,
                label = { Text(stringResource(if (isEncryptMode) R.string.crypto_result_label_encrypted else R.string.crypto_result_label_decrypted)) },
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(outputText))
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
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CryptoStats(
                charCount = charCount,
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
                        val history = preferences[SettingKeys.KEY_HISTORY_LIST] ?: emptySet()
                        if (!history.contains(newKey) && newKey.isNotBlank()) {
                            preferences[SettingKeys.KEY_HISTORY_LIST] = history + newKey
                        }
                    }
                }
                showKeySelectionDialog = false
                Toast.makeText(context, context.getString(R.string.key_selection_dialog_key_updated, newKey), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * 密钥选择对话框 Composable
 */
@Composable
private fun KeySelectionDialog(
    currentSelectedKey: String,
    onDismissRequest: () -> Unit,
    onKeySelected: (String) -> Unit
) {
    val context = LocalContext.current
    var newKeyInput by remember { mutableStateOf("") }
    // --- 新增：搜索文本状态 ---
    var searchQuery by remember { mutableStateOf("") }

    val keyHistoryFlow = remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[SettingKeys.KEY_HISTORY_LIST] ?: emptySet()
        }
    }
    val keyHistory by keyHistoryFlow.collectAsState(initial = emptySet())

    // --- 新增：根据搜索查询过滤历史密钥 ---
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

                // --- 历史密钥部分 ---
                if (keyHistory.isNotEmpty()) { // 即使过滤后为空，只要原始历史不为空，就显示标题和搜索框
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.key_selection_dialog_history_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))

                    // --- 新增：搜索栏 ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.key_selection_dialog_search_label)) }, // 新增字符串资源
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = stringResource(R.string.key_selection_dialog_search_icon_desc) // 新增字符串资源
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.key_selection_dialog_clear_search_desc) // 新增字符串资源
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(8.dp))

                    // --- 使用过滤后的历史密钥 ---
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
                    } else if (searchQuery.isNotEmpty()) { // 如果搜索后列表为空
                        Text(
                            stringResource(R.string.key_selection_dialog_no_search_results), // 新增字符串资源
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    // 如果原始历史为空，则不显示任何内容 (由外层的 if (keyHistory.isNotEmpty()) 控制)
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


/**
 * 一个用于展示统计信息（字符数和耗时）的组件。
 */
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

/**
 * 一个用于展示和选择密钥的自定义组件。
 */
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
        shape = cardShape, // 使用上面定义的变量
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // 可以稍微降低一点阴影，使其更融入
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
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(id = R.string.crypto_select_key_icon_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
