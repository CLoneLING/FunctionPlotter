package com.yumoo.functionplotter

import android.app.Application
import com.yumoo.functionplotter.data.SettingsRepository
import com.yumoo.functionplotter.data.dataStore

class FunctionPlotterApp : Application() {

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(applicationContext.dataStore)
    }

    override fun onCreate() {
        super.onCreate()
        // 非标准 Android 环境（鸿蒙卓易通等）拿不到 logcat，
        // 崩溃时把堆栈落盘，方便用户反馈
        runCatching { CrashLog.install(this) }
    }
}