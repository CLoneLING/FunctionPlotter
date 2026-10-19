package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CalcKey(
    val label: String,
    val insert: String? = null,
    val isPlot: Boolean = false,
    val isBackspace: Boolean = false,
    val isClear: Boolean = false,
    val isFunction: Boolean = false
)

@Composable
fun MathKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onPlot: () -> Unit
) {
    val rows: List<List<CalcKey>> = listOf(
        listOf(
            CalcKey("sin", "sin(", isFunction = true),
            CalcKey("cos", "cos(", isFunction = true),
            CalcKey("tan", "tan(", isFunction = true),
            CalcKey("√", "sqrt(", isFunction = true),
            CalcKey("ln", "ln(", isFunction = true),
            CalcKey("log", "log(", isFunction = true)
        ),
        listOf(
            CalcKey("7", "7"), CalcKey("8", "8"), CalcKey("9", "9"),
            CalcKey("÷", "/"), CalcKey("(", "("), CalcKey(")", ")")
        ),
        listOf(
            CalcKey("4", "4"), CalcKey("5", "5"), CalcKey("6", "6"),
            CalcKey("×", "*"), CalcKey("^", "^"), CalcKey("x", "x")
        ),
        listOf(
            CalcKey("1", "1"), CalcKey("2", "2"), CalcKey("3", "3"),
            CalcKey("-", "-"), CalcKey("π", "pi"), CalcKey("e", "e")
        ),
        listOf(
            CalcKey("0", "0"), CalcKey(".", "."), CalcKey("+", "+"),
            CalcKey("=", isPlot = true),
            CalcKey("⌫", isBackspace = true),
            CalcKey("C", isClear = true)
        )
    )

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val onClick: () -> Unit = when {
                        key.isPlot -> onPlot
                        key.isBackspace -> onBackspace
                        key.isClear -> onClear
                        else -> {
                            val ins = key.insert
                            { if (ins != null) onKey(ins) }
                        }
                    }

                    val isAccent = key.isPlot
                    val modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                        .height(46.dp)

                    val content: @Composable () -> Unit = {
                        Text(
                            key.label,
                            fontSize = if (key.label.length >= 3) 12.sp else 16.sp,
                            maxLines = 1
                        )
                    }

                    if (isAccent) {
                        Button(
                            onClick = onClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = modifier
                        ) { content() }
                    } else if (key.isFunction) {
                        FilledTonalButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = modifier,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) { content() }
                    } else {
                        FilledTonalButton(
                            onClick = onClick,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = modifier
                        ) { content() }
                    }
                }
            }
        }
    }
}