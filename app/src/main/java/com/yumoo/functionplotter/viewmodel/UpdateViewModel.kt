package com.yumoo.functionplotter.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumoo.functionplotter.BuildConfig
import com.yumoo.functionplotter.FunctionPlotterApp
import com.yumoo.functionplotter.data.update.DownloadNotifications
import com.yumoo.functionplotter.data.update.UpdateManager
import com.yumoo.functionplotter.data.update.UpdateResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 更新流程的唯一状态源。
 *
 * 挂在这一层（Activity 作用域）而不是某个页面，是为了让"设置页检查更新"
 * 和"启动时自动检查"共用同一份状态，也方便下载在页面切换后继续。
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as FunctionPlotterApp).settingsRepository
    private val manager = UpdateManager(app)
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
    private var observeJob: Job? = null

    init {
        // 进程重启后如果还有未完成的下载，接着跟踪进度
        viewModelScope.launch {
            val storedId = repository.pendingDownloadId.first()
            if (storedId > 0) {
                _progress.value = 0
                observe(storedId)
            }
        }
    }

    /**
     * 启动时自动检查，最多每 24 小时一次。
     * 之前这个开关只是把值写进 DataStore，没有任何代码读它，属于空开关。
     */
    fun autoCheckOnce() {
        if (autoCheckStarted) return
        autoCheckStarted = true
        viewModelScope.launch {
            if (!repository.autoCheckUpdate.first()) return@launch
            val lastCheck = repository.lastUpdateCheck.first()
            val now = System.currentTimeMillis()
            // 每次冷启动检查一次，不再做 24 小时节流

            _checking.value = true
            val result = manager.checkForUpdate(BuildConfig.VERSION_NAME)
            _checking.value = false

            if (result !is UpdateResult.Error) {
                repository.setLastUpdateCheck(now)
            }
            if (result is UpdateResult.Available) {
                _available.value = result
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
            _available.value = result
        }
        return result
    }

    fun dismissAvailable() {
        _available.value = null
    }

    fun installPermissionIntent(): Intent = manager.installPermissionIntent()

    /**
     * 下载入口：先确认「安装未知应用」权限。
     * 没权限就不下载，只把用户送去授权页；返回后由 onResume 续上。
     */
    fun requestDownload(url: String) {
        if (_progress.value != null) return
        if (manager.canInstallPackages()) {
            startDownload(url)
        } else {
            pendingUrl = url
            _permissionRequests.value += 1
        }
    }

    /** 页面回到前台时调用：用户从系统设置授权回来后自动开始下载。 */
    fun onResume() {
        val url = pendingUrl ?: return
        if (manager.canInstallPackages()) {
            pendingUrl = null
            _available.value = null
            startDownload(url)
        }
    }

    fun dismissProgress() {
        // 只隐藏应用内进度条，下载与通知继续
        _progress.value = null
    }

    fun install() {
        manager.installDownloadedApk()
        _progress.value = null
        notifications.clear()
    }

    private fun startDownload(url: String) {
        _available.value = null
        _progress.value = 0
        notifications.ensureChannel()
        val id = manager.downloadApk(url)
        if (id <= 0) {
            _progress.value = -1
            return
        }
        viewModelScope.launch { repository.setPendingDownloadId(id) }
        observe(id)
    }

    private fun observe(downloadId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            manager.observeProgress(downloadId).collect { percent ->
                _progress.value = percent
                when {
                    percent < 0 -> {
                        notifications.failed()
                        repository.setPendingDownloadId(-1)
                    }
                    percent >= 100 -> {
                        notifications.complete()
                        repository.setPendingDownloadId(-1)
                    }
                    else -> notifications.progress(percent)
                }
            }
        }
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
