package com.yumoo.functionplotter.domain.parser

private enum class TokenKind { Number, Identifier, Operator, OpenParen, CloseParen, Comma, Equals }

private data class Token(val kind: TokenKind, val text: String)

/**
 * 把"人能看懂"的表达式翻译成 exp4j 能解析的表达式。
 *
 * 解决的问题：
 *  - exp4j 没有 `ln`，它的 `log` 其实是自然对数，`lg` 更不存在；
 *  - exp4j 不支持省略乘号（`2x`、`2(x+1)`）；
 *  - 用户会直接输入 `×`、`÷`、`−`、`π`、`√`、`|x|`、`x²` 这类数学符号；
 *  - 对数需要指定底数，而 exp4j 只有 log/log10/log2。
 *
 * 规则只做"等价改写"，不会改变数学含义；无法理解的片段原样保留，
 * 交给 exp4j 报错，从而不会把错误悄悄吞掉。
 */
object ExpressionNormalizer {

    /** exp4j 0.4.8 内置函数名（含本项目用来做翻译的别名）。 */
    val knownFunctions: Set<String> = setOf(
        "sin", "cos", "tan", "cot", "asin", "acos", "atan",
        "sinh", "cosh", "tanh", "abs", "log", "log10", "log2", "log1p",
        "sqrt", "cbrt", "pow", "exp", "expm1", "signum", "floor", "ceil"
    )

    private val SUPERSCRIPT_DIGITS = mapOf(
        '⁰' to '0', '¹' to '1', '²' to '2', '³' to '3', '⁴' to '4',
        '⁵' to '5', '⁶' to '6', '⁷' to '7', '⁸' to '8', '⁹' to '9',
        '⁺' to '+', '⁻' to '-', '⁽' to '(', '⁾' to ')'
    )

    private val ALIASES: List<Pair<String, String>> = listOf(
        "arcsin" to "asin",
        "arccos" to "acos",
        "arctan" to "atan",
        "ln" to "log",
        "lg" to "log10",
        "log₁₀" to "log10",
        "log₂" to "log2",
        "logₑ" to "log"
    )

    fun normalize(raw: String): String {
        var s = raw.trim()
        if (s.isEmpty()) return s
        s = convertSuperscripts(s)
        s = convertOperators(s)
        s = s.replace(Regex("\\s+"), "")
        s = s.replace("π", "pi").replace("Π", "pi")
        s = convertRoot(s, '√', "sqrt")
        s = convertRoot(s, '∛', "cbrt")
        s = applyAliases(s)
        s = convertAbsoluteValue(s)
        s = rewriteLogWithBase(s)
        s = insertImplicitMultiplication(s)
        return s
    }

    // ---------------------------------------------------------------- 上标

    private fun convertSuperscripts(input: String): String {
        val sb = StringBuilder(input.length + 8)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (SUPERSCRIPT_DIGITS.containsKey(c)) {
                val digits = StringBuilder()
                while (i < input.length && SUPERSCRIPT_DIGITS.containsKey(input[i])) {
                    digits.append(SUPERSCRIPT_DIGITS.getValue(input[i]))
                    i++
                }
                sb.append("^(").append(digits).append(')')
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    // ---------------------------------------------------------------- 符号

    private fun convertOperators(input: String): String = input
        .replace('×', '*')
        .replace('⋅', '*')
        .replace('·', '*')
        .replace('∗', '*')
        .replace('✕', '*')
        .replace('÷', '/')
        .replace('∕', '/')
        .replace('−', '-')
        .replace('–', '-')
        .replace('—', '-')
        .replace('（', '(')
        .replace('）', ')')
        .replace('，', ',')

    /**
     * `√9` → `sqrt(9)`，`√x` → `sqrt(x)`，`√(x+1)` → `sqrt(x+1)`。
     * 键盘插入的 `√(` 本身已配对括号，这里不会重复包一层。
     */
    private fun convertRoot(input: String, symbol: Char, function: String): String {
        if (!input.contains(symbol)) return input
        val escaped = Regex.escape(symbol.toString())
        var out = Regex("$escaped\\s*\\(([^()]*)\\)").replace(input) {
            "$function(${it.groupValues[1]})"
        }
        out = Regex("$escaped\\s*(\\d+(?:\\.\\d+)?)").replace(out) {
            "$function(${it.groupValues[1]})"
        }
        out = Regex("$escaped\\s*([a-zA-Z]+)").replace(out) {
            "$function(${it.groupValues[1]})"
        }
        // 剩下的孤立符号（例如 `√(`）直接当作函数前缀
        return out.replace(symbol.toString(), "$function(")
    }

    private fun applyAliases(input: String): String {
        var out = input
        ALIASES.forEach { (from, to) ->
            val pattern = "(?<![A-Za-z])" + Regex.escape(from) + "(?![A-Za-z0-9])"
            out = Regex(pattern, RegexOption.IGNORE_CASE).replace(out, to)
        }
        return out
    }

    /** `|x|` → `abs(x)`；竖线不成对时原样返回，由表达式检查给出提示。 */
    private fun convertAbsoluteValue(input: String): String {
        if (!input.contains('|')) return input
        val sb = StringBuilder(input.length + 8)
        var depth = 0
        for (c in input) {
            if (c == '|') {
                sb.append(if (depth == 0) "abs(" else ")")
                depth = 1 - depth
            } else {
                sb.append(c)
            }
        }
        return if (depth == 0) sb.toString() else input
    }

    // ---------------------------------------------------------------- 对数底数

    /** `log(2, 8)` → `(log(8)/log(2))`，嵌套写法也支持。 */
    private fun rewriteLogWithBase(input: String): String {
        if (input.length < 5 || !input.contains(',')) return input
        val lower = input.lowercase()
        val sb = StringBuilder(input.length + 16)
        var i = 0
        while (i < input.length) {
            if (lower.startsWith("log(", i) && (i == 0 || !input[i - 1].isLetterOrDigit())) {
                val open = i + 3
                val close = findMatchingParen(input, open)
                if (close > open) {
                    val args = splitTopLevel(input.substring(open + 1, close))
                    if (args.size == 2) {
                        val base = rewriteLogWithBase(args[0].trim())
                        val value = rewriteLogWithBase(args[1].trim())
                        sb.append("(log(").append(value).append(")/log(").append(base).append("))")
                        i = close + 1
                        continue
                    }
                }
            }
            sb.append(input[i])
            i++
        }
        return sb.toString()
    }

    private fun findMatchingParen(s: String, openIndex: Int): Int {
        var depth = 0
        for (i in openIndex until s.length) {
            when (s[i]) {
                '(' -> depth++
                ')' -> {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun splitTopLevel(s: String): List<String> {
        val parts = ArrayList<String>(2)
        val current = StringBuilder()
        var depth = 0
        for (c in s) {
            when (c) {
                '(' -> { depth++; current.append(c) }
                ')' -> { depth--; current.append(c) }
                ',' -> if (depth == 0) {
                    parts.add(current.toString())
                    current.clear()
                } else {
                    current.append(c)
                }
                else -> current.append(c)
            }
        }
        parts.add(current.toString())
        return parts
    }

    // ---------------------------------------------------------------- 省略乘号

    private fun insertImplicitMultiplication(input: String): String {
        val tokens = tokenize(input)
        if (tokens.isEmpty()) return input
        val sb = StringBuilder(input.length + tokens.size / 2)
        var previous: Token? = null
        for (token in tokens) {
            if (previous != null && needsMultiplication(previous, token)) sb.append('*')
            sb.append(token.text)
            previous = token
        }
        return sb.toString()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = ArrayList<Token>(input.length / 2 + 4)
        var i = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c.isDigit() || c == '.' -> {
                    var j = i
                    while (j < input.length && (input[j].isDigit() || input[j] == '.')) j++
                    // 科学计数法 1e-3 / 2.5E6 整体作为一个数字，避免被拆成 1*e-3
                    if (j < input.length && (input[j] == 'e' || input[j] == 'E')) {
                        var k = j + 1
                        if (k < input.length && (input[k] == '+' || input[k] == '-')) k++
                        if (k < input.length && input[k].isDigit()) {
                            while (k < input.length && input[k].isDigit()) k++
                            j = k
                        }
                    }
                    tokens.add(Token(TokenKind.Number, input.substring(i, j)))
                    i = j
                }
                c.isLetter() -> {
                    var j = i
                    while (j < input.length && input[j].isLetter()) j++
                    var name = input.substring(i, j)
                    if (name.equals("log", ignoreCase = true)) {
                        var k = j
                        while (k < input.length && (input[k].isDigit() || input[k].isLetter())) k++
                        val extended = input.substring(i, k).lowercase()
                        if (extended in knownFunctions) {
                            name = extended
                            j = k
                        }
                    }
                    tokens.add(Token(TokenKind.Identifier, name))
                    i = j
                }
                c == '(' -> { tokens.add(Token(TokenKind.OpenParen, "(")); i++ }
                c == ')' -> { tokens.add(Token(TokenKind.CloseParen, ")")); i++ }
                c == ',' -> { tokens.add(Token(TokenKind.Comma, ",")); i++ }
                c == '=' -> { tokens.add(Token(TokenKind.Equals, "=")); i++ }
                else -> { tokens.add(Token(TokenKind.Operator, c.toString())); i++ }
            }
        }
        return tokens
    }

    private fun needsMultiplication(previous: Token, current: Token): Boolean {
        val leftIsValue = previous.kind == TokenKind.Number ||
            previous.kind == TokenKind.CloseParen ||
            (previous.kind == TokenKind.Identifier &&
                previous.text.lowercase() !in knownFunctions)
        val rightStartsValue = current.kind == TokenKind.Number ||
            current.kind == TokenKind.OpenParen ||
            current.kind == TokenKind.Identifier
        return leftIsValue && rightStartsValue
    }
}
