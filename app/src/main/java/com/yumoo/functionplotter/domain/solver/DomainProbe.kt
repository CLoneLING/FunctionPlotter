package com.yumoo.functionplotter.domain.solver

/**
 * 按表达式结构推导理论定义域（不做数值采样）。
 *
 * 只覆盖常见约束：tan/sec、cot/csc、log/ln/lg、sqrt、除以变量。
 * 输出是给人看的约束式，例如：
 *  - 1/x            → x ≠ 0
 *  - tan(x)         → x ≠ π/2 + kπ（k∈ℤ）
 *  - ln(x)          → x > 0
 *  - sqrt(x)        → x ≥ 0
 *  - x^2            → (-∞, +∞)
 */
object DomainProbe {

    private val TAN = Regex("(tan|sec)\\s*\\(")
    private val COT = Regex("(cot|csc)\\s*\\(")
    private val LOG = Regex("(log10|log|ln|lg)\\s*\\(([^()]*)\\)")
    private val SQRT = Regex("(sqrt|√|∛)\\s*\\(([^()]*)\\)")
    private val DIVIDE_X = Regex("/\\s*\\(?\\s*x\\s*\\)?(?![a-zA-Z0-9])")

    fun describe(expression: String): String? {
        if (expression.isBlank()) return null
        val e = expression.lowercase().replace(" ", "")
        if (e.isEmpty()) return null

        val constraints = LinkedHashSet<String>()

        if (TAN.containsMatchIn(e)) constraints += "x ≠ π/2 + kπ（k∈ℤ）"
        if (COT.containsMatchIn(e)) constraints += "x ≠ kπ（k∈ℤ）"

        LOG.findAll(e).forEach { match ->
            val inner = match.groupValues[2].trim()
            if (inner.isNotEmpty() && inner != "x") {
                constraints += "($inner) > 0"
            } else {
                constraints += "x > 0"
            }
        }

        SQRT.findAll(e).forEach { match ->
            val inner = match.groupValues[2].trim()
            val isCube = match.groupValues[1] == "∛"
            if (!isCube && inner.isNotEmpty()) {
                constraints += if (inner == "x") "x ≥ 0" else "($inner) ≥ 0"
            }
        }

        if (DIVIDE_X.containsMatchIn(e)) constraints += "x ≠ 0"

        return if (constraints.isEmpty()) "(-∞, +∞)" else constraints.joinToString("；")
    }
}