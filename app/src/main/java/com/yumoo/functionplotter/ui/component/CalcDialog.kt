package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.domain.solver.FunctionProbe

/**
 * 正反向求值：给 x 求 y，或给 y 反解 x。
 */
@Composable
fun CalcDialog(
    expression: String,
    onDismiss: () -> Unit
) {
    var xText by remember { mutableStateOf("1") }
    var yText by remember { mutableStateOf("1") }

    val yResult = remember(xText, expression) {
        xText.toDoubleOrNull()?.let { FunctionProbe.evalAt(expression, it) }
    }
    val xResults = remember(yText, expression) {
        yText.toDoubleOrNull()?.let { FunctionProbe.solveX(expression, it) } ?: emptyList()
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
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = xText,
                    onValueChange = { xText = it },
                    label = { Text("给定 x") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (yResult != null) "y = ${fmt(yResult)}" else if (xText.isBlank()) "" else "无法求值",
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = yText,
                    onValueChange = { yText = it },
                    label = { Text("给定 y") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = when {
                        yText.isBlank() -> ""
                        xResults.isEmpty() -> "该 y 在 ±200 内无解"
                        else -> "x = " + xResults.joinToString("  ") { fmt(it) }
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }
    )
}

private fun fmt(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "—"
    val rounded = String.format("%.4f", value).trimEnd('0').trimEnd('.')
    return if (rounded == "-0") "0" else rounded
}