package com.yumoo.functionplotter.domain

import com.yumoo.functionplotter.domain.parser.ImportParser
import org.junit.Assert.assertEquals
import org.junit.Test

class ImportParserTest {

    @Test
    fun stripsNumberingAndPrefix() {
        val result = ImportParser.parse("1. y = x^2\n2) f(x)=sin(x)")
        assertEquals(listOf("x^2", "sin(x)"), result.expressions)
    }

    @Test
    fun splitsCommaBetweenExpressions() {
        val result = ImportParser.parse("y=x^2, y=2x")
        assertEquals(listOf("x^2", "2x"), result.expressions)
    }

    @Test
    fun keepsLogArgumentsIntact() {
        val result = ImportParser.parse("log(2,x)")
        assertEquals(listOf("log(2,x)"), result.expressions)
    }

    @Test
    fun dropsChineseProse() {
        val result = ImportParser.parse("这是一段说明文字\ny=x")
        assertEquals(listOf("x"), result.expressions)
    }

    @Test
    fun convertsLatex() {
        val result = ImportParser.parse("\\frac{1}{x}")
        assertEquals(listOf("(1)/(x)"), result.expressions)
    }

    @Test
    fun dropsUnparsableExpression() {
        val result = ImportParser.parse("2x+=0")
        assertEquals(emptyList<String>(), result.expressions)
    }

    @Test
    fun deduplicates() {
        val result = ImportParser.parse("y=x\ny=x")
        assertEquals(listOf("x"), result.expressions)
    }
}