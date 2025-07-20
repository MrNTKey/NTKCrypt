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
    val decryptionWindowShowTime:Long by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.DECRYPTION_WINDOW_SHOW_TIME, 1500)
    }, initialValue = 1500)

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


    // —————————————————————————— helper ——————————————————————————

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

