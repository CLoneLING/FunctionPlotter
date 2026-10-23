package com.yumoo.functionplotter.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 写法说明。首页顶栏的 "?" 与输入错误提示都会用到。 */
@Composable
fun SyntaxHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("写法说明") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HelpLine("y = f(x)", "直接输入 f(x) 即可，例如 x^2、sin(x)、1/x")
                HelpLine("方程", "含等号表示方程，例如 x^2+y^2=9 画圆、x^2=4 求根")
                HelpLine("√", "平方根，√9 与 √(x+1) 都支持；三次方根用 ∛")
                HelpLine("ln", "自然对数（底数 e）")
                HelpLine("lg", "常用对数（底数 10）")
                HelpLine("log(a, b)", "以 a 为底的 b 的对数，例如 log(2, 8) = 3")
                HelpLine("^", "乘方，x^2 就是 x 的平方；也可以写 x²")
                HelpLine("|x|", "绝对值，也可以写 abs(x)")
                HelpLine("省略乘号", "2x、2(x+1)、sin(x)cos(x) 都能识别")
                HelpLine("π 与 e", "圆周率和自然常数，可直接当数字用")
                HelpLine("手势", "拖动平移、双指缩放、点按曲线读数")
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
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text = description, fontSize = 13.sp)
    }
}
