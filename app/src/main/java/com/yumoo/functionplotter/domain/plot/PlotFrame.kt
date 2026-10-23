package com.yumoo.functionplotter.domain.plot

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

data class PlotFrame(
    val curves: List<PlotCurve>,
    val invalidIds: Set<Long>,
    val equation: EquationInfo? = null
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
                    }
                }
                is PlotExpression.Implicit -> {
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
    return PlotFrame(curves, invalid, equation)
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
