package com.yumoo.functionplotter.domain

import com.yumoo.functionplotter.domain.solver.CalcInput
import com.yumoo.functionplotter.domain.solver.CalcValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcInputTest {

    private fun scalar(text: String): Double? =
        (CalcInput.parse(text) as? CalcValue.Scalar)?.value

    private fun interval(text: String): CalcValue.Interval? =
        CalcInput.parse(text) as? CalcValue.Interval

    @Test
    fun parsesPlainAndFraction() {
        assertEquals(2.5, scalar("2.5")!!, 1e-9)
        assertEquals(0.75, scalar("3/4")!!, 1e-9)
        assertEquals(-1.5, scalar("-3/2")!!, 1e-9)
    }

    @Test
    fun parsesPiMultiples() {
        assertEquals(Math.PI, scalar("π")!!, 1e-9)
        assertEquals(3 * Math.PI, scalar("3π")!!, 1e-9)
        assertEquals(Math.PI / 2, scalar("π/2")!!, 1e-9)
    }

    @Test
    fun parsesClosedInterval() {
        val value = interval("[1,3]")!!
        assertEquals(1.0, value.from!!, 1e-9)
        assertEquals(3.0, value.to!!, 1e-9)
        assertTrue(value.closedFrom && value.closedTo)
    }

    @Test
    fun parsesHalfOpenAndInfinite() {
        val value = interval("(0,∞)")!!
        assertEquals(0.0, value.from!!, 1e-9)
        assertNull(value.to)
        assertEquals(false, value.closedFrom)
    }

    @Test
    fun swapsReversedInterval() {
        val value = interval("[3,1]")!!
        assertEquals(1.0, value.from!!, 1e-9)
        assertEquals(3.0, value.to!!, 1e-9)
        assertTrue(value.swapped)
    }

    @Test
    fun rejectsGarbage() {
        assertNull(CalcInput.parse("abc"))
        assertNull(CalcInput.parse("[1,]"))
    }
}