package com.yumoo.functionplotter.domain.plot

/**
 * 视口：画布中心对应的世界坐标 + 每单位像素数。
 *
 * 用"世界坐标中心"而不是"像素偏移"来表示相机，有两个好处：
 *  - 平移/缩放后数值不会无限累积，天然避免浮点溢出；
 *  - 坐标上限可以直接按数学量级约束（见 MAX_WORLD）。
 */
data class PlotViewport(
    val centerX: Float,
    val centerY: Float,
    val scale: Float,
    val width: Float,
    val height: Float
) {
    val isUsable: Boolean
        get() = scale.isFinite() && scale > 0f &&
            centerX.isFinite() && centerY.isFinite() &&
            width > 0f && height > 0f

    fun worldX(screenX: Float): Double =
        (((screenX - width / 2f) / scale) + centerX).toDouble()

    fun worldY(screenY: Float): Double =
        (centerY - ((screenY - height / 2f) / scale)).toDouble()

    fun screenX(worldX: Double): Float =
        ((worldX - centerX) * scale + width / 2f).toFloat()

    fun screenY(worldY: Double): Float =
        ((centerY - worldY) * scale + height / 2f).toFloat()

    fun screenX(worldX: Float): Float = screenX(worldX.toDouble())

    fun screenY(worldY: Float): Float = screenY(worldY.toDouble())
}
