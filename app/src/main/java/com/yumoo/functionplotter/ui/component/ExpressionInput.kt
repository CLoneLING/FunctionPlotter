package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp

@Composable
fun ExpressionInput(
    value: String,
    onValueChange: (String) -> Unit,
    onPlot: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text("f(x) = ...", style = MaterialTheme.typography.titleMedium)
            },
            singleLine = true,
            readOnly = true,
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
            modifier = Modifier.size(52.dp)
        ) {
            Icon(
                Icons.Default.ContentPaste,
                contentDescription = "粘贴",
                modifier = Modifier.size(20.dp)
            )
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
}