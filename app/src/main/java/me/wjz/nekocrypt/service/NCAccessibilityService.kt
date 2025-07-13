package me.wjz.nekocrypt.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    val isImmersiveMode: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.IS_IMMERSIVE_MODE, false)
    }, initialValue = false)
    val cryptoKeys: Array<String> by serviceScope.observeAsState(flowProvider = {
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

        val eventPackage = event.packageName.toString() // 事件来自的包名

        // 情况一：事件来自我们支持的应用
        if (handlerFactory.containsKey(eventPackage)) {
            // 如果当前没有处理器，或者处理器不是对应这个App的，就进行切换
            if (currentHandler?.packageName != eventPackage) {
                currentHandler?.onHandlerDeactivated()
                currentHandler = handlerFactory[eventPackage]?.invoke()
                currentHandler?.onHandlerActivated(this)
            }

            // 将事件分发给当前处理器
            currentHandler?.onAccessibilityEvent(event, this)
        }
        // 情况二：事件来自我们不支持的应用
        else {

            // 关键逻辑：只有当我们的处理器正在运行，并且当前活跃窗口已经不是它负责的应用时，才停用它
            val activeWindowPackage = rootInActiveWindow?.packageName?.toString()
            if (currentHandler != null && currentHandler?.packageName != activeWindowPackage) {
                Log.d(
                    tag,
                    "检测到用户已离开 [${currentHandler?.packageName}]，当前窗口为 [${activeWindowPackage}]。停用处理器。"
                )
                currentHandler?.onHandlerDeactivated()
                currentHandler = null
            }
            // 否则，即使收到了其他包的事件，但只要活跃窗口没变，就保持处理器不变，忽略这些“噪音”事件。
        }

        // debug逻辑
//        if (event.packageName == PACKAGE_NAME_QQ)
//            Log.i(tag,
//                "接收到QQ的事件 -> 类型: ${AccessibilityEvent.eventTypeToString(event.eventType)}, 包名: ${event.packageName}"
//            )
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {//点击了屏幕
            Log.d(tag, "检测到点击事件，开始调试节点...")
            debugNodeTree(event.source)
        }
    }

    override fun onInterrupt() {
        Log.w(tag, "无障碍服务被打断！")
    }

    /**
     * 调试节点树的函数
     */
    private fun debugNodeTree(sourceNode: AccessibilityNodeInfo?) {
        if (sourceNode == null) {
            Log.d(tag, "===== DEBUG NODE: 节点为空 =====")
            return
        }
        // 使用一个醒目的分隔符，方便在 Logcat 中查找
        Log.d(tag, "===== Neko 节点调试器 =====")

        // 1. 打印被点击的节点本身的信息
        Log.d(tag, "[点击的节点] -> ${getNodeDescription(sourceNode)}")
        // 2. 打印它的父节点信息
        val parentNode = sourceNode.parent
        if (parentNode != null) {
            Log.d(tag, "[父节点]   -> ${getNodeDescription(parentNode)}")
        } else {
            Log.d(tag, "[父节点]   -> (无父节点)")
        }
        // 3. 遍历并打印它的所有直接子节点信息
        if (sourceNode.childCount > 0) {
            Log.d(tag, "--- 子节点列表 (共 ${sourceNode.childCount} 个) ---")
            for (i in 0 until sourceNode.childCount) {
                val childNode = sourceNode.getChild(i)
                if (childNode != null) {
                    Log.d(tag, "[子节点 $i] -> ${getNodeDescription(childNode)}")
                }
            }
        } else {
            Log.d(tag, "--- (无子节点) ---")
        }

        Log.d(tag, "==========================")
    }

    /**
     * 辅助函数，用来格式化节点的描述信息，方便阅读。
     */
    private fun getNodeDescription(node: AccessibilityNodeInfo): String {
        // 我们把最关键的几个属性都打印出来
        return "类名: ${node.className}, 文本: '${node.text}', 描述: '${node.contentDescription}', ID: ${node.viewIdResourceName}"
    }
}

