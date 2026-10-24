package com.yumoo.functionplotter.ui.component

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yumoo.functionplotter.ui.theme.AtomicShapes
import com.yumoo.functionplotter.ui.theme.atomicSurface
import com.yumoo.functionplotter.viewmodel.UpdateViewModel

/**
 * 更新相关的全局交互：新版本对话框、"安装未知应用"权限跳转、通知权限申请。
 * 由 AppNav 挂载一次，避免多个页面重复弹窗。
 */
@Composable
fun UpdateHost(viewModel: UpdateViewModel) {
    val context = LocalContext.current
    val available by viewModel.available.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val permissionRequests by viewModel.permissionRequests.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 拒绝也不影响下载，只是没有通知 */ }

    // 未授权「安装未知应用」时跳系统设置；返回后由 onResume 续下载
    LaunchedEffect(permissionRequests) {
        if (permissionRequests > 0) {
            runCatching { context.startActivity(viewModel.installPermissionIntent()) }
        }
    }

    var notificationAsked by remember { mutableStateOf(false) }
    LaunchedEffect(progress != null) {
        if (!notificationAsked && progress != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationAsked = true
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val info = available
    if (info != null) {
        UpdateDialog(
            info = info,
            onDismiss = viewModel::dismissAvailable,
            onDownload = { viewModel.requestDownload(info.downloadUrl) }
        )
    }
}

/**
 * 应用内下载进度条。点"隐藏"只收起 UI，下载和通知继续，
 * 满足"关闭弹窗仍可后台下载"。
 */
@Composable
fun UpdateBanner(viewModel: UpdateViewModel, modifier: Modifier = Modifier) {
    val progress by viewModel.progress.collectAsState()
    val current = progress ?: return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .atomicSurface(AtomicShapes.Control)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = when {
                        current == -2 -> "安装包校验失败，已删除"
                        current < 0 -> "更新下载失败"
                        current >= 100 -> "更新已下载完成，可以安装"
                        else -> "正在下载更新 $current%"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (current in 0..99) {
                    LinearProgressIndicator(
                        progress = { current / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
            }
            if (current >= 100) {
                TextButton(onClick = viewModel::install) { Text("安装") }
            } else {
                TextButton(onClick = viewModel::dismissProgress) { Text("隐藏") }
            }
        }
    }
}
