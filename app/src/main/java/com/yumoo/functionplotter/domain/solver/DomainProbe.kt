package com.yumoo.functionplotter.domain.solver

/**
 * 用采样法估计函数在某个范围里的定义域，输出 [a,b) ∪ ... 形式。
 * 只用来给出"大致可用区间"，不做符号推导。
 */
object DomainProbe {

    fun describe(expression: String, from: Double = -50.0, to: Double = 50.0): String? {
        if (expression.isBlank()) return null
        val samples = 400
        val xs = DoubleArray(samples + 1) { from + (to - from) * it / samples }
        val valid = BooleanArray(samples + 1) { FunctionProbe.evalAt(expression, xs[it]) != null }

        if (valid.all { it }) return "(-∞, +∞)"
        if (valid.none { it }) return null

        val parts = ArrayList<String>()
        var start = -1
        for (i in valid.indices) {
            if (valid[i] && start < 0) start = i
            if ((!valid[i] || i == valid.lastIndex) && start >= 0) {
                val end = if (valid[i]) i else i - 1
                parts += format(xs[start], xs[end], valid[start], valid[end])
                start = -1
            }
        }
        return parts.joinToString(" ∪ ")
    }

    private fun format(a: Double, b: Double, closedA: Boolean, closedB: Boolean): String {
        val left = if (closedA) "[" else "("
        val right = if (closedB) "]" else ")"
        return "$left${num(a)}, ${num(b)}$right"
    }

    private fun num(value: Double): String {
        val rounded = String.format("%.2f", value).trimEnd('0').trimEnd('.')
        return if (rounded == "-0") "0" else rounded
    }
}