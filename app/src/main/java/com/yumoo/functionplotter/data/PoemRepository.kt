package com.yumoo.functionplotter.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 一言（诗词分类）抓取。失败时返回 null，由调用方静默跳过。
 */
object PoemRepository {

    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("https://v1.hitokoto.cn/?c=i&c=d&encode=json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "FunctionPlotter-Android")
            }
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (body.isBlank()) return@withContext null
            val json = JSONObject(body)
            val text = json.optString("hitokoto").trim()
            val from = json.optString("from").trim()
            when {
                text.isEmpty() -> null
                from.isEmpty() -> text
                else -> "$text  —— $from"
            }
        } catch (t: Throwable) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}