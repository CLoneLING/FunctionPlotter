package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.plot.PlotViewport
import com.yumoo.functionplotter.domain.plot.buildCurvePath
import kotlin.math.abs
import kotlin.math.round

@Composable
fun FunctionCanvas(
    expression: String,
    showGrid: Boolean,
    showAxisLabels: Boolean,
    sampleStep: Float,
    modifier: Modifier = Modifier
) {
    val evaluator = remember { Exp4jEvaluator() }
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableFloatStateOf(60f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val curveColor = MaterialTheme.colorScheme.primary
    val axisColor = Color.Gray
    val labelColor = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val ns = (scale * zoom).coerceIn(10f, 500f)
                    if (ns.isFinite()) scale = ns
                    val nx = offset.x + pan.x
                    val ny = offset.y + pan.y
                    if (nx.isFinite() && ny.isFinite()) {
                        offset = Offset(
                            nx.coerceIn(-100_000f, 100_000f),
                            ny.coerceIn(-100_000f, 100_000f)
                        )
                    }
                }
            }
    ) {
        val centerX = size.width / 2f + offset.x
        val centerY = size.height / 2f + offset.y

        if (!centerX.isFinite() || !centerY.isFinite() ||
            !scale.isFinite() || scale <= 0f
        ) return@Canvas

        val viewport = PlotViewport(centerX, centerY, scale)
        val maxTicks = 60

        // 网格
        if (showGrid) {
            val gridColor = axisColor.copy(alpha = 0.15f)
            var count = 0
            var gx = centerX % scale
            if (gx < 0f) gx += scale
            while (gx < size.width && count < maxTicks * 4) {
                drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), 1f)
                gx += scale
                count++
            }
            count = 0
            var gy = centerY % scale
            if (gy < 0f) gy += scale
            while (gy < size.height && count < maxTicks * 4) {
                drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), 1f)
                gy += scale
                count++
            }
        }

        // 坐标轴
        if (centerY > -1e5f && centerY < size.height + 1e5f) {
            drawLine(axisColor, Offset(0f, centerY), Offset(size.width, centerY), 3f)
        }
        if (centerX > -1e5f && centerX < size.width + 1e5f) {
            drawLine(axisColor, Offset(centerX, 0f), Offset(centerX, size.height), 3f)
        }

        // 刻度
        if (showAxisLabels) {
            val style = TextStyle(fontSize = 10.sp, color = labelColor)
            var count = 0
            var gx = centerX % scale
            if (gx < 0f) gx += scale
            while (gx < size.width && count < maxTicks) {
                if (abs(gx - centerX) > 1f) {
                    val mathX = (gx - centerX) / scale
                    if (mathX.isFinite()) {
                        drawText(
                            textMeasurer,
                            formatTick(mathX),
                            topLeft = Offset(gx + 2f, centerY + 4f),
                            style = style
                        )
                    }
                }
                gx += scale
                count++
            }
            count = 0
            var gy = centerY % scale
            if (gy < 0f) gy += scale
            while (gy < size.height && count < maxTicks) {
                if (abs(gy - centerY) > 1f) {
                    val mathY = (centerY - gy) / scale
                    if (mathY.isFinite()) {
                        drawText(
                            textMeasurer,
                            formatTick(mathY),
                            topLeft = Offset(centerX + 4f, gy + 2f),
                            style = style
                        )
                    }
                }
                gy += scale
                count++
            }
        }

        // 曲线
        if (expression.isNotBlank() && evaluator.isValid(expression)) {
            val path = buildCurvePath(evaluator, expression, size.width, viewport, sampleStep)
            drawPath(path, color = curveColor, style = Stroke(width = 4f))
        }
    }
}

private fun formatTick(v: Float): String {
    if (!v.isFinite()) return ""
    val rounded = round(v * 100f) / 100f
    return if (rounded == rounded.toLong().toFloat()) rounded.toLong().toString()
    else rounded.toString()
}