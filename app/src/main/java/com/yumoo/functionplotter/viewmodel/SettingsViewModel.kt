package com.yumoo.functionplotter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumoo.functionplotter.BuildConfig
import com.yumoo.functionplotter.FunctionPlotterApp
import com.yumoo.functionplotter.data.update.UpdateManager
import com.yumoo.functionplotter.data.update.UpdateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: Int = 0,
    val showGrid: Boolean = true,
    val showAxisLabels: Boolean = true,
    val autoCheckUpdate: Boolean = true,
    val sampleStep: Float = 2f
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as FunctionPlotterApp).settingsRepository
    private val updateManager = UpdateManager(app)

    val uiState: StateFlow<SettingsUiState> = combine(
        repo.themeMode,
        repo.showGrid,
        repo.showAxisLabels,
        repo.autoCheckUpdate,
        repo.sampleStep
    ) { theme, grid, labels, auto, step ->
        SettingsUiState(theme, grid, labels, auto, step)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    private val _updateInfo = MutableStateFlow<UpdateResult.Available?>(null)
    val updateInfo: StateFlow<UpdateResult.Available?> = _updateInfo.asStateFlow()

    fun setThemeMode(v: Int) = viewModelScope.launch { repo.setThemeMode(v) }
    fun setShowGrid(v: Boolean) = viewModelScope.launch { repo.setShowGrid(v) }
    fun setShowAxisLabels(v: Boolean) = viewModelScope.launch { repo.setShowAxisLabels(v) }
    fun setAutoCheckUpdate(v: Boolean) = viewModelScope.launch { repo.setAutoCheckUpdate(v) }
    fun setSampleStep(v: Float) = viewModelScope.launch { repo.setSampleStep(v) }

    suspend fun checkForUpdate(): UpdateResult {
        val result = updateManager.checkForUpdate(BuildConfig.VERSION_NAME)
        if (result is UpdateResult.Available) _updateInfo.value = result
        return result
    }

    fun dismissUpdateDialog() { _updateInfo.value = null }

    fun downloadUpdate(url: String) { updateManager.downloadApk(url) }

    fun installDownloaded() { updateManager.installDownloadedApk() }
}