package me.wjz.nekocrypt.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import me.wjz.nekocrypt.R // 确保你有一个空的布局文件 R.layout.floating_view

class FloatingWindowService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null//其他组件不要主动绑定它
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window_layout, null)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or // 1. 事件不被自己消费，可以穿透下去
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or   // 2. 不获取焦点，这样就不会影响下面的输入框等
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,  // 3. 允许布局到屏幕之外
            PixelFormat.TRANSPARENT // 窗口本身完全透明
        )

        params.gravity = Gravity.START or Gravity.TOP

        //设置触摸监听器
        floatingView?.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // 当手指按下时，获取坐标并发送“坐标情报”
                val x = event.rawX.toInt()
                val y = event.rawY.toInt()
                Log.d("FloatingWindow", "触摸坐标: ($x, $y)")
                sendCoordinatesBroadcast(x, y)
            }
            // 返回 false 意味着我们不“消费”这个事件，让它可以继续传递下去
            return@setOnTouchListener false
        }

        //把我们的floatingView添加到屏幕上
        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Log.e("FloatingWindow", "添加悬浮窗失败", e)
        }
    }

    //发送坐标的广播
    private fun sendCoordinatesBroadcast(x: Int, y: Int) {
        val intent = Intent(ACTION_TOUCH)
        intent.putExtra(EXTRA_X, x)
        intent.putExtra(EXTRA_Y, y)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let { windowManager.removeView(it) }
    }

    companion object {
        const val ACTION_TOUCH = "me.wjz.nekocrypt.ACTION_TOUCH"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
    }
}