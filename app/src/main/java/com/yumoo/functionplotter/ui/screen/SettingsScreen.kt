package com.yumoo.functionplotter.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.BuildConfig
import com.yumoo.functionplotter.data.update.UpdateResult
import com.yumoo.functionplotter.ui.component.UpdateBanner
import com.yumoo.functionplotter.viewmodel.SettingsViewModel
import com.yumoo.functionplotter.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val checking by updateViewModel.checking.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UpdateBanner(viewModel = updateViewModel)

            SectionCard("外观") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("跟随系统", "浅色", "深色").forEachIndexed { index, label ->
                        FilterChip(
                            selected = state.themeMode == index,
                            onClick = { viewModel.setThemeMode(index) },
                            label = { Text(label) }
                        )
                    }
                }
                SwitchRow("显示网格", state.showGrid, viewModel::setShowGrid)
                SwitchRow("显示刻度标签", state.showAxisLabels, viewModel::setShowAxisLabels)
            }

            SectionCard("绘图") {
                Text("采样精度（数值越小越精细）")
                Slider(
                    value = state.sampleStep,
                    onValueChange = viewModel::setSampleStep,
                    valueRange = 1f..5f,
                    steps = 3
                )
            }

            SectionCard("更新") {
                Text("当前版本 ${BuildConfig.VERSION_NAME}")
                SwitchRow("启动时自动检查更新", state.autoCheckUpdate, viewModel::setAutoCheckUpdate)
                Button(
                    onClick = {
                        scope.launch {
                            when (val result = updateViewModel.checkNow()) {
                                is UpdateResult.Available -> Unit
                                UpdateResult.UpToDate -> snackbar.showSnackbar("已是最新版本")
                                UpdateResult.NoApkFound -> snackbar.showSnackbar("新版本未提供 APK")
                                is UpdateResult.Error -> snackbar.showSnackbar("检查失败：${result.message}")
                            }
                        }
                    },
                    enabled = !checking,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (checking) "正在检查…" else "立即检查更新")
                }
            }

            SectionCard("关于") {
                Text("FunctionPlotter", style = MaterialTheme.typography.titleMedium)
                Text("项目主页：Github - CLoneLING/FunctionPlotter")
                Text("开源许可：MIT License")
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
