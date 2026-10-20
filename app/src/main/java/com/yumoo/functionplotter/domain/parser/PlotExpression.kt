package com.yumoo.functionplotter.domain.parser

sealed class PlotExpression {
    data class Explicit(val expr: String) : PlotExpression()
    data class Implicit(val expr: String) : PlotExpression()
    data class Invalid(val raw: String) : PlotExpression()
}

fun parsePlotExpression(raw: String): PlotExpression {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return PlotExpression.Invalid(raw)

    val eqIndex = trimmed.indexOf('=')
    if (eqIndex >= 0) {
        val left = trimmed.substring(0, eqIndex).trim()
        val right = trimmed.substring(eqIndex + 1).trim()
        if (left.isBlank() || right.isBlank()) return PlotExpression.Invalid(raw)
        return PlotExpression.Implicit("($left)-($right)")
    }
    return PlotExpression.Explicit(trimmed)
}