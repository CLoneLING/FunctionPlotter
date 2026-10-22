package com.yumoo.functionplotter.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yumoo.functionplotter.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("FunctionPlotter", style = MaterialTheme.typography.headlineSmall)
            Text("一个简洁的函数图像绘制工具。")
            Text("支持多项式、指数、对数、三角函数、双曲函数，以及 x²+y²=9 这类隐函数曲线。")
            Text("作者：YuMoo_")
            Text("版本：${BuildConfig.VERSION_NAME}")

            Text("使用说明", style = MaterialTheme.typography.titleMedium)
            Text("· 输入表达式后点 ▶ 即可绘制；输入框右侧的 ? 有完整写法说明。")
            Text("· 拖动平移、双指缩放、双击复位视图。")
            Text("· 点按曲线可以读取该点的坐标。")
            Text("· 最多同时绘制 5 条曲线，点击顶部的函数标签切换编辑对象。")

            Text("用户协议", style = MaterialTheme.typography.titleMedium)
            Text("本应用按「现状」提供，用于学习与日常绘图，不对计算结果的准确性作任何担保。请勿将本应用用于安全攸关的场景。")

            Text("隐私说明", style = MaterialTheme.typography.titleMedium)
            Text("应用不收集、不上传任何个人数据。表达式、设置只保存在本机。仅在检查更新时访问 GitHub 公开接口。")

            Text("开源许可", style = MaterialTheme.typography.titleMedium)
            Text("本项目以 MIT License 开源，使用 exp4j 进行表达式求值。")
        }
    }
}
