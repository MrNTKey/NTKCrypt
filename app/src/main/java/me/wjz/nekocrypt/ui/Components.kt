package me.wjz.nekocrypt.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.delay
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import androidx.core.graphics.toColorInt

/**
 * 这是一个自定义的、用于显示设置分组标题的组件。
 * @param title 要显示的标题文字。
 */
@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}


/**
 * 这是一个自定义的、带开关的设置项组件。
 * 它内部管理着开关自己的状态。
 * @param icon 左侧显示的图标。
 * @param title 主标题文字。
 * @param subtitle 副标题（描述性文字）。
 */
@Composable
fun SwitchSettingItem(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
) {
    var isChecked by rememberDataStoreState(key, defaultValue)

    //用Row来水平排列元素
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isChecked = !isChecked }//点击整行也能更新状态
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //显示图标
        icon()
        // 占一点间距
        Spacer(modifier = Modifier.width(16.dp))
        //用Column来垂直排列主标题和副标题
        Column(modifier = Modifier.weight(1f)) {// weight(1f)让这一列占满所有剩余空间
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            ) // 让副标题颜色浅一点
        }
        Switch(checked = isChecked, onCheckedChange = {
            isChecked = it
            onClick()
        })
    }
}

@Composable
fun ClickableSettingItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 设置点击事件
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun SwitchSettingCard(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onCheckedChanged: (Boolean) -> Unit = {},
) {
    var isChecked by rememberDataStoreState(key, defaultValue)
    // 将形状定义为一个变量，方便复用
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable {
                isChecked = !isChecked
                onCheckedChanged(isChecked)
            },
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 开关的状态直接绑定到我们内部的 isChecked 变量
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onCheckedChanged(it)
                }
            )
        }
    }
}

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


// 分段按钮实现

data class RadioOption(val key: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonSetting(
    settingKey: Preferences.Key<String>,
    title: String,
    options: List<RadioOption>,
    defaultOptionKey: String,
    modifier: Modifier = Modifier,
    titleExtraContent: (@Composable () -> Unit)? = null,    //标题旁边的内容
) {
    var currentSelection by rememberDataStoreState(settingKey, defaultOptionKey)

    Column(modifier = modifier.padding(start = 8.dp, end = 8.dp)) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp), // 调整内边距以适应IconButton
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start   // 从左到右排列
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            // 如果传入了额外内容，就在这里显示它
            titleExtraContent?.invoke()
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            options.forEachIndexed { index, option ->
                // ✨ 关键改动：根据位置动态计算形状！
                val shape = when (index) {
                    // 第一个按钮：左边是圆角，右边是直角
                    0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                    // 最后一个按钮：左边是直角，右边是圆角
                    options.lastIndex -> RoundedCornerShape(
                        topEndPercent = 50,
                        bottomEndPercent = 50
                    )
                    // 中间的按钮：两边都是直角
                    else -> RectangleShape
                }

                SegmentedButton(
                    shape = shape, // ✨ 使用我们动态计算的形状
                    onClick = { currentSelection = option.key },
                    selected = currentSelection == option.key
                ) {
                    Text(option.label)
                }
            }
        }

    }
}

// 带tooltip的infoIcon实现
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoDialogIcon(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    contentDescription: String? = null,
) {
    // ✨ 关键：组件自己管理自己的弹窗状态，外部完全无需关心！
    var showDialog by remember { mutableStateOf(false) }

    // 1. 这是用户能看到的触发器：一个图标按钮
    IconButton(
        onClick = { showDialog = true }, // 点击时，只改变自己的内部状态
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }

    // 2. 这是与触发器绑定的弹窗UI
    //    当内部状态为 true 时，它就会自动显示出来
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = { Text(text = text) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun SliderSettingItem(
    key: Preferences.Key<Long>,
    defaultValue: Long,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    valueRange: LongRange,
    step: Long, // 单步步长
    modifier: Modifier = Modifier,
) {
    // 使用你的 Hook 来自动同步 DataStore
    var currentValue by rememberDataStoreState(key, defaultValue)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧的图标
            Box(modifier = Modifier.padding(end = 16.dp)) {
                icon()
            }
            // 右侧的文字和滑块
            Column(modifier = Modifier.weight(1f)) {
                // 标题和当前值
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // 实时显示当前选中的值
                    Text(
                        text = "$currentValue ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // 副标题
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 滑块本体
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = {
                        // 当用户滑动时，更新状态
                        currentValue = it.toLong()
                    },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = ((valueRange.last - valueRange.first) / step - 1).toInt(), // 设置步数，让滑块可以吸附到整数值
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}


/**
 *  一个独立的、可复用的解密弹窗 Composable UI
 * 它只关心需要显示什么文本 (text)，以及被关闭时该做什么 (onDismiss)。
 * 它完全不知道什么是无障碍服务，什么是处理器。
 */
@Composable
fun DecryptionPopupContent(text: String, durationMills: Long = 3000, onDismiss: () -> Unit) {
    // 不手动指定darkTheme，会出问题。
    val animationTime = 250 // 动画时间

    // 创建一个从1.0开始的动画值。
    val progress = remember { Animatable(1.0f) }
    // ✨ 1. 新增一个状态来控制整体的可见性，用于驱动出入场动画
    var isVisible by remember { mutableStateOf(false) }

    // ✨ 2. 使用两个 LaunchedEffect，职责分离
    // 第一个：负责UI的出现和消失
    LaunchedEffect(text) {
        isVisible = true // 触发“出现”动画
        // 启动倒计时
        progress.animateTo(
            0.0f,
            animationSpec = tween(durationMills.toInt(), easing = LinearEasing)
        )
        // 倒计时结束后，触发“消失”动画
        isVisible = false
        // 等待消失动画播放完毕
        delay(animationTime.toLong())
        // 动画完全结束后，才真正调用 onDismiss
        onDismiss()
    }

    NekoCryptTheme(darkTheme = false) {
        // ✨ 关键！在最外层用一个 Box 包裹，并给它加上内边距。
        // 这个内边距就是我们为动画和阴影预留的“安全区”，
        // 确保它们在“弹跳”时不会被最外层的边界裁剪掉。
        Box(modifier = Modifier.padding(16.dp)) {

            // ✨ 3. 用 AnimatedVisibility 包裹整个UI，赋予它出入场动画
            AnimatedVisibility(
                visible = isVisible,
                // ✨ 出现动画：像气泡一样“啵”地一下弹出来！
                enter = scaleIn(
                    animationSpec = spring( // 使用 spring 动画，让效果更Q弹
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(animationTime)),
                // ✨ 消失动画：优雅地缩小并淡出
                exit = scaleOut(animationSpec = tween(animationTime)) + fadeOut(
                    animationSpec = tween(
                        animationTime
                    )
                )
            ) {
                Card(
                    // ✨ 4. 关键！让 Card 的尺寸自适应内容
                    modifier = Modifier
                        .wrapContentSize() // 让卡片包裹内容，而不是撑满
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
                            alpha = 0.92f
                        )
                    ),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 8.dp,
                            bottom = 8.dp,
                            end = 8.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = text,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), // 阴影颜色
                                    offset = Offset(2f, 2f), // 阴影偏移量
                                    blurRadius = 4f // 阴影模糊半径
                                )
                            ),
                            // ✨ 限制文本的最大宽度，防止一行文本过长导致弹窗撑得太大
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(25.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { progress.value },
                                modifier = Modifier.size(25.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                            IconButton(onClick = {
                                // ✨ 点击关闭按钮时，也应该优雅地播放“消失”动画
                                isVisible = false
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "关闭",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 新增一个可以点击的颜色设置，用来设置一个RGBA颜色
@Composable
fun ColorSettingItem(
    key: Preferences.Key<String>,
    defaultValue: String,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    // ✨ 核心修正 1：我们现在需要两个状态
    // `storedColorHex` 是我们与DataStore同步的“仓库”状态
    var storedColorHex by rememberDataStoreState(key, defaultValue)
    // `displayedColorHex` 是我们UI上立即显示的“公告板”状态
    var displayedColorHex by remember { mutableStateOf(defaultValue) }
    var showDialog by remember { mutableStateOf(false) }

    // ✨ 核心修正 2：用 LaunchedEffect 来保持“公告板”和“仓库”同步
    // 当 `storedColorHex` (仓库) 因任何原因改变时，立刻更新 `displayedColorHex` (公告板)
    LaunchedEffect(storedColorHex) {
        displayedColorHex = storedColorHex
    }

    // ✨ 核心修正 3：UI现在完全信任“公告板”上的颜色
    val currentColor = try {
        Color(displayedColorHex.toColorInt())
    } catch (e: Exception) {
        Color.Red
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }
        // 右侧的颜色预览
        Surface(
            modifier = Modifier.size(width = 50.dp, height = 30.dp),
            shape = RoundedCornerShape(8.dp), // 使用圆角矩形
            color = currentColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {}
    }

    // 当 showDialog 为 true 时，显示我们的颜色选择对话框
    if (showDialog) {
        ColorPickerDialog(
            initialColorHex = displayedColorHex,
            onDismissRequest = { showDialog = false },
            onColorSelected = { newColorHex ->
                // ✨ 核心修正 5：当用户选择新颜色时...
                // 1. 立刻更新“公告板”，UI瞬间响应！
                displayedColorHex = newColorHex
                // 2. 同时派出“慢性子信使”去更新“仓库”
                storedColorHex = newColorHex
                // 3. 关闭对话框
                showDialog = false
            }
        )
    }
}


/**
 * ✨ [新增] 我们的自定义颜色选择对话框。
 */
@Composable
private fun ColorPickerDialog(
    initialColorHex: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    // 对话框内部的临时状态，只有点“确认”时才会更新到外面
    var tempColorHex by remember { mutableStateOf(initialColorHex) }
    val isHexValid = remember(tempColorHex) {
        // 正则表达式，用于验证6位或8位Hex颜色代码（可带#号）
        tempColorHex.matches("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$".toRegex())
    }
    val errorColor = MaterialTheme.colorScheme.error
    val parsedColor = remember(tempColorHex, isHexValid) {
        if (isHexValid) {
            try {
                Color(if (tempColorHex.startsWith("#")) tempColorHex.toColorInt() else "#$tempColorHex".toColorInt())
            } catch (e: Exception) {
                errorColor
            }
        } else {
            errorColor
        }
    }

    // 一些预设的颜色，方便用户快速选择
    val predefinedColors = listOf(
        "#80FF69B4", "#80FF4500", "#80FFD700", "#80ADFF2F",
        "#8000CED1", "#801E90FF", "#809370DB", "#FFFFFFFF", // 白色全不透明
        "#FFC0C0C0", "#FF808080", "#FF000000", "#5066ccff",  // 灰色黑色全不透明，最后一个保持原样
        "#00000000" //纯透明
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column {
                // 颜色预览和Hex输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(    //左侧的颜色预览
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = parsedColor,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {}
                    Spacer(modifier = Modifier.width(16.dp))
                    TextField(
                        value = tempColorHex,
                        onValueChange = { tempColorHex = it },
                        label = { Text("Hex (A)RGB") },
                        isError = !isHexValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 预设颜色网格
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(predefinedColors.size) { index ->
                        val colorHex = predefinedColors[index]
                        val color = Color(colorHex.toColorInt())
                        val isSelected = tempColorHex.equals(colorHex, ignoreCase = true)

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { tempColorHex = colorHex },
                            shape = RoundedCornerShape(8.dp),
                            color = color,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                        ) {
                            // Surface 的 content lambda 提供了一个干净的 BoxScope，消除了歧义
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn(animationSpec = tween(250)),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.align(Alignment.CenterHorizontally) // 确保图标居中
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(tempColorHex) },
                enabled = isHexValid // 只有当输入的Hex有效时才能确认
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}