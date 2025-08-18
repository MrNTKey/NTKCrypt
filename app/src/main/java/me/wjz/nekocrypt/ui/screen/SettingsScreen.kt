package me.wjz.nekocrypt.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.ui.ClickableSettingItem
import me.wjz.nekocrypt.ui.ColorSettingItem
import me.wjz.nekocrypt.ui.SettingsHeader
import me.wjz.nekocrypt.ui.SliderSettingItem
import me.wjz.nekocrypt.ui.SwitchSettingItem

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // verticalArrangement = Arrangement.spacedBy(16.dp), 选项之间不需要间隔
    ) {
        // 第一个分组：加解密设置
        item {
            SettingsHeader(stringResource(R.string.crypto_settings))
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem(  //  长按发送密文所需时间
                key = SettingKeys.ENCRYPTION_LONG_PRESS_DELAY,
                defaultValue = 500L, // 默认 500 毫秒
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.decryption_long_press_delay),
                subtitle = stringResource(R.string.decryption_long_press_delay_desc),
                valueRange = 50L..1000L, // 允许用户在 200ms 到 1500ms 之间选择
                step = 50L //每50ms一个挡位
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem( //   点击密文解密的所需时间。
                key = SettingKeys.DECRYPTION_WINDOW_SHOW_TIME,
                defaultValue = 500L, // 默认 500 毫秒
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.decryption_window_show_time),
                subtitle = stringResource(R.string.decryption_window_show_time_desc),
                valueRange = 500L..3000L,
                step = 250L // 单步步长
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        // ————————————————————————————————

        // 第二个分组，界面相关设置
        item {
            SettingsHeader(stringResource(R.string.crypto_ui_settings))
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem( //   沉浸式下，密文位置更新间隔
                key = SettingKeys.DECRYPTION_WINDOW_POSITION_UPDATE_DELAY,
                defaultValue = 250L, // 默认 250
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "position update delay") },
                title = stringResource(R.string.decryption_window_position_update_delay),
                subtitle = stringResource(R.string.decryption_window_position_update_delay_desc),
                valueRange = 0L..1000L,
                step = 50L // 单步步长
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        // ————————————————————————————————
        item {
            ColorSettingItem(
                key = SettingKeys.SEND_BTN_OVERLAY_COLOR,
                defaultValue = "#5066ccff",
                title = stringResource(R.string.send_btn_overlay_color),
                subtitle = stringResource(R.string.send_btn_overlay_color_desc)
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem(  //  控制拉起附件发送悬浮窗的时间间隔。
                key = SettingKeys.SHOW_ATTACHMENT_VIEW_DOUBLE_CLICK_THRESHOLD,
                defaultValue = 250L,
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.double_click_threshold),
                subtitle = stringResource(R.string.double_click_threshold_desc),
                valueRange = 250L..1000L, // 允许用户在 200ms 到 1500ms 之间选择
                step = 250L //每50ms一个挡位
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }


        item {
            SwitchSettingItem(
                key = SettingKeys.IS_GLOBAL_ENCRYPTION_MODE,
                defaultValue = false,
                icon = { Icon(Icons.Default.Lock, contentDescription = "Enable Encryption") },
                title = "启用全局加密",
                subtitle = "开启后，将自动处理复制的文本",
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SwitchSettingItem(
                key = SettingKeys.IS_GLOBAL_ENCRYPTION_MODE,
                defaultValue = false,
                icon = { Icon(Icons.Default.Lock, contentDescription = "Enable Encryption") },
                title = "123123",
                subtitle = "开启后，测试测试",
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        // 第二个分组：关于
        item {
            SettingsHeader("关于")
        }
        item {
            ClickableSettingItem(
                icon = { Icon(Icons.Default.Info, contentDescription = "About App") },
                title = "关于 NekoCrypt",
                onClick = { /* 在这里处理点击事件，比如弹出一个对话框 */ }
            )
        }
    }
}