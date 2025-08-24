package com.mrntkey.ntkcrypt.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.mrntkey.ntkcrypt.Constant
import com.mrntkey.ntkcrypt.CryptoMode
import com.mrntkey.ntkcrypt.NTKCryptApp
import com.mrntkey.ntkcrypt.SettingKeys
import com.mrntkey.ntkcrypt.hook.observeAsState
import com.mrntkey.ntkcrypt.service.handler.ChatAppHandler
import com.mrntkey.ntkcrypt.service.handler.QQHandler
import com.mrntkey.ntkcrypt.service.handler.WeChatHandler
import com.mrntkey.ntkcrypt.util.isSystemApp

class NCAccessibilityService : AccessibilityService() {
    val tag = "NTKCryptAccessibility"

    // 1. 创建一个 Service 自己的协程作用域，它的生命周期和 Service 绑定
    val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 添加保活服务状态标记
    private var isKeepAliveServiceStarted = false

    // 获取App里注册的dataManager实例
    private val dataStoreManager by lazy {
        (application as NTKCryptApp).dataStoreManager
    }

    // ——————————————————————————设置选项——————————————————————————

    //  所有密钥
    val cryptoKeys: Array<String> by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getKeyArrayFlow()
    }, initialValue = arrayOf(Constant.DEFAULT_SECRET_KEY))

    //  当前密钥
    val currentKey: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    }, initialValue = Constant.DEFAULT_SECRET_KEY)

    //是否开启加密功能
    val useAutoEncryption: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.USE_AUTO_ENCRYPTION, false)
    }, initialValue = false)

    //是否开启解密功能
    val useAutoDecryption: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.USE_AUTO_DECRYPTION, false)
    }, initialValue = false)

    // ✨ 新增：监听当前的“加密模式”
    val encryptionMode: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.ENCRYPTION_MODE, CryptoMode.STANDARD.key)
    }, initialValue = CryptoMode.STANDARD.key)

    // ✨ 新增：监听当前的“解密模式”
    val decryptionMode: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.DECRYPTION_MODE, CryptoMode.STANDARD.key)
    }, initialValue = CryptoMode.STANDARD.key)

    // 标准加密模式下的长按发送delay。
    val longPressDelay: Long by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.ENCRYPTION_LONG_PRESS_DELAY, 250)
    }, initialValue = 250)

    // 标准解密模式下的密文悬浮窗显示时长。
    val decryptionWindowShowTime: Long by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.DECRYPTION_WINDOW_SHOW_TIME, 1500)
    }, initialValue = 1500)

    // 沉浸式解密下密文弹窗位置的更新间隔。
    val decryptionWindowUpdateInterval: Long by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.DECRYPTION_WINDOW_POSITION_UPDATE_DELAY, 250)
    }, initialValue = 250)

    // 盖在发送按钮上的遮罩颜色。
    val sendBtnOverlayColor: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.SEND_BTN_OVERLAY_COLOR, "#5066ccff")
    }, initialValue = "#5066ccff")

    // 控制弹出图片&文件的弹窗触发用的双击时间间隔
    val showAttachmentViewDoubleClickThreshold: Long by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.SHOW_ATTACHMENT_VIEW_DOUBLE_CLICK_THRESHOLD, 250)
    }, initialValue = 250)

    // —————————————————————————— override ——————————————————————————

    // handler工厂方法
    private val handlerFactory: Map<String, () -> ChatAppHandler> = mapOf(
        QQHandler.PACKAGE_NAME to { QQHandler() },
        WeChatHandler.PACKAGE_NAME to { WeChatHandler()}
    )
    private var currentHandler: ChatAppHandler? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(tag, "无障碍服务已连接！")
        // startPeriodicScreenScan()// 做debug扫描
        // 🎯 关键：启动保活服务
        startKeepAliveService()
    }

    // ✨ 新增：重写 onDestroy 方法，这是服务生命周期结束时最后的清理机会
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "无障碍服务正在销毁...")
        // ✨ 非常重要：取消协程作用域，释放所有运行中的协程，防止内存泄漏
        serviceScope.cancel()
        // 🎯 关键：停止保活服务
        stopKeepAliveService()
    }

    override fun onInterrupt() {
        Log.w(tag, "无障碍服务被打断！")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {

        // debug逻辑，会变卡
//        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
//        ) {//点击了屏幕
//            Log.d(tag, "检测到点击事件，开始调试节点...")
//            debugNodeTree(event.source)
//        }

        val eventPackage = event.packageName?.toString() ?: "unknown" // 事件来自的包名

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
            if (activeWindowPackage!=null && currentHandler != null && currentHandler?.packageName != activeWindowPackage
                && !isSystemApp(activeWindowPackage) // 这里判断是否是系统app，直接看开头是不是com.android.provider。
            ) {
                Log.d(
                    tag,
                    "检测到用户已离开 [${currentHandler?.packageName}]，当前窗口为 [${activeWindowPackage}]。停用处理器。"
                )
                currentHandler?.onHandlerDeactivated()
                currentHandler = null
            }
            // 否则，即使收到了其他包的事件，但只要活跃窗口没变，就保持处理器不变，忽略这些“噪音”事件。
        }

        // 打印事件名
//        if (event.packageName == PACKAGE_NAME_QQ) {
//            Log.d(
//                tag,
//                "QQ事件类型: ${AccessibilityEvent.eventTypeToString(event.eventType)} | 类名: ${event.className} | 文本: ${event.text} | 描述: ${event.contentDescription}"
//            )
//        }
    }

    /**
     * 启动保活服务
     */
    private fun startKeepAliveService() {
        if (!isKeepAliveServiceStarted) {
            try {
                KeepAliveService.start(this)
                isKeepAliveServiceStarted = true
                Log.d(tag, "✅ 保活服务已启动")
            } catch (e: Exception) {
                Log.e(tag, "❌ 启动保活服务失败", e)
            }
        }
    }

    /**
     * 停止保活服务
     */
    private fun stopKeepAliveService() {
        if (isKeepAliveServiceStarted) {
            try {
                KeepAliveService.stop(this)
                isKeepAliveServiceStarted = false
                Log.d(tag, "🛑 保活服务已停止")
            } catch (e: Exception) {
                Log.e(tag, "❌ 停止保活服务失败", e)
            }
        }
    }

    // —————————————————————————— helper ——————————————————————————

    /**
     * 调试节点树的函数 (列表全扫描版)
     * 它会向上查找到列表容器(RecyclerView/ListView)，然后递归遍历并打印出该容器下所有的文本内容。
     */
    private fun debugNodeTree(sourceNode: AccessibilityNodeInfo?) {
        if (sourceNode == null) {
            Log.d(tag, "===== DEBUG NODE: 节点为空 =====")
            return
        }
        printNodeDetails(sourceNode,0)
        Log.d(tag, "===== NTKCrypt 节点调试器 (列表全扫描) =====")

        // 1. 向上查找列表容器
        var listContainerNode: AccessibilityNodeInfo? = null
        var currentNode: AccessibilityNodeInfo? = sourceNode
        for (i in 1..30) { // 增加查找深度，确保能爬到顶
            val className = currentNode?.className?.toString() ?: ""
            // 我们要找的就是这个能滚动的列表！
            if (className.contains("RecyclerView") || className.contains("ListView")) {
                listContainerNode = currentNode
                Log.d(
                    tag,
                    "🎉 找到了列表容器! Class: $className ID: ${listContainerNode?.viewIdResourceName}"
                )
                break
            }
            currentNode = currentNode?.parent
            if (currentNode == null) {
                Log.d(tag,"已找到最祖先根节点，结束循环")
                break
            } // 爬到顶了就停
        }

        // 2. 如果成功找到了列表容器，就遍历它下面的所有文本
        if (listContainerNode != null) {
            Log.d(tag, "--- 遍历列表容器 [${listContainerNode.className}] 下的所有文本 ---")
            printAllTextFromNode(listContainerNode, 0) // 从深度0开始递归
        } else {
            // 如果找不到列表，就执行一个备用方案：打印整个窗口的内容
            Log.d(tag, "警告: 未能在父节点中找到 RecyclerView 或 ListView。")
            Log.d(tag, "--- 备用方案: 遍历整个窗口的所有文本 ---")

            rootInActiveWindow?.let {
                printAllTextFromNode(it, 0)
            }
        }

        Log.d(tag, "==================================================")
    }

    /**
     * 递归辅助函数，用于深度遍历节点并打印所有非空文本。
     * @param node 当前要处理的节点。
     * @param depth 当前的递归深度，用于格式化输出（创建缩进）。
     */
    private fun printAllTextFromNode(node: AccessibilityNodeInfo, depth: Int) {
        // 根据深度创建缩进，让日志的层级关系一目了然
        val indent = "  ".repeat(depth)
        // 1. 检查当前节点本身是否有文本，如果有就打印出来
        val text = node.text
        if (!text.isNullOrEmpty()) {
            // 为了更清晰，我们把ID也打印出来
            Log.d(tag, "$indent[文本] -> '$text' (ID: ${node.viewIdResourceName})")
        }

        // 2. 遍历所有子节点，并对每个子节点递归调用自己
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                printAllTextFromNode(child, depth + 1)
            }
        }
    }

    private fun printNodeDetails(node: AccessibilityNodeInfo?, depth: Int) {
        val indent = "  ".repeat(depth)
        if (node == null) {
            Log.d(tag, "$indent[节点] -> null")
            return
        }
        val text = node.text?.toString()?.take(50)
        val desc = node.contentDescription?.toString()?.take(50)

        Log.d(tag, "$indent[文本] -> '$text'")
        Log.d(tag, "$indent[描述] -> '$desc'")
        Log.d(tag, "$indent[类名] -> ${node.className}")
        Log.d(tag, "$indent[ID]   -> ${node.viewIdResourceName}")
        Log.d(tag, "$indent[子节点数] -> ${node.childCount}")
        Log.d(tag, "$indent[父节点] -> ${node.parent?.className}")
        Log.d(tag, "$indent[属性] -> [可点击:${node.isClickable}, 可滚动:${node.isScrollable}, 可编辑:${node.isEditable}]")
    }

}

