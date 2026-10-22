package com.yumoo.functionplotter.domain.plot

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt

/** 常见的有理分母，覆盖 30°/45°/60°/22.5°/15° 这些课本角度。 */
internal val PI_DENOMINATORS = intArrayOf(1, 2, 3, 4, 6, 8, 12)

/** 只吸附到最常见的角度，避免 π 的有理倍数太密、几乎每次点击都被"纠正"。 */
private val SNAP_DENOMINATORS = intArrayOf(1, 2, 3, 4, 6)

private const val MAX_NUMERATOR = 24

/**
 * 把数值表示成 π 的有理倍数，例如 1.5708 → "π/2"、2.356 → "3π/4"。
 *
 * @param tolerance 允许误差（世界坐标单位），一般取半个像素对应的量级。
 * @return 无法表示成简单 π 倍数时返回 null。
 */
fun asPiFraction(value: Double, tolerance: Double): String? {
    if (!value.isFinite() || !tolerance.isFinite() || tolerance < 0.0) return null
    // 0 用小数形式更清楚，不显示成 "0π"
    if (abs(value) < 1e-9) return null

    for (denominator in PI_DENOMINATORS) {
        val numerator = (value * denominator / PI).roundToInt()
        if (numerator == 0 || abs(numerator) > MAX_NUMERATOR) continue
        val candidate = numerator * PI / denominator
        if (abs(value - candidate) <= tolerance) {
            return formatPiFraction(numerator, denominator)
        }
    }
    return null
}

/**
 * 把被点击的 x 吸附到最近的 π 有理倍数。
 *
 * 手指落在屏幕上最小只能精确到约一个像素，不吸附的话几乎永远点不到 π/2。
 * 吸附距离以像素为单位，超出 [snapPx] 就保持原值。
 */
fun snapToPiMultiple(worldX: Double, scale: Float, snapPx: Float = 4f): Double {
    if (!worldX.isFinite() || !scale.isFinite() || scale <= 0f) return worldX
    if (!snapPx.isFinite() || snapPx <= 0f) return worldX

    val tolerance = (snapPx / scale).toDouble()
    var best: Double? = null
    var bestDistance = Double.MAX_VALUE

    for (denominator in SNAP_DENOMINATORS) {
        val numerator = (worldX * denominator / PI).roundToInt()
        if (numerator == 0 || abs(numerator) > MAX_NUMERATOR) continue
        val candidate = numerator * PI / denominator
        val distance = abs(worldX - candidate)
        if (distance <= tolerance && distance < bestDistance) {
            bestDistance = distance
            best = candidate
        }
    }
    return best ?: worldX
}

private fun formatPiFraction(numerator: Int, denominator: Int): String {
    val divisor = gcd(abs(numerator), denominator)
    val n = numerator / divisor
    val d = denominator / divisor
    val sign = if (n < 0) "-" else ""
    val magnitude = abs(n)
    val head = if (magnitude == 1) "π" else "${magnitude}π"
    return if (d == 1) "$sign$head" else "$sign$head/$d"
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
