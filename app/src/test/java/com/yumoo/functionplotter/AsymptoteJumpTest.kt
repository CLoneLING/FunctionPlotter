package com.yumoo.functionplotter

import com.yumoo.functionplotter.domain.plot.isAsymptoteJump
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AsymptoteJumpTest {

    private val limit = 1900.0

    @Test
    fun `跨越渐近线要被识别`() {
        // tan(x) 在 pi/2 两侧：一侧冲到屏幕上方很远处，另一侧冲到下方很远处
        assertTrue(isAsymptoteJump(-2500.0, 3500.0, limit))
        assertTrue(isAsymptoteJump(3500.0, -2500.0, limit))
        assertTrue(isAsymptoteJump(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, limit))
    }

    @Test
    fun `同一侧很远的两点不应断开`() {
        // 连续但很陡：两点都在屏幕上方很远处
        assertFalse(isAsymptoteJump(-2500.0, -6000.0, limit))
        assertFalse(isAsymptoteJump(-6000.0, -2500.0, limit))
        assertFalse(isAsymptoteJump(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, limit))
    }

    @Test
    fun `正常范围内的相邻点保持连续`() {
        assertFalse(isAsymptoteJump(400.0, 800.0, limit))
        assertFalse(isAsymptoteJump(-500.0, 300.0, limit))
        assertFalse(isAsymptoteJump(0.0, 0.0, limit))
    }

    @Test
    fun `非法输入不误判`() {
        assertFalse(isAsymptoteJump(Double.NaN, 100.0, limit))
        assertFalse(isAsymptoteJump(100.0, Double.NaN, limit))
        assertFalse(isAsymptoteJump(0.0, 100.0, 0.0))
    }
}
