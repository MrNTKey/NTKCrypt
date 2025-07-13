package me.wjz.nekocrypt.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

//建立一个LocalDataStoreManager的CompositionLocal，专门给ComposeUI用的
val LocalDataStoreManager = staticCompositionLocalOf<DataStoreManager> {
    error("No DataStoreManager provided")
}

class DataStoreManager (private val context: Context) {

    //通用的读取方法 (使用泛型)
    fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.catch { exception -> throw exception }
    }

    //通用的写入方法
    suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    /**
     * (可选) 通用的清除单个设置的方法
     */
    suspend fun <T> clearSetting(key: Preferences.Key<T>) {
        context.dataStore.edit { preferences -> preferences.remove(key) }
    }

    /**
     * (可选) 清除所有设置的方法
     */
    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences -> preferences.clear() }
    }

    /**
     * 保存密钥数组。
     * 调用者只需要传入一个数组，无需关心JSON转换的细节。
     */
    suspend fun saveKeyArray(keys: Array<String>) {
        val jsonString = JSON.toJSONString(keys)
        saveSetting(SettingKeys.ALL_THE_KEYS, jsonString)
    }

    /**
     * 获取密钥数组
     */
    fun getKeyArrayFlow(): Flow<Array<String>> {
        return getSettingFlow(SettingKeys.ALL_THE_KEYS, "[]").map { jsonString ->
            if (jsonString.isEmpty()) arrayOf(Constant.DEFAULT_SECRET_KEY)
            else {
                try {
                    JSON.parseObject(jsonString, Array<String>::class.java)
                } catch (e: Exception) {
                    Log.e("Neko", "解析密钥数组失败!", e)
                    arrayOf(Constant.DEFAULT_SECRET_KEY) //解析失败返回默认值
                }
            }
        }

    }
}