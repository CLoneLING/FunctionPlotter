package com.yumoo.functionplotter

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 启动失败的现场记录。
 *
 * 目标设备（鸿蒙经卓易通运行等非标准环境）拿不到 logcat，
 * 这里把堆栈写到应用外部目录，用户可以用文件管理器取出来反馈：
 * Android/data/com.yumoo.functionplotter/files/crash-*.txt
 */
object CrashLog {

    private var appContext: Context? = null

    fun install(context: Context) {
        appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            write(error)
            previous?.uncaughtException(thread, error)
        }
    }

    fun write(error: Throwable) {
        val context = appContext ?: return
        runCatching {
            val directory = context.getExternalFilesDir(null) ?: context.filesDir
            directory.mkdirs()
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val file = File(directory, "crash-$stamp.txt")
            file.writeText(buildString {
                appendLine("time=$stamp")
                appendLine("version=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("device=${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("android=${Build.VERSION.RELEASE} / api=${Build.VERSION.SDK_INT}")
                appendLine("stack=")
                appendLine(error.stackTraceToString())
            })
            // 只保留最近 5 份，避免堆积
            directory.listFiles { f -> f.name.startsWith("crash-") }
                ?.sortedByDescending { it.name }
                ?.drop(5)
                ?.forEach { it.delete() }
        }
    }
}