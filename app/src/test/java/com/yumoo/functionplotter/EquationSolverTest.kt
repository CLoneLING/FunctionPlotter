package com.yumoo.functionplotter

import com.yumoo.functionplotter.domain.parser.Exp4jEvaluator
import com.yumoo.functionplotter.domain.solver.equationDifference
import com.yumoo.functionplotter.domain.solver.solveRoots
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class EquationSolverTest {

    private val evaluator = Exp4jEvaluator()

    private fun rootsOf(diff: String, from: Double, to: Double): List<Double> {
        val prepared = evaluator.prepareExplicit(diff)!!
        return solveRoots(prepared, from, to).roots.map { Math.round(it * 1000) / 1000.0 }
    }

    @Test
    fun `二次方程两个根`() {
        assertEquals(listOf(-2.0, 2.0), rootsOf("(x^2)-(4)", -10.0, 10.0))
    }

    @Test
    fun `三角方程多个根`() {
        val roots = rootsOf("(sin(x))-(0)", -10.0, 10.0)
        assertEquals(7, roots.size)
        assertEquals(-3 * PI, roots.first(), 0.01)
        assertEquals(3 * PI, roots.last(), 0.01)
    }

    @Test
    fun `无实根返回空`() {
        assertTrue(rootsOf("(x^2)+(1)", -10.0, 10.0).isEmpty())
    }

    @Test
    fun `方程解析`() {
        assertEquals("(x^2)-(4)", equationDifference("x^2=4"))
        assertEquals("(sin(x))-(0.5)", equationDifference("sin(x) = 0.5"))
        // 含 y 的隐函数不求解
        assertNull(equationDifference("x^2+y^2=9"))
        // 没有等号
        assertNull(equationDifference("x^2"))
        // 等号不完整
        assertNull(equationDifference("x^2="))
    }

    @Test
    fun `根的数量上限`() {
        val prepared = evaluator.prepareExplicit("(sin(x))-(0)")!!
        val info = solveRoots(prepared, -1000.0, 1000.0, maxRoots = 5)
        assertEquals(5, info.roots.size)
        assertTrue(info.truncated)
    }
}
