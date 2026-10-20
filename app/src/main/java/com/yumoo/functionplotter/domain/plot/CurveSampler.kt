package com.yumoo.functionplotter.domain.plot

import androidx.compose.ui.graphics.Path
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import kotlin.math.abs

data class PlotViewport(
    val centerX: Float,
    val centerY: Float,
    val scale: Float
)

fun buildCurvePath(
    evaluator: Exp4jEvaluator,
    expression: String,
    width: Float,
    viewport: PlotViewport,
    sampleStep: Float
): Path {
    val path = Path()
    if (!width.isFinite() || width <= 0f) return path
    if (!viewport.centerX.isFinite() ||
        !viewport.centerY.isFinite() ||
        !viewport.scale.isFinite() ||
        viewport.scale <= 0f
    ) return path

    var started = false
    var screenX = 0f
    val step = if (sampleStep.isFinite()) sampleStep.coerceIn(1.5f, 8f) else 2f
    val maxIter = (width / step).toInt() + 50
    var safety = 0

    while (screenX <= width && safety < maxIter) {
        val mathX: Double = ((screenX - viewport.centerX) / viewport.scale).toDouble()
        if (!mathX.isFinite()) { started = false; screenX += step; safety++; continue }

        val mathY: Double = evaluator.evaluate(expression, mathX)
        if (mathY.isNaN() || mathY.isInfinite()) {
            started = false; screenX += step; safety++; continue
        }

        val screenY = viewport.centerY - mathY.toFloat() * viewport.scale
        if (screenY.isFinite() && abs(screenY) < 1e6f) {
            if (!started) { path.moveTo(screenX, screenY); started = true }
            else path.lineTo(screenX, screenY)
        } else started = false

        screenX += step
        safety++
    }
    return path
}