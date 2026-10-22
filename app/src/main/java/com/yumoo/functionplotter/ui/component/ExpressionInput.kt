package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExpressionInput(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onPlot: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var showHelp by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text("f(x) = ...", style = MaterialTheme.typography.titleMedium)
                },
                singleLine = true,
                readOnly = true,
                isError = error != null,
                shape = RoundedCornerShape(14.dp),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconButton(
                onClick = {
                    val text = clipboard.getText()?.text
                    if (!text.isNullOrBlank()) onValueChange(text.trim())
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "粘贴",
                    modifier = Modifier.size(18.dp)
                )
            }

            FilledTonalIconButton(
                onClick = { showHelp = true },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(46.dp)
            ) {
                Text("?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            FilledIconButton(
                onClick = onPlot,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "应用",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }

    if (showHelp) {
        SyntaxHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun SyntaxHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写法说明") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HelpLine("y = f(x)", "直接输入 f(x) 即可，例如 x^2、sin(x)、1/x")
                HelpLine("方程", "含等号表示方程，例如 x^2+y^2=9 画圆")
                HelpLine("√", "平方根，√9 与 √(x+1) 都支持；三次方根用 ∛")
                HelpLine("ln", "自然对数（底数 e）")
                HelpLine("lg", "常用对数（底数 10）")
                HelpLine("log(a, b)", "以 a 为底的 b 的对数，例如 log(2, 8) = 3")
                HelpLine("^", "乘方，x^2 就是 x 的平方；也可以写 x²")
                HelpLine("|x|", "绝对值，也可以写 abs(x)")
                HelpLine("省略乘号", "2x、2(x+1)、sin(x)cos(x) 都能识别")
                HelpLine("π 与 e", "圆周率和自然常数，可直接当数字用")
                HelpLine("手势", "拖动平移、双指缩放、双击复位、点按曲线读数")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("知道了") }
        }
    )
}

@Composable
private fun HelpLine(symbol: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = symbol,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = description, fontSize = 13.sp)
    }
}
