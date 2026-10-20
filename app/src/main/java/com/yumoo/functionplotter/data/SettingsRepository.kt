package com.yumoo.functionplotter.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val themeMode = dataStore.data.map { it[KEY_THEME_MODE] ?: 0 }
    val showGrid = dataStore.data.map { it[KEY_SHOW_GRID] ?: true }
    val showAxisLabels = dataStore.data.map { it[KEY_SHOW_AXIS_LABELS] ?: true }
    val autoCheckUpdate = dataStore.data.map { it[KEY_AUTO_CHECK_UPDATE] ?: true }
    val sampleStep = dataStore.data.map { it[KEY_SAMPLE_STEP] ?: 2f }

    val functionsJson = dataStore.data.map { it[KEY_FUNCTIONS_JSON] ?: "" }
    val selectedFunctionId = dataStore.data.map { it[KEY_SELECTED_ID] ?: -1L }
    val draftExpression = dataStore.data.map { it[KEY_DRAFT] ?: "" }

    suspend fun setThemeMode(v: Int) = dataStore.edit { it[KEY_THEME_MODE] = v }
    suspend fun setShowGrid(v: Boolean) = dataStore.edit { it[KEY_SHOW_GRID] = v }
    suspend fun setShowAxisLabels(v: Boolean) = dataStore.edit { it[KEY_SHOW_AXIS_LABELS] = v }
    suspend fun setAutoCheckUpdate(v: Boolean) = dataStore.edit { it[KEY_AUTO_CHECK_UPDATE] = v }
    suspend fun setSampleStep(v: Float) = dataStore.edit { it[KEY_SAMPLE_STEP] = v }

    suspend fun setFunctionsJson(v: String) = dataStore.edit { it[KEY_FUNCTIONS_JSON] = v }
    suspend fun setSelectedFunctionId(v: Long) = dataStore.edit { it[KEY_SELECTED_ID] = v }
    suspend fun setDraftExpression(v: String) = dataStore.edit { it[KEY_DRAFT] = v }

    companion object {
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        private val KEY_SHOW_GRID = booleanPreferencesKey("show_grid")
        private val KEY_SHOW_AXIS_LABELS = booleanPreferencesKey("show_axis_labels")
        private val KEY_AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        private val KEY_SAMPLE_STEP = floatPreferencesKey("sample_step")
        private val KEY_FUNCTIONS_JSON = stringPreferencesKey("functions_json")
        private val KEY_SELECTED_ID = longPreferencesKey("selected_function_id")
        private val KEY_DRAFT = stringPreferencesKey("draft_expression")
    }
}