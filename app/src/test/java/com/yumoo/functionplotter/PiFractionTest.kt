package com.yumoo.functionplotter

import com.yumoo.functionplotter.domain.plot.asPiFraction
import com.yumoo.functionplotter.domain.plot.snapToPiMultiple
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class PiFractionTest {

    @Test
    fun `常见角度的 π 表示`() {
        assertEquals("π", asPiFraction(PI, 1e-3))
        assertEquals("π/2", asPiFraction(PI / 2, 1e-3))
        assertEquals("-π/3", asPiFraction(-PI / 3, 1e-3))
        assertEquals("3π/4", asPiFraction(3 * PI / 4, 1e-3))
        assertEquals("5π/6", asPiFraction(5 * PI / 6, 1e-3))
        assertEquals("2π", asPiFraction(2 * PI, 1e-3))
        assertEquals("π/12", asPiFraction(PI / 12, 1e-3))
    }

    @Test
    fun `约分后的写法`() {
        assertEquals("π", asPiFraction(2 * PI / 2, 1e-3))
        assertEquals("π", asPiFraction(4 * PI / 4, 1e-3))
        assertEquals("2π", asPiFraction(4 * PI / 2, 1e-3))
    }

    @Test
    fun `不是 π 倍数时返回空`() {
        assertNull(asPiFraction(0.0, 1e-3))
        assertNull(asPiFraction(1.0, 1e-3))
        assertNull(asPiFraction(1.6, 1e-3))
        assertNull(asPiFraction(Double.NaN, 1e-3))
    }

    @Test
    fun `容差内的近似值也能识别`() {
        assertEquals("π/2", asPiFraction(PI / 2 + 0.01, 0.02))
        assertNull(asPiFraction(PI / 2 + 0.01, 0.005))
    }

    @Test
    fun `点击吸附`() {
        // scale=60 时 4px 约 0.067 个世界单位
        // 1.0 距 π/3 约 2.8px，会被吸附到课本角
        assertEquals(PI / 3, snapToPiMultiple(1.0, 60f), 1e-9)
        assertEquals(PI / 2, snapToPiMultiple(1.58, 60f), 1e-9)
        // 0.65 距最近的 π/6、π/4 都超过 4px，保持原值
        assertEquals(0.65, snapToPiMultiple(0.65, 60f), 1e-9)
        // 3.0 距 π 约 8.5px，不吸附
        assertEquals(3.0, snapToPiMultiple(3.0, 60f), 1e-9)
        // 非法的缩放值不应改动输入
        assertEquals(1.6, snapToPiMultiple(1.6, 0f), 1e-9)
    }
}
