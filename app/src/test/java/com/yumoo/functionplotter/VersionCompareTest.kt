package com.yumoo.functionplotter

import com.yumoo.functionplotter.data.update.compareVersions
import com.yumoo.functionplotter.data.update.isNewerVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun `位数变化也能正确比较`() {
        assertTrue(isNewerVersion("0.1.25", "0.1.24"))
        assertFalse(isNewerVersion("0.1.9", "0.1.24"))
        assertTrue(isNewerVersion("0.2", "0.1.24"))
    }

    @Test
    fun `忽略 v 前缀与预发布后缀`() {
        assertTrue(isNewerVersion("v1.0.0", "0.9.9"))
        assertEquals(0, compareVersions("1.0", "1.0.0"))
        assertEquals(0, compareVersions("1.2.3-beta", "1.2.3"))
    }
}
