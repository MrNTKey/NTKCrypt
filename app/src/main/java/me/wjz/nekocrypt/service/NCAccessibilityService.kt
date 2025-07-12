package me.wjz.nekocrypt.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.Constant.PACKAGE_NAME_QQ
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.hook.observeAsState
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.service.handler.QQHandler

class NCAccessibilityService : AccessibilityService() {
    private val tag = "NekoAccessibility"

    // 1. 创建一个 Service 自己的协程作用域，它的生命周期和 Service 绑定
    val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 获取App里注册的dataManager实例
    private val dataStoreManager by lazy {
        (application as NekoCryptApp).dataStoreManager
    }

    private val isImmersiveMode: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.IS_IMMERSIVE_MODE, false)
    }, initialValue = false)
    private val cryptoKeys: Array<String> by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getKeyArrayFlow()
    }, initialValue = arrayOf(Constant.DEFAULT_SECRET_KEY))
    val currentKey: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    }, initialValue = Constant.DEFAULT_SECRET_KEY)

    // handler工厂方法
    private val handlerFactory: Map<String, () -> ChatAppHandler> = mapOf(
        PACKAGE_NAME_QQ to { QQHandler() }
    )
    private var currentHandler: ChatAppHandler? = null

    //设置项，是否开启自动加密
    val useAutoEncryption: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.USE_AUTO_ENCRYPTION, false)
    }, initialValue = false)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(tag, "无障碍服务已连接！")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName == null) return

        //加入一点debug逻辑
        if (event.packageName == PACKAGE_NAME_QQ)
            Log.i(
                tag,
                "接收到QQ的事件 -> 类型: ${AccessibilityEvent.eventTypeToString(event.eventType)}, 包名: ${event.packageName}"
            )

        val eventPackage = event.packageName.toString()

        // 检查是否需要切换处理器
        if (currentHandler?.packageName != eventPackage) {
            // 如果旧的处理器存在，则停用它
            currentHandler?.onHandlerDeactivated()

            // 查找并激活新的处理器
            currentHandler = handlerFactory[event.packageName.toString()]?.invoke()
            currentHandler?.onHandlerActivated(this)
        }
        // 将事件分发给当前激活的处理器
        currentHandler?.onAccessibilityEvent(event, this)
    }

    override fun onInterrupt() {
        Log.w(tag, "无障碍服务被打断！")
    }

}

