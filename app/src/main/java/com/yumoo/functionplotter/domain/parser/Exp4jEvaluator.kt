package com.yumoo.functionplotter.domain.parser

import net.objecthunter.exp4j.ExpressionBuilder

class Exp4jEvaluator {

    fun evaluate(expression: String, x: Double): Double {
        return try {
            ExpressionBuilder(expression)
                .variables("x")
                .build()
                .setVariable("x", x)
                .evaluate()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    fun isValid(expression: String): Boolean {
        if (expression.isBlank()) return false
        return try {
            ExpressionBuilder(expression).variables("x").build()
            true
        } catch (e: Exception) {
            false
        }
    }
}