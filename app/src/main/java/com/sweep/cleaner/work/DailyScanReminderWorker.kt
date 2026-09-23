package com.sweep.cleaner.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sweep.cleaner.MainActivity
import com.sweep.cleaner.R
import com.sweep.cleaner.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.flow.first

class DailyScanReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)
        val notificationsEnabled = prefs.notificationsEnabled.first()
        if (!notificationsEnabled) {
            return Result.success()
        }

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sweep_maintenance_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sweep Maintenance",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily storage review reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for a quick storage review")
            .setContentText("Sweep can find similar photos, old downloads, and large videos to free space.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "sweep_daily_scan_reminder"
    }
}
