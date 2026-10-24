package com.yumoo.functionplotter.domain

import com.yumoo.functionplotter.domain.solver.DomainProbe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainProbeTest {

    private fun describe(expression: String) = DomainProbe.describe(expression) ?: ""

    @Test
    fun polynomialHasFullDomain() {
        assertEquals("(-∞, +∞)", describe("x^2"))
        assertEquals("(-∞, +∞)", describe("x^2+2x-3"))
    }

    @Test
    fun reciprocalExcludesZero() {
        val text = describe("1/x")
        assertTrue("实际: $text", text.contains("x ≠ 0"))
    }

    @Test
    fun shiftedReciprocalExcludesOne() {
        val text = describe("1/(x-1)")
        assertTrue("实际: $text", text.contains("x ≠ 1"))
    }

    @Test
    fun logDomainIsPositiveInfinity() {
        val text = describe("lg(x)")
        assertTrue("实际: $text", text.startsWith("(0, +∞"))
    }

    @Test
    fun sqrtDomainStartsAtBoundary() {
        val text = describe("sqrt(x-2)")
        assertTrue("实际: $text", text.startsWith("[2, +∞"))
    }

    @Test
    fun tangentExcludesPoles() {
        val text = describe("tan(x)")
        assertTrue("实际: $text", text.contains("π/2"))
    }
}