package com.yumoo.functionplotter.data.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 下载进度的常驻通知。
 * 权限被拒绝（Android 13+ 未授予 POST_NOTIFICATIONS）时静默降级，
 * 应用内的进度条仍然可用。
 */
class DownloadNotifications(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannel() {
        // minSdk 26，通知渠道 API 一定可用
        val channel = NotificationChannel(
            CHANNEL_ID,
            "应用更新",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "显示更新包的下载进度"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    fun progress(percent: Int) {
        ensureChannel()
        val builder = baseBuilder()
            .setContentTitle("正在下载更新")
            .setContentText("$percent%")
            .setProgress(100, percent.coerceIn(0, 100), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        post(builder)
    }

    fun complete() {
        ensureChannel()
        post(
            baseBuilder()
                .setContentTitle("更新已下载完成")
                .setContentText("点击应用内「安装」完成更新")
                .setOngoing(false)
                .setAutoCancel(true)
        )
    }

    fun failed() {
        ensureChannel()
        post(
            baseBuilder()
                .setContentTitle("更新下载失败")
                .setContentText("请检查网络后重试")
                .setOngoing(false)
                .setAutoCancel(true)
        )
    }

    fun clear() {
        try {
            manager.cancel(NOTIFICATION_ID)
        } catch (e: SecurityException) {
            // 没有通知权限时忽略
        }
    }

    private fun baseBuilder(): NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)

    private fun post(builder: NotificationCompat.Builder) {
        try {
            manager.notify(NOTIFICATION_ID, builder.build())
        } catch (e: SecurityException) {
            // 未授予通知权限，静默降级
        }
    }

    companion object {
        private const val CHANNEL_ID = "update_download"
        private const val NOTIFICATION_ID = 1001
    }
}
