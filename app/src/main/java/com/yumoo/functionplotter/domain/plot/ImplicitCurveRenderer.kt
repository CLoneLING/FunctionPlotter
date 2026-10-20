package com.yumoo.functionplotter.domain.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator

fun buildImplicitPath(
    evaluator: Exp4jEvaluator,
    expression: String,
    viewport: PlotViewport,
    width: Float,
    height: Float,
    gridStep: Float
): Path {
    val path = Path()
    if (width <= 0f || height <= 0f) return path
    if (!viewport.scale.isFinite() || viewport.scale <= 0f) return path
    if (!viewport.centerX.isFinite() || !viewport.centerY.isFinite()) return path

    val step = gridStep.coerceIn(6f, 24f)
    val cols = ((width / step).toInt() + 2).coerceAtMost(160)
    val rows = ((height / step).toInt() + 2).coerceAtMost(160)

    val values = Array(rows) { DoubleArray(cols) }
    for (r in 0 until rows) {
        val screenY = r * step
        val mathY = ((viewport.centerY - screenY) / viewport.scale).toDouble()
        for (c in 0 until cols) {
            val screenX = c * step
            val mathX = ((screenX - viewport.centerX) / viewport.scale).toDouble()
            values[r][c] = evaluator.evaluate2(expression, mathX, mathY)
        }
    }

    for (r in 0 until rows - 1) {
        for (c in 0 until cols - 1) {
            val v00 = values[r][c]
            val v10 = values[r][c + 1]
            val v01 = values[r + 1][c]
            val v11 = values[r + 1][c + 1]
            if (v00.isNaN() || v10.isNaN() || v01.isNaN() || v11.isNaN()) continue
            if (v00.isInfinite() || v10.isInfinite() || v01.isInfinite() || v11.isInfinite()) continue

            var mask = 0
            if (v00 > 0) mask = mask or 1
            if (v10 > 0) mask = mask or 2
            if (v11 > 0) mask = mask or 4
            if (v01 > 0) mask = mask or 8
            if (mask == 0 || mask == 15) continue

            val x0 = c * step
            val x1 = (c + 1) * step
            val y0 = r * step
            val y1 = (r + 1) * step

            val points = mutableListOf<Offset>()
            if (((mask and 1) != 0) != ((mask and 2) != 0)) {
                val t = (v00 / (v00 - v10)).toFloat()
                if (t.isFinite()) points.add(Offset(x0 + step * t, y0))
            }
            if (((mask and 2) != 0) != ((mask and 4) != 0)) {
                val t = (v10 / (v10 - v11)).toFloat()
                if (t.isFinite()) points.add(Offset(x1, y0 + step * t))
            }
            if (((mask and 8) != 0) != ((mask and 4) != 0)) {
                val t = (v01 / (v01 - v11)).toFloat()
                if (t.isFinite()) points.add(Offset(x0 + step * t, y1))
            }
            if (((mask and 1) != 0) != ((mask and 8) != 0)) {
                val t = (v00 / (v00 - v01)).toFloat()
                if (t.isFinite()) points.add(Offset(x0, y0 + step * t))
            }

            if (points.size == 2) {
                path.moveTo(points[0].x, points[0].y)
                path.lineTo(points[1].x, points[1].y)
            } else if (points.size == 4) {
                val center = (v00 + v10 + v01 + v11) / 4
                if (center > 0) {
                    path.moveTo(points[0].x, points[0].y); path.lineTo(points[3].x, points[3].y)
                    path.moveTo(points[1].x, points[1].y); path.lineTo(points[2].x, points[2].y)
                } else {
                    path.moveTo(points[0].x, points[0].y); path.lineTo(points[1].x, points[1].y)
                    path.moveTo(points[2].x, points[2].y); path.lineTo(points[3].x, points[3].y)
                }
            }
        }
    }
    return path
}