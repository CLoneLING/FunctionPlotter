package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.domain.solver.FunctionProbe

private enum class CalcField { X, Y }

/**
 * 正反向求值：给 x 求 y，或给 y 反解 x。
 * 输入用内置小键盘（数字 / π / 区间符号），不唤起系统输入法。
 */
@Composable
fun CalcDialog(
    expression: String,
    onDismiss: () -> Unit
) {
    var xText by remember { mutableStateOf("1") }
    var yText by remember { mutableStateOf("1") }
    var active by remember { mutableStateOf(CalcField.X) }

    val xValue = parseValue(xText)
    val yValue = parseValue(yText)

    val yResult = remember(xValue, expression) {
        xValue?.let { FunctionProbe.evalAt(expression, it) }
    }
    val xResults = remember(yValue, expression) {
        yValue?.let { FunctionProbe.solveX(expression, it) } ?: emptyList()
    }

    fun append(text: String, interval: Boolean = false) {
        if (active == CalcField.X) {
            xText = if (interval) toggleInterval(xText) else xText + text
        } else {
            yText = if (interval) toggleInterval(yText) else yText + text
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("函数求值") },
        text = {
            Column {
                Text(
                    "f(x) = ${expression.ifBlank { "（空）" }}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(10.dp))
                CalcFieldRow(
                    label = when {
                        xValue == null -> "给定 x（支持分数与 π）"
                        else -> "给定 x = ${fmt(xValue)}"
                    },
                    value = xText,
                    active = active == CalcField.X,
                    onClick = { active = CalcField.X }
                )
                Text(
                    text = if (yResult != null) "y = ${fmt(yResult)}" else if (xValue == null) "请输入数字、分数或 π" else "无法求值",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                CalcFieldRow(
                    label = when {
                        yValue == null -> "给定 y（支持分数与 π）"
                        else -> "给定 y = ${fmt(yValue)}"
                    },
                    value = yText,
                    active = active == CalcField.Y,
                    onClick = { active = CalcField.Y }
                )
                Text(
                    text = when {
                        yValue == null -> "请输入数字、分数或 π"
                        xResults.isEmpty() -> "该 y 在 ±200 内无解"
                        else -> "x = " + xResults.joinToString("  ") { fmt(it) }
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(12.dp))
                CalcKeypad(
                    onKey = { append(it) },
                    onBracket = { append("", interval = true) },
                    onBackspace = {
                        if (active == CalcField.X) xText = xText.dropLast(1) else yText = yText.dropLast(1)
                    },
                    onClear = {
                        if (active == CalcField.X) xText = "" else yText = ""
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

@Composable
private fun CalcFieldRow(
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // 透明层接管点击：切换当前输入目标，且不唤起输入法
        androidx.compose.foundation.layout.Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
    if (active) {
        Text(
            text = "◀ 正在输入",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
@Composable
private fun CalcKeypad(
    onKey: (String) -> Unit,
    onBracket: () -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "/", "π"),
        listOf("4", "5", "6", "[", "]"),
        listOf("1", "2", "3", "(", ")"),
        listOf("0", ".", ",", "∪", "∩")
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = {
                            when (key) {
                                "[", "]" -> onBracket()
                                "−" -> onKey("-")
                                else -> onKey(key)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                    ) {
                        Text(key, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilledTonalButton(
                onClick = onBackspace,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(38.dp)
            ) { Text("⌫", fontSize = 14.sp) }
            FilledTonalButton(
                onClick = onClear,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.weight(1f).height(38.dp)
            ) { Text("清空", fontSize = 13.sp) }
        }
    }
}

/** 解析 "3/4"、"π"、"π/2"、"-1.5" 这类输入。 */
internal fun parseValue(text: String): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    return parseSimple(trimmed)
}

private fun parseSimple(text: String): Double? {
    val t = text.replace('−', '-').replace(" ", "")
    fun token(s: String): Double? = when (s) {
        "π", "pi" -> Math.PI
        "" -> 1.0
        else -> s.toDoubleOrNull()
    }
    // 3π、2π、π/2 这类隐式乘法
    val implicit = Regex("(\\d)\\s*π|(\\d)\\s*pi")
    if (implicit.containsMatchIn(t)) {
        val replaced = t
            .replace("π", "*π").replace("pi", "*pi")
            .replace("*π", Math.PI.toString()).replace("*pi", Math.PI.toString())
        return parseSimple(replaced.replace("**", "*"))
    }
    if ('/' in t) {
        val parts = t.split('/')
        if (parts.size != 2) return null
        val a = token(parts[0].trim()) ?: return null
        val b = token(parts[1].trim()) ?: return null
        return if (b == 0.0) null else a / b
    }
    return token(t)
}

private fun fmt(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = String.format("%.4f", value).trimEnd('0').trimEnd('.')
    return if (rounded == "-0") "0" else rounded
}

/** 在 () 与 [] 之间切换，方便输入区间。 */
private fun toggleInterval(text: String): String = when {
    text.endsWith("(") -> text.dropLast(1) + "["
    text.endsWith("[") -> text.dropLast(1) + "("
    else -> text + "("
}