package com.yumoo.functionplotter.data.update

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * 应用内下载器：不依赖系统 DownloadManager，也不用 OkHttp 的 Response.body
 * （当前 OkHttp 5 的 body 字段不可直接访问，peekBody 又会先缓冲整个响应体，
 * 导致没有下载进度）。这里用 HttpURLConnection 做真正的流式下载。
 */
class ApkDownloader(private val context: Context) {

    fun updateDir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates").apply { mkdirs() }

    fun clearPrevious() {
        updateDir().listFiles()?.forEach { it.delete() }
    }

    fun apkFor(version: String): File = File(updateDir(), "FunctionPlotter-$version.apk")

    /**
     * 流式下载到 [apkFor]。成功返回文件，失败返回 null。
     * [onProgress] 回调 0..100。
     */
    fun download(url: String, version: String, onProgress: (Int) -> Unit): File? {
        clearPrevious()
        val target = apkFor(version)
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "FunctionPlotter-Android")
                setRequestProperty("Accept", "application/octet-stream")
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                target.delete()
                return null
            }
            val total = connection.contentLengthLong
            var written = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) {
                            onProgress((written * 100 / total).toInt().coerceIn(0, 100))
                        }
                    }
                    output.flush()
                }
            }
            if (written <= 0 || (total > 0 && written != total)) {
                target.delete()
                null
            } else {
                target
            }
        } catch (t: Throwable) {
            target.delete()
            null
        } finally {
            connection?.disconnect()
        }
    }

    /** 校验 SHA-256；expected 为空或格式不对时跳过。 */
    fun verifySha256(file: File, expected: String?): Boolean {
        val want = expected?.trim()?.lowercase() ?: return true
        if (want.length != 64) return true
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) } == want
        } catch (t: Throwable) {
            false
        }
    }
}