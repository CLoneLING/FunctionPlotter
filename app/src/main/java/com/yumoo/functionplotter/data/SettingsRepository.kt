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

    /** 界面模块开关：默认全部显示，可在设置里隐藏。 */
    val showHelpButton = dataStore.data.map { it[KEY_SHOW_HELP] ?: true }
    val showKeyHints = dataStore.data.map { it[KEY_SHOW_KEY_HINTS] ?: false }
    val showKeyTabs = dataStore.data.map { it[KEY_SHOW_KEY_TABS] ?: false }

    /** 界面动效开关（默认开启）。 */
    val animations = dataStore.data.map { it[KEY_ANIMATIONS] ?: true }

    /** 护眼模式：0 关闭 / 1 定时（20:00–次日 7:00）/ 2 常开，默认定时。 */
    val eyeCareMode = dataStore.data.map { it[KEY_EYE_CARE] ?: 1 }

    val functionsJson = dataStore.data.map { it[KEY_FUNCTIONS_JSON] ?: "" }
    val selectedFunctionId = dataStore.data.map { it[KEY_SELECTED_ID] ?: -1L }
    val draftExpression = dataStore.data.map { it[KEY_DRAFT] ?: "" }

    /** 上次成功检查更新的时间（毫秒），用于"启动时自动检查"的节流。 */
    val lastUpdateCheck = dataStore.data.map { it[KEY_LAST_UPDATE_CHECK] ?: 0L }

    /** 正在进行的下载任务 id，进程被杀后重新进入应用可以继续跟踪进度。 */
    val pendingDownloadId = dataStore.data.map { it[KEY_PENDING_DOWNLOAD] ?: -1L }

    suspend fun setThemeMode(v: Int) = dataStore.edit { it[KEY_THEME_MODE] = v }
    suspend fun setShowGrid(v: Boolean) = dataStore.edit { it[KEY_SHOW_GRID] = v }
    suspend fun setShowAxisLabels(v: Boolean) = dataStore.edit { it[KEY_SHOW_AXIS_LABELS] = v }
    suspend fun setAutoCheckUpdate(v: Boolean) = dataStore.edit { it[KEY_AUTO_CHECK_UPDATE] = v }
    suspend fun setSampleStep(v: Float) = dataStore.edit { it[KEY_SAMPLE_STEP] = v }

    suspend fun setShowHelpButton(v: Boolean) = dataStore.edit { it[KEY_SHOW_HELP] = v }
    suspend fun setShowKeyHints(v: Boolean) = dataStore.edit { it[KEY_SHOW_KEY_HINTS] = v }
    suspend fun setShowKeyTabs(v: Boolean) = dataStore.edit { it[KEY_SHOW_KEY_TABS] = v }

    suspend fun setAnimations(v: Boolean) = dataStore.edit { it[KEY_ANIMATIONS] = v }
    suspend fun setEyeCareMode(v: Int) = dataStore.edit { it[KEY_EYE_CARE] = v }

    suspend fun setFunctionsJson(v: String) = dataStore.edit { it[KEY_FUNCTIONS_JSON] = v }
    suspend fun setSelectedFunctionId(v: Long) = dataStore.edit { it[KEY_SELECTED_ID] = v }
    suspend fun setDraftExpression(v: String) = dataStore.edit { it[KEY_DRAFT] = v }

    suspend fun setLastUpdateCheck(v: Long) = dataStore.edit { it[KEY_LAST_UPDATE_CHECK] = v }
    suspend fun setPendingDownloadId(v: Long) = dataStore.edit { it[KEY_PENDING_DOWNLOAD] = v }

    companion object {
        private val KEY_THEME_MODE = intPreferencesKey("theme_mode")
        private val KEY_SHOW_GRID = booleanPreferencesKey("show_grid")
        private val KEY_SHOW_AXIS_LABELS = booleanPreferencesKey("show_axis_labels")
        private val KEY_AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        private val KEY_SAMPLE_STEP = floatPreferencesKey("sample_step")
        private val KEY_SHOW_HELP = booleanPreferencesKey("show_help_button")
        private val KEY_SHOW_KEY_HINTS = booleanPreferencesKey("show_key_hints")
        private val KEY_SHOW_KEY_TABS = booleanPreferencesKey("show_key_tabs")
        private val KEY_ANIMATIONS = booleanPreferencesKey("animations")
        private val KEY_EYE_CARE = intPreferencesKey("eye_care_mode")
        private val KEY_FUNCTIONS_JSON = stringPreferencesKey("functions_json")
        private val KEY_SELECTED_ID = longPreferencesKey("selected_function_id")
        private val KEY_DRAFT = stringPreferencesKey("draft_expression")
        private val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
        private val KEY_PENDING_DOWNLOAD = longPreferencesKey("pending_download_id")
    }
}
