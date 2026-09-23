package com.sweep.cleaner.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sweep.cleaner.data.repository.StorageRepository

class DailyTrashPurgeWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = StorageRepository(applicationContext)
            repository.purgeExpiredTrashItems()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "sweep_daily_trash_purge"
    }
}
