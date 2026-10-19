package com.yumoo.functionplotter.domain.advanced

/**
 * 微积分可视化接口
 */
interface CalculusEngine {
    fun derivative(expression: String): String?
    fun integral(expression: String, a: Double, b: Double): String?
}