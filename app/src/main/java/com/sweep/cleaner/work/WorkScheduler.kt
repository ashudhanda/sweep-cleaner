package com.sweep.cleaner.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun scheduleMaintenanceTasks(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Daily 30-day trash purge
        val purgeRequest = PeriodicWorkRequestBuilder<DailyTrashPurgeWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            DailyTrashPurgeWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            purgeRequest
        )

        // Daily storage reminder
        val reminderRequest = PeriodicWorkRequestBuilder<DailyScanReminderWorker>(
            24, TimeUnit.HOURS,
            2, TimeUnit.HOURS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            DailyScanReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderRequest
        )
    }
}
