package me.wjz.nekocrypt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import me.wjz.nekocrypt.hook.rememberDataStoreState

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
    onClick: () -> Unit = {},
) {
    var isChecked by rememberDataStoreState(key, defaultValue)

    //用Row来水平排列元素
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isChecked = !isChecked }//点击整行也能更新状态
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
        Switch(checked = isChecked, onCheckedChange = {
            isChecked = it
            onClick()
        })
    }
}

@Composable
fun ClickableSettingItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
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



@Composable
fun SwitchSettingCard(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onCheckedChanged: (Boolean) -> Unit = {},
) {
    var isChecked by rememberDataStoreState(key, defaultValue)
    // 将形状定义为一个变量，方便复用
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable {
                isChecked = !isChecked
                onCheckedChanged(isChecked)
            },
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 开关的状态直接绑定到我们内部的 isChecked 变量
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onCheckedChanged(it)
                }
            )
        }
    }
}