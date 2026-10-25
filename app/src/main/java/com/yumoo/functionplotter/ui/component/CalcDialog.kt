package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.yumoo.functionplotter.domain.solver.CalcInput
import com.yumoo.functionplotter.domain.solver.CalcValue
import com.yumoo.functionplotter.domain.solver.FunctionProbe
import com.yumoo.functionplotter.domain.solver.FunctionRange

private enum class CalcField { X, Y }

/**
 * 函数求值：输入可以是单个数或区间。
 *  - 给定 x（或 x 区间）→ 求 y（或 y 的取值范围）
 *  - 给定 y（或 y 区间）→ 反解 x
 */
@Composable
fun CalcDialog(
    expression: String,
    onDismiss: () -> Unit
) {
    var xText by remember { mutableStateOf("1") }
    var yText by remember { mutableStateOf("1") }
    var active by remember { mutableStateOf(CalcField.X) }

    val xValue = remember(xText) { CalcInput.parse(xText) }
    val yValue = remember(yText) { CalcInput.parse(yText) }

    val yResult = remember(xValue, expression) { evaluateY(expression, xValue) }
    val xResult = remember(yValue, expression) { solveX(expression, yValue) }

    fun append(text: String) {
        if (active == CalcField.X) xText += text else yText += text
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
                    label = "x",
                    value = xText,
                    active = active == CalcField.X,
                    onClick = { active = CalcField.X }
                )
                Text(
                    text = yResult,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(12.dp))
                CalcFieldRow(
                    label = "y",
                    value = yText,
                    active = active == CalcField.Y,
                    onClick = { active = CalcField.Y }
                )
                Text(
                    text = xResult,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(6.dp))
                Text(
                    "支持 3/4、3π、π/2；区间写 [1,3]、(0,∞)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(10.dp))
                CalcKeypad(
                    onKey = ::append,
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

private fun evaluateY(expression: String, value: CalcValue?): String {
    if (expression.isBlank()) return "请先输入函数"
    return when (value) {
        null -> "请输入数字、分数或区间"
        is CalcValue.Scalar -> {
            val y = FunctionProbe.evalAt(expression, value.value)
            if (y == null) "该 x 处无定义" else "y = ${fmt(y)}"
        }
        is CalcValue.Interval -> {
            val from = value.from ?: -200.0
            val to = value.to ?: 200.0
            val range = FunctionRange.overInterval(expression, from, to, samples = 800)
            when {
                range == null -> "该区间内无定义"
                range.unbounded -> "y ∈ (-∞, +∞)"
                value.from == null || value.to == null ->
                    "y ∈ [${fmt(range.min)}, ${fmt(range.max)}]（按 ±200 采样）"
                else -> "y ∈ [${fmt(range.min)}, ${fmt(range.max)}]"
            }
        }
    }
}

private fun solveX(expression: String, value: CalcValue?): String {
    if (expression.isBlank()) return "请先输入函数"
    return when (value) {
        null -> "请输入数字、分数或区间"
        is CalcValue.Scalar -> {
            val roots = FunctionProbe.solveX(expression, value.value)
            if (roots.isEmpty()) "该 y 在 ±200 内无解"
            else "x = " + roots.joinToString("  ") { fmt(it) }
        }
        is CalcValue.Interval -> {
            val from = value.from ?: -200.0
            val to = value.to ?: 200.0
            val ranges = FunctionRange.solveYRange(expression, from, to)
            if (ranges.isEmpty()) "该区间内无解"
            else "x ∈ " + ranges.joinToString(" ∪ ") { "[${fmt(it.first)}, ${fmt(it.second)}]" }
        }
    }
}

@Composable
private fun CalcFieldRow(
    label: String,
    value: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = {
                if (active) {
                    Text("◀", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
        // 透明层接管点击：切换输入目标，不唤起系统输入法
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
private fun CalcKeypad(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit
) {
    val rows = listOf(
        listOf("7", "8", "9", "/", "π"),
        listOf("4", "5", "6", "[", "]"),
        listOf("1", "2", "3", "(", ")"),
        listOf("0", ".", ",", "∞", "−")
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    FilledTonalButton(
                        onClick = { onKey(if (key == "−") "-" else key) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp),
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
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) { Text("⌫", fontSize = 14.sp) }
            FilledTonalButton(
                onClick = onClear,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
            ) { Text("清空", fontSize = 13.sp) }
        }
    }
}

private fun fmt(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = String.format("%.4f", value).trimEnd('0').trimEnd('.')
    return if (rounded == "-0") "0" else rounded
}
