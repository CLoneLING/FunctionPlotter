package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.data.model.PlotFunction
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.parser.PlotExpression
import com.yumoo.functionplotter.domain.parser.parsePlotExpression
import com.yumoo.functionplotter.domain.plot.PlotFrame
import com.yumoo.functionplotter.domain.plot.PlotViewport
import com.yumoo.functionplotter.domain.plot.asPiFraction
import com.yumoo.functionplotter.domain.plot.buildPlotFrame
import com.yumoo.functionplotter.domain.plot.snapToPiMultiple
import com.yumoo.functionplotter.domain.solver.EquationInfo
import com.yumoo.functionplotter.domain.solver.formatRoot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round

private const val DEFAULT_SCALE = 60f
private const val MIN_SCALE = 8f
private const val MAX_SCALE = 900f

/** 单次手势内允许的最大临时缩放，松手时再落定到真实倍率。 */
private const val MAX_LIVE_ZOOM = 4f

/** 相机中心允许的最大世界坐标；超过后平移会被截断，彻底避免浮点溢出。 */
private const val MAX_WORLD = 1_000_000f

/** 手势期间允许的最大像素位移，同样是为了防止累加溢出。 */
private const val MAX_TRANSLATION = 40_000_000f

private data class Camera(val x: Float, val y: Float, val scale: Float) {
    companion object {
        val Default = Camera(0f, 0f, DEFAULT_SCALE)
    }
}

private data class TapInfo(
    val screen: Offset,
    val worldX: Float,
    val worldY: Float,
    val colorArgb: Long
)

/**
 * 函数图绘制区。
 *
 * 线程模型：
 *  - 曲线路径在 Dispatchers.Default 上计算，produceState 会自动取消过期任务，
 *    主线程只负责绘制；
 *  - 手势期间不重算路径，只用 graphicsLayer 做 GPU 变换；
 *  - 网格、坐标轴、刻度、比例尺直接按"实时视口"在屏幕空间绘制，
 *    任何倍率下都清晰，且不会因为坐标轴拖到极远处而画出非法坐标。
 */
@Composable
fun FunctionCanvas(
    functions: List<PlotFunction>,
    showGrid: Boolean,
    showAxisLabels: Boolean,
    sampleStep: Float,
    modifier: Modifier = Modifier,
    onInvalidIdsChange: (Set<Long>) -> Unit = {}
) {
    val evaluator = remember { Exp4jEvaluator() }
    val textMeasurer = rememberTextMeasurer(cacheSize = 32)

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var camera by remember { mutableStateOf(Camera.Default) }
    var liveZoom by remember { mutableFloatStateOf(1f) }
    var livePan by remember { mutableStateOf(Offset.Zero) }
    var tapInfo by remember { mutableStateOf<TapInfo?>(null) }
    var showEquationDialog by remember { mutableStateOf(false) }

    val width = canvasSize.width.toFloat()
    val height = canvasSize.height.toFloat()
    val hasSize = canvasSize.width > 0 && canvasSize.height > 0

    val frame by produceState(
        initialValue = PlotFrame.Empty,
        functions,
        camera,
        canvasSize,
        sampleStep
    ) {
        if (!hasSize) return@produceState
        val viewport = PlotViewport(camera.x, camera.y, camera.scale, width, height)
        value = withContext(Dispatchers.Default) {
            try {
                buildPlotFrame(evaluator, functions, viewport, sampleStep)
            } catch (t: Throwable) {
                // 绘图永远不应该让应用崩溃：异常时保留上一帧
                PlotFrame.Empty
            }
        }
    }

    val invalidCallback by rememberUpdatedState(onInvalidIdsChange)
    LaunchedEffect(frame.invalidIds) { invalidCallback(frame.invalidIds) }

    // 视口一变，之前打的点就失效了，避免留下"幽灵标记"
    LaunchedEffect(camera) { tapInfo = null }

    fun commitTransform(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) {
            liveZoom = 1f
            livePan = Offset.Zero
            return
        }
        val zoom = liveZoom
        val denominator = camera.scale * zoom
        if (!zoom.isFinite() || zoom <= 0f || !denominator.isFinite() || denominator <= 0f) {
            liveZoom = 1f
            livePan = Offset.Zero
            return
        }
        val shiftX = (w / 2f) * (zoom - 1f) + livePan.x
        val shiftY = (h / 2f) * (zoom - 1f) + livePan.y
        val newCenterX = camera.x - shiftX / denominator
        val newCenterY = camera.y + shiftY / denominator
        camera = Camera(
            x = if (newCenterX.isFinite()) newCenterX.coerceIn(-MAX_WORLD, MAX_WORLD) else camera.x,
            y = if (newCenterY.isFinite()) newCenterY.coerceIn(-MAX_WORLD, MAX_WORLD) else camera.y,
            scale = denominator.coerceIn(MIN_SCALE, MAX_SCALE)
        )
        liveZoom = 1f
        livePan = Offset.Zero
    }

    val liveScale = (camera.scale * liveZoom).coerceIn(MIN_SCALE, MAX_SCALE)
    val liveCenterX = camera.x -
        (((width / 2f) * (liveZoom - 1f) + livePan.x) / liveScale)
    val liveCenterY = camera.y +
        (((height / 2f) * (liveZoom - 1f) + livePan.y) / liveScale)
    val liveViewport = PlotViewport(liveCenterX, liveCenterY, liveScale, width, height)

    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val hintColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val tickStyle = remember(labelColor) { TextStyle(fontSize = 10.sp, color = labelColor) }
    val markerBackground = MaterialTheme.colorScheme.surface
    val markerTextStyle = remember(labelColor) { TextStyle(fontSize = 11.sp, color = labelColor) }
    val markerSubTextStyle = remember(labelColor) {
        TextStyle(fontSize = 9.sp, color = labelColor.copy(alpha = 0.65f))
    }

    val viewportState = rememberUpdatedState(liveViewport)
    val functionsState = rememberUpdatedState(functions)

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .clipToBounds()
    ) {
        // 曲线层：GPU 变换，手势期间不重算
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = liveZoom
                    scaleY = liveZoom
                    transformOrigin = TransformOrigin(0f, 0f)
                    translationX = livePan.x
                    translationY = livePan.y
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                frame.curves.forEach { curve ->
                    drawPath(
                        path = curve.path,
                        color = Color(curve.colorArgb),
                        style = Stroke(width = if (curve.implicit) 2.5f else 3.5f)
                    )
                }
            }
        }

        // 网格 + 坐标轴 + 刻度 + 比例尺：屏幕空间实时绘制
        Canvas(Modifier.fillMaxSize()) {
            if (!liveViewport.isUsable) return@Canvas
            if (showGrid) drawGridLayer(liveViewport, gridColor)
            drawAxesLayer(liveViewport, axisColor)
            if (showAxisLabels) drawTickLabels(liveViewport, textMeasurer, tickStyle)
            drawScaleBar(liveViewport, textMeasurer, tickStyle, axisColor)
        }

        // 点按读数：能写成 π 的有理倍数时，同时给出 π 形式与小数形式
        tapInfo?.let { info ->
            Canvas(Modifier.fillMaxSize()) {
                val color = Color(info.colorArgb)
                drawCircle(color, radius = 7f, center = info.screen, style = Stroke(width = 2.5f))
                drawCircle(color, radius = 2.5f, center = info.screen)

                val tolerance =
                    if (liveViewport.isUsable) (0.6f / liveViewport.scale).toDouble() else 0.0
                val piX = asPiFraction(info.worldX.toDouble(), tolerance)
                val piY = asPiFraction(info.worldY.toDouble(), tolerance)
                val decimalText = "(${fmt(info.worldX)}, ${fmt(info.worldY)})"
                val piText = if (piX != null || piY != null) {
                    "(${piX ?: fmt(info.worldX)}, ${piY ?: fmt(info.worldY)})"
                } else {
                    null
                }

                val mainLayout = textMeasurer.measure(piText ?: decimalText, markerTextStyle)
                val subLayout =
                    if (piText != null) textMeasurer.measure(decimalText, markerSubTextStyle) else null
                val boxWidth = maxOf(mainLayout.size.width, subLayout?.size?.width ?: 0).toFloat()
                val boxHeight =
                    (mainLayout.size.height + (subLayout?.size?.height ?: 0)).toFloat()

                val tx = (info.screen.x + 12f)
                    .coerceAtMost(size.width - boxWidth - 12f)
                    .coerceAtLeast(6f)
                val ty = (info.screen.y - boxHeight - 12f)
                    .coerceAtMost(size.height - boxHeight - 6f)
                    .coerceAtLeast(6f)

                drawRect(
                    color = markerBackground.copy(alpha = 0.92f),
                    topLeft = Offset(tx - 4f, ty - 2f),
                    size = Size(boxWidth + 8f, boxHeight + 4f)
                )
                drawText(mainLayout, topLeft = Offset(tx, ty))
                if (subLayout != null) {
                    drawText(subLayout, topLeft = Offset(tx, ty + mainLayout.size.height))
                }
            }
        }

        if (frame.curves.isEmpty() && functions.all { it.expression.isBlank() }) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "输入表达式开始绘图",
                    style = MaterialTheme.typography.titleSmall,
                    color = hintColor
                )
                Text(
                    text = "拖动平移 · 双指缩放 · 双击复位 · 点按读数",
                    style = MaterialTheme.typography.bodySmall,
                    color = hintColor
                )
            }
        }

        // 手势层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // 只保留单击读数：双击复位容易在连点时误触发，已移除
                    detectTapGestures(
                        onTap = { position ->
                            tapInfo = hitTest(
                                evaluator = evaluator,
                                functions = functionsState.value,
                                viewport = viewportState.value,
                                screen = position
                            )
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.count { it.pressed }

                            if (pressedPointers >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom.isFinite() && zoom > 0f && zoom != 1f) {
                                    val centroid = event.calculateCentroid(useCurrent = true)
                                    if (!centroid.x.isNaN() && !centroid.y.isNaN()) {
                                        // 临时倍率也遵守总倍率上下限，避免松手时"回弹"
                                        val maxZoom = (MAX_SCALE / camera.scale)
                                            .coerceAtMost(MAX_LIVE_ZOOM)
                                        val minZoom = (MIN_SCALE / camera.scale)
                                            .coerceAtLeast(1f / MAX_LIVE_ZOOM)
                                        if (maxZoom >= minZoom) {
                                            val target = (liveZoom * zoom).coerceIn(minZoom, maxZoom)
                                            val factor = target / liveZoom
                                            if (factor.isFinite() && factor > 0f && factor != 1f) {
                                                // screen = base * k + t，绕 centroid 缩放即 t' = t*z + c*(1-z)
                                                livePan = livePan * factor + centroid * (1f - factor)
                                                liveZoom = target
                                            }
                                        }
                                    }
                                }
                            }

                            val pan = event.calculatePan()
                            if (pan.x.isFinite() && pan.y.isFinite()) {
                                livePan = Offset(
                                    (livePan.x + pan.x).coerceIn(-MAX_TRANSLATION, MAX_TRANSLATION),
                                    (livePan.y + pan.y).coerceIn(-MAX_TRANSLATION, MAX_TRANSLATION)
                                )
                            }
                        } while (event.changes.any { it.pressed })

                        // 纯点击（没有平移/缩放）不提交，避免无谓地重算 Path
                        if (liveZoom != 1f || livePan != Offset.Zero) {
                            commitTransform(size.width.toFloat(), size.height.toFloat())
                        }
                    }
                }
        )
        // 方程解信息：左下角淡灰小字，信息量大时点 "›" 查看全部
        frame.equation?.let { equation ->
            EquationOverlay(
                info = equation,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
                onShowAll = { showEquationDialog = true }
            )
        }
    }

    if (showEquationDialog) {
        frame.equation?.let { equation ->
            EquationDialog(info = equation, onDismiss = { showEquationDialog = false })
        }
    }
}

// ------------------------------------------------------------------ 方程解信息

@Composable
private fun EquationOverlay(
    info: EquationInfo,
    modifier: Modifier,
    onShowAll: () -> Unit
) {
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = baseColor)
    val summary = remember(info) { equationSummary(info) }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = summary.text, style = style)
        if (summary.expandable) {
            Text(
                text = " ‹›",
                style = style.copy(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)),
                modifier = Modifier
                    .clickable(onClick = onShowAll)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun EquationDialog(info: EquationInfo, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("方程的解") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (info.isEmpty) {
                    Text("当前可视范围内没有实根。")
                } else {
                    info.roots.forEachIndexed { index, value ->
                        Text("x${subscript(index + 1)} = ${formatRoot(value)}")
                    }
                    if (info.truncated) {
                        Text(
                            text = "根较多，仅显示前 ${info.roots.size} 个。放大后再看可减少数量。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private data class EquationSummary(val text: String, val expandable: Boolean)

private fun equationSummary(info: EquationInfo): EquationSummary {
    if (info.isEmpty) return EquationSummary("无实根", false)
    val shown = info.roots.take(3)
    val body = shown.mapIndexed { index, value ->
        "x${subscript(index + 1)}=${formatRoot(value)}"
    }.joinToString("  ")
    val hasMore = info.roots.size > shown.size || info.truncated
    return if (hasMore) EquationSummary("$body  …", true) else EquationSummary(body, false)
}

private fun subscript(index: Int): String = when (index) {
    1 -> "₁"
    2 -> "₂"
    3 -> "₃"
    4 -> "₄"
    5 -> "₅"
    6 -> "₆"
    7 -> "₇"
    8 -> "₈"
    9 -> "₉"
    else -> "${index}"
}

// ------------------------------------------------------------------ 绘制辅助

private fun DrawScope.drawGridLayer(viewport: PlotViewport, color: Color) {
    if (!viewport.isUsable) return
    val step = niceNumber(76f / viewport.scale)
    val pixelStep = step * viewport.scale
    if (!step.isFinite() || step <= 0f || !pixelStep.isFinite() || pixelStep < 6f) return

    val firstCol = floor(viewport.worldX(0f) / step).toInt()
    val lastCol = ceil(viewport.worldX(size.width) / step).toInt()
    if (lastCol - firstCol in 0..400) {
        for (i in firstCol..lastCol) {
            val x = viewport.screenX(i * step)
            if (x.isFinite()) {
                drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
            }
        }
    }

    val firstRow = floor(viewport.worldY(size.height) / step).toInt()
    val lastRow = ceil(viewport.worldY(0f) / step).toInt()
    if (lastRow - firstRow in 0..400) {
        for (i in firstRow..lastRow) {
            val y = viewport.screenY(i * step)
            if (y.isFinite()) {
                drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
            }
        }
    }
}

private fun DrawScope.drawAxesLayer(viewport: PlotViewport, color: Color) {
    if (!viewport.isUsable) return
    val axisY = viewport.screenY(0.0)
    val axisX = viewport.screenX(0.0)
    if (axisY.isFinite() && axisY in 0f..size.height) {
        drawLine(color, Offset(0f, axisY), Offset(size.width, axisY), 2.5f)
    }
    if (axisX.isFinite() && axisX in 0f..size.width) {
        drawLine(color, Offset(axisX, 0f), Offset(axisX, size.height), 2.5f)
    }
}

private fun DrawScope.drawTickLabels(
    viewport: PlotViewport,
    textMeasurer: TextMeasurer,
    style: TextStyle
) {
    if (!viewport.isUsable) return
    val step = niceNumber(150f / viewport.scale)
    val pixelStep = step * viewport.scale
    if (!step.isFinite() || step <= 0f || !pixelStep.isFinite() || pixelStep < 24f) return

    val axisY = viewport.screenY(0.0)
    val axisX = viewport.screenX(0.0)
    val labelTop = if (axisY.isFinite() && axisY in 0f..size.height) axisY + 4f else size.height - 18f
    val labelLeft = if (axisX.isFinite() && axisX in 0f..size.width) axisX + 4f else 4f

    val firstCol = floor(viewport.worldX(0f) / step).toInt()
    val lastCol = ceil(viewport.worldX(size.width) / step).toInt()
    if (lastCol - firstCol in 0..80) {
        for (i in firstCol..lastCol) {
            if (i == 0) continue
            val value = i * step
            if (abs(value) < 1e-9f) continue
            val x = viewport.screenX(value)
            if (!x.isFinite()) continue
            // 先量再画：越界的标签直接跳过。
            // 不能用 drawText(textMeasurer, text, offset) 那个重载——它会用
            // "画布宽 - offset.x" 推导约束，标签一旦落到右边界外就抛出
            // IllegalArgumentException: maxWidth(-N) must be >= than minWidth(0)。
            drawLabel(textMeasurer, fmt(value), x + 3f, labelTop, style)
        }
    }

    val firstRow = floor(viewport.worldY(size.height) / step).toInt()
    val lastRow = ceil(viewport.worldY(0f) / step).toInt()
    if (lastRow - firstRow in 0..80) {
        for (i in firstRow..lastRow) {
            if (i == 0) continue
            val value = i * step
            val y = viewport.screenY(value)
            if (!y.isFinite()) continue
            drawLabel(textMeasurer, fmt(value), labelLeft, y + 2f, style)
        }
    }
}

/** 测量后检查边界再绘制，保证绘制约束始终合法。 */
private fun DrawScope.drawLabel(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    style: TextStyle
) {
    if (!x.isFinite() || !y.isFinite()) return
    if (x < 0f || y < 0f || x >= size.width || y >= size.height) return
    val layout = textMeasurer.measure(text, style)
    if (x + layout.size.width > size.width) return
    if (y + layout.size.height > size.height) return
    drawText(layout, topLeft = Offset(x, y))
}

private fun DrawScope.drawScaleBar(
    viewport: PlotViewport,
    textMeasurer: TextMeasurer,
    style: TextStyle,
    color: Color
) {
    if (!viewport.isUsable) return
    val nice = niceNumber(90f / viewport.scale)
    val pixelLength = nice * viewport.scale
    if (!pixelLength.isFinite() || pixelLength <= 0f) return

    val y = size.height - 18f
    val x2 = size.width - 14f
    val x1 = (x2 - pixelLength).coerceAtLeast(14f)
    drawLine(color, Offset(x1, y), Offset(x2, y), 2f)
    drawLine(color, Offset(x1, y - 5f), Offset(x1, y + 5f), 2f)
    drawLine(color, Offset(x2, y - 5f), Offset(x2, y + 5f), 2f)

    val layout = textMeasurer.measure(fmt(nice), style)
    drawText(
        layout,
        topLeft = Offset((x1 + x2) / 2f - layout.size.width / 2f, y - layout.size.height - 6f)
    )
}

// ------------------------------------------------------------------ 命中测试

private fun hitTest(
    evaluator: Exp4jEvaluator,
    functions: List<PlotFunction>,
    viewport: PlotViewport,
    screen: Offset
): TapInfo? {
    if (!viewport.isUsable) return null

    var best: TapInfo? = null
    var bestDistance = 40f
    val worldX = viewport.worldX(screen.x)
    val worldY = viewport.worldY(screen.y)

    for (function in functions) {
        if (!function.visible) continue
        when (val parsed = parsePlotExpression(function.expression)) {
            is PlotExpression.Explicit -> {
                val prepared = evaluator.prepareExplicit(parsed.expr) ?: continue
                // 落在 π 的有理倍数附近时吸附过去，让 "π/2" 这类读数点得到；
                // 吸附后算出异常的 y（例如 tan 的渐近线）就退回原始位置。
                val snappedX = snapToPiMultiple(worldX, viewport.scale)
                var pickedX = snappedX
                var value = prepared.eval(pickedX)
                if (!value.isFinite() || abs(value) > 1e6) {
                    pickedX = worldX
                    value = prepared.eval(pickedX)
                }
                if (!value.isFinite()) continue
                val y = viewport.screenY(value)
                if (!y.isFinite()) continue
                val distance = abs(y - screen.y)
                if (distance < bestDistance) {
                    bestDistance = distance
                    best = TapInfo(
                        screen = Offset(viewport.screenX(pickedX), y),
                        worldX = pickedX.toFloat(),
                        worldY = value.toFloat(),
                        colorArgb = function.colorArgb
                    )
                }
            }
            is PlotExpression.Implicit -> {
                val prepared = evaluator.prepareImplicit(parsed.expr) ?: continue
                val value = prepared.eval(worldX, worldY)
                if (!value.isFinite()) continue
                if (best == null && abs(value) < 0.1) {
                    best = TapInfo(screen, worldX.toFloat(), worldY.toFloat(), function.colorArgb)
                }
            }
            is PlotExpression.Invalid -> Unit
        }
    }
    return best
}

// ------------------------------------------------------------------ 数字格式化

private fun niceNumber(value: Float): Float {
    if (!value.isFinite() || value <= 0f) return 1f
    val exponent = floor(log10(value.toDouble())).toInt()
    val base = 10.0.pow(exponent).toFloat()
    if (!base.isFinite() || base <= 0f) return 1f
    val normalized = value / base
    val nice = when {
        normalized < 1.5f -> 1f
        normalized < 3.5f -> 2f
        normalized < 7.5f -> 5f
        else -> 10f
    }
    return nice * base
}

private fun fmt(value: Float): String {
    if (!value.isFinite()) return ""
    val magnitude = abs(value)
    // cos(π/2) 这类结果会带 1e-17 量级的浮点噪声，显示成 0 才符合直觉
    if (magnitude < 5e-4f) return "0"
    if (magnitude >= 100_000f) {
        return String.format(Locale.US, "%.2e", value)
    }
    val rounded = round(value * 1000f) / 1000f
    return if (rounded == rounded.toLong().toFloat()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
