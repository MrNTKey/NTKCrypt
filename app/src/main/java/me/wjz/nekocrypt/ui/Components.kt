package me.wjz.nekocrypt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.data.DataStoreManager
import me.wjz.nekocrypt.data.LocalDataStoreManager

/**
 * 这是一个自定义的、用于显示设置分组标题的组件。
 * @param title 要显示的标题文字。
 */
@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}


/**
 * 这是一个自定义的、带开关的设置项组件。
 * 它内部管理着开关自己的状态。
 * @param icon 左侧显示的图标。
 * @param title 主标题文字。
 * @param subtitle 副标题（描述性文字）。
 */
@Composable
fun SwitchSettingItem(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    //这里可以直接拿到，不需要新建实例
    val dataStoreManager = LocalDataStoreManager.current
    val scope = rememberCoroutineScope()

    val isChecked by dataStoreManager.getSettingFlow(key, defaultValue)
        .collectAsStateWithLifecycle(initialValue = defaultValue)

    //用Row来水平排列元素
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                scope.launch {
                    dataStoreManager.saveSetting(key, !isChecked)
                    onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //显示图标
        icon()
        // 占一点间距
        Spacer(modifier = Modifier.width(16.dp))
        //用Column来垂直排列主标题和副标题
        Column(modifier = Modifier.weight(1f)) {// weight(1f)让这一列占满所有剩余空间
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            ) // 让副标题颜色浅一点
        }
        Switch(checked = isChecked, onCheckedChange = { newCheckedValue ->
            scope.launch {
                dataStoreManager.saveSetting(key, newCheckedValue)
            }
        })
    }
}


@Composable
fun ClickableSettingItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 设置点击事件
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
