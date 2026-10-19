package com.yumoo.functionplotter

import android.app.Application
import com.yumoo.functionplotter.data.SettingsRepository
import com.yumoo.functionplotter.data.dataStore

class FunctionPlotterApp : Application() {
    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext.dataStore)
    }
}