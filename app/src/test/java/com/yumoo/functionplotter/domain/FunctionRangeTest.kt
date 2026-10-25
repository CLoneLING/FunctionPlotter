package com.yumoo.functionplotter.domain

import com.yumoo.functionplotter.domain.solver.FunctionRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionRangeTest {

    @Test
    fun rangeOverIntervalFindsExtremes() {
        val range = FunctionRange.overInterval("x^2", -1.0, 2.0)!!
        assertEquals(0.0, range.min, 1e-3)
        assertEquals(4.0, range.max, 1e-3)
        assertTrue(!range.unbounded)
    }

    @Test
    fun solveYRangeReturnsTwoBranches() {
        val ranges = FunctionRange.solveYRange("x^2", 1.0, 4.0, samples = 4000)
        assertEquals(2, ranges.size)
        val left = ranges.first { it.first < 0 }
        val right = ranges.first { it.second > 0 }
        assertEquals(-2.0, left.first, 0.05)
        assertEquals(-1.0, left.second, 0.05)
        assertEquals(1.0, right.first, 0.05)
        assertEquals(2.0, right.second, 0.05)
    }
}