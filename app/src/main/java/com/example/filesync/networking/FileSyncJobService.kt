package com.example.filesync.networking

import android.app.IntentService
import android.app.job.JobParameters
import android.app.job.JobService

class FileSyncJobService : JobService() {

    override fun onStartJob(params: JobParameters?): Boolean {
        FileSyncService.startActionNetConnected(applicationContext)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }
}