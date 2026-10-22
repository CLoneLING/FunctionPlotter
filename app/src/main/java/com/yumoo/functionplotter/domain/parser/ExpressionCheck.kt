package com.yumoo.functionplotter.domain.parser

/**
 * 输入框的实时体检结果：要么可绘制，要么给一句人话解释为什么不行。
 * 之前的实现会静默丢弃无法解析的表达式，用户完全不知道哪里错了。
 */
sealed interface ExpressionCheck {
    data class Valid(val expression: PlotExpression) : ExpressionCheck
    data class Invalid(val message: String) : ExpressionCheck
}

fun checkExpression(raw: String, evaluator: Exp4jEvaluator): ExpressionCheck {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ExpressionCheck.Invalid("请输入表达式")

    val normalized = ExpressionNormalizer.normalize(trimmed)
    if (normalized.count { it == '=' } > 1) {
        return ExpressionCheck.Invalid("只支持一个等号，例如 x^2+y^2=4")
    }
    if (!isParenBalanced(normalized)) {
        return ExpressionCheck.Invalid("括号不匹配")
    }
    if (normalized.any { it == '|' }) {
        return ExpressionCheck.Invalid("绝对值符号要成对出现，例如 |x|")
    }

    val parsed = parsePlotExpression(trimmed)
    val ok = when (parsed) {
        is PlotExpression.Invalid -> false
        is PlotExpression.Explicit -> evaluator.prepareExplicit(parsed.expr) != null
        is PlotExpression.Implicit -> evaluator.prepareImplicit(parsed.expr) != null
    }
    if (!ok) {
        if (parsed is PlotExpression.Explicit && normalized.contains('y')) {
            return ExpressionCheck.Invalid("含 y 时请写成方程，例如 y=x^2 或 x^2+y^2=9")
        }
        val unknown = firstUnknownName(normalized)
        return if (unknown != null) {
            ExpressionCheck.Invalid("无法识别「$unknown」，请检查函数名或变量")
        } else {
            ExpressionCheck.Invalid("语法有误，请检查运算符和括号")
        }
    }
    return ExpressionCheck.Valid(parsed)
}

private fun isParenBalanced(s: String): Boolean {
    var depth = 0
    for (c in s) {
        when (c) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth < 0) return false
            }
        }
    }
    return depth == 0
}

private val BUILTIN_IDENTIFIERS = setOf("x", "y", "e", "pi")

private fun firstUnknownName(expression: String): String? =
    Regex("[A-Za-z]+")
        .findAll(expression)
        .map { it.value.lowercase() }
        .firstOrNull { it !in ExpressionNormalizer.knownFunctions && it !in BUILTIN_IDENTIFIERS }
