package com.yumoo.functionplotter.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumoo.functionplotter.BuildConfig
import com.yumoo.functionplotter.FunctionPlotterApp
import com.yumoo.functionplotter.data.update.ApkDownloader
import com.yumoo.functionplotter.data.update.DownloadNotifications
import com.yumoo.functionplotter.data.update.UpdateManager
import com.yumoo.functionplotter.data.update.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 更新流程的唯一状态源。
 *
 * 下载改用 [ApkDownloader]（OkHttp 流式下载）而不是系统 DownloadManager：
 * 系统下载器在目标文件已存在、任务被清理、厂商省电策略下行为不可控，
 * 曾导致"一直装回旧版本"和"卡 0% 无反馈"。
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as FunctionPlotterApp).settingsRepository
    private val manager = UpdateManager(app)
    private val downloader = ApkDownloader(app)
    private val notifications = DownloadNotifications(app)

    private val _checking = MutableStateFlow(false)
    val checking = _checking.asStateFlow()

    private val _available = MutableStateFlow<UpdateResult.Available?>(null)
    val available = _available.asStateFlow()

    /** null 表示没有下载任务；0..100 为进度；-1 表示失败。 */
    private val _progress = MutableStateFlow<Int?>(null)
    val progress = _progress.asStateFlow()

    /** 自增计数，>0 表示需要跳转系统「安装未知应用」页面。 */
    private val _permissionRequests = MutableStateFlow(0)
    val permissionRequests = _permissionRequests.asStateFlow()

    private var pendingUrl: String? = null
    private var autoCheckStarted = false
    private var downloadJob: Job? = null

    private var targetVersion: String? = null
    private var targetSha256: String? = null

    fun autoCheckOnce() {
        if (autoCheckStarted) return
        autoCheckStarted = true
        viewModelScope.launch {
            if (!repository.autoCheckUpdate.first()) return@launch
            val now = System.currentTimeMillis()

            _checking.value = true
            val result = manager.checkForUpdate(BuildConfig.VERSION_NAME)
            _checking.value = false

            if (result !is UpdateResult.Error) {
                repository.setLastUpdateCheck(now)
            }
            if (result is UpdateResult.Available) {
                onAvailable(result)
            }
        }
    }

    suspend fun checkNow(): UpdateResult {
        _checking.value = true
        val result = try {
            manager.checkForUpdate(BuildConfig.VERSION_NAME)
        } finally {
            _checking.value = false
        }
        if (result !is UpdateResult.Error) {
            repository.setLastUpdateCheck(System.currentTimeMillis())
        }
        if (result is UpdateResult.Available) {
            onAvailable(result)
        }
        return result
    }

    private fun onAvailable(result: UpdateResult.Available) {
        parseMirror(result.notes)
        targetVersion = result.version
        targetSha256 = manager.expectedSha()
        _available.value = result
    }

    fun dismissAvailable() {
        _available.value = null
    }

    /** 版本说明里带的蓝奏云备用下载信息。 */
    private var mirrorUrl: String? = null
    private var mirrorCode: String? = null

    fun hasMirror(): Boolean = mirrorUrl != null

    /** 打开蓝奏云分享页（浏览器），并提示提取码。 */
    fun openMirror() {
        val url = mirrorUrl ?: return
        try {
            val context = getApplication<Application>()
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            mirrorCode?.let {
                Toast.makeText(context, "提取码：$it", Toast.LENGTH_LONG).show()
            }
        } catch (_: Throwable) {
        }
    }

    private fun parseMirror(notes: String) {
        mirrorUrl = Regex("蓝奏云[:：]\\s*(https?://\\S+)").find(notes)?.groupValues?.getOrNull(1)
        mirrorCode = Regex("提取码[:：]\\s*([A-Za-z0-9]+)").find(notes)?.groupValues?.getOrNull(1)
    }

    fun installPermissionIntent(): Intent = manager.installPermissionIntent()

    fun requestDownload(url: String) {
        if (_progress.value != null) return
        if (!manager.canInstallPackages()) {
            pendingUrl = url
            _permissionRequests.value += 1
            return
        }
        startDownload(url)
    }

    /** 从系统「安装未知应用」设置返回后继续下载。 */
    fun onResume() {
        val url = pendingUrl ?: return
        if (manager.canInstallPackages()) {
            pendingUrl = null
            _available.value = null
            startDownload(url)
        }
    }

    fun dismissProgress() {
        // 只隐藏应用内进度条，下载继续（通知栏仍有进度）
        _progress.value = null
    }

    fun install() {
        val version = targetVersion
        val file: File? = if (version != null) downloader.apkFor(version) else null
        if (file != null && file.exists()) {
            manager.installApkFile(file)
        } else {
            manager.installDownloadedApk()
        }
        _progress.value = null
        notifications.clear()
    }

    private fun startDownload(url: String) {
        val version = targetVersion ?: return
        _available.value = null
        _progress.value = 0
        notifications.ensureChannel()

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            val file = withContext(Dispatchers.IO) {
                downloader.download(url, version) { percent ->
                    _progress.value = percent
                    notifications.progress(percent)
                }
            }
            if (file == null) {
                _progress.value = -1
                notifications.failed()
                return@launch
            }
            val verified = withContext(Dispatchers.IO) {
                downloader.verifySha256(file, targetSha256)
            }
            if (!verified) {
                file.delete()
                _progress.value = -1
                notifications.failed()
                return@launch
            }
            _progress.value = 100
            notifications.complete()
        }
    }
}
