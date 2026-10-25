package com.yumoo.functionplotter.domain.solver

import kotlin.math.PI

/**
 * 求值框的输入：既可以是单个数（支持分数与 π），也可以是一个区间。
 * 例：``3/4``、``3π``、``π/2``、``[1,3]``、``(0,∞)``、``[-2,2)``。
 */
sealed interface CalcValue {
    data class Scalar(val value: Double) : CalcValue

    data class Interval(
        val from: Double?,          // null = -∞
        val to: Double?,            // null = +∞
        val closedFrom: Boolean,
        val closedTo: Boolean,
        /** 用户把左右端点写反了，已自动交换 */
        val swapped: Boolean = false
    ) : CalcValue {
        val isBounded: Boolean get() = from != null && to != null
    }
}

object CalcInput {

    fun parse(raw: String): CalcValue? {
        val text = raw.trim().replace('−', '-').replace(" ", "")
        if (text.isEmpty()) return null

        val first = text.first()
        if (text.length >= 3 && (first == '[' || first == '(') && text.last().let { it == ']' || it == ')' }) {
            val inner = text.substring(1, text.length - 1)
            val comma = topLevelComma(inner)
            if (comma >= 0) {
                val leftText = inner.substring(0, comma).trim()
                val rightText = inner.substring(comma + 1).trim()
                // 空的一侧必须显式写 ∞，避免 "[1,]" 这种含糊写法
                if (leftText.isEmpty() || rightText.isEmpty()) return null
                val from = parseNumber(leftText)
                val to = parseNumber(rightText)
                // 至少一端可解析，且不出现 [∞, -∞] 这种明显错误
                if (from != null || to != null) {
                    val reversed = from != null && to != null && from > to
                    // 端点写反时自动交换，并保持括号的开闭跟着端点走
                    return CalcValue.Interval(
                        from = if (reversed) to else from,
                        to = if (reversed) from else to,
                        closedFrom = if (reversed) text.last() == ']' else first == '[',
                        closedTo = if (reversed) first == '[' else text.last() == ']',
                        swapped = reversed
                    )
                }
                return null
            }
        }
        val value = parseNumber(text) ?: return null
        return CalcValue.Scalar(value)
    }

    /** 顶部层级的逗号（忽略括号内的） */
    private fun topLevelComma(text: String): Int {
        var depth = 0
        for (i in text.indices) {
            when (text[i]) {
                '(' -> depth++
                ')' -> depth--
                ',' -> if (depth == 0) return i
            }
        }
        return -1
    }

    /**
     * 解析一个数：``3``、``-1.5``、``3/4``、``π``、``3π``、``π/2``、``∞``。
     * ∞ 返回 null（区间端点用 null 表示无界）。
     */
    fun parseNumber(raw: String): Double? {
        val text = raw.trim().replace('−', '-').replace(" ", "")
        if (text.isEmpty()) return null
        if (text == "∞" || text == "+∞" || text == "-∞" || text == "inf" || text == "-inf") return null

        val sign = when {
            text.startsWith("-") -> -1.0
            text.startsWith("+") -> 1.0
            else -> 1.0
        }
        val body = text.trimStart('+', '-')

        // 分数
        if (body.count { it == '/' } == 1) {
            val parts = body.split('/')
            val numerator = parseFactor(parts[0])
            val denominator = parseFactor(parts[1])
            if (numerator == null || denominator == null || denominator == 0.0) return null
            return sign * (numerator / denominator)
        }
        val factor = parseFactor(body) ?: return null
        return sign * factor
    }

    /** 因子：数字、π、3π、2pi、e */
    private fun parseFactor(text: String): Double? {
        if (text.isEmpty()) return null
        val lower = text.lowercase()
        return when {
            lower == "π" || lower == "pi" -> PI
            lower == "e" -> Math.E
            lower.endsWith("π") || lower.endsWith("pi") -> {
                val head = lower.removeSuffix("π").removeSuffix("pi").trimEnd('*')
                (if (head.isEmpty()) 1.0 else head.toDoubleOrNull())?.times(PI)
            }
            else -> text.toDoubleOrNull()
        }
    }
}