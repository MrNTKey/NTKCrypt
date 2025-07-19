package me.wjz.nekocrypt.ui

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.hook.rememberDataStoreState

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
        modifier = modifier.fillMaxWidth().clickable{},
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