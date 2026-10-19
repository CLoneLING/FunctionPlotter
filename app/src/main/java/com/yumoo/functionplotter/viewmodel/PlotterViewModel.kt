package com.yumoo.functionplotter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.yumoo.functionplotter.FunctionPlotterApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PlotterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as FunctionPlotterApp).settingsRepository

    private val _expression = MutableStateFlow("x^2")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _plotted = MutableStateFlow("x^2")
    val plotted: StateFlow<String> = _plotted.asStateFlow()

    val showGrid = repo.showGrid
    val showAxisLabels = repo.showAxisLabels
    val sampleStep = repo.sampleStep

    fun onExpressionChange(value: String) { _expression.value = value }

    fun appendToExpression(value: String) { _expression.value += value }

    fun backspace() {
        _expression.value = _expression.value.dropLast(1)
    }

    fun clear() { _expression.value = "" }

    fun plot() { _plotted.value = _expression.value }
}