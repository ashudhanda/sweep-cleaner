package com.sweep.cleaner

import android.app.Application
import com.sweep.cleaner.work.WorkScheduler

class SweepApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize background maintenance with unique jobs
        try {
            WorkScheduler.scheduleMaintenanceTasks(this)
        } catch (_: Exception) {}
    }
}
