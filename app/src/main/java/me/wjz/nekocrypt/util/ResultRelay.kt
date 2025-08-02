package me.wjz.nekocrypt.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ResultRelay {
    private val _flow = MutableSharedFlow<String>()
    val flow = _flow.asSharedFlow()

    suspend fun send(url: String) {
        _flow.emit(url)
    }

}