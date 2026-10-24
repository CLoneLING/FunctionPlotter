package com.yumoo.functionplotter.domain.plot

import kotlin.math.abs
import androidx.compose.ui.graphics.Path
import com.yumoo.functionplotter.data.model.PlotFunction
import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.parser.PlotExpression
import com.yumoo.functionplotter.domain.parser.parsePlotExpression
import com.yumoo.functionplotter.domain.solver.EquationInfo
import com.yumoo.functionplotter.domain.solver.equationDifference
import com.yumoo.functionplotter.domain.solver.solveRoots

data class PlotCurve(
    val id: Long,
    val colorArgb: Long,
    val path: Path,
    val implicit: Boolean
)

data class PlotPoint(val x: Double, val y: Double)

data class PlotFrame(
    val curves: List<PlotCurve>,
    val invalidIds: Set<Long>,
    val equation: EquationInfo? = null,
    val intersections: List<PlotPoint> = emptyList()
) {
    companion object {
        val Empty = PlotFrame(emptyList(), emptySet())
    }
}

/**
 * 一次性算出当前视口下所有曲线的路径，以及单变量方程在可视域内的根。
 * 该函数是纯计算，调用方应放到后台线程执行（见 FunctionCanvas）。
 *
 * 每条曲线单独 try/catch：某一条表达式在极端坐标下抛错，
 * 不会连带让整个画布崩掉。
 */
fun buildPlotFrame(
    evaluator: Exp4jEvaluator,
    functions: List<PlotFunction>,
    viewport: PlotViewport,
    sampleStep: Float
): PlotFrame {
    if (!viewport.isUsable) return PlotFrame.Empty

    val curves = ArrayList<PlotCurve>(functions.size)
    val invalid = HashSet<Long>()
    var equation: EquationInfo? = null
    val explicit = ArrayList<Pair<Long, (Double) -> Double>>()

    for (function in functions) {
        if (!function.visible) continue
        if (function.expression.isBlank()) continue

        try {
            // 只看第一条可求解的单变量方程，避免多条方程的根混在一起
            if (equation == null) {
                equation = solveEquation(evaluator, function.expression, viewport)
            }
            when (val parsed = parsePlotExpression(function.expression)) {
                is PlotExpression.Explicit -> {
                    val prepared = evaluator.prepareExplicit(parsed.expr)
                    if (prepared == null) {
                        invalid.add(function.id)
                    } else {
                        curves.add(
                            PlotCurve(
                                id = function.id,
                                colorArgb = function.colorArgb,
                                path = buildCurvePath(prepared, viewport, sampleStep),
                                implicit = false
                            )
                        )
                        explicit += function.id to { x: Double -> prepared.eval(x) }
                    }
                }
                is PlotExpression.Implicit -> {
                    // 只含 x 的方程（如 1/(x-1)=0）只求解，不画曲线，
                    // 否则会在分母零点处画出无意义的竖线。
                    if (!function.expression.contains("y")) {
                        continue
                    }
                    val prepared = evaluator.prepareImplicit(parsed.expr)
                    if (prepared == null) {
                        invalid.add(function.id)
                    } else {
                        curves.add(
                            PlotCurve(
                                id = function.id,
                                colorArgb = function.colorArgb,
                                path = buildImplicitPath(prepared, viewport),
                                implicit = true
                            )
                        )
                    }
                }
                is PlotExpression.Invalid -> invalid.add(function.id)
            }
        } catch (t: Throwable) {
            invalid.add(function.id)
        }
    }
    return PlotFrame(curves, invalid, equation, findIntersections(explicit, viewport))
}

private fun solveEquation(
    evaluator: Exp4jEvaluator,
    expression: String,
    viewport: PlotViewport
): EquationInfo? {
    val difference = equationDifference(expression) ?: return null
    val prepared = try {
        evaluator.prepareExplicit(difference)
    } catch (t: Throwable) {
        null
    } ?: return null

    val from = viewport.worldX(0f)
    val to = viewport.worldX(viewport.width)
    return try {
        solveRoots(prepared, from, to)
    } catch (t: Throwable) {
        null
    }
}

/** 两组显函数联立求交点：f(x)-g(x)=0 的实根即方程组的解。 */
private fun findIntersections(
    functions: List<Pair<Long, (Double) -> Double>>,
    viewport: PlotViewport,
    maxPoints: Int = 6
): List<PlotPoint> {
    if (functions.size < 2) return emptyList()
    val result = ArrayList<PlotPoint>()
    val samples = 480
    val height = viewport.height
    for (i in 0 until functions.size - 1) {
        for (j in i + 1 until functions.size) {
            val f = functions[i].second
            val g = functions[j].second
            var prevX = 0.0
            var prevDiff = Double.NaN
            for (k in 0..samples) {
                val x = viewport.worldX(viewport.width * k / samples)
                val a = f(x)
                val b = g(x)
                val diff = if (a.isFinite() && b.isFinite()) a - b else Double.NaN
                if (!prevDiff.isNaN() && !diff.isNaN() && prevDiff * diff < 0) {
                    val root = bisectIntersection(f, g, prevX, x)
                    if (root != null) {
                        val y = f(root)
                        if (y.isFinite() && abs(y) < 1.0e7) {
                            val sy = viewport.screenY(y)
                            if (sy.isFinite() && sy > -height && sy < 2 * height) {
                                val tooClose = result.any { abs(it.x - root) < 1.0e-6 }
                                if (!tooClose) {
                                    result += PlotPoint(root, y)
                                    if (result.size >= maxPoints) return result
                                }
                            }
                        }
                    }
                }
                prevX = x
                prevDiff = diff
            }
        }
    }
    return result
}

private fun bisectIntersection(
    f: (Double) -> Double,
    g: (Double) -> Double,
    left: Double,
    right: Double
): Double? {
    var a = left
    var b = right
    var fa = f(a) - g(a)
    repeat(64) {
        val mid = (a + b) / 2
        val fm = f(mid) - g(mid)
        if (!fm.isFinite()) return null
        if (fa * fm <= 0) b = mid else { a = mid; fa = fm }
    }
    val root = (a + b) / 2
    return if (root.isFinite()) root else null
}