package com.yumoo.functionplotter.domain.solver

import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import kotlin.math.abs

/**
 * 理论定义域：按表达式结构找出约束项（分母、log/sqrt 参数、tan 参数），
 * 对约束项求根并用符号判定区间，输出数学式而非"画图结果"。
 */
object DomainProbe {

    private const val FROM = -10000.0
    private const val TO = 10000.0

    private val LOG_ARG = Regex("(?:log10|log|ln|lg)\\s*\\(([^()]*)\\)")
    private val SQRT_ARG = Regex("(?:sqrt|√)\\s*\\(([^()]*)\\)")
    private val TAN_ARG = Regex("(?:tan|sec)\\s*\\(([^()]*)\\)")
    private val COT_ARG = Regex("(?:cot|csc)\\s*\\(([^()]*)\\)")

    fun describe(expression: String): String? {
        if (expression.isBlank()) return null
        val e = expression.lowercase().replace(" ", "")
        if (e.isEmpty()) return null
        val parts = ArrayList<String>()

        // 1) 分母 ≠ 0
        val excluded = LinkedHashSet<Double>()
        for (den in denominators(e)) {
            if (den.contains("x")) excluded.addAll(roots(den))
        }
        if (excluded.isNotEmpty()) {
            parts += "x ≠ " + excluded.sorted().joinToString(", ") { num(it) }
        }

        // 2) sqrt ≥ 0 / log > 0
        for (m in SQRT_ARG.findAll(e)) {
            val inner = m.groupValues[1].trim()
            if (inner.isNotEmpty()) signIntervals(inner, strict = false)?.let { parts += it }
        }
        for (m in LOG_ARG.findAll(e)) {
            val inner = m.groupValues[1].trim()
            if (inner.isNotEmpty()) signIntervals(inner, strict = true)?.let { parts += it }
        }

        // 3) tan / cot 周期排斥
        TAN_ARG.find(e)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { arg ->
            parts += periodicConstraint(arg, Math.PI / 2)
        }
        COT_ARG.find(e)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { arg ->
            parts += periodicConstraint(arg, 0.0)
        }

        return if (parts.isEmpty()) "(-∞, +∞)" else parts.joinToString("；")
    }

    /** 抽出所有分母项：/(x-1)、/7(x^2-5/6)、/x^2。 */
    private fun denominators(e: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < e.length) {
            if (e[i] != '/') { i++; continue }
            var j = i + 1
            var depth = 0
            val sb = StringBuilder()
            while (j < e.length) {
                val c = e[j]
                if (depth == 0 && (c == '+' || c == '-' || c == ',' || c == ';')) break
                if (c == '(') depth++
                if (c == ')') {
                    if (depth == 0) break
                    depth--
                }
                sb.append(c)
                j++
            }
            val term = sb.toString().trim().trimEnd('^')
            if (term.isNotEmpty()) out += term
            i = j + 1
        }
        return out
    }

    /**
     * tan/cot 的周期排斥。
     *  - 参数是 x：直接给符号式
     *  - 参数是 a*x：给 (π/2 + kπ)/a
     *  - 参数非线性（如 x^2）：按 k 取值解出前几个具体点
     */
    private fun periodicConstraint(arg: String, offset: Double): String {
        val suffix = if (offset == 0.0) "kπ（k∈ℤ）" else "π/2 + kπ（k∈ℤ）"
        if (arg == "x") return "x ≠ $suffix"

        val linear = Regex("^([-+]?[0-9]*\\.?[0-9]+)\\s*\\*?\\s*x$").find(arg)
        val coefficient = linear?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        if (coefficient != null && coefficient != 0.0) {
            return if (coefficient == 1.0) "x ≠ $suffix"
            else "x ≠ ($suffix) / ${trimNumber(coefficient)}"
        }

        val points = ArrayList<Double>()
        for (k in -3..3) {
            points += FunctionProbe.solveX(arg, offset + k * Math.PI, from = -50.0, to = 50.0, maxRoots = 4)
        }
        val sorted = points.filter { it.isFinite() }
            .distinctBy { Math.round(it * 1e4) }
            .sorted()
        return if (sorted.isEmpty()) {
            "$arg ≠ $suffix"
        } else {
            "x ≠ " + sorted.take(8).joinToString(", ") { num(it) } + " …（k∈ℤ）"
        }
    }

    private fun trimNumber(value: Double): String {
        val text = String.format("%.3f", value).trimEnd('0').trimEnd('.')
        return if (text == "-0") "0" else text
    }
    private fun roots(sub: String): List<Double> {
        val samples = 4000
        val found = ArrayList<Double>()
        var prevX = FROM
        var prev = valueAt(sub, prevX)
        for (i in 1..samples) {
            val x = FROM + (TO - FROM) * i / samples
            val v = valueAt(sub, x)
            if (v != null && abs(v) < 1e-9) found += x
            if (prev != null && v != null && prev * v < 0) bisect(sub, prevX, x)?.let { found += it }
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
            if (fa * fm <= 0) b = mid else {
                a = mid
                fa = fm
            }
        }
        val root = (a + b) / 2
        return if (root.isFinite()) root else null
    }

    private fun signIntervals(sub: String, strict: Boolean): String? {
        val rs = roots(sub).sorted()
        val bounds = ArrayList<Double>()
        bounds += FROM
        bounds += rs
        bounds += TO
        val pieces = ArrayList<String>()
        for (i in 0 until bounds.size - 1) {
            val a = bounds[i]
            val b = bounds[i + 1]
            val mid = (a + b) / 2
            val v = valueAt(sub, mid) ?: continue
            val ok = if (strict) v > 0 else v >= 0
            if (!ok) continue
            val left = if (strict) "(" else "["
            val right = if (strict) ")" else "]"
            pieces += "$left${num(a)}, ${num(b)}$right"
        }
        return if (pieces.isEmpty()) null else pieces.joinToString(" ∪ ")
    }

    private fun valueAt(sub: String, x: Double): Double? {
        val normalized = sub.replace(")(", ")*(").replace(Regex("(\\d)\\("), "$1*(")
        return try {
            val prepared = Exp4jEvaluator().prepareExplicit(normalized) ?: return null
            val value = prepared.eval(x)
            if (value.isFinite()) value else null
        } catch (t: Throwable) {
            null
        }
    }

    private fun num(value: Double): String {
        if (value <= FROM + 1e-9) return "-∞"
        if (value >= TO - 1e-9) return "+∞"
        val rounded = String.format("%.3f", value).trimEnd('0').trimEnd('.')
        return if (rounded == "-0") "0" else rounded
    }
}