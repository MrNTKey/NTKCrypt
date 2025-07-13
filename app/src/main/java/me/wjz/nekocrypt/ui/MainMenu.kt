package me.wjz.nekocrypt.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.screen.CryptoScreen
import me.wjz.nekocrypt.ui.screen.HomeScreen
import me.wjz.nekocrypt.ui.screen.KeyScreen
import me.wjz.nekocrypt.ui.screen.SettingsScreen

//用一个枚举类定义所有的屏幕
enum class Screen(val route: String) {
    Home("home"),
    Crypto("crypto"),
    Key("key"),
    Setting("setting")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MainMenu() {
    val navController = rememberNavController()
    val navItems = remember { listOf(Screen.Home, Screen.Crypto, Screen.Key, Screen.Setting) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // 遍历导航项列表来动态创建 BottomBar 的 Item
                navItems.forEachIndexed { index, screen ->
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
                                modifier = Modifier.size(28.dp)
                            )

                            Screen.Crypto -> Icon(Icons.Outlined.Lock, contentDescription = label)
                            Screen.Key -> Icon(Icons.Outlined.Key, contentDescription = label)
                            Screen.Setting -> Icon(
                                Icons.Outlined.Settings,
                                contentDescription = label
                            )
                        }
                    }

                    NavigationBarItem(
                        icon = icon,
                        label = { Text(label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            }
        },
        //  暂时不要悬浮按钮

//        floatingActionButton =
//            {
//                FloatingActionButton(
//                    onClick = { },
//                    containerColor = MaterialTheme.colorScheme.primary,
//                    contentColor = MaterialTheme.colorScheme.onPrimary
//                ) {
//                    Icon(Icons.Default.Add, contentDescription = "Add")
//                }
//            }
    )
    { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            // 定义全局的进入和退出动画
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            // ✨ 步骤3: 在每个 composable 中定义更详细的、模拟 Pager 的动画
            val animationSpec = tween<IntOffset>(durationMillis = 300)

            // 为每个页面定义路由和动画
            navItems.forEachIndexed { index, screen ->
                composable(
                    route = screen.route,
                    enterTransition = {
                        // 根据页面索引判断是向左还是向右滑动
                        val direction =
                            if ((initialState.destination.route?.let { navItems.indexOfFirst { s -> s.route == it } }
                                    ?: -1) < index)
                                AnimatedContentTransitionScope.SlideDirection.Left
                            else
                                AnimatedContentTransitionScope.SlideDirection.Right
                        slideIntoContainer(direction, animationSpec)
                    },
                    exitTransition = {
                        val direction =
                            if ((targetState.destination.route?.let { navItems.indexOfFirst { s -> s.route == it } }
                                    ?: -1) > index)
                                AnimatedContentTransitionScope.SlideDirection.Left
                            else
                                AnimatedContentTransitionScope.SlideDirection.Right
                        slideOutOfContainer(direction, animationSpec)
                    }
                ) {
                    // 根据路由显示对应的屏幕
                    when (screen) {
                        Screen.Home -> HomeScreen()
                        Screen.Crypto -> CryptoScreen()
                        Screen.Key -> KeyScreen()
                        Screen.Setting -> SettingsScreen()
                    }
                }
            }
        }
    }
}