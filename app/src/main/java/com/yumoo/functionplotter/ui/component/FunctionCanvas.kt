package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.data.model.PlotFunction
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.parser.PlotExpression
import com.yumoo.functionplotter.domain.parser.parsePlotExpression
import com.yumoo.functionplotter.domain.plot.PlotViewport
import com.yumoo.functionplotter.domain.plot.buildCurvePath
import com.yumoo.functionplotter.domain.plot.buildImplicitPath
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round

private const val MIN_SCALE = 10f
private const val MAX_SCALE = 500f
private const val MAX_LIVE_SCALE = 10f

private sealed class DrawItem {
    data class ExplicitItem(val id: Long, val path: Path, val colorArgb: Long) : DrawItem()
    data class ImplicitItem(val id: Long, val path: Path, val colorArgb: Long) : DrawItem()
}

private data class TapInfo(
    val screen: Offset,
    val mathX: Float,
    val mathY: Float,
    val colorArgb: Long
)

@OptIn(FlowPreview::class)
@Composable
fun FunctionCanvas(
    functions: List<PlotFunction>,
    showGrid: Boolean,
    showAxisLabels: Boolean,
    sampleStep: Float,
    modifier: Modifier = Modifier
) {
    val evaluator = remember { Exp4jEvaluator() }
    val textMeasurer = rememberTextMeasurer()

    var committedScale by remember { mutableFloatStateOf(60f) }
    var committedOffset by remember { mutableStateOf(Offset.Zero) }
    var liveScale by remember { mutableFloatStateOf(1f) }
    var liveOffset by remember { mutableStateOf(Offset.Zero) }
    var gestureActive by remember { mutableStateOf(false) }
    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    var tapInfo by remember { mutableStateOf<TapInfo?>(null) }

    val canvasCenterX = if (sizePx.width > 0) sizePx.width / 2f else 0f
    val canvasCenterY = if (sizePx.height > 0) sizePx.height / 2f else 0f

    val committedCenterX = canvasCenterX + committedOffset.x
    val committedCenterY = canvasCenterY + committedOffset.y
    val committedViewport = PlotViewport(committedCenterX, committedCenterY, committedScale)

    // 实时视口：GPU 变换后数学 0 点的真实屏幕位置
    val liveCenterX = canvasCenterX + liveOffset.x + committedOffset.x * liveScale
    val liveCenterY = canvasCenterY + liveOffset.y + committedOffset.y * liveScale
    val liveTotalScale = (committedScale * liveScale).coerceIn(MIN_SCALE, MAX_SCALE)
    val liveViewport = PlotViewport(liveCenterX, liveCenterY, liveTotalScale)

    // Path 仅依赖 committed，手势期间不重算
    val drawItems = remember(functions, committedViewport, sizePx, sampleStep) {
        buildDrawItems(evaluator, functions, committedViewport, sizePx, sampleStep)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(gestureActive, liveScale, liveOffset) }
            .debounce(120)
            .collect { (active, s, o) ->
                if (!active && (s != 1f || o != Offset.Zero)) {
                    committedScale = (committedScale * s).coerceIn(MIN_SCALE, MAX_SCALE)
                    committedOffset = Offset(
                        (committedOffset.x + o.x).coerceIn(-100_000f, 100_000f),
                        (committedOffset.y + o.y).coerceIn(-100_000f, 100_000f)
                    )
                    liveScale = 1f
                    liveOffset = Offset.Zero
                }
            }
    }

    val axisColor = Color.Gray
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelBg = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.onSizeChanged { sizePx = it }.clipToBounds()) {

        // Layer 1：函数曲线（GPU 变换）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = liveScale
                    scaleY = liveScale
                    translationX = liveOffset.x
                    translationY = liveOffset.y
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawItems.forEach { item ->
                    when (item) {
                        is DrawItem.ExplicitItem ->
                            drawPath(item.path, color = Color(item.colorArgb), style = Stroke(width = 4f))
                        is DrawItem.ImplicitItem ->
                            drawPath(item.path, color = Color(item.colorArgb), style = Stroke(width = 3f))
                    }
                }
                tapInfo?.let { info ->
                    val c = Color(info.colorArgb)
                    drawCircle(c, radius = 8f, center = info.screen, style = Stroke(width = 3f))
                    drawCircle(c, radius = 3f, center = info.screen)
                }
            }
        }

        // Layer 2：网格 + 刻度数字（GPU 变换）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = liveScale
                    scaleY = liveScale
                    translationX = liveOffset.x
                    translationY = liveOffset.y
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (showGrid) drawGrid(this, committedViewport, axisColor)
                if (showAxisLabels) drawAxisLabels(this, committedViewport, labelColor, textMeasurer)
            }
        }

        // Layer 3：坐标轴 + 比例尺（实时重绘，基于 liveViewport）
        Canvas(Modifier.fillMaxSize()) {
            if (!liveViewport.scale.isFinite() || liveViewport.scale <= 0f) return@Canvas
            if (!liveViewport.centerX.isFinite() || !liveViewport.centerY.isFinite()) return@Canvas
            if (liveViewport.centerY in -1e5f..(size.height + 1e5f)) {
                drawLine(axisColor, Offset(0f, liveViewport.centerY), Offset(size.width, liveViewport.centerY), 3f)
            }
            if (liveViewport.centerX in -1e5f..(size.width + 1e5f)) {
                drawLine(axisColor, Offset(liveViewport.centerX, 0f), Offset(liveViewport.centerX, size.height), 3f)
            }
            drawScaleBar(textMeasurer, labelColor, liveViewport, size)
        }

        // 坐标标签（非变换层，始终保持清晰）
        tapInfo?.let { info ->
            Canvas(Modifier.fillMaxSize()) {
                val layout = textMeasurer.measure(
                    "(${fmt(info.mathX)}, ${fmt(info.mathY)})",
                    style = TextStyle(fontSize = 11.sp, color = labelColor)
                )
                val tx = (info.screen.x + 12f).coerceAtMost(size.width - layout.size.width - 8f)
                val ty = (info.screen.y - layout.size.height - 12f).coerceAtLeast(4f)
                drawRect(
                    labelBg.copy(alpha = 0.92f),
                    topLeft = Offset(tx - 4f, ty - 2f),
                    size = Size(layout.size.width + 8f, layout.size.height + 4f)
                )
                drawText(layout, topLeft = Offset(tx, ty))
            }
        }

        // 手势层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { pos ->
                        if (!gestureActive) tapInfo = handleTap(evaluator, functions, committedViewport, pos)
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        gestureActive = true
                        do {
                            val event = awaitPointerEvent()
                            val pointerCount = event.changes.count { it.pressed }
                            if (pointerCount >= 2) {
                                val zoom = event.calculateZoom()
                                if (zoom.isFinite() && zoom > 0f && zoom != 1f) {
                                    val newTotal = (committedScale * liveScale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                                    val newLive = (newTotal / committedScale).coerceAtMost(MAX_LIVE_SCALE)
                                    if (newLive.isFinite() && newLive > 0f) liveScale = newLive
                                }
                            }
                            val pan = event.calculatePan()
                            if (pan.x.isFinite() && pan.y.isFinite()) liveOffset += pan
                            event.changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })
                        gestureActive = false
                    }
                }
        )
    }
}

private fun drawGrid(drawScope: DrawScope, viewport: PlotViewport, axisColor: Color) {
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return
    val targetPx = 80f
    val gridMathStep = niceNumber(targetPx / viewport.scale)
    val gridPxStep = gridMathStep * viewport.scale
    if (!gridPxStep.isFinite() || gridPxStep < 20f) return
    val gridColor = axisColor.copy(alpha = 0.15f)
    val size = drawScope.size
    var gx = viewport.centerX % gridPxStep
    if (gx < 0f) gx += gridPxStep
    var count = 0
    while (gx < size.width && count < 300) {
        drawScope.drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1f)
        gx += gridPxStep; count++
    }
    var gy = viewport.centerY % gridPxStep
    if (gy < 0f) gy += gridPxStep
    count = 0
    while (gy < size.height && count < 300) {
        drawScope.drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), 1f)
        gy += gridPxStep; count++
    }
}

private fun drawAxisLabels(
    drawScope: DrawScope,
    viewport: PlotViewport,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return
    val targetPx = 160f
    val labelMathStep = niceNumber(targetPx / viewport.scale)
    val labelPxStep = labelMathStep * viewport.scale
    if (!labelPxStep.isFinite() || labelPxStep < 40f) return
    val style = TextStyle(fontSize = 10.sp, color = labelColor)
    val size = drawScope.size
    var gx = viewport.centerX % labelPxStep
    if (gx < 0f) gx += labelPxStep
    var count = 0
    while (gx < size.width && count < 50) {
        if (abs(gx - viewport.centerX) > 1f) {
            val mathX = (gx - viewport.centerX) / viewport.scale
            if (mathX.isFinite()) {
                drawScope.drawText(textMeasurer, fmt(mathX), Offset(gx + 2f, viewport.centerY + 4f), style = style)
            }
        }
        gx += labelPxStep; count++
    }
    var gy = viewport.centerY % labelPxStep
    if (gy < 0f) gy += labelPxStep
    count = 0
    while (gy < size.height && count < 50) {
        if (abs(gy - viewport.centerY) > 1f) {
            val mathY = (viewport.centerY - gy) / viewport.scale
            if (mathY.isFinite()) {
                drawScope.drawText(textMeasurer, fmt(mathY), Offset(viewport.centerX + 4f, gy + 2f), style = style)
            }
        }
        gy += labelPxStep; count++
    }
}

private fun buildDrawItems(
    evaluator: Exp4jEvaluator,
    functions: List<PlotFunction>,
    viewport: PlotViewport,
    sizePx: IntSize,
    sampleStep: Float
): List<DrawItem> {
    if (sizePx.width <= 0 || sizePx.height <= 0) return emptyList()
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return emptyList()
    if (!viewport.centerX.isFinite() || !viewport.centerY.isFinite()) return emptyList()

    val result = mutableListOf<DrawItem>()
    val width = sizePx.width.toFloat()
    val height = sizePx.height.toFloat()
    val gridStep = maxOf(8f, min(width, height) / 80f)

    for (f in functions) {
        if (!f.visible) continue
        when (val parsed = parsePlotExpression(f.expression)) {
            is PlotExpression.Explicit -> {
                if (evaluator.isValid(parsed.expr)) {
                    val path = buildCurvePath(evaluator, parsed.expr, width, viewport, sampleStep)
                    result.add(DrawItem.ExplicitItem(f.id, path, f.colorArgb))
                }
            }
            is PlotExpression.Implicit -> {
                if (evaluator.isValid2(parsed.expr)) {
                    val path = buildImplicitPath(evaluator, parsed.expr, viewport, width, height, gridStep)
                    result.add(DrawItem.ImplicitItem(f.id, path, f.colorArgb))
                }
            }
            is PlotExpression.Invalid -> Unit
        }
    }
    return result
}

private fun handleTap(
    evaluator: Exp4jEvaluator,
    functions: List<PlotFunction>,
    viewport: PlotViewport,
    screen: Offset
): TapInfo? {
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return null
    val mathX = (screen.x - viewport.centerX) / viewport.scale
    val mathY = (viewport.centerY - screen.y) / viewport.scale
    if (!mathX.isFinite() || !mathY.isFinite()) return null

    var best: TapInfo? = null
    var bestDist = 36f

    for (f in functions) {
        if (!f.visible) continue
        when (val parsed = parsePlotExpression(f.expression)) {
            is PlotExpression.Explicit -> {
                if (!evaluator.isValid(parsed.expr)) continue
                val fy = evaluator.evaluate(parsed.expr, mathX.toDouble())
                if (fy.isNaN() || fy.isInfinite()) continue
                val screenY = viewport.centerY - fy.toFloat() * viewport.scale
                if (!screenY.isFinite()) continue
                val d = abs(screenY - screen.y)
                if (d < bestDist) {
                    bestDist = d
                    best = TapInfo(screen, mathX, fy.toFloat(), f.colorArgb)
                }
            }
            is PlotExpression.Implicit -> {
                if (!evaluator.isValid2(parsed.expr)) continue
                val v = evaluator.evaluate2(parsed.expr, mathX.toDouble(), mathY.toDouble())
                if (v.isNaN() || v.isInfinite()) continue
                if (abs(v) < 0.2 && best == null) best = TapInfo(screen, mathX, mathY, f.colorArgb)
            }
            is PlotExpression.Invalid -> Unit
        }
    }
    return best
}

private fun DrawScope.drawScaleBar(
    textMeasurer: TextMeasurer,
    labelColor: Color,
    viewport: PlotViewport,
    size: Size
) {
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return
    val nice = niceNumber(80f / viewport.scale)
    val pxLen = nice * viewport.scale
    if (!pxLen.isFinite() || pxLen <= 0f) return
    val padding = 16f
    val x2 = size.width - padding
    val x1 = (x2 - pxLen).coerceAtLeast(padding)
    val y = size.height - padding
    val barColor = labelColor.copy(alpha = 0.75f)
    drawLine(barColor, Offset(x1, y), Offset(x2, y), strokeWidth = 3f)
    drawLine(barColor, Offset(x1, y - 6f), Offset(x1, y + 6f), strokeWidth = 3f)
    drawLine(barColor, Offset(x2, y - 6f), Offset(x2, y + 6f), strokeWidth = 3f)
    val layout = textMeasurer.measure(fmt(nice), style = TextStyle(fontSize = 10.sp, color = labelColor))
    drawText(layout, topLeft = Offset((x1 + x2) / 2f - layout.size.width / 2f, y - layout.size.height - 6f))
}

private fun niceNumber(v: Float): Float {
    if (!v.isFinite() || v <= 0f) return 1f
    val exp = floor(log10(v.toDouble())).toInt()
    val base = 10.0.pow(exp).toFloat()
    val n = v / base
    val nice = when {
        n < 1.5f -> 1f
        n < 3.5f -> 2f
        n < 7.5f -> 5f
        else -> 10f
    }
    return nice * base
}

private fun fmt(v: Float): String {
    if (!v.isFinite()) return ""
    val rounded = round(v * 100f) / 100f
    return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString()
    else rounded.toString()
}