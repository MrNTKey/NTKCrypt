package com.mrntkey.ntkcrypt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close // 新增：导入 Close 图标
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search // 新增：导入 Search 图标
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mrntkey.ntkcrypt.Constant.DEFAULT_SECRET_KEY
import com.mrntkey.ntkcrypt.R
import com.mrntkey.ntkcrypt.SettingKeys
import com.mrntkey.ntkcrypt.data.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    var newKeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var keyToDeleteState by remember { mutableStateOf<String?>(null) }

    // --- 新增：搜索文本状态 ---
    var searchQuery by remember { mutableStateOf("") }

    val currentKeyFlow = remember(context) {
        context.dataStore.data.map { preferences ->
            preferences[SettingKeys.CURRENT_KEY] ?: DEFAULT_SECRET_KEY
        }
    }
    val currentKey by currentKeyFlow.collectAsState(initial = DEFAULT_SECRET_KEY)

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

    // ... (saveAndSetCurrentKey 和 removeKeyFromHistory 函数保持不变) ...
    fun saveAndSetCurrentKey(keyToSave: String, isRestoringDefault: Boolean = false) {
        if (keyToSave.isNotBlank() || isRestoringDefault) {
            scope.launch {
                context.dataStore.edit { preferences ->
                    preferences[SettingKeys.CURRENT_KEY] = keyToSave
                    if (!isRestoringDefault && keyToSave.isNotBlank()) {
                        val existingHistory = preferences[SettingKeys.KEY_HISTORY_LIST] ?: emptySet()
                        val newHistory = existingHistory.toMutableSet()
                        newHistory.add(keyToSave)
                        preferences[SettingKeys.KEY_HISTORY_LIST] = newHistory
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_saved_and_set, keyToSave),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (isRestoringDefault) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_restored_default),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else if (!isRestoringDefault) {
            Toast.makeText(
                context,
                context.getString(R.string.key_screen_toast_key_empty),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun removeKeyFromHistory(keyToRemove: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                val existingHistory = preferences[SettingKeys.KEY_HISTORY_LIST] ?: emptySet()
                val newHistory = existingHistory.toMutableSet()
                if (newHistory.remove(keyToRemove)) {
                    preferences[SettingKeys.KEY_HISTORY_LIST] = newHistory
                    if (preferences[SettingKeys.CURRENT_KEY] == keyToRemove) {
                        preferences[SettingKeys.CURRENT_KEY] = DEFAULT_SECRET_KEY
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_removed_and_current_reset, keyToRemove),
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_removed, keyToRemove),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.key_screen_title),
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.key_screen_current_key_label, currentKey),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newKeyInput,
            onValueChange = { newKeyInput = it },
            label = { Text(stringResource(R.string.key_screen_input_new_key_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (newKeyInput.isNotBlank()) {
                    saveAndSetCurrentKey(newKeyInput)
                    newKeyInput = ""
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.key_screen_toast_key_empty),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.key_screen_set_and_save_button))
        }
        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                saveAndSetCurrentKey(DEFAULT_SECRET_KEY, isRestoringDefault = true)
                newKeyInput = ""
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.key_screen_restore_default_button))
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.key_screen_history_keys_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(8.dp)) //  <-- 新增 Spacer

        // --- 新增：历史密钥搜索栏 ---
        if (keyHistory.isNotEmpty()) { // 只有在有历史密钥时才显示搜索栏
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(stringResource(R.string.key_screen_search_history_label)) }, // 新增字符串资源
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = stringResource(R.string.key_screen_search_icon_desc) // 新增字符串资源
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.key_screen_clear_search_desc) // 新增字符串资源
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp) // 与其他输入框样式一致
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- 修改：历史密钥列表 ---
        if (filteredHistory.isEmpty()) { //  <-- 使用 filteredHistory
            Spacer(Modifier.height(16.dp))
            Text(
                // 根据是否有搜索词来决定显示 "无历史" 还是 "无搜索结果"
                text = if (searchQuery.isBlank() && keyHistory.isEmpty()) stringResource(R.string.key_screen_no_history_keys)
                else if (searchQuery.isNotBlank()) stringResource(R.string.key_screen_no_search_results) // 新增字符串资源
                else stringResource(R.string.key_screen_no_history_keys), // 备用，理论上不会到这里如果keyHistory不为空
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 确保列表能正确填充剩余空间
            ) {
                items(filteredHistory) { key -> //  <-- 使用 filteredHistory
                    KeyHistoryItem(
                        keyText = key,
                        isCurrent = key == currentKey,
                        onSelect = { selectedKey ->
                            saveAndSetCurrentKey(selectedKey)
                            newKeyInput = ""
                            searchQuery = "" // 选择后清空搜索，可选
                        },
                        onDeleteRequest = { keyToDelete ->
                            keyToDeleteState = keyToDelete
                            showConfirmDeleteDialog = true
                        }
                    )
                    Divider()
                }
            }
        }
    }

    // ... (AlertDialog 保持不变) ...
    if (showConfirmDeleteDialog && keyToDeleteState != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                keyToDeleteState = null
            },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = stringResource(R.string.key_screen_confirm_delete_title),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.key_screen_confirm_delete_message, keyToDeleteState!!),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        keyToDeleteState?.let { key ->
                            removeKeyFromHistory(key)
                        }
                        showConfirmDeleteDialog = false
                        keyToDeleteState = null
                    }
                ) {
                    Text(
                        stringResource(R.string.key_screen_delete_confirm_button),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmDeleteDialog = false
                        keyToDeleteState = null
                    }
                ) {
                    Text(
                        stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

// ... (KeyHistoryItem Composable 保持不变) ...
@Composable
fun KeyHistoryItem(
    keyText: String,
    isCurrent: Boolean,
    onSelect: (String) -> Unit,
    onDeleteRequest: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(keyText) }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = keyText,
            style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        IconButton(onClick = { onDeleteRequest(keyText) }) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.key_history_item_delete_content_description, keyText),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

