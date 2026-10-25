package com.yumoo.functionplotter.domain.solver

import kotlin.math.abs

/**
 * 区间上的取值分析：给定 x 区间求 y 的取值范围，给定 y 区间求满足条件的 x。
 * 都用采样 + 边界细化，适合"看个大概"而不是符号推导。
 */
object FunctionRange {

    data class YRange(
        val min: Double,
        val max: Double,
        val unbounded: Boolean
    )

    /** x ∈ [from, to] 时 y 的取值范围。整段无定义返回 null。 */
    fun overInterval(
        expression: String,
        from: Double,
        to: Double,
        samples: Int = 400
    ): YRange? {
        if (to <= from) return null
        var min = Double.MAX_VALUE
        var max = -Double.MAX_VALUE
        var any = false
        var unbounded = false
        for (i in 0..samples) {
            val x = from + (to - from) * i / samples
            val y = FunctionProbe.evalAt(expression, x) ?: continue
            any = true
            if (abs(y) > 1e6) unbounded = true
            if (y < min) min = y
            if (y > max) max = y
        }
        return if (!any) null else YRange(min, max, unbounded)
    }

    /**
     * 满足 yMin ≤ f(x) ≤ yMax 的 x 区间集合（按采样合并，边界用二分细化）。
     */
    fun solveYRange(
        expression: String,
        yMin: Double,
        yMax: Double,
        from: Double = -200.0,
        to: Double = 200.0,
        samples: Int = 4000
    ): List<Pair<Double, Double>> {
        val step = (to - from) / samples
        val out = ArrayList<Pair<Double, Double>>()
        var start: Double? = null
        var previous = from

        fun inside(x: Double): Boolean {
            val y = FunctionProbe.evalAt(expression, x) ?: return false
            return y >= yMin && y <= yMax
        }

        for (i in 0..samples) {
            val x = from + step * i
            val ok = inside(x)
            if (ok && start == null) {
                start = refine(expression, yMin, yMax, previous, x, entering = true) ?: x
            } else if (!ok && start != null) {
                val edge = refine(expression, yMin, yMax, previous, x, entering = false) ?: previous
                out += start!! to edge
                start = null
            }
            previous = x
        }
        start?.let { out += it to to }
        return out
    }

    /** 在 [a, b] 上二分找区间边界。 */
    private fun refine(
        expression: String,
        yMin: Double,
        yMax: Double,
        a: Double,
        b: Double,
        entering: Boolean
    ): Double? {
        var lo = a
        var hi = b
        repeat(40) {
            val mid = (lo + hi) / 2
            val y = FunctionProbe.evalAt(expression, mid)
            val inRange = y != null && y >= yMin && y <= yMax
            if (entering) {
                if (inRange) hi = mid else lo = mid
            } else {
                if (inRange) lo = mid else hi = mid
            }
        }
        val result = (lo + hi) / 2
        return if (result.isFinite()) result else null
    }
}