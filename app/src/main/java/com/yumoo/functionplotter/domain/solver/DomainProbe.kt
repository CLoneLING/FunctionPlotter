package com.yumoo.functionplotter.domain.solver

import kotlin.math.abs

/**
 * 理论定义域：按表达式结构定位约束点（分母、log/sqrt 的参数、tan 的参数），
 * 用求根 + 符号判定得到区间，而不是去"看画出来的曲线"。
 *
 * 输出示例：
 *  - 1/(x-1)      → x ≠ 1
 *  - 1/(x^2-1)    → x ≠ -1, 1
 *  - sqrt(x-2)    → x ≥ 2
 *  - ln(x)        → x > 0
 *  - tan(x)       → x ≠ π/2 + kπ（k∈ℤ）
 */
object DomainProbe {

    private const val SCAN_FROM = -1000.0
    private const val SCAN_TO = 1000.0

    /** 收集所有约束片段（分母、log/sqrt 参数）。 */
    private val DIV_PAREN = Regex("/\\s*\\(([^()]*)\\)")
    private val DIV_TERM = Regex("/\\s*([a-zA-Z0-9^]+)")
    private val LOG_ARG = Regex("(?:log10|log|ln|lg)\\s*\\(([^()]*)\\)")
    private val SQRT_ARG = Regex("(?:sqrt|√)\\s*\\(([^()]*)\\)")
    private val TAN_ARG = Regex("(?:tan|sec)\\s*\\(([^()]*)\\)")
    private val COT_ARG = Regex("(?:cot|csc)\\s*\\(([^()]*)\\)")

    fun describe(expression: String): String? {
        if (expression.isBlank()) return null
        val e = expression.lowercase().replace(" ", "")
        if (e.isEmpty()) return null

        val parts = ArrayList<String>()

        // 1) 分母 ≠ 0：求分母的根，直接列成 x ≠ a, b
        val excluded = LinkedHashSet<Double>()
        DIV_PAREN.findAll(e).forEach { m ->
            val inner = m.groupValues[1].trim()
            if (inner.isNotEmpty()) excluded += roots(inner)
        }
        DIV_TERM.findAll(e).forEach { m ->
            val term = m.groupValues[1].trim()
            if (term.startsWith("x")) {
                val wrapped = Regex("/\\s*\\(\\s*" + Regex.escape(term) + "\\s*\\)")
                if (!wrapped.containsMatchIn(e)) excluded += roots(term)
            }
        }
        if (excluded.isNotEmpty()) {
            parts += "x ≠ " + excluded.sorted().joinToString(", ") { num(it) }
        }

        // 2) sqrt 参数 ≥ 0
        SQRT_ARG.findAll(e).forEach { m ->
            val inner = m.groupValues[1].trim()
            if (inner.isNotEmpty()) signIntervals(inner, strict = false)?.let { parts += it }
        }

        // 3) log 参数 > 0
        LOG_ARG.findAll(e).forEach { m ->
            val inner = m.groupValues[1].trim()
            if (inner.isNotEmpty()) signIntervals(inner, strict = true)?.let { parts += it }
        }

        // 4) tan / cot 的周期排斥
        TAN_ARG.find(e)?.groupValues?.getOrNull(1)?.trim()?.let { arg ->
            if (arg.isNotEmpty()) {
                parts += if (arg == "x") "x ≠ π/2 + kπ（k∈ℤ）" else "$arg ≠ π/2 + kπ（k∈ℤ）"
            }
        }
        COT_ARG.find(e)?.groupValues?.getOrNull(1)?.trim()?.let { arg ->
            if (arg.isNotEmpty()) {
                parts += if (arg == "x") "x ≠ kπ（k∈ℤ）" else "$arg ≠ kπ（k∈ℤ）"
            }
        }

        return if (parts.isEmpty()) "(-∞, +∞)" else parts.joinToString("；")
    }

    /** 求 sub 在扫描区间内的实根（去重）。 */
    private fun roots(sub: String): List<Double> {
        val samples = 4000
        val found = ArrayList<Double>()
        var prevX = SCAN_FROM
        var prev = valueAt(sub, prevX)
        for (i in 1..samples) {
            val x = SCAN_FROM + (SCAN_TO - SCAN_FROM) * i / samples
            val v = valueAt(sub, x)
            if (v != null && abs(v) < 1e-9) found += x
            if (prev != null && v != null && prev * v < 0) {
                bisect(sub, prevX, x)?.let { found += it }
            }
            prevX = x
            prev = v
        }
        return found.filter { it.isFinite() }.distinctBy { Math.round(it * 1e6) }
    }

    private fun bisect(sub: String, left: Double, right: Double): Double? {
        var a = left
        var b = right
        var fa = valueAt(sub, a) ?: return null
        repeat(60) {
            val mid = (a + b) / 2
            val fm = valueAt(sub, mid) ?: return null
            if (fa * fm <= 0) b = mid else { a = mid; fa = fm }
        }
        return (a + b) / 2
    }

    /**
     * 对 sub 的符号做区间划分：求出根，按根切分区间，取满足条件的区间。
     */
    private fun signIntervals(sub: String, strict: Boolean): String? {
        val rs = roots(sub).sorted()
        val bounds = ArrayList<Double>()
        bounds += SCAN_FROM
        bounds += rs
        bounds += SCAN_TO
        if (bounds.size < 2) return null

        val pieces = ArrayList<String>()
        for (i in 0 until bounds.size - 1) {
            val a = bounds[i]
            val b = bounds[i + 1]
            val mid = (a + b) / 2
            val v = valueAt(sub, mid) ?: continue
            val ok = if (strict) v > 0 else v >= 0
            if (ok) pieces += "${if (strict) "(" else "["}${num(a)}, ${num(b)}${if (strict) ")" else "]"}"
        }
        return if (pieces.isEmpty()) null else pieces.joinToString(" ∪ ")
    }

    private fun valueAt(sub: String, x: Double): Double? {
        return try {
            val prepared = com.yumoo.functionplotter.domain.parser.Exp4jEvaluator()
                .prepareExplicit(sub) ?: return null
            val v = prepared.eval(x)
            if (v.isFinite()) v else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun num(value: Double): String {
        val rounded = String.format("%.3f", value).trimEnd('0').trimEnd('.')
        return if (rounded == "-0") "0" else rounded
    }
}