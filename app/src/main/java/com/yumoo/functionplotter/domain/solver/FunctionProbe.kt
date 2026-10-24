package com.yumoo.functionplotter.domain.solver

import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import kotlin.math.abs

/**
 * 选中函数的正反向求值：给 x 求 y，或给 y 反解 x（数值）。
 */
object FunctionProbe {

    private val evaluator = Exp4jEvaluator()

    fun evalAt(expression: String, x: Double): Double? {
        return try {
            val prepared = evaluator.prepareExplicit(expression) ?: return null
            val y = prepared.eval(x)
            if (y.isFinite()) y else null
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * 反解：在 [from, to] 上找 f(x)=target 的根，最多返回 [maxRoots] 个。
     */
    fun solveX(
        expression: String,
        target: Double,
        from: Double = -200.0,
        to: Double = 200.0,
        maxRoots: Int = 3
    ): List<Double> {
        val prepared = try {
            evaluator.prepareExplicit(expression)
        } catch (t: Throwable) {
            null
        } ?: return emptyList()

        val roots = ArrayList<Double>()
        val samples = 2000
        var prevX = from
        var prevDiff = Double.NaN
        for (i in 0..samples) {
            val x = from + (to - from) * i / samples
            val value = try { prepared.eval(x) } catch (t: Throwable) { Double.NaN }
            val diff = if (value.isFinite()) value - target else Double.NaN
            if (!diff.isNaN() && diff == 0.0) {
                if (roots.none { abs(it - x) < 1e-6 }) {
                    roots += x
                    if (roots.size >= maxRoots) return roots
                }
            }
            if (!prevDiff.isNaN() && !diff.isNaN() && prevDiff * diff < 0) {
                val root = bisect(prepared::eval, target, prevX, x)
                if (root != null && roots.none { abs(it - root) < 1e-6 }) {
                    roots += root
                    if (roots.size >= maxRoots) return roots
                }
            }
            prevX = x
            prevDiff = diff
        }
        return roots
    }

    private fun bisect(f: (Double) -> Double, target: Double, left: Double, right: Double): Double? {
        var a = left
        var b = right
        var fa = f(a) - target
        repeat(64) {
            val mid = (a + b) / 2
            val fm = f(mid) - target
            if (!fm.isFinite()) return null
            if (fa * fm <= 0) b = mid else { a = mid; fa = fm }
        }
        val root = (a + b) / 2
        return if (root.isFinite()) root else null
    }
}