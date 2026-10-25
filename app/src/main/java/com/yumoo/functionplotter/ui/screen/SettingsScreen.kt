package com.yumoo.functionplotter.ui.screen

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yumoo.functionplotter.BuildConfig
import com.yumoo.functionplotter.data.update.UpdateResult
import com.yumoo.functionplotter.ui.component.MarkdownText
import com.yumoo.functionplotter.ui.component.UpdateBanner
import com.yumoo.functionplotter.ui.theme.AtomicShapes
import com.yumoo.functionplotter.ui.theme.atomicSurface
import com.yumoo.functionplotter.viewmodel.SettingsViewModel
import com.yumoo.functionplotter.viewmodel.UpdateViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.os.Build

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    updateViewModel: UpdateViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val checking by updateViewModel.checking.collectAsState()
    val downloadProgress by updateViewModel.progress.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // 大屏上限宽 720dp 居中，避免通栏长句
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val sideInset = (((screenWidth - 720).coerceAtLeast(0)) / 2).dp

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
                .padding(horizontal = 16.dp + sideInset, vertical = 16.dp),
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
                ChoiceRow(
                    label = "护眼模式（仅浅色）",
                    options = listOf("关闭" to 0, "定时" to 1, "常开" to 2),
                    selected = state.eyeCareMode,
                    onSelect = viewModel::setEyeCareMode
                )
                if (state.eyeCareMode == 1) {
                    ChoiceRow(
                        label = "护眼时段（开始）",
                        options = listOf("18" to 18, "19" to 19, "20" to 20, "21" to 21, "22" to 22),
                        selected = state.eyeCareStart,
                        onSelect = viewModel::setEyeCareStart
                    )
                    ChoiceRow(
                        label = "护眼时段（结束）",
                        options = listOf("5" to 5, "6" to 6, "7" to 7, "8" to 8, "9" to 9),
                        selected = state.eyeCareEnd,
                        onSelect = viewModel::setEyeCareEnd
                    )
                }
                SwitchRow("界面动效", state.animations, viewModel::setAnimations)
                SwitchRow("画布常驻诗词", state.poemEnabled, viewModel::setPoemEnabled)
            }

            SectionCard("界面模块") {
                Text(
                    text = "隐藏后相邻模块会自动补齐空间",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                SwitchRow("显示写法帮助按钮（?）", state.showHelpButton, viewModel::setShowHelpButton)
                SwitchRow("显示按键说明文字", state.showKeyHints, viewModel::setShowKeyHints)
                SwitchRow("显示键盘分页栏", state.showKeyTabs, viewModel::setShowKeyTabs)
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
                    enabled = !checking && downloadProgress == null,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            checking -> "正在检查…"
                            downloadProgress != null -> "正在下载更新…"
                            else -> "立即检查更新"
                        }
                    )
                }
            }

            SectionCard("赞助与支持") {
                Text(
                    "如果这个工具帮到了你，可以请我喝杯咖啡吗 (ゝω・´★)",
                    fontSize = 13.sp
                )
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://ifdian.net/a/yumoo"))) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("前往爱发电支持")
                }
                Text(
                    "本应用对所有用户免费开放，赞助不附带任何功能或特权\n" +
                        "未成年人请在监护人知情并同意的前提下再考虑支持",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            SectionCard("关于") {
                MarkdownText(markdown = ABOUT_MARKDOWN)
                Button(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(feedbackUrl()))) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("提交反馈 / 建议")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .atomicSurface(AtomicShapes.Card)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
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

/** 关于页正文：用 Markdown 渲染，方便后续直接在字符串里加内容。 */
private val ABOUT_MARKDOWN = """
    **FunctionPlotter** · 简洁的函数图像绘制工具

    - 显函数、隐函数方程，以及单变量方程求根
    - 符号翻译：`√ ∛ |x| x² 2x`、`log(底数, 真数)`
    - 点按读数支持 π 形式

    ### 用户协议
    本应用按「现状」提供，用于学习与日常绘图，不对计算结果的准确性作保证

    ### 隐私说明
    不收集、不上传任何个人数据；表达式与设置只保存在本机，仅在检查更新时访问 GitHub 公开接口

    ### 鸣谢
    - API：一言（hitokoto.cn）、GitHub Releases
    - 开源：Jetpack Compose、OkHttp、Retrofit、exp4j、Gson（MIT / Apache-2.0）

    ### 开源许可
    MIT License · 表达式求值使用 exp4j
""".trimIndent()

/** 预填版本与机型，减少反馈时的来回沟通。 */
private fun feedbackUrl(): String {
    val body = """
        **问题 / 建议描述**



        ---
        - 版本：${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        - 机型：${Build.MODEL}（Android ${Build.VERSION.RELEASE} / API ${Build.VERSION.SDK_INT}）
    """.trimIndent()

    return Uri.Builder()
        .scheme("https")
        .authority("github.com")
        .appendPath("CLoneLING")
        .appendPath("FunctionPlotter")
        .appendPath("issues")
        .appendPath("new")
        .appendQueryParameter("title", "[反馈] ")
        .appendQueryParameter("body", body)
        .build()
        .toString()
}

@Composable
private fun ChoiceRow(
    label: String,
    options: List<Pair<String, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (text, value) ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    label = { Text(text, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    }
}