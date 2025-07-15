package me.wjz.nekocrypt.util

import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import me.wjz.nekocrypt.service.NCAccessibilityService

object PermissionUtil {
    /**
     * 检查“显示在其他应用上层”（悬浮窗）权限是否已授予。
     */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 检查我们的无障碍服务是否已启用。
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName = context.packageName + "/" + NCAccessibilityService::class.java.name
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
            stringColonSplitter.setString(enabledServices)
            while (stringColonSplitter.hasNext()) {
                val componentName = stringColonSplitter.next()
                if (componentName.equals(serviceName, ignoreCase = true)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // 忽略异常
        }
        return false
    }
}
