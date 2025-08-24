package com.mrntkey.ntkcrypt.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit // 导入 DataStore edit
import androidx.datastore.preferences.core.stringPreferencesKey // 导入 stringPreferencesKey
import com.mrntkey.ntkcrypt.SettingKeys // 假设您的 SettingKeys 在这里
import com.mrntkey.ntkcrypt.data.dataStore
import kotlinx.coroutines.launch

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    var newKeyInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 用于启动协程来保存数据

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("设置新密钥", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newKeyInput,
            onValueChange = { newKeyInput = it },
            label = { Text("输入新密钥") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (newKeyInput.isNotBlank()) {
                    scope.launch {
                        context.dataStore.edit { preferences ->
                            preferences[SettingKeys.CURRENT_KEY] = newKeyInput
                        }
                        // 清空输入框并提示用户
                        newKeyInput = ""
                        Toast.makeText(context, "密钥已保存!", Toast.LENGTH_SHORT).show()
                        // 你可能还想在这里导航回上一个屏幕
                        // navController.popBackStack()
                    }
                } else {
                    Toast.makeText(context, "密钥不能为空!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("保存密钥")
        }
    }
}
