package me.wjz.nekocrypt.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // --- 状态管理 ---
    // 在真实应用中，这个状态应该从 ViewModel 获取，ViewModel 负责检查无障碍服务是否真的在运行。
    // 这里我们用一个 mutableStateOf 来模拟这个状态，方便您在预览中查看两种效果。
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    // 获取当前 Android 上下文，用于启动系统设置页面。
    val context = LocalContext.current

    // --- 动画定义 ---
    // 1. 颜色动画：当 `isAccessibilityEnabled` 状态改变时，颜色会平滑地过渡。
    val circleColor by animateColorAsState(
        targetValue = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 1000), // 1秒的颜色过渡动画
        label = "circle color animation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isAccessibilityEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(durationMillis = 1000),
        label = "content color animation"
    )

    // 2. 无限循环动画：用于创建“呼吸”或“脉冲”效果。
    val infiniteTransition = rememberInfiniteTransition(label = "infinite transition")
    // 脉冲效果：让圆圈的缩放大小在 1f 和 1.05f 之间来回变化。
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isAccessibilityEnabled) 1.02f else 1f, // 开启时才有脉冲
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse scale animation"
    )
    // 旋转光环效果：让一个角度值从 0 度变化到 360 度，无限循环。
    val rotatingAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotating angle animation"
    )

    // --- UI 布局 ---
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // 富有特效的大圆圈
        Box(
            modifier = Modifier
                .size(260.dp)
                // graphicsLayer 用于应用缩放、旋转等变换，性能较高。
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
                // drawBehind 用于在组件内容之后绘制自定义图形（比如我们的光环）。
                .drawBehind {
                    if (isAccessibilityEnabled) {
                        // 如果服务开启，我们绘制一个旋转的渐变光环。
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to Color.Transparent,
                                0.5f to contentColor.copy(alpha = 0.5f),
                                1f to Color.Transparent
                            ),
                            startAngle = rotatingAngle,
                            sweepAngle = 180f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }
                }
                .clip(CircleShape) // 将 Box 裁剪成圆形
                .background(circleColor)
                .clickable(
                    // 移除点击时的涟漪效果，让视觉更纯粹
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 点击事件：
                    // 在真实应用中，您总是应该跳转到设置页面。
                    // 这里我们为了演示，也切换一下状态。
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)

                    // 仅为演示用，点击后切换状态，模拟用户开启权限后返回App的场景。
                    isAccessibilityEnabled = !isAccessibilityEnabled
                },
            contentAlignment = Alignment.Center
        ) {
            // 圆圈内部的内容：图标和文字
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isAccessibilityEnabled) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "服务已开启",
                        modifier = Modifier.size(80.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "服务运行中",
                        fontSize = 22.sp,
                        color = contentColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.LockOpen,
                        contentDescription = "服务未开启",
                        modifier = Modifier.size(80.dp),
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "点击开启服务",
                        fontSize = 22.sp,
                        color = contentColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}