package com.yumoo.functionplotter.data.update

import android.content.Context
import android.os.Environment
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 应用内下载器：不依赖系统 DownloadManager。
 *
 * 系统下载器在"目标文件已存在 / 任务被清理 / 厂商省电策略"下行为不可控，
 * 出现过一直装回旧包、卡 0% 无法感知等问题。这里自己流式下载到应用私有目录：
 *  - 文件名带版本号，天然不会读到旧包；
 *  - 进度、超时、异常都由自己掌握；
 *  - 下载后可直接校验 SHA-256。
 */
class ApkDownloader(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun updateDir(): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "updates").apply { mkdirs() }

    /** 删除历史包，避免占用空间与误装。 */
    fun clearPrevious() {
        updateDir().listFiles()?.forEach { it.delete() }
    }

    fun apkFor(version: String): File = File(updateDir(), "FunctionPlotter-$version.apk")

    /**
     * 下载到 [apkFor] 指定的文件。成功返回文件，失败返回 null。
     * [onProgress] 回调 0..100，回调线程为调用线程。
     */
    fun download(
        url: String,
        version: String,
        onProgress: (Int) -> Unit
    ): File? {
        clearPrevious()
        val target = apkFor(version)
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "FunctionPlotter-Android")
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.peekBody(Long.MAX_VALUE)
                val total = body.contentLength()
                var written = 0L
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
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
                    }
                }
                if (total > 0 && written != total) {
                    target.delete()
                    null
                } else {
                    target
                }
            }
        } catch (t: Throwable) {
            target.delete()
            null
        }
    }

    /** 校验 SHA-256；expected 为空时跳过。 */
    fun verifySha256(file: File, expected: String?): Boolean {
        val want = expected?.lowercase()?.trim() ?: return true
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
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            actual == want
        } catch (t: Throwable) {
            false
        }
    }
}