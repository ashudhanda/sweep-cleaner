package com.sweep.cleaner

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.sweep.cleaner.work.WorkScheduler

class SweepApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Initialize background maintenance with unique jobs
        try {
            WorkScheduler.scheduleMaintenanceTasks(this)
        } catch (_: Exception) {}
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}

