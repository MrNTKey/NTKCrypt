package me.wjz.nekocrypt.data

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
//建立一个LocalDataStoreManager的CompositionLocal
val LocalDataStoreManager = staticCompositionLocalOf<DataStoreManager> {
    error("No DataStoreManager provided")
}

class DataStoreManager(private val context: Context) {
    //通用的读取方法 (使用泛型)
    fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.catch { exception ->
            throw exception
        }

    }

    //通用的写入方法
    suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }

    }

    /**
     * (可选) 通用的清除单个设置的方法
     */
    suspend fun <T> clearSetting(key: Preferences.Key<T>) {
        context.dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }

    /**
     * (可选) 清除所有设置的方法
     */
    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}