package com.guotu.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueueFullScan() {
        // Regular OneTimeWork — expedited requires ForegroundInfo and was failing silently.
        val request = OneTimeWorkRequestBuilder<ScanWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ScanWorker.UNIQUE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
