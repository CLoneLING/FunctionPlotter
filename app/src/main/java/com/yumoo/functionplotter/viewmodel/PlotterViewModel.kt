package com.yumoo.functionplotter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.yumoo.functionplotter.FunctionPlotterApp
import com.yumoo.functionplotter.data.model.PlotFunction
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class PlotterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as FunctionPlotterApp).settingsRepository
    private val gson = Gson()

    private var nextId = 1L

    private val _functions = MutableStateFlow(defaultFunctions())
    val functions: StateFlow<List<PlotFunction>> = _functions.asStateFlow()

    private val _selectedId = MutableStateFlow(1L)
    val selectedId: StateFlow<Long> = _selectedId.asStateFlow()

    private val _draft = MutableStateFlow("x^2")
    val draft: StateFlow<String> = _draft.asStateFlow()

    val showGrid = repo.showGrid
    val showAxisLabels = repo.showAxisLabels
    val sampleStep = repo.sampleStep

    init {
        restoreAndObserve()
    }

    private fun restoreAndObserve() {
        viewModelScope.launch {
            // 1. 读取本地快照
            val savedJson = repo.functionsJson.first()
            val savedId = repo.selectedFunctionId.first()
            val savedDraft = repo.draftExpression.first()

            // 2. 恢复内存状态
            val list = parseSaved(savedJson) ?: defaultFunctions()
            _functions.value = list
            _selectedId.value = list.firstOrNull { it.id == savedId }?.id ?: list.first().id
            _draft.value = savedDraft.ifBlank {
                list.firstOrNull { it.id == _selectedId.value }?.expression ?: ""
            }
            nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L

            // 3. 函数列表与选中项：低频变化，立即写
            launch {
                combine(_functions, _selectedId) { f, id -> f to id }
                    .collect { (f, id) ->
                        repo.setFunctionsJson(gson.toJson(f))
                        repo.setSelectedFunctionId(id)
                    }
            }

            // 4. 草稿：高频变化，防抖 300ms 后写
            launch {
                _draft.debounce(300).collect {
                    repo.setDraftExpression(it)
                }
            }
        }
    }

    private fun parseSaved(json: String): List<PlotFunction>? {
        if (json.isBlank()) return null
        return try {
            val arr = gson.fromJson(json, Array<PlotFunction>::class.java) ?: return null
            arr.toList().filterNotNull().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    fun onDraftChange(v: String) { _draft.value = v }

    fun appendToDraft(v: String) { _draft.value += v }

    fun backspace() { _draft.value = _draft.value.dropLast(1) }

    fun clearDraft() { _draft.value = "" }

    fun applyDraft() {
        val text = _draft.value.trim()
        if (text.isBlank()) return
        val id = _selectedId.value
        _functions.value = _functions.value.map {
            if (it.id == id) it.copy(expression = text) else it
        }
    }

    fun addFunction() {
        if (_functions.value.size >= MAX_FUNCTIONS) return
        val id = nextId++
        val color = PALETTE[_functions.value.size % PALETTE.size]
        _functions.value = _functions.value + PlotFunction(id, "", color)
        _selectedId.value = id
        _draft.value = ""
    }

    fun selectFunction(id: Long) {
        _selectedId.value = id
        _draft.value = _functions.value.firstOrNull { it.id == id }?.expression ?: ""
    }

    fun removeFunction(id: Long) {
        if (_functions.value.size <= 1) return
        _functions.value = _functions.value.filterNot { it.id == id }
        if (_selectedId.value == id) {
            val first = _functions.value.firstOrNull() ?: return
            _selectedId.value = first.id
            _draft.value = first.expression
        }
    }

    fun toggleVisible(id: Long) {
        _functions.value = _functions.value.map {
            if (it.id == id) it.copy(visible = !it.visible) else it
        }
    }

    private fun defaultFunctions(): List<PlotFunction> =
        listOf(PlotFunction(id = 1L, expression = "x^2", colorArgb = PALETTE[0]))

    companion object {
        const val MAX_FUNCTIONS = 5
        val PALETTE = longArrayOf(
            0xFF3A7BD5,
            0xFFE05A5A,
            0xFF35A76B,
            0xFFB36BDF,
            0xFFE39B2B
        )
    }
}