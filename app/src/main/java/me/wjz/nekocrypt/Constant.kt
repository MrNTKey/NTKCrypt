package me.wjz.nekocrypt

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object Constant {
    const val APP_NAME = "NekoCrypt"
    const val DEFAULT_SECRET_KEY = "20040821"//You know what it means.
    const val ID_QQ_SEND_BTN="com.tencent.mobileqq:id/send_btn"
    const val ID_QQ_INPUT="com.tencent.mobileqq:id/input"
}

object SettingKeys {
    val IS_GLOBAL_ENCRYPTION_MODE = booleanPreferencesKey("global_encryption_enabled")
    val CURRENT_KEY = stringPreferencesKey("current_key")
    val IS_IMMERSIVE_MODE= booleanPreferencesKey("is_immersive_mode")
    // 用 String 类型的 Key 来存储序列化后的密钥数组
    val ALL_THE_KEYS = stringPreferencesKey("all_the_keys")
    val USE_AUTO_ENCRYPTION = booleanPreferencesKey("use_auto_encryption")
    val PACKAGE_NAME_QQ ="com.tencent.mobileqq"
}

object CommonKeys {

}