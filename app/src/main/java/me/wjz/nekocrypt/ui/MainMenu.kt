package me.wjz.nekocrypt.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.R

//用一个枚举类定义所有的屏幕
enum class Screen {
    EncryptDecrypt,
    Settings,
    About
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu() {
    //现在默认打开设置页
    var currentScreen: Screen by remember { mutableStateOf(Screen.Settings) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        }, bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("设置") },
                    // 判断当前项是否被选中
                    selected = (currentScreen == Screen.Settings),
                    // 点击时，将当前屏幕状态切换到“设置”
                    onClick = { currentScreen = Screen.Settings })
                // 第二个导航项：加解密
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Encrypt/Decrypt") },
                    label = { Text("加解密") },
                    selected = (currentScreen == Screen.EncryptDecrypt),
                    onClick = { currentScreen = Screen.EncryptDecrypt }
                )

                // 第三个导航项：关于
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Info, contentDescription = "About") },
                    label = { Text("关于") },
                    selected = (currentScreen == Screen.About),
                    onClick = { currentScreen = Screen.About }
                )
            }

        }, floatingActionButton = {
            FloatingActionButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        })
    { innerPadding ->
        //使用when语句根据currentScreen决定显示哪个页面
        when(currentScreen){
            Screen.Settings -> SettingsScreen(modifier = Modifier.padding(innerPadding))
            Screen.EncryptDecrypt -> EncryptDecryptScreen(modifier = Modifier.padding(innerPadding))
            Screen.About -> AboutScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}


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
 * @param initialChecked 开关的初始状态（是开还是关）。
 */
@Composable
fun SwitchSettingItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    initialChecked: Boolean
) {
    var isChecked by remember { mutableStateOf(initialChecked) }
    //用Row来水平排列元素
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isChecked = !isChecked }
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
        Switch(checked = isChecked, onCheckedChange = { isChecked = it })
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

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
//            verticalArrangement = Arrangement.spacedBy(16.dp), 选项之间不需要间隔
    ) {
        // 第一个分组：主要设置
        item {
            SettingsHeader(stringResource(R.string.main_settings))
        }
        item {
            SwitchSettingItem(
                icon = { Icon(Icons.Default.Lock, contentDescription = "Enable Encryption") },
                title = "启用全局加密",
                subtitle = "开启后，将自动处理复制的文本",
                initialChecked = true
            )
        }
        item {
            SwitchSettingItem(
                icon = { Icon(Icons.Default.Lock, contentDescription = "Enable Encryption") },
                title = "123123",
                subtitle = "开启后，测试测试",
                initialChecked = true
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

@Composable
fun EncryptDecryptScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("这里是加解密页面", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("这里是关于页面", style = MaterialTheme.typography.headlineMedium)
    }
}
