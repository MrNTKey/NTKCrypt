package me.wjz.nekocrypt.service.handler

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.CryptoMode
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.ui.DecryptionPopupContent
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.appendNekoTalk
import me.wjz.nekocrypt.util.WindowPopupManager

abstract class BaseChatAppHandler : ChatAppHandler {
    protected val tag = "NCBaseHandler"

    // 由子类提供具体应用的ID
    abstract override val inputId: String
    abstract override val sendBtnId: String
    abstract override val messageTextId: String
    abstract override val messageListClassName: String

    // 处理器内部状态
    private var service: NCAccessibilityService? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayManagementJob: Job? = null

    // 为我们的界面节点变量做缓存
    private var cachedSendBtnNode: AccessibilityNodeInfo? = null
    private var cachedInputNode: AccessibilityNodeInfo? = null
    private var cachedMessageListNode: AccessibilityNodeInfo? =
        null// 为 RecyclerView/ListView 创建一个专属的缓存

    // ✨ 新增：为沉浸式解密创建一个“防抖”任务，避免过于频繁的扫描
    private var immersiveDecryptionJob: Job? = null
    private val immersiveDecryptionCache = mutableMapOf<String, Long>()

    private val colorInt = "#5066ccff".toColorInt() //debug的时候调成可见色，正式环境应该是纯透明

    /**
     * 根据发送按钮的矩形区域，基于每个APP定向创建加密悬浮窗的布局参数。
     *
     * @param anchorRect 发送按钮在屏幕上的位置和大小。
     * @return 配置好的 WindowManager.LayoutParams 对象。
     */
    abstract fun getOverlayLayoutParams(
        anchorRect: Rect,
    ): WindowManager.LayoutParams

    // 根据不同APP定向修改解密弹窗的window位置。
    abstract fun modifyDecryptionWindowRect(rect: Rect): Rect


    override fun onAccessibilityEvent(event: AccessibilityEvent, service: NCAccessibilityService) {
        // 悬浮窗管理逻辑
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (service.useAutoEncryption) {
                //只有开启加密，才会加上悬浮窗，每次事件改变，都要更新悬浮窗位置
                overlayManagementJob?.cancel()
                overlayManagementJob = service.serviceScope.launch(Dispatchers.Default) {
                    handleOverlayManagement()   // 可能是添加、更新、删除悬浮窗
                }
            } else {
                removeOverlayView()
            }
        }

        // 解密逻辑，开启解密，才进行解密操作。
        if (service.useAutoDecryption) {
            when (service.decryptionMode) {
                // 标准模式：用户点击密文时，才进行解密
                CryptoMode.STANDARD.key -> {
                    if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        handleDecryption(event.source)
                    }
                }
                // 沉浸模式：当窗口内容变化时，主动扫描并解密
                CryptoMode.IMMERSIVE.key -> {
                    if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                        //带防抖处理
                        immersiveDecryptionJob?.cancel()
                        // 启动一个新的扫描任务
                        immersiveDecryptionJob = service.serviceScope.launch(Dispatchers.Default) {
                            // ✨ 等待300毫秒，如果在这期间又有新的事件进来，这个任务就会被取消
                            delay(250L)
                            Log.d(tag, "UI稳定，开始执行沉浸式解密...")
                            performImmersiveDecryption()
                        }
                    }
                }
            }
        }

    }

    // 启动服务
    override fun onHandlerActivated(service: NCAccessibilityService) {
        this.service = service
        this.windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(tag, "激活$packageName 处理器。")
    }

    // 销毁服务
    override fun onHandlerDeactivated() {
        overlayManagementJob?.cancel()
        cachedSendBtnNode = null
        cachedInputNode = null
        removeOverlayView {
            // 在视图置空后，其他引用量也要置为空，方便gc回收
            this.service = null
            this.windowManager = null
            Log.d(tag, "取消$packageName 处理器。")
        }
    }

    /**
     * 处理节点检查是否需要解密。
     * @param sourceNode 用户点击的源节点。
     */
    private fun handleDecryption(
        sourceNode: AccessibilityNodeInfo?,
        isShowLongTime: Boolean = false,
    ) {
        val node = sourceNode ?: return
        val text = node.text?.toString() ?: return
        val currentService = service ?: return

        // 1. 判断节点文本是否包含我们的“猫语”
        if (CryptoManager.containsCiphertext(text)) {
            Log.d(tag, "检测到密文: $text")
            // 2. 尝试用所有密钥进行解密
            Log.d(tag, "目前的全部密钥${currentService.cryptoKeys.joinToString()}")
            for (key in currentService.cryptoKeys) {
                val decryptedText = CryptoManager.decrypt(text, key)
                // 3. 只要有一个密钥解密成功...
                if (decryptedText != null) {
                    Log.d(tag, "解密成功 -> $decryptedText")
                    // ...就立刻显示我们的解密弹窗！
                    showDecryptionPopup(
                        decryptedText,
                        node,
                        if (isShowLongTime) 60 else currentService.decryptionWindowShowTime
                    )
                    break // 停止尝试其他密钥
                }
            }
        }
    }

    /**
     * 执行沉浸式解密相关逻辑。
     */
    private fun performImmersiveDecryption() {
        val currentService = service ?: return
        // ✨ 1. 优先信任缓存：检查消息列表节点的缓存是否依然有效
        val messageList = if (isNodeValid(cachedMessageListNode)) {
            cachedMessageListNode
        } else {
            // ✨ 2. 缓存无效，则在整个窗口中查找一次列表容器，并存入缓存
            findMessageListContainer(currentService.rootInActiveWindow)?.also {
                Log.d(tag, "找到了新的消息列表容器并已缓存！")
                cachedMessageListNode = it
            }
        }
        // 3. 如果最终我们拥有了一个有效的列表容器...
        if (messageList != null) {
            val messageNodes = messageList.findAccessibilityNodeInfosByViewId(messageTextId)
            if (messageNodes.isNullOrEmpty()) return

            // ✨ --- 关键优化 2：将UI更新任务“分批”处理 --- ✨
            // 我们不再是在后台循环里疯狂地向主线程扔任务，
            // 而是先在后台把所有需要解密的“任务清单”准备好。
            val tasks = mutableListOf<Pair<String, AccessibilityNodeInfo>>()
            for (node in messageNodes) {
                val text = node.text?.toString()
                if (text.isNullOrEmpty() || !CryptoManager.containsCiphertext(text)) continue

                val nodeBounds = Rect()
                node.getBoundsInScreen(nodeBounds)
                val cacheKey = "${nodeBounds}_${text}"
                val lastDecryptionTime = immersiveDecryptionCache[cacheKey]
                val currentTime = System.currentTimeMillis()

                if (lastDecryptionTime == null || currentTime - lastDecryptionTime > 5000) {
                    for (key in currentService.cryptoKeys) {
                        val decryptedText = CryptoManager.decrypt(text, key)
                        if (decryptedText != null) {
                            // 只是把任务加到清单里，不做任何UI操作
                            tasks.add(decryptedText to node)
                            immersiveDecryptionCache[cacheKey] = currentTime
                            break
                        }
                    }
                }
            }

            // ✨ 当所有后台计算都完成后，我们只启动一个协程，
            // 让“服务员”按照自己的节奏，从容地、一个一个地去上菜。
            if (tasks.isNotEmpty()) {
                currentService.serviceScope.launch(Dispatchers.Main) {
                    for ((decryptedText, node) in tasks) {
                        showDecryptionPopup(
                            decryptedText,
                            node,
                            currentService.decryptionWindowShowTime
                        )
                        // ✨ 每显示一个弹窗，就“喘口气”（让出一帧），
                        // 把执行机会让给动画渲染等更重要的任务。
                        delay(32L) // 约等于2帧的时间
                    }
                }
            }
        } else {
            Log.w(tag, "未找到消息列表容器，无法执行沉浸式解密。")
        }
    }

    /**
     * ✨ 新增：一个专门用来查找消息列表容器的辅助函数。
     * 它使用广度优先搜索来查找第一个符合条件的节点。
     */
    private fun findMessageListContainer(rootNode: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (rootNode == null) return null

        val nodesToProcess = ArrayDeque<AccessibilityNodeInfo>()
        nodesToProcess.add(rootNode)

        while (nodesToProcess.isNotEmpty()) {
            val node = nodesToProcess.removeFirst()
            if (node.className?.toString()?.contains(messageListClassName) == true) {
                return node // 找到了！
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { nodesToProcess.add(it) }
            }
        }
        return null // 没找到
    }

    private fun showDecryptionPopup(
        decryptedText: String,
        anchorNode: AccessibilityNodeInfo,
        showTime: Long,
    ) {
        val currentService = service ?: return

        val anchorRect = Rect()
        anchorNode.getBoundsInScreen(anchorRect)

        // 每个弹窗都有自己的管理器实例。
        var popupManager: WindowPopupManager? = null
        // 创建并显示我们的弹窗
        popupManager = WindowPopupManager(
            context = currentService,
            onDismissRequest = { popupManager = null },// 关闭时清理引用
            anchorRect = modifyDecryptionWindowRect(anchorRect)
        ) {
            // 把UI内容传进去
            DecryptionPopupContent(
                text = decryptedText,
                onDismiss = { popupManager?.dismiss() },
                durationMills = showTime
            )
        }
        popupManager?.show()
    }


    // --- 所有悬浮窗和加密逻辑都内聚在这里 ---

    /**
     * ✨ 终极形态的悬浮窗管理逻辑 ✨
     * 先检查缓存，再搜索
     */
    protected fun handleOverlayManagement() {
        // delay(50)
        var sendBtnNode: AccessibilityNodeInfo?

        // 1. 优先信任缓存：用廉价的 refresh() 给缓存“体检”
        if (isNodeValid(cachedSendBtnNode)) {
            sendBtnNode = cachedSendBtnNode
        }
        // 2. 缓存无效，才启动昂贵的“全楼大搜查”
        else {
            val rootNode = service?.rootInActiveWindow ?: return
            sendBtnNode = findNodeById(rootNode, sendBtnId)
            // 搜索到了就更新缓存，为下一次做准备
            cachedSendBtnNode = sendBtnNode
        }

        // 3. 根据最终的节点状态来决定如何操作
        if (sendBtnNode != null) {
            val rect = Rect()
            sendBtnNode.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                createOrUpdateOverlayView(rect)
            } else {
                // 节点虽然存在，但没有实际尺寸，也视为无效
                removeOverlayView()
            }
        } else {
            // 如果最终还是没有有效节点，就清理
            removeOverlayView()
        }
    }

    /**
     * @param rect 悬浮窗的目标位置和大小。
     */
    protected fun createOrUpdateOverlayView(rect: Rect) {
        val currentService = service ?: return
        //  绘制悬浮窗位置所需要用到的参数
        val params = getOverlayLayoutParams(rect)
        currentService.serviceScope.launch(Dispatchers.Main) {
            if (overlayView == null) {
                overlayView = View(currentService).apply {
                    setBackgroundColor(colorInt)

                    // ✨ 1. 首先，为视图定义一个标准的“单击”行为
                    // 这就是我们的“标准门铃按钮”。
                    setOnClickListener {
                        // 标准模式下，短按执行普通发送
                        if (currentService.encryptionMode == CryptoMode.STANDARD.key) {
                            Log.d(currentService.tag, "标准模式短按，执行普通发送！")
                            doNormalClick()
                        }
                        // 沉浸模式下，短按（单击）也执行加密发送
                        else if (currentService.encryptionMode == CryptoMode.IMMERSIVE.key) {
                            Log.d(currentService.tag, "沉浸模式点击，执行加密！")
                            doEncryptAndClick()
                        }
                    }

                    // ✨ 2. 然后，我们只用 onTouch 来“监听”手势，特别是长按
                    var longPressJob: Job? = null
                    setOnTouchListener { v, event -> // 'v' 就是这个 View 本身
                        // 只在标准模式下才需要区分长按和短按
                        if (currentService.encryptionMode == CryptoMode.STANDARD.key) {
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> {
                                    longPressJob = currentService.serviceScope.launch {
                                        delay(currentService.longPressDelay) // 长按阈值
                                        Log.d(currentService.tag, "标准模式长按，执行加密！")
                                        doEncryptAndClick()
                                    }
                                    true // 我们要处理后续事件
                                }

                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    // 如果手指抬起时，长按任务还在“准备中”...
                                    if (longPressJob?.isActive == true) {
                                        // ...说明这是一个短按，取消长按任务...
                                        longPressJob.cancel()
                                        // ✨ ...然后“按响”那个标准的门铃！
                                        v.performClick()
                                    }
                                    // 如果长按任务已经执行或被取消，这里就什么都不做
                                    true
                                }

                                else -> false
                            }
                        } else {
                            // 在沉浸模式下，我们让标准的 OnClickListener 去处理所有点击
                            // onTouch 只需要返回 false，表示“我不管，让别人来处理”
                            false
                        }
                    }
                }
                windowManager?.addView(overlayView, params)
            } else {
                windowManager?.updateViewLayout(overlayView, params)
            }
        }
    }

    // 移除悬浮窗
    protected fun removeOverlayView(onComplete: (() -> Unit)? = null) {
        service?.serviceScope?.launch(Dispatchers.Main) {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null

                onComplete?.invoke()
            }
        }
    }

    // 自动加密并发送消息
    protected fun doEncryptAndClick() {
        val currentService = service ?: return

        // 1. 获取发送按钮节点 (优先从缓存，不行再找)
        if (!isNodeValid(cachedSendBtnNode)) {
            val root = currentService.rootInActiveWindow ?: return
            cachedSendBtnNode = findNodeById(root, sendBtnId)
        }
        val sendBtnNode = cachedSendBtnNode ?: return

        // 2. 获取输入框节点 (逻辑同上)
        if (!isNodeValid(cachedInputNode)) {
            val root = currentService.rootInActiveWindow ?: return
            cachedInputNode = findNodeById(root, inputId)
        }
        val inputNode = cachedInputNode ?: return

        // 3. 执行核心加密逻辑
        val originalText = inputNode.text?.toString()
        if (originalText.isNullOrEmpty() || CryptoManager.containsCiphertext(originalText)) {
            sendBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val encryptedText =
                CryptoManager.encrypt(originalText, currentService.currentKey).appendNekoTalk()
            performSetText(inputNode, encryptedText)
            currentService.serviceScope.launch {
                delay(50)
                sendBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
    }

    // 普通点击的发送逻辑 (用于标准模式的短按)
    protected fun doNormalClick() {
        if (!isNodeValid(cachedSendBtnNode)) {
            val root = service?.rootInActiveWindow ?: return
            cachedSendBtnNode = findNodeById(root, sendBtnId)
        }
        cachedSendBtnNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    protected fun findNodeById(
        rootNode: AccessibilityNodeInfo,
        viewId: String,
    ): AccessibilityNodeInfo? {
        return rootNode.findAccessibilityNodeInfosByViewId(viewId).firstOrNull()
    }

    /**
     * 使用 ACTION_SET_TEXT 来直接设置文本，这是更安全、更专业的做法。
     * 它不会污染用户的剪贴板！
     * @param nodeInfo 目标节点。
     * @param text 要设置的文本。
     */
    protected fun performSetText(nodeInfo: AccessibilityNodeInfo, text: String) {
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
            Log.d(tag, "已将加密内容直接设置到节点: $text")
        } else {
            // 如果节点不支持直接设置文本（比如不可编辑的TextView），我们再考虑其他策略。
            // 比如弹窗提示，或者把解密内容复制到剪贴板（并明确告知用户）。
            Log.d(tag, "节点不支持设置文本。加密内容: $text")
        }
    }

    /**
     * ✨ 验证缓存节点有效性的“金标准”方法 ✨
     */
    private fun isNodeValid(node: AccessibilityNodeInfo?): Boolean {
        return node?.refresh() ?: false
    }
}