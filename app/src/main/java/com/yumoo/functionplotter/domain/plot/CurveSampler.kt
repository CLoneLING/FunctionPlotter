package com.yumoo.functionplotter.domain.plot

import androidx.compose.ui.graphics.Path
import com.yumoo.functionplotter.domain.parser.PreparedFunction
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val MIN_STEP_PX = 0.75f
private const val MAX_STEP_PX = 8f

/** 向定义域边界/渐近线逼近时的二分次数。 */
private const val BOUNDARY_REFINEMENTS = 9

/**
 * 按屏幕列采样 y = f(x)。
 *
 * 与旧实现的区别：
 *  - 复用预编译表达式，不再逐点 build 解析器；
 *  - 所有路径坐标都被夹在画布上下各半个画布高度的范围内，避免把
 *    ±10^6 级别的点写进 Path（这是坐标轴拖远后崩溃/花屏的常见来源）；
 *  - 相邻采样点在**夹取前**的纵向跨度超过阈值时判定为跨过渐近线，断开子路径。
 *    `tan(x)`、`1/x`、`1/(x-2)` 不会再出现一条贯通上下的假竖线；
 *  - 采样点进入无定义区域前，用二分法把曲线推进到定义域边界，
 *    `ln(x)`、`1/x` 能一路画到屏幕边缘，而不是提前断掉。
 */
fun buildCurvePath(
    function: PreparedFunction,
    viewport: PlotViewport,
    stepPx: Float
): Path {
    val path = Path()
    if (!viewport.isUsable) return path

    val step = if (stepPx.isFinite()) stepPx.coerceIn(MIN_STEP_PX, MAX_STEP_PX) else 2f
    val width = viewport.width
    val height = viewport.height

    // 路径坐标只允许落在 [-height/2, 1.5*height]，超出部分贴边
    val overshoot = height * 24f
    val minY = -overshoot
    val maxY = height + overshoot
    val jumpLimit = height * 2f

    val columns = (width / step).toInt() + 1
    var previousScreenX = 0f
    var previousY = Float.NaN
    var previousRawY = Double.NaN
    var started = false

    // 从上一个有效采样点向当前无效采样点二分，尽可能贴近边界
    fun extendToBoundary(invalidX: Float) {
        // 一个端点确定有效、另一个端点确定无效，二分只会朝边界收敛。
        // 注意 left/right 的先后顺序取决于推进方向，不能假定 invalidX 在右侧。
        var validEnd = previousScreenX
        var invalidEnd = invalidX
        repeat(BOUNDARY_REFINEMENTS) {
            val mid = (validEnd + invalidEnd) * 0.5f
            if (!mid.isFinite() || mid == validEnd || mid == invalidEnd) return@repeat
            val worldY = function.eval(viewport.worldX(mid))
            if (!worldY.isFinite()) {
                invalidEnd = mid
                return@repeat
            }
            val screenY = viewport.screenY(worldY)
            if (!screenY.isFinite()) {
                invalidEnd = mid
                return@repeat
            }
            val clamped = screenY.coerceIn(minY, maxY)
            // 说明已经越过渐近线，不要再连过去
            if (abs(clamped - previousY) > jumpLimit * 4) {
                invalidEnd = mid
                return@repeat
            }
            path.lineTo(mid, clamped)
            previousY = clamped
            previousScreenX = mid
            validEnd = mid
        }
    }

    for (i in 0..columns) {
        val screenX = (i * step).coerceAtMost(width)
        val worldY = function.eval(viewport.worldX(screenX))
        val rawY = if (worldY.isFinite()) viewport.screenY(worldY).toDouble() else Double.NaN

        if (rawY.isNaN()) {
            if (started) extendToBoundary(screenX)
            started = false
            previousY = Float.NaN
            previousRawY = Double.NaN
            previousScreenX = screenX
            continue
        }

        // 用夹取前的坐标判断是否跨过渐近线：夹取之后两点间距被压缩，
        // 用夹取后的值判断会让 tan(x)、1/(x-2) 这类函数被连成一条竖线。
        if (started && isAsymptoteJump(previousRawY, rawY, jumpLimit.toDouble())) {
            started = false
        }

        // 极点常常正好落在两个采样点之间：此时两侧的值都还不大，屏幕差值
        // 不足以触发上面的判断，连起来就是一条竖线。这里对陡峭段做中点探测：
        // 中点在两端的范围之外发散（或无定义）就说明中间有极点，断开。
        if (started && abs(rawY - previousRawY) > height * 0.6) {
            val midScreenX = (previousScreenX + screenX) / 2f
            val midWorldY = function.eval(viewport.worldX(midScreenX))
            val midScreenY: Double = if (midWorldY.isFinite()) viewport.screenY(midWorldY).toDouble() else Double.NaN
            if (isPoleBetween(previousRawY, rawY, midScreenY, height.toDouble())) {
                // 借此机会把曲线继续逼近到极点，尽量顶到屏幕边缘
                extendToBoundary(screenX)
                started = false
            }
        }

        val screenY = rawY.coerceIn(minY.toDouble(), maxY.toDouble()).toFloat()
        if (!started) {
            path.moveTo(screenX, screenY)
            started = true
        } else {
            path.lineTo(screenX, screenY)
        }
        previousY = screenY
        previousRawY = rawY
        previousScreenX = screenX
    }
    return path
}

/**
 * 判断两个相邻采样点之间是否跨过了渐近线。
 *
 * 两侧的值先按 [limit] 截断再比较：这样 +∞ 与 -∞ 会落在两端、
 * 差值达到 2×limit；而两个都在同一侧很远处的点截断后相等，不会被误判。
 * 用截断后的差值而不是原始差值，是为了避免 Infinity - Infinity 得到 NaN。
 */
internal fun isAsymptoteJump(previous: Double, current: Double, limit: Double): Boolean {
    if (!limit.isFinite() || limit <= 0.0) return false
    if (previous.isNaN() || current.isNaN()) return false
    val a = previous.coerceIn(-limit, limit)
    val b = current.coerceIn(-limit, limit)
    return abs(b - a) > limit
}

/**
 * 判断两个相邻采样点之间是否夹着一个极点：
 * 把中点也求值一次，如果中点跑到两端区间之外足够远（或直接无定义），
 * 说明中间有发散点，不能连线。
 */
internal fun isPoleBetween(previous: Double, current: Double, mid: Double, limit: Double): Boolean {
    if (previous.isNaN() || current.isNaN()) return false
    val top = min(previous, current)
    val bottom = max(previous, current)
    if (mid.isNaN()) return true
    return mid < top - limit || mid > bottom + limit
}