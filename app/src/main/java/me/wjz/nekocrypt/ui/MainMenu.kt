package me.wjz.nekocrypt.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.screen.CryptoScreen
import me.wjz.nekocrypt.ui.screen.HomeScreen
import me.wjz.nekocrypt.ui.screen.KeyScreen
import me.wjz.nekocrypt.ui.screen.SettingsScreen

//用一个枚举类定义所有的屏幕
enum class Screen {
    Home,
    Crypto,
    Key,
    Setting
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenu() {
    val navItems = remember { listOf(Screen.Home, Screen.Crypto, Screen.Key, Screen.Setting) }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { navItems.size }
    )

    // 3. 创建一个协程作用域，用于启动页面切换动画。
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }, bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                // 遍历导航项列表来动态创建 BottomBar 的 Item
                navItems.forEachIndexed { index, screen ->
                    // 根据 screen 类型获取对应的图标和标签文本
                    val label = when (screen) {
                        Screen.Home -> stringResource(R.string.home)
                        Screen.Crypto -> stringResource(R.string.crypto)
                        Screen.Key -> stringResource(R.string.key)
                        Screen.Setting -> stringResource(R.string.settings)
                    }
                    val icon = @Composable {
                        when (screen) {
                            Screen.Home -> Icon(
                                Icons.Outlined.Home,
                                contentDescription = label,
                                //home感觉比其他的小一号，调大一点
                                modifier = Modifier.size(28.dp)
                            )
                            Screen.Crypto -> Icon(
                                Icons.Outlined.Lock,
                                contentDescription = label
                            )
                            Screen.Key -> Icon(
                                Icons.Outlined.Key,
                                contentDescription = label
                            )
                            Screen.Setting -> Icon(
                                Icons.Outlined.Settings,
                                contentDescription = label
                            )
                        }
                    }

                    NavigationBarItem(
                        icon = icon,
                        label = { Text(label) },
                        // 4. 当前 Pager 的页面索引等于此项的索引时，此项被选中。
                        selected = (pagerState.currentPage == index),
                        // 5. 点击时，使用动画平滑滚动到对应页面。
                        onClick = {
                            // 启动一个协程来执行滚动动画
                            scope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        }, floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        })
    { innerPadding ->
        //使用HorizontalPaper替换掉原来的when。Paper会预加载附近页面实现切换的平滑
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding),
            verticalAlignment = Alignment.Top
        ) { pageIndex ->
            //根据pageIndex来显示不同的页面
            when (navItems[pageIndex]) {
                Screen.Home -> HomeScreen()
                Screen.Crypto -> CryptoScreen()
                Screen.Key -> KeyScreen()
                Screen.Setting -> SettingsScreen()
            }
        }
    }
}









