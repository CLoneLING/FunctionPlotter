package com.yumoo.functionplotter.domain.parser

/**
 * 批量导入的规则解析器。
 *
 * 面向"从别处复制一堆函数过来"的场景：格式往往乱七八糟（带序号、带 LaTeX、
 * 中英文混排、逗号/换行/分号混用）。这里不做语义求解，只做：
 *  1. 切分：换行、分号、以及"看起来是新表达式"的逗号处切开；
 *  2. 清洗：去序号/项目符号、去 `y=` `f(x)=` 前缀、去行内代码与 `$`、
 *     把常见 LaTeX 片段换成键盘能识别的写法；
 *  3. 过滤：丢弃中文说明文字、空行与括号不配平的片段。
 */
object ImportParser {

    data class Result(
        val expressions: List<String>,
        val skipped: Int
    ) {
        val isEmpty: Boolean get() = expressions.isEmpty()
    }

    private val BULLET = Regex("^\\s*(?:[-*•·>]|\\d+\\s*[.、)]|\\(\\d+\\)|[①-⑳])\\s*")
    private val LABEL = Regex("^[^=:：]{1,12}[:：]\\s*(?=[a-zA-Z0-9(（√∛π|])")
    private val FUNCTION_PREFIX = Regex(
        "^(?:[yY]|(?:[fFgGhH]|[a-zA-Z])\\s*\\(\\s*x\\s*\\))\\s*=\\s*"
    )
    private val CJK = Regex("[\\u4e00-\\u9fff]")
    /** 结尾是运算符，说明是残缺表达式 */
    private val TRAILING_OP = Regex("[-+*/^×÷=]$")
    /** 等号前面紧跟运算符，如 "2x+=0" */
    private val OP_BEFORE_EQ = Regex("[-+*/^×÷]\\s*=")

    fun parse(raw: String): Result {
        var skipped = 0
        val kept = LinkedHashSet<String>()

        for (segment in split(raw)) {
            val cleaned = clean(segment)
            if (cleaned.isEmpty()) continue
            if (!looksLikeExpression(cleaned)) {
                skipped++
                continue
            }
            // 只有真正能画出来的才导入，避免 "2x+=0" 这类残句混进来
            if (!isDrawable(cleaned)) {
                skipped++
                continue
            }
            kept += cleaned
        }
        return Result(kept.toList(), skipped)
    }

    /** 只用于预览提示：判断能否导入。 */
    fun isUsable(text: String): Boolean = clean(text).let { it.isNotEmpty() && looksLikeExpression(it) }


    /** 用与绘图一致的解析链路验证，避免导入后画不出来。 */
    private fun isDrawable(expression: String): Boolean {
        val evaluator = com.yumoo.functionplotter.domain.parser.Exp4jEvaluator()
        return when (val parsed = parsePlotExpression(expression)) {
            is PlotExpression.Explicit -> evaluator.prepareExplicit(parsed.expr) != null
            is PlotExpression.Implicit -> evaluator.prepareImplicit(parsed.expr) != null
            is PlotExpression.Invalid -> false
        }
    }
    private fun split(raw: String): List<String> {
        val text = raw
            .replace('\uFF1D', '=')   // ＝
            .replace('\uFF0C', ',')   // ，
            .replace('\uFF1B', ';')   // ；
            .replace('\r', '\n')

        val out = mutableListOf<String>()
        for (line in text.split('\n', ';')) {
            var start = 0
            var depth = 0
            for (i in line.indices) {
                when (line[i]) {
                    '(', '（' -> depth++
                    ')', '）' -> if (depth > 0) depth--
                    ',' -> if (depth == 0 && startsNewExpression(line.substring(i + 1))) {
                        out += line.substring(start, i)
                        start = i + 1
                    }
                }
            }
            out += line.substring(start)
        }
        return out
    }

    private fun startsNewExpression(tail: String): Boolean {
        val t = tail.trimStart()
        if (t.isEmpty()) return false
        if (t.startsWith("y=") || t.startsWith("Y=")) return true
        if (Regex("^[a-zA-Z]\\s*\\(\\s*x\\s*\\)\\s*=").containsMatchIn(t)) return true
        return t[0].isDigit() || t[0] in "+-√∛πxyXY|("
    }

    private fun clean(segment: String): String {
        var s = segment.trim()
        if (s.isEmpty()) return ""

        s = s.replace("`", "").replace("$", "").trim()
        s = BULLET.replace(s, "")
        s = LABEL.replace(s, "")
        s = latexToPlain(s)
        s = FUNCTION_PREFIX.replace(s, "")
        s = s.trim().trimEnd(',', '，', '.', '。', ';')
        s = s.replace(Regex("\\s+"), " ")
        // 等号两侧统一紧凑，避免 "y = x^2" 里残留空格干扰后续解析
        s = s.replace(Regex("\\s*=\\s*"), "=")
        return s.trim()
    }

    /** 把常见 LaTeX 写法换成键盘/归一化器认得的写法。 */
    private fun latexToPlain(input: String): String {
        var s = input
            .replace("\\left", "")
            .replace("\\right", "")
            .replace("\\cdot", "*")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\pi", "π")
            .replace("\\infty", "∞")
            .replace("\\{", "(")
            .replace("\\}", ")")
            .replace("^{", "^(")
            .replace("_{", "(")
            .replace("\\sqrt", "√")
            .replace("\\frac", "frac")

        // \frac{a}{b} 在去掉反斜杠后是 frac{a}{b}
        s = Regex("frac\\s*\\{([^{}]*)\\}\\s*\\{([^{}]*)\\}").replace(s) { m ->
            "(${m.groupValues[1]})/(${m.groupValues[2]})"
        }
        // 剩余的 { } 一律当括号
        s = s.replace("{", "(").replace("}", ")")
        return s
    }

    private fun looksLikeExpression(s: String): Boolean {
        if (s.length > 200) return false
        if (TRAILING_OP.containsMatchIn(s)) return false
        if (OP_BEFORE_EQ.containsMatchIn(s)) return false
        if (CJK.containsMatchIn(s)) return false
        if (!s.any { it.isLetterOrDigit() || it in "πxyXY" }) return false
        // 括号配平
        var depth = 0
        for (c in s) {
            when (c) {
                '(', '（' -> depth++
                ')', '）' -> { depth--; if (depth < 0) return false }
            }
        }
        if (depth != 0) return false
        // 含等号时两侧都不能空
        val eq = s.indexOf('=')
        if (eq >= 0) {
            if (s.substring(0, eq).isBlank() || s.substring(eq + 1).isBlank()) return false
        }
        return true
    }
}
