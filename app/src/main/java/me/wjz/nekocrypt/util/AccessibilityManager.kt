package me.wjz.nekocrypt.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 一个 Composable 函数，用于记住并监听无障碍服务的开启状态。
 * 当应用从后台返回前台时（例如，用户在设置页开启权限后返回），它会自动刷新状态。
 *
 * @param context 上下文环境。
 * @param serviceClass 你的无障碍服务的类名，例如：MyAccessibilityService::class.java。
 * @return 一个 State<Boolean> 对象，实时代表着服务是否开启。
 */
@Composable
fun rememberAccessibilityServiceState(
    context: Context,
    serviceClass: Class<out AccessibilityService>
): State<Boolean> {
    val accessibilityState= remember{ mutableStateOf(isAccessibilityServiceEnabled(context, serviceClass)) }
    // 2. 获取当前 Composable 的生命周期所有者
    val lifecycleOwner = LocalLifecycleOwner.current
    // 3. 使用 DisposableEffect 来添加和移除生命周期观察者，防止内存泄漏
    DisposableEffect(lifecycleOwner) {
        // 创建一个观察者
        val observer = LifecycleEventObserver { _, event ->
            // 当生命周期事件为 ON_RESUME (恢复) 时，说明界面回到了前台
            if (event == Lifecycle.Event.ON_RESUME) {
                // 重新检查一次无障碍权限的状态，并更新 state
                accessibilityState.value = isAccessibilityServiceEnabled(context, serviceClass)
            }
        }

        // 将观察者添加到生命周期中
        lifecycleOwner.lifecycle.addObserver(observer)

        // onDispose 会在 Composable 离开屏幕时被调用
        onDispose {
            // 从生命周期中移除观察者，避免内存泄漏
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // 4. 返回这个 state，UI 可以订阅它的变化
    return accessibilityState
}


/**
 * 检查指定的无障碍服务是否已经启用。
 *
 * @param context 上下文环境。
 * @param serviceClass 你的无障碍服务的类名。
 * @return 如果服务已启用，返回 true，否则返回 false。
 */
fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    // 构造服务的唯一标识符，格式为 "包名/类全名"
    val serviceId = "${context.packageName}/${serviceClass.name}"
    try {
        // 从系统设置中获取当前所有已启用的无障碍服务列表字符串
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        // 使用 TextUtils.SimpleStringSplitter 来安全地分割字符串
        val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
        if (enabledServices != null) {
            stringColonSplitter.setString(enabledServices)
            while (stringColonSplitter.hasNext()) {
                val componentName = stringColonSplitter.next()
                if (componentName.equals(serviceId, ignoreCase = true)) {
                    return true
                }
            }
        }
    } catch (e: Exception) {
        // 发生异常时，默认返回 false
        e.printStackTrace()
    }
    return false
}

/**
 * 创建一个意图(Intent)并跳转到系统的无障碍功能设置页面。
 *
 * @param context 上下文环境。
 */
fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    // 确保在 Activity 栈外启动新的任务
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}