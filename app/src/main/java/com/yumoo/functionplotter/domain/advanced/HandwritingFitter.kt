package com.yumoo.functionplotter.domain.advanced

/**
 * 手绘拟合接口
*/
interface HandwritingFitter {
    fun fit(points: List<Pair<Float, Float>>): String?
}