package me.wjz.nekocrypt.service.handler

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
}