package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.ui.theme.AtomicShapes

/**
 * 表达式输入行：输入框 + 粘贴 + 应用。
 * 写法帮助已移到首页顶栏的 "?"（见 [SyntaxHelpDialog]），这里不再占位。
 */
@Composable
fun ExpressionInput(
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onPlot: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

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
                shape = AtomicShapes.Control,
                textStyle = MaterialTheme.typography.titleMedium,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    errorContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                ),
                modifier = Modifier.weight(1f)
            )

            FilledTonalIconButton(
                onClick = {
                    val text = clipboard.getText()?.text
                    if (!text.isNullOrBlank()) onValueChange(text.trim())
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    Icons.Default.ContentPaste,
                    contentDescription = "粘贴",
                    modifier = Modifier.size(19.dp)
                )
            }

            FilledIconButton(
                onClick = onPlot,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(54.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "应用",
                    modifier = Modifier.size(21.dp)
                )
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 10.dp, top = 2.dp)
            )
        }
    }
}
