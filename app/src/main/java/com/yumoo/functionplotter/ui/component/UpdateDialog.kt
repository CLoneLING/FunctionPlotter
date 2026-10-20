package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.data.update.UpdateResult

@Composable
fun UpdateDialog(
    info: UpdateResult.Available,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 ${info.version}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(info.notes.ifBlank { "本次更新暂无说明。" })
            }
        },
        confirmButton = {
            TextButton(onClick = onDownload) { Text("下载并安装") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("稍后") }
        }
    )
}

@Composable
fun DownloadProgressDialog(
    progress: Int?,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 下载中不允许点击外部关闭 */ },
        title = { Text("下载更新") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    progress == null || progress < 0 -> {
                        Text("准备中…")
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    progress < 100 -> {
                        Text("正在下载… $progress%")
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {
                        Text("下载完成，点击安装")
                        LinearProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (progress == 100) {
                TextButton(onClick = onInstall) { Text("安装") }
            } else {
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}