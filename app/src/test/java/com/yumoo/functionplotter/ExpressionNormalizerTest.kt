package com.yumoo.functionplotter

import com.yumoo.functionplotter.domain.parser.ExpressionNormalizer
import com.yumoo.functionplotter.domain.parser.PlotExpression
import com.yumoo.functionplotter.domain.parser.parsePlotExpression
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionNormalizerTest {

    private fun normalize(input: String) = ExpressionNormalizer.normalize(input)

    @Test
    fun `省略乘号`() {
        assertEquals("2*x", normalize("2x"))
        assertEquals("2*(x+1)", normalize("2(x+1)"))
        assertEquals("2*pi", normalize("2π"))
        assertEquals("sin(x)*cos(x)", normalize("sin(x)cos(x)"))
    }

    @Test
    fun `数学符号`() {
        assertEquals("2*3", normalize("2×3"))
        assertEquals("6/3", normalize("6÷3"))
        assertEquals("x^(2)", normalize("x²"))
        assertEquals("abs(x)", normalize("|x|"))
    }

    @Test
    fun `根号`() {
        assertEquals("sqrt(9)", normalize("√9"))
        assertEquals("sqrt(x+1)", normalize("√(x+1)"))
        assertEquals("cbrt(8)", normalize("∛8"))
    }

    @Test
    fun `对数语义`() {
        // exp4j 的 log 是自然对数，ln 必须翻译成 log
        assertEquals("log(x)", normalize("ln(x)"))
        assertEquals("log10(x)", normalize("lg(x)"))
        assertEquals("(log(8)/log(2))", normalize("log(2,8)"))
    }

    @Test
    fun `显式与隐式解析`() {
        val explicit = parsePlotExpression("y=x^2")
        assertTrue(explicit is PlotExpression.Explicit)
        assertEquals("x^2", (explicit as PlotExpression.Explicit).expr)

        val implicit = parsePlotExpression("x^2+y^2=9")
        assertTrue(implicit is PlotExpression.Implicit)
    }
}
