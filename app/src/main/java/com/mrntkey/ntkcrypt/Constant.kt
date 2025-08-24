package com.mrntkey.ntkcrypt

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

object Constant {
    const val APP_NAME = "NTKCrypt"
    const val DEFAULT_SECRET_KEY = "20040821"//You know what it means...

    // ---- 其他 ----
    const val EDIT_TEXT="EditText"
    const val VIEW_ID_BTN = "Button"
}

object SettingKeys {
    val IS_GLOBAL_ENCRYPTION_MODE = booleanPreferencesKey("global_encryption_enabled")
    val CURRENT_KEY = stringPreferencesKey("current_key")
    val KEY_HISTORY_LIST = stringSetPreferencesKey("key_history_list") // 新增，用于存储密钥列表
    // 用 String 类型的 Key 来存储序列化后的密钥数组
    val ALL_THE_KEYS = stringPreferencesKey("all_the_keys")
    val USE_AUTO_ENCRYPTION = booleanPreferencesKey("use_auto_encryption")
    val USE_AUTO_DECRYPTION = booleanPreferencesKey("use_auto_decryption")
    val ENCRYPTION_MODE = stringPreferencesKey("encryption_mode")
    val DECRYPTION_MODE = stringPreferencesKey("decryption_mode")
    // 标准加密模式下，长按时间设置
    val ENCRYPTION_LONG_PRESS_DELAY = longPreferencesKey("encryption_long_press_delay")
    // 标准解密模式下，悬浮窗的显示时间设置
    val DECRYPTION_WINDOW_SHOW_TIME = longPreferencesKey("decryption_window_show_time")
    // 沉浸式解密下密文弹窗位置更新间隔
    val DECRYPTION_WINDOW_POSITION_UPDATE_DELAY = longPreferencesKey("decryption_window_position_update_delay")
    // 按钮遮罩的颜色
    val SEND_BTN_OVERLAY_COLOR = stringPreferencesKey("send_btn_overlay_color")
    // 控制弹出发送图片or文件视图的双击最大间隔时间
    val SHOW_ATTACHMENT_VIEW_DOUBLE_CLICK_THRESHOLD = longPreferencesKey("show_attachment_view_double_click_threshold")
}

object CommonKeys {
    const val ENCRYPTION_MODE_STANDARD = "standard"
    const val ENCRYPTION_MODE_IMMERSIVE = "immersive"
    const val DECRYPTION_MODE_STANDARD = "standard"
    const val DECRYPTION_MODE_IMMERSIVE = "immersive"
}

enum class CryptoMode(val key: String, @StringRes val labelResId: Int){
    STANDARD("standard", R.string.mode_standard),
    IMMERSIVE("immersive", R.string.mode_immersive);

    companion object {
        /**
         * 一个辅助函数，可以根据存储的 key 安全地找回对应的枚举实例。
         * 如果找不到，就返回一个默认值。
         */
        fun fromKey(key: String?): CryptoMode {
            // entries 是一个由编译器自动生成的属性，包含了枚举的所有实例
            return entries.find { it.key == key } ?: STANDARD
        }
    }
}