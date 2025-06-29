package me.wjz.nekocrypt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import me.wjz.nekocrypt.data.DataStoreManager
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.ui.MainMenu
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()//让App可以上下扩展到最顶端和最低端

        //这是从传统 Android 视图系统切换到 Jetpack Compose 世界的“传送门”！
        // 一旦调用了它，你就可以在这个大括号 {} 里面，用我们之前学过的 @Composable 函数来描绘你的 App 界面了。
        setContent {

            //在这里创建dataStoreManager的唯一实例，用remember确保视图重组时不会被重新创建
            val dataStoreManager = remember { DataStoreManager(applicationContext) }
            NekoCryptTheme {
                CompositionLocalProvider(LocalDataStoreManager provides dataStoreManager) {
                    MainMenu()
                }
            }
        }
    }
}
