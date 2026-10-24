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

    private val repository = (app as FunctionPlotterApp).settingsRepository
    private val gson = Gson()

    private var nextId = 1L

    private val _functions = MutableStateFlow(defaultFunctions())
    val functions: StateFlow<List<PlotFunction>> = _functions.asStateFlow()

    private val _selectedId = MutableStateFlow(1L)
    val selectedId: StateFlow<Long> = _selectedId.asStateFlow()

    private val _draft = MutableStateFlow(DEFAULT_EXPRESSION)
    val draft: StateFlow<String> = _draft.asStateFlow()

    val showGrid = repository.showGrid
    val showAxisLabels = repository.showAxisLabels
    val sampleStep = repository.sampleStep
    val showHelpButton = repository.showHelpButton
    val showKeyHints = repository.showKeyHints
    val showKeyTabs = repository.showKeyTabs

    init {
        restoreAndObserve()
    }

    private fun restoreAndObserve() {
        viewModelScope.launch {
            // 1. 读取本地快照
            val savedJson = repository.functionsJson.first()
            val savedId = repository.selectedFunctionId.first()
            val savedDraft = repository.draftExpression.first()

            // 2. 恢复内存状态
            val restored = parseSaved(savedJson) ?: defaultFunctions()
            _functions.value = restored
            val restoredSelection = restored.firstOrNull { it.id == savedId } ?: restored.first()
            _selectedId.value = restoredSelection.id
            _draft.value = savedDraft.ifBlank { restoredSelection.expression }
            nextId = (restored.maxOfOrNull { it.id } ?: 0L) + 1L

            // 3. 函数列表与选中项：低频变化，立即写
            launch {
                combine(_functions, _selectedId) { list, id -> list to id }
                    .collect { (list, id) ->
                        repository.setFunctionsJson(gson.toJson(list))
                        repository.setSelectedFunctionId(id)
                    }
            }

            // 4. 草稿：高频变化，防抖 300ms 后写
            launch {
                _draft.debounce(300).collect {
                    repository.setDraftExpression(it)
                }
            }
        }
    }

    private fun parseSaved(json: String): List<PlotFunction>? {
        if (json.isBlank()) return null
        return try {
            val array = gson.fromJson(json, Array<PlotFunction>::class.java) ?: return null
            array.toList().filterNotNull().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    fun onDraftChange(value: String) {
        _draft.value = value
    }

    fun appendToDraft(value: String) {
        _draft.value += value
    }

    fun replaceDraft(value: String) {
        _draft.value = value
    }

    fun backspace() {
        _draft.value = _draft.value.dropLast(1)
    }

    fun clearDraft() {
        _draft.value = ""
    }

    /**
     * 应用草稿。允许空表达式：这样才能把某条曲线"清空"，
     * 旧实现遇到空白直接 return，用户只能删掉整条函数。
     */
    fun applyDraft() {
        val text = _draft.value.trim()
        val id = _selectedId.value
        _functions.value = _functions.value.map {
            if (it.id == id) it.copy(expression = text) else it
        }
    }

    fun addFunction() {
        if (_functions.value.size >= MAX_FUNCTIONS) return
        val id = nextId++
        val used = _functions.value.map { it.colorArgb }.toSet()
        val color = PALETTE.firstOrNull { it !in used } ?: PALETTE[_functions.value.size % PALETTE.size]
        _functions.value = _functions.value + PlotFunction(id = id, expression = "", colorArgb = color)
        _selectedId.value = id
        _draft.value = ""
    }

    /**
     * 批量导入：先填空槽，再追加到上限。返回实际导入条数。
     */
    fun importExpressions(expressions: List<String>): Int {
        val queue = ArrayDeque(expressions.map { it.trim() }.filter { it.isNotEmpty() })
        if (queue.isEmpty()) return 0

        val current = _functions.value.toMutableList()
        val seen = current.map { it.expression.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        var added = 0

        for (i in current.indices) {
            if (queue.isEmpty()) break
            if (current[i].expression.isBlank()) {
                val next = queue.removeFirst()
                if (seen.add(next)) {
                    current[i] = current[i].copy(expression = next)
                    added++
                }
            }
        }
        while (queue.isNotEmpty() && current.size < MAX_FUNCTIONS) {
            val next = queue.removeFirst()
            if (!seen.add(next)) continue
            val used = current.map { it.colorArgb }.toSet()
            val color = PALETTE.firstOrNull { it !in used } ?: PALETTE[current.size % PALETTE.size]
            current += PlotFunction(id = nextId++, expression = next, colorArgb = color)
            added++
        }

        _functions.value = current
        current.firstOrNull { it.expression.isNotBlank() }?.let {
            _selectedId.value = it.id
            _draft.value = it.expression
        }
        return added
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
        listOf(PlotFunction(id = 1L, expression = DEFAULT_EXPRESSION, colorArgb = PALETTE[0]))

    companion object {
        /** 首次启动时的默认函数。 */
        private const val DEFAULT_EXPRESSION = "x^2"

        const val MAX_FUNCTIONS = 10
        val PALETTE = longArrayOf(
            0xFF3A7BD5,
            0xFFE05A5A,
            0xFF35A76B,
            0xFFB36BDF,
            0xFFE39B2B
        )
    }
}
