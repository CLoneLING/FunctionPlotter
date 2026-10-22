package com.yumoo.functionplotter.data.update

/**
 * 语义化版本比较。旧实现用字符串相等判断（`0.1.9` != `0.1.24`），
 * 一旦版本号位数变化就会误判"有新版本"或"已是最新"。
 */
fun compareVersions(left: String, right: String): Int {
    val a = parseVersion(left)
    val b = parseVersion(right)
    for (index in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(index) { 0 } - b.getOrElse(index) { 0 }
        if (diff != 0) return if (diff > 0) 1 else -1
    }
    return 0
}

fun isNewerVersion(remote: String, current: String): Boolean =
    compareVersions(remote, current) > 0

private fun parseVersion(version: String): List<Int> =
    version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .split('.', '-', '+', ' ')
        .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
