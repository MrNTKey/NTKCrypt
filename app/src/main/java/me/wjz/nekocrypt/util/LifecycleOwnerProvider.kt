// 文件路径: me/wjz/nekocrypt/util/LifecycleOwnerProvider.kt
package me.wjz.nekocrypt.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner

/**
 * 一个实现了所有生命周期相关接口的“便携式电源包”。
 * 它可以为任何没有自带生命周期的View（比如添加到WindowManager的View）提供一个完整的、可控的生命周期。
 */
class LifecycleOwnerProvider : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    // --- “发电机”部分：提供 Lifecycle ---
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    // --- “遥控器中枢”部分：提供 ViewModelStore ---
    // ✨ 关键修复：使用带下划线的私有变量作为真正的存储（幕后字段）
    private val _viewModelStore = ViewModelStore()
    // ✨ 公开的属性只提供 get() 方法，返回私有变量的值，解决了命名冲突和歧义
    override val viewModelStore: ViewModelStore get() = _viewModelStore

    // --- “信号源”部分：提供 SavedStateRegistry ---
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    /**
     * 初始化“电源包”，把它和自己的各个部件连接起来。
     */
    init {
        // 在 init 阶段恢复已保存的状态
        savedStateRegistryController.performRestore(null)
    }

    /**
     * 手动“开机”！将生命周期推进到 RESUMED 状态。
     */
    fun resume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * 手动“关机”！将生命周期推进到 DESTROYED 状态，并清理所有资源。
     */
    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        // ✨ 清理时调用私有变量的 clear()
        _viewModelStore.clear()
    }
}
