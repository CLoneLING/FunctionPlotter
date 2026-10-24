package com.yumoo.functionplotter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yumoo.functionplotter.FunctionPlotterApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themeMode: Int = 0,
    val showGrid: Boolean = true,
    val showAxisLabels: Boolean = true,
    val autoCheckUpdate: Boolean = true,
    val sampleStep: Float = 2f,
    val showHelpButton: Boolean = true,
    val showKeyHints: Boolean = false,
    val showKeyTabs: Boolean = false,
    val animations: Boolean = true,
    val eyeCareMode: Int = 1,
    val poemEnabled: Boolean = false,
    val eyeCareStart: Int = 20,
    val eyeCareEnd: Int = 7
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as FunctionPlotterApp).settingsRepository

    private val moduleFlags = combine(
        repository.showHelpButton,
        repository.showKeyHints,
        repository.showKeyTabs
    ) { help, hints, tabs -> Triple(help, hints, tabs) }

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.themeMode,
        repository.showGrid,
        repository.showAxisLabels,
        repository.autoCheckUpdate,
        repository.sampleStep
    ) { theme, grid, labels, auto, step ->
        SettingsUiState(theme, grid, labels, auto, step)
    }.combine(moduleFlags) { state, (help, hints, tabs) ->
        state.copy(showHelpButton = help, showKeyHints = hints, showKeyTabs = tabs)
    }.combine(repository.animations) { state, anim ->
        state.copy(animations = anim)
    }.combine(repository.eyeCareMode) { state, eye ->
        state.copy(eyeCareMode = eye)
    }.combine(repository.poemEnabled) { state, poem ->
        state.copy(poemEnabled = poem)
    }.combine(repository.eyeCareStart) { state, start ->
        state.copy(eyeCareStart = start)
    }.combine(repository.eyeCareEnd) { state, end ->
        state.copy(eyeCareEnd = end)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun setThemeMode(value: Int) = viewModelScope.launch { repository.setThemeMode(value) }
    fun setShowGrid(value: Boolean) = viewModelScope.launch { repository.setShowGrid(value) }
    fun setShowAxisLabels(value: Boolean) = viewModelScope.launch { repository.setShowAxisLabels(value) }
    fun setAutoCheckUpdate(value: Boolean) = viewModelScope.launch { repository.setAutoCheckUpdate(value) }
    fun setSampleStep(value: Float) = viewModelScope.launch { repository.setSampleStep(value) }

    fun setShowHelpButton(value: Boolean) = viewModelScope.launch { repository.setShowHelpButton(value) }
    fun setShowKeyHints(value: Boolean) = viewModelScope.launch { repository.setShowKeyHints(value) }
    fun setShowKeyTabs(value: Boolean) = viewModelScope.launch { repository.setShowKeyTabs(value) }

    fun setAnimations(value: Boolean) = viewModelScope.launch { repository.setAnimations(value) }
    fun setEyeCareMode(value: Int) = viewModelScope.launch { repository.setEyeCareMode(value) }
    fun setPoemEnabled(value: Boolean) = viewModelScope.launch { repository.setPoemEnabled(value) }
    fun setEyeCareStart(value: Int) = viewModelScope.launch { repository.setEyeCareStart(value) }
    fun setEyeCareEnd(value: Int) = viewModelScope.launch { repository.setEyeCareEnd(value) }
}
