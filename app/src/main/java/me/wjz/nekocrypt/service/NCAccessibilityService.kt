package me.wjz.nekocrypt.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.data.DataStoreManager
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.decrypt

class NCAccessibilityService : AccessibilityService() {
    private val tag = "NekoAccessibility"

    // 1. 创建一个 Service 自己的协程作用域，它的生命周期和 Service 绑定
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // 2. 声明一个 DataStoreManager 变量，我们稍后会初始化它
    private lateinit var dataStoreManager: DataStoreManager

    // [新增] 创建一个 Gson 实例，作为我们的序列化工具
    private val gson = Gson()

    // 3. 这是一个普通的类成员变量，不再是 Composable State，用来存储最新的设置值
    private var isImmersiveMode: Boolean = false

    // 所有密钥
    private var currentKeys: Array<String> = arrayOf(Constant.DEFAULT_SECRET_KEY)//给个默认值。

    override fun onCreate() {
        super.onCreate()
        // 4. 在 Service 创建时，手动创建 DataStoreManager 的实例
        //    我们把 Service 自己的 context (上下文) 传给它
        dataStoreManager = DataStoreManager(this)
        Log.d(tag, "DataStoreManager in Service has been initialized.")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(tag, "服务已连接，开始监听所有设置...")

        // 任务一：监听模式变化
        listenForModeChanges()
        // 任务二：监听密钥数组变化
        listenForKeyArrayChanges()
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        //加入一点debug逻辑，单点QQ聊天界面居然不会触发日志打印。。。
        if (event.packageName == "com.tencent.mobileqq")
            Log.i(
                tag,
                "接收到QQ的事件 -> 类型: ${AccessibilityEvent.eventTypeToString(event.eventType)}, 包名: ${event.packageName}"
            )

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {//点击了屏幕
            Log.d(tag, "检测到点击事件，开始调试节点...")
            debugNodeTree(event.source)
        }
//
//        // 根据当前模式，选择不同的事件处理逻辑
//        if (isImmersiveMode) {
//            // --- 全面模式的逻辑 ---
//            // 我们只关心窗口内容发生变化的事件，这包括滚动、刷新、新窗口等。
//            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
//                // event.source 代表触发事件的那个节点（控件）信息。
//                // 如果它不为空，我们就从这个节点开始，进行“深度优先”的递归遍历，检查屏幕上的所有文字。
//                event.source?.let { findAndProcessNodes(it) }
//            }
//        } else {
//            // --- 简约模式的逻辑 ---
//            // 我们只关心用户点击的事件。
//            if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
//                Log.d(tag, "检测到点击事件")
//                // 在简约模式下，我们只处理被用户直接点击的那个节点，效率最高。
//                event.source?.let { processSingleNode(it) }
//            }
//        }
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

    override fun onInterrupt() {
        //TODO("Not yet implemented")
    }

    override fun onDestroy() {
        super.onDestroy()
        // 7. 这是非常重要的一步！在服务销毁时，取消所有正在运行的协程，防止内存泄漏。
        serviceScope.cancel()
        Log.d(tag, "服务已销毁，所有协程已取消。")
    }

    /**
     * 递归地查找并处理一个节点及其所有的子子孙孙。
     * 这是“全面模式”的核心。
     * @param nodeInfo 我们要开始遍历的起始节点。
     */
    private fun findAndProcessNodes(nodeInfo: AccessibilityNodeInfo) {
        // 1. 先处理当前的这一个节点
        processSingleNode(nodeInfo)

        // 2. 然后，像剥洋葱一样，一层一层地处理它的所有子节点
        for (i in 0 until nodeInfo.childCount) {
            val childNode = nodeInfo.getChild(i)
            // 如果子节点不为空，就以它为新的起点，再次调用自己（这就是“递归”）
            childNode?.let { findAndProcessNodes(it) }
        }
    }

    /**
     * 处理单个节点，检查它的文本，并尝试解密和替换。
     * 这是所有模式最终都会调用的核心处理单元。
     * @param nodeInfo 要处理的单个节点。
     */
    private fun processSingleNode(nodeInfo: AccessibilityNodeInfo) {
        // 获取节点的文本内容。如果节点没有文本（比如它是一个图片），则直接返回。
        val text = nodeInfo.text?.toString() ?: return

        // 调用我们自己定义的函数，判断这段文本是不是我们感兴趣的“密文”。
        if (CryptoManager.containsCiphertext(text)) {
            Log.d(tag, "发现密文: $text")
            // 如果是密文，就调用我们自己的解密函数。
            var decryptedText: String? = null
            for (i in 0 until currentKeys.size) {//尝试用所有的密钥解密
                decryptedText = decrypt(text, currentKeys[i])
                if (decryptedText != null) break
            }
            // 尝试将解密后的明文“粘贴”到这个节点上。
            decryptedText?.let { performSetText(nodeInfo, it) }

        }
    }

    /**
     * 使用 ACTION_SET_TEXT 来直接设置文本，这是更安全、更专业的做法。
     * 它不会污染用户的剪贴板！
     * @param nodeInfo 目标节点。
     * @param text 要设置的文本。
     */
    private fun performSetText(nodeInfo: AccessibilityNodeInfo, text: String) {
        // 检查节点是否支持“设置文本”这个动作。
        if (nodeInfo.actionList.contains(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_TEXT)) {
            // 1. 创建一个 Bundle (包裹)，用来存放我们要设置的文本。
            val arguments = Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )

            // 2. 对节点下达“执行设置文本”的命令，并把装有文本的“包裹”递给它。
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(tag, "已将解密内容直接设置到节点: $text")
        } else {
            // 如果节点不支持直接设置文本（比如不可编辑的TextView），我们再考虑其他策略。
            // 比如弹窗提示，或者把解密内容复制到剪贴板（并明确告知用户）。
            Log.d(tag, "节点不支持设置文本。解密内容: $text")
        }
    }

    /**
     * 启动一个协程，专门监听“模式”设置的变化。
     */
    private fun listenForModeChanges() {
        serviceScope.launch {
            dataStoreManager.getSettingFlow(SettingKeys.IS_IMMERSIVE_MODE_KEY, false)
                .collectLatest { mode ->
                    isImmersiveMode = mode
                    Log.d(tag, "模式已更新为: ${if (isImmersiveMode) "沉浸模式" else "简约模式"}")
                }
        }
    }

    /**
     * 启动一个协程，专门监听“密钥数组”设置的变化。
     * 序列化/反序列化的逻辑现在被封装在这里。
     */
    private fun listenForKeyArrayChanges() {
        serviceScope.launch {
            dataStoreManager.getSettingFlow(
                SettingKeys.ALL_THE_KEYS,
                gson.toJson(arrayOf(Constant.DEFAULT_SECRET_KEY))
            )
                .map { jsonString ->
                    if (jsonString.isEmpty()) {
                        // 返回包含默认字符串密钥的数组
                        arrayOf(Constant.DEFAULT_SECRET_KEY)
                    } else {
                        try {
                            // [修正] 把 JSON 字符串变回 Array<String>
                            gson.fromJson(jsonString, Array<String>::class.java)
                        } catch (e: Exception) {
                            Log.e(tag, "解析密钥数组失败!", e)
                            arrayOf(Constant.DEFAULT_SECRET_KEY)
                        }
                    }
                }
                .collectLatest { keyArray ->
                    currentKeys = keyArray
                    Log.d(tag, "密钥数组已更新: ${currentKeys.joinToString()}")
                }
        }
    }

    private suspend fun saveKeysToDataStore(keys: Array<String>) {
        // 使用 Gson 将 Array<String> 序列化成 JSON 字符串
        val jsonString = gson.toJson(keys)
        dataStoreManager.saveSetting(SettingKeys.ALL_THE_KEYS, jsonString)
    }
}

