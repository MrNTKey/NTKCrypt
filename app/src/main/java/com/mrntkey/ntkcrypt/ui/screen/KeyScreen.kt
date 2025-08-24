package com.mrntkey.ntkcrypt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource // 导入 stringResource
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.mrntkey.ntkcrypt.Constant.DEFAULT_SECRET_KEY
import com.mrntkey.ntkcrypt.R // 假设你的 R 文件在这里
import com.mrntkey.ntkcrypt.SettingKeys
import com.mrntkey.ntkcrypt.data.dataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign

@Composable
fun KeyScreen(modifier: Modifier = Modifier /*, navController: NavController? = null*/) {
    var newKeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showConfirmDeleteDialog by remember { mutableStateOf(false) }
    var keyToDeleteState by remember { mutableStateOf<String?>(null) }

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
                            context.getString(R.string.key_screen_toast_key_saved_and_set, keyToSave), // 使用资源
                            Toast.LENGTH_SHORT
                        ).show()
                    } else if (isRestoringDefault) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_restored_default), // 使用资源
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else if (!isRestoringDefault) {
            Toast.makeText(
                context,
                context.getString(R.string.key_screen_toast_key_empty), // 使用资源
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
                            context.getString(R.string.key_screen_toast_key_removed_and_current_reset, keyToRemove), // 使用资源
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.key_screen_toast_key_removed, keyToRemove), // 使用资源
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
            stringResource(R.string.key_screen_title), // 使用资源
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.key_screen_current_key_label, currentKey), // 使用资源
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = newKeyInput,
            onValueChange = { newKeyInput = it },
            label = { Text(stringResource(R.string.key_screen_input_new_key_label)) }, // 使用资源
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
                        context.getString(R.string.key_screen_toast_key_empty), // 使用资源
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(stringResource(R.string.key_screen_set_and_save_button)) // 使用资源
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
            Text(stringResource(R.string.key_screen_restore_default_button)) // 使用资源
        }

        Spacer(Modifier.height(16.dp))
        Divider()
        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.key_screen_history_keys_title), // 使用资源
            style = MaterialTheme.typography.headlineSmall
        )

        if (keyHistory.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.key_screen_no_history_keys), // 使用资源
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(keyHistory.toList().sortedDescending()) { key ->
                    KeyHistoryItem(
                        keyText = key,
                        isCurrent = key == currentKey,
                        onSelect = { selectedKey ->
                            saveAndSetCurrentKey(selectedKey)
                            newKeyInput = ""
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

    if (showConfirmDeleteDialog && keyToDeleteState != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDeleteDialog = false
                keyToDeleteState = null
            },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(
                    text = stringResource(R.string.key_screen_confirm_delete_title), // 已使用资源
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.key_screen_confirm_delete_message, keyToDeleteState!!), // 已使用资源
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
                        stringResource(R.string.key_screen_delete_confirm_button), // 已使用资源
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
                        stringResource(R.string.cancel), // 已使用资源
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        )
    }
}

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
            text = keyText, // keyText 是动态数据，不是静态字符串资源
            style = if (isCurrent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        )
        IconButton(onClick = { onDeleteRequest(keyText) }) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.key_history_item_delete_content_description, keyText), // 使用资源
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

