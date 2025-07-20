package me.wjz.nekocrypt.service.handler

import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import me.wjz.nekocrypt.Constant

/**
 * 针对 QQ 的具体处理器实现。
 */
class QQHandler : BaseChatAppHandler() {
    override val packageName: String
        get() = Constant.PACKAGE_NAME_QQ

    override val inputId: String
        get() = Constant.ID_QQ_INPUT

    override val sendBtnId: String
        get() = Constant.ID_QQ_SEND_BTN

    /**
     * ✨ 为 QQ 定制的悬浮窗布局实现。
     * 这里我们提供了之前作为默认值的实现。
     * 如果将来 QQ 的布局变了，我们只需要修改这里，而不会影响到任何其他 Handler。
     */
    override fun getOverlayLayoutParams(anchorRect: Rect): WindowManager.LayoutParams {
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 这里是 QQ 的具体布局逻辑
        val yOffset = 6 // 可以在这里为QQ定义特定的偏移值
        val xOffset = 0

        return WindowManager.LayoutParams(
            anchorRect.width(),
            anchorRect.height(),
            anchorRect.left + xOffset,
            anchorRect.top - anchorRect.height() + yOffset,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    // QQ定制解密悬浮窗显示位置微调
    override fun modifyDecryptionWindowRect(rect: Rect): Rect {
        rect.offset(10, -10)
        return rect
    }
}