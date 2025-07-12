package me.wjz.nekocrypt

import android.app.Application
import me.wjz.nekocrypt.data.DataStoreManager

class NekoCryptApp: Application() {
    // 在 Application 创建时，我们懒加载地创建 DataStoreManager 的实例。
    // 它只会被创建一次！
    val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(this)
    }
}