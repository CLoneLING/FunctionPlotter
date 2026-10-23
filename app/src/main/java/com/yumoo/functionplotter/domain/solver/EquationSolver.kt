package com.yumoo.functionplotter.domain.solver

import com.yumoo.functionplotter.domain.parser.ExpressionNormalizer
import com.yumoo.functionplotter.domain.parser.PreparedFunction
import kotlin.math.abs
import kotlin.math.round

/**
 * 单变量方程的数值根信息。
 *
 * @param roots 在可视区间内找到的实根（升序）
 * @param truncated 根太多、只保留了前 [MAX_ROOTS] 个
 */
data class EquationInfo(
    val roots: List<Double>,
    val truncated: Boolean
) {
    val isEmpty: Boolean get() = roots.isEmpty()

    companion object {
        const val MAX_ROOTS = 24
    }
}

private const val SAMPLE_COUNT = 640
private const val BISECT_STEPS = 60

/**
 * 把 `x^2=4` 这类方程转成求根用的 `(左)-(右)` 表达式。
 *
 * 只在**单变量**（不含 y）时返回，隐函数无法给出有意义的"解"。
 */
fun equationDifference(raw: String): String? {
    val trimmed = raw.trim()
    if (!trimmed.contains('=')) return null
    val normalized = ExpressionNormalizer.normalize(trimmed)
    val eq = normalized.indexOf('=')
    if (eq <= 0 || eq >= normalized.length - 1) return null
    val left = normalized.substring(0, eq)
    val right = normalized.substring(eq + 1)
    if (left.isEmpty() || right.isEmpty()) return null
    if (left.contains('y') || right.contains('y')) return null
    return "($left)-($right)"
}

/**
 * 在 [from, to] 上等距采样 + 二分求根。
 * 只捕捉变号根；切根（如 x²=0）依赖采样点命中，属于已知限制。
 */
fun solveRoots(
    function: PreparedFunction,
    from: Double,
    to: Double,
    maxRoots: Int = EquationInfo.MAX_ROOTS
): EquationInfo {
    if (!from.isFinite() || !to.isFinite() || to <= from) {
        return EquationInfo(emptyList(), false)
    }
    val step = (to - from) / SAMPLE_COUNT
    val roots = ArrayList<Double>()

    var previousX = from
    var previousY = function.eval(previousX)
    if (previousY.isFinite() && abs(previousY) < 1e-12) addRoot(roots, previousX, step)

    for (i in 1..SAMPLE_COUNT) {
        val x = from + i * step
        val y = function.eval(x)
        if (y.isFinite() && previousY.isFinite()) {
            if (abs(y) < 1e-12) {
                addRoot(roots, x, step)
            } else if ((previousY > 0) != (y > 0)) {
                bisect(function, previousX, x, previousY, y)?.let { addRoot(roots, it, step) }
            }
        }
        previousX = x
        previousY = y
    }

    val truncated = roots.size > maxRoots
    return EquationInfo(if (truncated) roots.take(maxRoots) else roots, truncated)
}

private fun bisect(
    function: PreparedFunction,
    lowStart: Double,
    highStart: Double,
    lowValue: Double,
    highValue: Double
): Double? {
    var low = lowStart
    var high = highStart
    var lowY = lowValue
    var highY = highValue
    repeat(BISECT_STEPS) {
        val mid = (low + high) / 2
        if (!mid.isFinite() || mid == low || mid == high) return@repeat
        val midY = function.eval(mid)
        if (!midY.isFinite()) return@repeat
        if (abs(midY) < 1e-13) return mid
        if ((lowY > 0) != (midY > 0)) {
            high = mid
            highY = midY
        } else {
            low = mid
            lowY = midY
        }
    }
    val candidate = (low + high) / 2
    if (!candidate.isFinite()) return null
    // 两端符号仍相反才算收敛成功
    return if ((lowY > 0) != (highY > 0)) candidate else null
}

/** 同一根被相邻采样重复命中时去重。 */
private fun addRoot(roots: MutableList<Double>, value: Double, step: Double) {
    val tolerance = abs(step) * 0.5
    val last = roots.lastOrNull()
    if (last == null || abs(last - value) > tolerance) {
        roots.add(value)
    }
}

/** 读数的紧凑写法：整数不带小数点，其余保留三位。 */
fun formatRoot(value: Double): String {
    if (!value.isFinite()) return "—"
    val rounded = round(value * 1000.0) / 1000.0
    if (abs(rounded) < 5e-4) return "0"
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}
