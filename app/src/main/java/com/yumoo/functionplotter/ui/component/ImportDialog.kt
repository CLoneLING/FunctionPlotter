package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.yumoo.functionplotter.domain.parser.ImportParser

/**
 * 批量导入对话框：粘贴 → 实时预览识别结果 → 导入。
 */
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<String>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val parsed = remember(text) { ImportParser.parse(text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量导入") },
        text = {
            Column {
                Text(
                    "粘贴任意整段文本，自动挑出其中的表达式\n支持 y=、f(x)=、方程、LaTeX 写法与序号列表",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                Spacer(Modifier.height(10.dp))
                TextButton(
                    onClick = {
                        val clip = clipboard.getText()?.text?.trim().orEmpty()
                        if (clip.isNotEmpty()) {
                            text = if (text.isBlank()) clip else text.trimEnd() + "\n" + clip
                        }
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp)
                ) { Text("从剪贴板粘贴", fontSize = 12.sp) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 220.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Ascii,
                        autoCorrectEnabled = false
                    ),
                    placeholder = {
                        Text("y = x^2\ny = sin(x)\nx^2 + y^2 = 9", fontSize = 12.sp)
                    }
                )
                if (parsed.expressions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "识别到 ${parsed.expressions.size} 个表达式",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(
                        Modifier
                            .heightIn(max = 140.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        parsed.expressions.take(12).forEach { expression ->
                            Text(expression, fontSize = 12.sp)
                        }
                    }
                } else if (text.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "没有识别到表达式",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImport(parsed.expressions) },
                enabled = parsed.expressions.isNotEmpty()
            ) {
                Text("导入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
