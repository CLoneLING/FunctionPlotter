package com.yumoo.functionplotter.domain.parser

import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder

/**
 * 预编译后的表达式。
 *
 * 旧实现每采样一个点就 `ExpressionBuilder(...).build()` 一次：一条曲线要重新解析
 * 上千次，隐式曲线更是要解析两万多次，这是卡顿/崩溃的主因。
 * 现在解析只发生一次，采样循环里只做 `setVariable().evaluate()`。
 */
class PreparedFunction internal constructor(private val expression: Expression) {

    fun eval(x: Double): Double = try {
        expression.setVariable("x", x).evaluate()
    } catch (t: Throwable) {
        Double.NaN
    }

    fun eval(x: Double, y: Double): Double = try {
        expression.setVariable("x", x).setVariable("y", y).evaluate()
    } catch (t: Throwable) {
        Double.NaN
    }
}

class Exp4jEvaluator {

    fun prepareExplicit(expression: String): PreparedFunction? = build(expression, twoVariables = false)

    fun prepareImplicit(expression: String): PreparedFunction? = build(expression, twoVariables = true)

    fun isValid(expression: String): Boolean = prepareExplicit(expression) != null

    fun evaluate(expression: String, x: Double): Double =
        prepareExplicit(expression)?.eval(x) ?: Double.NaN

    fun evaluate2(expression: String, x: Double, y: Double): Double =
        prepareImplicit(expression)?.eval(x, y) ?: Double.NaN

    private fun build(expression: String, twoVariables: Boolean): PreparedFunction? {
        if (expression.isBlank()) return null
        return try {
            val builder = if (twoVariables) {
                ExpressionBuilder(expression).variables("x", "y", "e", "pi")
            } else {
                ExpressionBuilder(expression).variables("x", "e", "pi")
            }
            val compiled = builder.build()
            // exp4j 没有内置常数，这里把 e / pi 作为变量注入
            compiled.setVariable("e", Math.E)
            compiled.setVariable("pi", Math.PI)
            if (twoVariables) compiled.setVariable("y", 0.0)
            PreparedFunction(compiled)
        } catch (t: Throwable) {
            null
        }
    }
}
