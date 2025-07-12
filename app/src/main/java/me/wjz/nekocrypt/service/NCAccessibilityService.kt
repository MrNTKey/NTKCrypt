package me.wjz.nekocrypt.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.hook.observeAsState
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.decrypt
import me.wjz.nekocrypt.util.CryptoManager.encrypt

class NCAccessibilityService : AccessibilityService() {
    private val tag = "NekoAccessibility"

    // 1. 创建一个 Service 自己的协程作用域，它的生命周期和 Service 绑定
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // [已修正] 通过 applicationContext 来安全地获取全局唯一的实例！
    private val dataStoreManager by lazy {
        (application as NekoCryptApp).dataStoreManager
    }

    private val isImmersiveMode: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.IS_IMMERSIVE_MODE, false)
    }, initialValue = false)
    private val cryptoKeys: Array<String> by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getKeyArrayFlow()
    }, initialValue = arrayOf(Constant.DEFAULT_SECRET_KEY))
    private val currentKey: String by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    }, initialValue = Constant.DEFAULT_SECRET_KEY)

    //设置项，是否开启自动加密
    private val useAutoEncryption: Boolean by serviceScope.observeAsState(flowProvider = {
        dataStoreManager.getSettingFlow(SettingKeys.USE_AUTO_ENCRYPTION, false)
    }, initialValue = false)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(tag, "无障碍服务已连接！")
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        //加入一点debug逻辑
        if (event.packageName == "com.tencent.mobileqq")
            Log.i(
                tag,
                "接收到QQ的事件 -> 类型: ${AccessibilityEvent.eventTypeToString(event.eventType)}, 包名: ${event.packageName}"
            )

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {//点击了屏幕
            Log.d(tag, "检测到点击事件，开始调试节点...")
            debugNodeTree(event.source)
        }

        //尝试做自动加密消息
        if (useAutoEncryption) {
            handleAutoEncryption(event)
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

    private fun findNodeAtRecursive(
        node: AccessibilityNodeInfo,
        x: Int,
        y: Int,
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.contains(x, y)) {
            // 如果坐标在这个节点内部，我们还要继续往下找，找到最小的那个子节点
            for (i in 0 until node.childCount) {
                val child = findNodeAtRecursive(node.getChild(i), x, y)
                if (child != null) {
                    return child // 找到了更深的子节点，返回它
                }
            }
            return node // 没有更深的子节点了，那目标就是我
        }
        return null
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
            for (i in 0 until cryptoKeys.size) {//尝试用所有的密钥解密
                decryptedText = decrypt(text, cryptoKeys[i])
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
     * 处理自动加密的核心逻辑。
     * @param event 接收到的无障碍事件。
     */
    private fun handleAutoEncryption(event: AccessibilityEvent) {
        // 步骤 1: 我们只关心 QQ 中发生的“长按点击”事件
        // 为什么不直接修改点击？因为实际上收到消息的时候，就已经处理完了，捕获到输入栏的内容是空的。
        // 硬要做也不是不行不过我感觉有点麻烦，长按挺好的，这样方便区分密文发送和正常发送。
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED || event.packageName != "com.tencent.mobileqq") {
            return
        }

        val sourceNode = event.source ?: return

        // 步骤 2: 判断被点击的节点是不是我们关心的“发送”按钮
        if (sourceNode.viewIdResourceName == Constant.ID_QQ_SEND_BTN) {
            Log.d(tag, "检测到QQ发送按钮被点击，启动自动加密流程...")

            // 步骤 3: 找到聊天界面的输入框。
            // 输入框和发送按钮不是直接的父子关系，所以需要从整个窗口的“根节点”开始查找。
            val rootNode = rootInActiveWindow ?: return
            // 使用我们新增的辅助函数来查找
            val inputNode = findNodeById(rootNode, Constant.ID_QQ_INPUT)

            if (inputNode == null) {
                Log.w(tag, "自动加密失败：未能在当前窗口找到QQ输入框节点。")
                return
            }

            // 步骤 4: 获取输入框里的原始文本
            val originalText = inputNode.text?.toString()

            Log.d(tag, "原文: '$originalText'，准备加密...")

            // 步骤 5: 进行关键判断，防止死循环或无效操作
            // - 文本不能为空
            // - 文本不能已经是加密过的（防止将密文再次加密）
            if (!originalText.isNullOrEmpty() && !CryptoManager.containsCiphertext(originalText)) {
                // 步骤 6: 使用你的加密管理器和当前的第一把密钥进行加密
                val encryptedText = encrypt(originalText, currentKey)

                // 步骤 7: 将加密后的密文，填写回输入框
                performSetText(inputNode, encryptedText)

                // 步骤 8: 【最关键的一步】代替用户，再次点击“发送”按钮
                // 这样，发送出去的就是我们刚刚填入的密文了。
                sourceNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(tag, "已将密文填入输入框并再次触发发送操作。")

            } else {
                // 如果输入框是空的，或者里面已经是密文了，那我们什么都不做，让它正常发送。
                Log.d(tag, "输入框为空或内容已是密文，不执行自动加密。")
            }
        }
    }

    /**
     * 辅助函数：通过资源ID在指定节点树下查找子节点。
     * @param rootNode 从哪个节点开始查找。
     * @param viewId 要查找的 View ID，例如 "com.tencent.mobileqq:id/input"。
     * @return 找到的第一个匹配节点，如果没找到则返回 null。
     */
    private fun findNodeById(
        rootNode: AccessibilityNodeInfo,
        viewId: String,
    ): AccessibilityNodeInfo? {
        // 直接使用系统提供的 findAccessibilityNodeInfosByViewId API，它性能很高。
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        return if (nodes.isNotEmpty()) {
            nodes[0] // 通常一个界面内ID是唯一的，我们直接返回列表中的第一个。
        } else {
            null // 如果列表是空的，说明没找到。
        }
    }
}

