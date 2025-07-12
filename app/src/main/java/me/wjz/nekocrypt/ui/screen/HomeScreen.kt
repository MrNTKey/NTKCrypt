package me.wjz.nekocrypt.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.service.NCAccessibilityService
import me.wjz.nekocrypt.ui.SwitchSettingCard
import me.wjz.nekocrypt.util.openAccessibilitySettings
import me.wjz.nekocrypt.util.rememberAccessibilityServiceState

// --- 主屏幕代码 ---

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 1. 获取当前上下文
    val context: Context = LocalContext.current

    // 2. 使用我们新的 Composable 函数来获取并监听无障碍服务的状态
    //    你需要将 MyAccessibilityService::class.java 替换成你自己的服务类名
    val isEnabled by rememberAccessibilityServiceState(context, NCAccessibilityService::class.java)

    val buttonFillColor by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(500),
        label = "ButtonFillAnimation"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(500),
        label = "ContentColorAnimation"
    )

    val shadowElevation by animateDpAsState(if (isEnabled) 16.dp else 8.dp, tween(500), label = "")

    val rotationSpeed by animateFloatAsState(
        targetValue = if (isEnabled) 15f else 5f,//这里控制动画速度。
        animationSpec = tween(1500),
        label = "RotationSpeedAnimation"
    )

    var rotationAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->//用于在 Compose 中调度下一帧的绘制
                if (lastFrameTimeNanos != 0L) {
                    //这里实现帧率无关的平滑动画，否则高帧率速度就会变快了
                    val deltaTimeMillis = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                    val deltaAngle = (rotationSpeed * deltaTimeMillis) / 1000f
                    rotationAngle = (rotationAngle + deltaAngle) % 360f
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }
    //这里控制的是外围圈的参数
    val ringSize by animateDpAsState(
        targetValue = if (isEnabled) 290.dp else 270.dp,//控制圈的半径
        animationSpec = tween(600),
        label = "RingSizeAnimation"
    )

    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val arcColor1 by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.primary else outlineColor,
        animationSpec = tween(700),
        label = "ArcColor1"
    )
    val arcColor2 by animateColorAsState(
        targetValue = if (isEnabled) MaterialTheme.colorScheme.tertiary else outlineColor,
        animationSpec = tween(700),
        label = "ArcColor2"
    )
    val arcBrush = Brush.sweepGradient(colors = listOf(arcColor1, arcColor2, arcColor1))

    //猫爪位置参数
    val palmOffsetY by animateFloatAsState(if (isEnabled) -10f else 0f, tween(400), label = "")
    val outerLeftToeX by animateFloatAsState(if (isEnabled) -18f else 0f, tween(400), label = "")
    val outerLeftToeY by animateFloatAsState(if (isEnabled) -15f else 0f, tween(400), label = "")
    val innerLeftToeX by animateFloatAsState(if (isEnabled) -10f else 0f, tween(400), label = "")
    val innerLeftToeY by animateFloatAsState(if (isEnabled) -25f else 0f, tween(400), label = "")
    val innerRightToeX by animateFloatAsState(if (isEnabled) 10f else 0f, tween(400), label = "")
    val innerRightToeY by animateFloatAsState(if (isEnabled) -25f else 0f, tween(400), label = "")
    val outerRightToeX by animateFloatAsState(if (isEnabled) 18f else 0f, tween(400), label = "")
    val outerRightToeY by animateFloatAsState(if (isEnabled) -15f else 0f, tween(400), label = "")

    // [NEW] 给缺口大小加上动画
    val gapAngle by animateFloatAsState(
        targetValue = if (isEnabled) 8f else 12f, // 启用时缺口更小
        animationSpec = tween(700),
        label = "GapAngleAnimation"
    )


    // 使用 Column 作为根布局，以垂直排列组件
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用带权重的 Column 来包裹原有的猫爪UI，使其占据大部分空间并保持居中
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {

                //绘制外围的圆圈。
                Canvas(modifier = Modifier.size(ringSize)) {
                    val strokeWidth = 10f //画笔粗细
                    val dashCount = 12  //短线数量
                    // 短线和总角度现在基于动画化的缺口大小来计算
                    val totalAnglePerDash = 360f / dashCount    //每一份短线+缺口应该占据的角度
                    val dashAngle = totalAnglePerDash - gapAngle // 使用动态计算的短线角度
                    // 绘制转圈效果
                    rotate(degrees = rotationAngle) {
                        for (i in 0 until dashCount) {
                            drawArc(
                                brush = arcBrush,
                                startAngle = i * totalAnglePerDash, //开始的角度
                                sweepAngle = dashAngle, // 扫过多少度
                                useCenter = false,  // 这个启动了，画出来的就会是扇形，含义是是否连到圆心，连了再转就是扇形了。
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round
                                )//绘制样式，Stroke是描边，第二个参数是指定线条两端的样式为圆形。
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .size(260.dp)
                        .shadow(
                            elevation = shadowElevation,
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null   //关闭默认的波纹动画效果
                        ) {
                            // 当按钮被点击时，不再是切换本地变量，
                            // 而是调用我们的工具函数来打开系统设置页面！
                            openAccessibilitySettings(context)
                        },
                    color = buttonFillColor
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 猫爪绘制逻辑
                        Canvas(modifier = Modifier.size(110.dp)) {
                            val strokeWidth = 11f
                            val palmSize = Size(size.width * 0.6f, size.height * 0.45f)
                            val palmBaseCenter = Offset(center.x, center.y + size.height * 0.2f)
                            val palmAnimatedCenter =
                                palmBaseCenter.copy(y = palmBaseCenter.y + palmOffsetY)
                            val palmTopLeft = Offset(
                                x = palmAnimatedCenter.x - palmSize.width / 2f,
                                y = palmAnimatedCenter.y - palmSize.height / 2f
                            )

                            //画一个椭圆，就是肉垫
                            drawOval(
                                color = contentColor,
                                topLeft = palmTopLeft,
                                size = palmSize,
                                style = Stroke(width = strokeWidth)
                            )

                            val toeRadius = size.width * 0.1f
                            val outerLeftBaseCenter =
                                Offset(
                                    center.x - size.width * 0.35f,
                                    center.y - size.height * 0.08f
                                )
                            val innerLeftBaseCenter =
                                Offset(
                                    center.x - size.width * 0.15f,
                                    center.y - size.height * 0.25f
                                )
                            val innerRightBaseCenter =
                                Offset(
                                    center.x + size.width * 0.15f,
                                    center.y - size.height * 0.25f
                                )
                            val outerRightBaseCenter =
                                Offset(
                                    center.x + size.width * 0.35f,
                                    center.y - size.height * 0.08f
                                )

                            drawCircle(
                                color = contentColor,
                                center = outerLeftBaseCenter.copy(
                                    x = outerLeftBaseCenter.x + outerLeftToeX,
                                    y = outerLeftBaseCenter.y + outerLeftToeY
                                ),
                                radius = toeRadius,
                                style = Stroke(width = strokeWidth)
                            )
                            drawCircle(
                                color = contentColor,
                                center = innerLeftBaseCenter.copy(
                                    x = innerLeftBaseCenter.x + innerLeftToeX,
                                    y = innerLeftBaseCenter.y + innerLeftToeY
                                ),
                                radius = toeRadius,
                                style = Stroke(width = strokeWidth)
                            )
                            drawCircle(
                                color = contentColor,
                                center = innerRightBaseCenter.copy(
                                    x = innerRightBaseCenter.x + innerRightToeX,
                                    y = innerRightBaseCenter.y + innerRightToeY
                                ),
                                radius = toeRadius,
                                style = Stroke(width = strokeWidth)
                            )
                            drawCircle(
                                color = contentColor,
                                center = outerRightBaseCenter.copy(
                                    x = outerRightBaseCenter.x + outerRightToeX,
                                    y = outerRightBaseCenter.y + outerRightToeY
                                ),
                                radius = toeRadius,
                                style = Stroke(width = strokeWidth)
                            )
                        }

                        AnimatedContent<Boolean>(
                            targetState = isEnabled,
                            transitionSpec = {
                                (slideInVertically { h -> h } + fadeIn(animationSpec = tween(250)))
                                    .togetherWith(slideOutVertically { h -> -h } + fadeOut(
                                        animationSpec = tween(
                                            250
                                        )
                                    ))
                                    .using(SizeTransform(clip = false))
                            },
                            label = "Status Text Animation"
                        ) { targetState ->
                            Text(
                                text = if (targetState)
                                    stringResource(id = R.string.accessibility_service_enabled)
                                else
                                    stringResource(id = R.string.accessibility_service_disabled),
                                color = contentColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 在底部添加我们的设置卡片
        SwitchSettingCard(
            key = SettingKeys.USE_AUTO_ENCRYPTION,
            defaultValue = false,
            title = stringResource(id = R.string.setting_encrypt_on_send_title),
            subtitle = stringResource(id = R.string.setting_encrypt_on_send_subtitle),
            modifier = Modifier.padding(16.dp) // 添加一些边距让布局更好看
        )
    }
}