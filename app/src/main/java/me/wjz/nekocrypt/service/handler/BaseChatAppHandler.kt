package me.wjz.nekocrypt.service.handler

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.appendNekoTalk

abstract class BaseChatAppHandler : ChatAppHandler {
    protected val tag = "NCBaseHandler"

    // 由子类提供具体应用的ID
    abstract override val inputId: String
    abstract override val sendBtnId: String

    // 处理器内部状态
    private var service: NCAccessibilityService? = null
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var overlayManagementJob: Job? = null

    private val colorInt = "#80ff0000".toColorInt() //debug的时候调成可见色，正式环境应该是纯透明

    override fun onHandlerActivated(service: NCAccessibilityService) {
        this.service = service
        this.windowManager = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        Log.d(tag, "激活$packageName 处理器。")
    }

    override fun onHandlerDeactivated() {
        overlayManagementJob?.cancel()
        removeOverlayView()
        this.service = null
        this.windowManager = null
        Log.d(tag, "取消$packageName 处理器。")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent, service: NCAccessibilityService) {
        // 悬浮窗管理逻辑
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (service.useAutoEncryption) {
                //只有开启自动加密，才会加上悬浮窗，每次事件改变，都要更新悬浮窗位置
                overlayManagementJob?.cancel()
                overlayManagementJob = service.serviceScope.launch(Dispatchers.Default) {
                    handleOverlayManagement()   // 可能是添加、更新、删除悬浮窗
                }
            } else {
                removeOverlayView()
            }
        }

    }

    // --- 所有悬浮窗和加密逻辑都内聚在这里 ---

    protected suspend fun handleOverlayManagement() {
        delay(50)   //延迟一下防抖
        val rootNode = service?.rootInActiveWindow ?: return
        val sendBtnNode = findNodeById(rootNode, sendBtnId)
        if (sendBtnNode != null) {
            val rect = Rect()
            sendBtnNode.getBoundsInScreen(rect)
            if (!rect.isEmpty) {
                if (overlayView == null) addOverlayView(rect) else updateOverlayPosition(rect)
            }
        } else {
            removeOverlayView()
        }
    }

    protected fun addOverlayView(rect: Rect) {
        val currentService = service ?: return
        currentService.serviceScope.launch(Dispatchers.Main) {
            if (overlayView != null) return@launch

            overlayView = View(currentService).apply {
                setBackgroundColor(colorInt)
                setOnClickListener { performClick(currentService.useAutoEncryption) }
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else WindowManager.LayoutParams.TYPE_PHONE

            val params = WindowManager.LayoutParams(
                rect.width(), rect.height(), rect.left, rect.top - rect.height(),
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }

            // 准备好悬浮窗后就添加到视图中
            windowManager?.addView(overlayView, params)
        }
    }

    protected fun updateOverlayPosition(rect: Rect) {
        service?.serviceScope?.launch(Dispatchers.Main) {
            overlayView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                params.x = rect.left
                params.y = rect.top - rect.height()
                params.width = rect.width()
                params.height = rect.height()
                // 更新视图约束
                windowManager?.updateViewLayout(view, params)
            }
        }
    }

    // 移除悬浮窗
    protected fun removeOverlayView() {
        service?.serviceScope?.launch(Dispatchers.Main) {
            if (overlayView != null && windowManager != null) {
                windowManager?.removeView(overlayView)
                overlayView = null
            }
        }
    }

    // 发送消息，自动判断是否加密
    protected fun performClick(isAutoEncryption: Boolean) {
        val currentService = service ?: return
        val root = currentService.rootInActiveWindow ?: return  //拿到根视图
        val sendBtnNode = findNodeById(root, sendBtnId) //发送按钮节点
        if (sendBtnNode == null) return

        //  根据是否开启自动加密修改发送内容。
        if (!isAutoEncryption) {
            sendBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }

        //  下面走加密消息逻辑
        val inputNode = findNodeById(root, inputId) //输入框节点
        if (inputNode == null) return


        val originalText = inputNode.text?.toString()
        // 如果本来输入栏是空的，或者已经有密文了，就不加密，直接发送。
        if (originalText.isNullOrEmpty() || CryptoManager.containsCiphertext(originalText)) {
            sendBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 做加密处理，并添加咪咪talk。
            val encryptedText =
                CryptoManager.encrypt(originalText, currentService.currentKey).appendNekoTalk()
            //
            performSetText(inputNode, encryptedText)
            currentService.serviceScope.launch {
                delay(50)
                sendBtnNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
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
            Log.d(tag, "已将解密内容直接设置到节点: $text")
        } else {
            // 如果节点不支持直接设置文本（比如不可编辑的TextView），我们再考虑其他策略。
            // 比如弹窗提示，或者把解密内容复制到剪贴板（并明确告知用户）。
            Log.d(tag, "节点不支持设置文本。解密内容: $text")
        }
    }
}