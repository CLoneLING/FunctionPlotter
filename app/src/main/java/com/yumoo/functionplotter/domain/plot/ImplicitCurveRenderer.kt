package com.yumoo.functionplotter.domain.plot

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import com.yumoo.functionplotter.domain.parser.PreparedFunction
import kotlin.math.max
import kotlin.math.min

private const val MAX_CELLS = 170

/**
 * 用 marching squares 画 F(x, y) = 0。
 *
 * 相比旧实现的改进：
 *  - 复用预编译表达式（旧版每个格点都重新 build 解析器，170×170 网格就是
 *    两万九千次解析，必然卡死主线程）；
 *  - 网格规模按画布尺寸自适应并封顶，坐标全部落在画布内；
 *  - NaN / Infinity 单元格直接跳过，不会污染 Path。
 */
fun buildImplicitPath(function: PreparedFunction, viewport: PlotViewport): Path {
    val path = Path()
    if (!viewport.isUsable) return path

    // 同时按短边和长边约束格子数量，长屏手机上也不会算出几万个格点
    val cellSize = maxOf(
        min(viewport.width, viewport.height) / MAX_CELLS,
        max(viewport.width, viewport.height) / 220f
    ).coerceIn(4f, 14f)
    val cols = (viewport.width / cellSize).toInt() + 2
    val rows = (viewport.height / cellSize).toInt() + 2
    if (cols < 4 || rows < 4 || cols > 400 || rows > 400) return path

    val values = Array(rows) { DoubleArray(cols) }
    for (row in 0 until rows) {
        val screenY = row * cellSize
        val worldY = viewport.worldY(screenY)
        val line = values[row]
        for (col in 0 until cols) {
            line[col] = function.eval(viewport.worldX(col * cellSize), worldY)
        }
    }

    for (row in 0 until rows - 1) {
        for (col in 0 until cols - 1) {
            val v00 = values[row][col]
            val v10 = values[row][col + 1]
            val v01 = values[row + 1][col]
            val v11 = values[row + 1][col + 1]
            if (!v00.isFinite() || !v10.isFinite() || !v01.isFinite() || !v11.isFinite()) continue

            var mask = 0
            if (v00 > 0) mask = mask or 1
            if (v10 > 0) mask = mask or 2
            if (v11 > 0) mask = mask or 4
            if (v01 > 0) mask = mask or 8
            if (mask == 0 || mask == 15) continue

            val x0 = col * cellSize
            val y0 = row * cellSize
            val x1 = x0 + cellSize
            val y1 = y0 + cellSize

            val points = ArrayList<Offset>(4)
            addCrossing(points, v00, v10, x0, y0, cellSize, 0f)
            addCrossing(points, v10, v11, x1, y0, 0f, cellSize)
            addCrossing(points, v01, v11, x0, y1, cellSize, 0f)
            addCrossing(points, v00, v01, x0, y0, 0f, cellSize)

            when (points.size) {
                2 -> {
                    path.moveTo(points[0].x, points[0].y)
                    path.lineTo(points[1].x, points[1].y)
                }
                4 -> {
                    // 鞍点：用格心符号决定两条线段如何配对
                    val center = (v00 + v10 + v01 + v11) / 4
                    if (center > 0) {
                        path.moveTo(points[0].x, points[0].y)
                        path.lineTo(points[3].x, points[3].y)
                        path.moveTo(points[1].x, points[1].y)
                        path.lineTo(points[2].x, points[2].y)
                    } else {
                        path.moveTo(points[0].x, points[0].y)
                        path.lineTo(points[1].x, points[1].y)
                        path.moveTo(points[2].x, points[2].y)
                        path.lineTo(points[3].x, points[3].y)
                    }
                }
            }
        }
    }
    return path
}

/** 线性插值求等值线与一条格边的交点；符号相同或退化时返回。 */
private fun addCrossing(
    out: MutableList<Offset>,
    valueA: Double,
    valueB: Double,
    startX: Float,
    startY: Float,
    deltaX: Float,
    deltaY: Float
) {
    if ((valueA > 0) == (valueB > 0)) return
    val denominator = valueA - valueB
    if (denominator == 0.0) return
    val t = (valueA / denominator).toFloat()
    if (!t.isFinite()) return
    out.add(Offset(startX + deltaX * t, startY + deltaY * t))
}
