package com.yumoo.functionplotter.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

class UpdateManager(private val context: Context) {

    /** 从版本说明里解析出的 SHA-256，下载后用于校验。 */
    private var expectedSha256: String? = null

    private val owner = "CLoneLING"
    private val repo = "FunctionPlotter"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "FunctionPlotter-Android")
                .header("Accept", "application/vnd.github+json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val api: GithubApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GithubApi::class.java)

    suspend fun checkForUpdate(currentVersion: String): UpdateResult {
        return try {
            val release = api.getLatestRelease(owner, repo)
            val latestTag = (release.tagName ?: "").removePrefix("v").trim()
            if (latestTag.isBlank() || !isNewerVersion(latestTag, currentVersion)) {
                return UpdateResult.UpToDate
            }

            val apkAsset = release.assets.orEmpty().firstOrNull {
                it.name?.endsWith(".apk", ignoreCase = true) == true
            } ?: return UpdateResult.NoApkFound
            val downloadUrl = apkAsset.browserDownloadUrl ?: return UpdateResult.NoApkFound

            expectedSha256 = extractSha256(release.body ?: "")

            UpdateResult.Available(
                version = latestTag,
                notes = release.body ?: "",
                downloadUrl = downloadUrl,
                htmlUrl = release.htmlUrl ?: ""
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "网络请求失败")
        }
    }

    /** 是否已允许"安装未知来源应用"。minSdk 26，API 一定存在。 */
    fun canInstallPackages(): Boolean = try {
        context.packageManager.canRequestPackageInstalls()
    } catch (e: Exception) {
        false
    }

    fun installPermissionIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun downloadApk(url: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("FunctionPlotter 更新")
            .setDescription("正在下载新版本…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                APK_FILE_NAME
            )
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    fun observeProgress(downloadId: Long): Flow<Int> = flow {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var missingRounds = 0
        while (true) {
            val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
            try {
                if (cursor.moveToFirst()) {
                    missingRounds = 0
                    val downloaded = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    )
                    val total = cursor.getLong(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    )
                    val status = cursor.getInt(
                        cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                    )
                    if (total > 0) {
                        emit((downloaded * 100 / total).toInt().coerceIn(0, 100))
                    }
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        if (!verifyDownloadedApk()) {
                            emit(-2)
                            return@flow
                        }
                        emit(100)
                        return@flow
                    }
                    if (status == DownloadManager.STATUS_FAILED) {
                        emit(-1)
                        return@flow
                    }
                } else {
                    // 下载记录被清理或任务已不存在，避免无限轮询
                    missingRounds++
                    if (missingRounds > 5) {
                        emit(-1)
                        return@flow
                    }
                }
            } finally {
                cursor.close()
            }
            delay(400)
        }
    }

    /**
     * 校验下载包的 SHA-256。版本说明里没有写哈希时跳过（兼容旧版本）。
     */
    private fun verifyDownloadedApk(): Boolean {
        val expected = expectedSha256?.lowercase() ?: return true
        val file = downloadedApkFile()
        if (!file.exists()) return false
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            val ok = actual == expected
            if (!ok) file.delete()
            ok
        } catch (t: Throwable) {
            false
        }
    }

    private fun extractSha256(text: String): String? =
        Regex("SHA-?256[^0-9a-fA-F]*([0-9a-fA-F]{64})")
            .find(text)?.groupValues?.getOrNull(1)

    fun installDownloadedApk(): Boolean {
        val apkFile = downloadedApkFile()
        if (!apkFile.exists()) return false

        if (!canInstallPackages()) {
            context.startActivity(installPermissionIntent())
            return false
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
        return true
    }

    private fun downloadedApkFile(): File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        APK_FILE_NAME
    )

    companion object {
        private const val APK_FILE_NAME = "FunctionPlotter-update.apk"
    }
}
