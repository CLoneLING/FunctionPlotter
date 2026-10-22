package com.yumoo.functionplotter.domain.parser

sealed class PlotExpression {
    /** y = f(x)，按列采样绘制。 */
    data class Explicit(val expr: String) : PlotExpression()

    /** F(x, y) = 0，用等值线（marching squares）绘制。 */
    data class Implicit(val expr: String) : PlotExpression()

    data class Invalid(val raw: String) : PlotExpression()
}

/** `y = ...`、`f(x) = ...` 直接按显式函数处理，比隐式等值线快一个数量级。 */
private val EXPLICIT_PREFIX =
    Regex("^\\s*(y|f\\s*\\(\\s*x\\s*\\))\\s*=\\s*(.+)$", RegexOption.IGNORE_CASE)

fun parsePlotExpression(raw: String): PlotExpression {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return PlotExpression.Invalid(raw)

    EXPLICIT_PREFIX.find(trimmed)?.let { match ->
        val right = ExpressionNormalizer.normalize(match.groupValues[2])
        return if (right.isBlank()) PlotExpression.Invalid(raw) else PlotExpression.Explicit(right)
    }

    val normalized = ExpressionNormalizer.normalize(trimmed)
    if (normalized.isBlank()) return PlotExpression.Invalid(raw)

    val eqIndex = normalized.indexOf('=')
    if (eqIndex < 0) return PlotExpression.Explicit(normalized)

    val left = normalized.substring(0, eqIndex).trim()
    val right = normalized.substring(eqIndex + 1).trim()
    if (left.isEmpty() || right.isEmpty()) return PlotExpression.Invalid(raw)
    return PlotExpression.Implicit("($left)-($right)")
}
