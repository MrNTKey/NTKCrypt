package me.wjz.nekocrypt.ui.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.ui.CatPawButton
import me.wjz.nekocrypt.ui.SwitchSettingCard
import me.wjz.nekocrypt.util.openAccessibilitySettings
import me.wjz.nekocrypt.util.rememberAccessibilityServiceState

// --- 主屏幕代码 ---

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 1. 获取当前上下文
    val context: Context = LocalContext.current

    // 2. 使用我们新的 Composable 函数来获取并监听无障碍服务的状态
    //    你需要将 MyAccessibilityService::class.java 替换成你自己的服务类名
    val isEnabled by rememberAccessibilityServiceState(context, NCAccessibilityService::class.java)

    // 使用 Column 作为根布局，以垂直排列组件
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用带权重的 Column 来包裹原有的猫爪UI，使其占据大部分空间并保持居中
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CatPawButton(
                isEnabled = isEnabled,
                statusText = if (isEnabled)
                    stringResource(id = R.string.accessibility_service_enabled)
                else
                    stringResource(id = R.string.accessibility_service_disabled),
                onClick = { openAccessibilitySettings(context) }
            )
        }

        // 在底部添加我们的设置卡片

        //沉浸式加密开关
        SwitchSettingCard(
            key = SettingKeys.USE_AUTO_ENCRYPTION,
            defaultValue = false,
            title = stringResource(id = R.string.setting_encrypt_on_send_title),
            subtitle = stringResource(id = R.string.setting_encrypt_on_send_subtitle),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // 添加一些边距让布局更好看
        )
        //沉浸式解密开关
        SwitchSettingCard(
            key = SettingKeys.IS_IMMERSIVE_DECRYPTION_MODE,
            defaultValue = false,
            title = stringResource(id = R.string.setting_decrypt_immersive_mod_title),
            subtitle = stringResource(id = R.string.setting_decrypt_immersive_mod_subtitle),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp) // 添加一些边距让布局更好看
        )

        Spacer(Modifier.padding(4.dp))
    }
}