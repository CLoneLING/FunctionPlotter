package com.yumoo.functionplotter.data.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

    private val owner = "GITHUB_OWNER"
    private val repo = "GITHUB_REPO"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "FunctionPlotter-Android")
                .header("Accept", "application/vnd.github+json")
                .build()
            chain.proceed(req)
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
            val latestTag = release.tagName.removePrefix("v").trim()
            if (latestTag == currentVersion) return UpdateResult.UpToDate

            val apkAsset = release.assets.firstOrNull {
                it.name.endsWith(".apk", ignoreCase = true)
            } ?: return UpdateResult.NoApkFound

            UpdateResult.Available(
                version = latestTag,
                notes = release.body,
                downloadUrl = apkAsset.browserDownloadUrl,
                htmlUrl = release.htmlUrl
            )
        } catch (e: Exception) {
            UpdateResult.Error(e.message ?: "网络请求失败")
        }
    }

    fun downloadApk(url: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("下载更新")
            .setDescription("正在下载新版本…")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "update.apk"
            )
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return dm.enqueue(request)
    }

    fun observeProgress(downloadId: Long): Flow<Int> = flow {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        while (true) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = dm.query(query)
            if (cursor.moveToFirst()) {
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
                    emit(100)
                    cursor.close()
                    return@flow
                }
                if (status == DownloadManager.STATUS_FAILED) {
                    emit(-1)
                    cursor.close()
                    return@flow
                }
            }
            cursor.close()
            delay(300)
        }
    }

    fun installDownloadedApk() {
        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "update.apk"
        )
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
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
    }
}