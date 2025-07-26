package me.wjz.nekocrypt.service

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.Constant.PACKAGE_NAME_QQ
import me.wjz.nekocrypt.CryptoMode
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.hook.observeAsState
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.service.handler.QQHandler

class NCAccessibilityService : AccessibilityService() {
    val tag = "NekoAccessibility"

    // 1. 创建一个 Service 自己的协程作用域，它的生命周期和 Service 绑定
    val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 获取App里注册的dataManager实例
    private val dataStoreManager by lazy {
        (application as NekoCryptApp).dataStoreManager
    }

    // 保活窗口
    private var keepAliveOverlay: View? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

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

    // —————————————————————————— override ——————————————————————————

    // handler工厂方法
    private val handlerFactory: Map<String, () -> ChatAppHandler> = mapOf(
        PACKAGE_NAME_QQ to { QQHandler() }
    )
    private var currentHandler: ChatAppHandler? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(tag, "无障碍服务已连接！")
        createKeepAliveOverlay()
    }

    // ✨ 新增：重写 onDestroy 方法，这是服务生命周期结束时最后的清理机会
    override fun onDestroy() {
        super.onDestroy()
        Log.d(tag, "无障碍服务正在销毁...")
        // 清理保活悬浮窗
        removeKeepAliveOverlay()
        // ✨ 非常重要：取消协程作用域，释放所有运行中的协程，防止内存泄漏
        serviceScope.cancel()
    }

    override fun onInterrupt() {
        Log.w(tag, "无障碍服务被打断！")
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

        // debug逻辑，会变卡
//        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {//点击了屏幕
//            Log.d(tag, "检测到点击事件，开始调试节点...")
//            debugNodeTree(event.source)
//        }
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
        Log.d(tag, "===== Neko 节点调试器 (列表全扫描) =====")

        // 1. 向上查找列表容器
        var listContainerNode: AccessibilityNodeInfo? = null
        var currentNode: AccessibilityNodeInfo? = sourceNode
        for (i in 1..15) { // 增加查找深度，确保能爬到顶
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
            if (currentNode == null) break // 爬到顶了就停
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


    private fun createKeepAliveOverlay() {
        if (keepAliveOverlay != null) return
        keepAliveOverlay = View(this)
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            0, 0, 0, 0, layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        try {
            windowManager.addView(keepAliveOverlay, params)
            Log.d(tag, "“保活”悬浮窗创建成功！")
        } catch (e: Exception) {
            Log.e(tag, "创建“保活”悬浮窗失败", e)
        }
    }

    private fun removeKeepAliveOverlay() {
        keepAliveOverlay?.let {
            try {
                windowManager.removeView(it)
                Log.d(tag, "“保活”悬浮窗已移除。")
            } catch (e: Exception) {
                // 忽略窗口已经不存在等异常
            } finally {
                keepAliveOverlay = null
            }
        }
    }
}

