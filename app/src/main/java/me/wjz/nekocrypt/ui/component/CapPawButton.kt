package me.wjz.nekocrypt.ui.component

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


// 猫爪按钮
@Composable
fun CatPawButton(
    isEnabled: Boolean,
    statusText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // --- 所有与猫爪按钮相关的动画状态都内聚在这里 ---
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
        if (isEnabled) 15f else 5f,
        tween(1500),
        label = "RotationSpeedAnimation"
    )
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    val ringSize by animateDpAsState(
        if (isEnabled) 290.dp else 270.dp,
        tween(600),
        label = "RingSizeAnimation"
    )
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val arcColor1 by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.primary else outlineColor,
        tween(700),
        label = "ArcColor1"
    )
    val arcColor2 by animateColorAsState(
        if (isEnabled) MaterialTheme.colorScheme.tertiary else outlineColor,
        tween(700),
        label = "ArcColor2"
    )
    val arcBrush = Brush.sweepGradient(colors = listOf(arcColor1, arcColor2, arcColor1))
    val palmOffsetY by animateFloatAsState(if (isEnabled) -10f else 0f, tween(400), label = "")
    val outerLeftToeX by animateFloatAsState(if (isEnabled) -18f else 0f, tween(400), label = "")
    val outerLeftToeY by animateFloatAsState(if (isEnabled) -15f else 0f, tween(400), label = "")
    val innerLeftToeX by animateFloatAsState(if (isEnabled) -10f else 0f, tween(400), label = "")
    val innerLeftToeY by animateFloatAsState(if (isEnabled) -25f else 0f, tween(400), label = "")
    val innerRightToeX by animateFloatAsState(if (isEnabled) 10f else 0f, tween(400), label = "")
    val innerRightToeY by animateFloatAsState(if (isEnabled) -25f else 0f, tween(400), label = "")
    val outerRightToeX by animateFloatAsState(if (isEnabled) 18f else 0f, tween(400), label = "")
    val outerRightToeY by animateFloatAsState(if (isEnabled) -15f else 0f, tween(400), label = "")
    val gapAngle by animateFloatAsState(
        if (isEnabled) 8f else 12f,
        tween(700),
        label = "GapAngleAnimation"
    )

    LaunchedEffect(Unit) {
        var lastFrameTimeNanos = 0L
        while (true) {
            withFrameNanos { frameTimeNanos ->
                if (lastFrameTimeNanos != 0L) {
                    val deltaTimeMillis = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                    val deltaAngle = (rotationSpeed * deltaTimeMillis) / 1000f
                    rotationAngle = (rotationAngle + deltaAngle) % 360f
                }
                lastFrameTimeNanos = frameTimeNanos
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidth = 10f
            val dashCount = 12
            val totalAnglePerDash = 360f / dashCount
            val dashAngle = totalAnglePerDash - gapAngle
            rotate(degrees = rotationAngle) {
                for (i in 0 until dashCount) {
                    drawArc(
                        brush = arcBrush,
                        startAngle = i * totalAnglePerDash,
                        sweepAngle = dashAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(260.dp)
                .shadow(elevation = shadowElevation, shape = CircleShape)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            color = buttonFillColor
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val strokeWidth = 11f
                    val palmSize = Size(size.width * 0.6f, size.height * 0.45f)
                    val palmBaseCenter = Offset(center.x, center.y + size.height * 0.2f)
                    val palmAnimatedCenter = palmBaseCenter.copy(y = palmBaseCenter.y + palmOffsetY)
                    val palmTopLeft = Offset(
                        palmAnimatedCenter.x - palmSize.width / 2f,
                        palmAnimatedCenter.y - palmSize.height / 2f
                    )
                    drawOval(
                        color = contentColor,
                        topLeft = palmTopLeft,
                        size = palmSize,
                        style = Stroke(width = strokeWidth)
                    )

                    val toeRadius = size.width * 0.1f
                    val outerLeftBaseCenter =
                        Offset(center.x - size.width * 0.35f, center.y - size.height * 0.08f)
                    val innerLeftBaseCenter =
                        Offset(center.x - size.width * 0.15f, center.y - size.height * 0.25f)
                    val innerRightBaseCenter =
                        Offset(center.x + size.width * 0.15f, center.y - size.height * 0.25f)
                    val outerRightBaseCenter =
                        Offset(center.x + size.width * 0.35f, center.y - size.height * 0.08f)

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

                AnimatedContent(
                    targetState = isEnabled,
                    transitionSpec = {
                        (slideInVertically { h -> h } + fadeIn(tween(250)))
                            .togetherWith(slideOutVertically { h -> -h } + fadeOut(tween(250)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "Status Text Animation"
                ) { _ ->
                    Text(
                        text = statusText, // ✨ 使用传入的文本
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