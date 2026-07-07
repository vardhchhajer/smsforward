package com.example.smsforwarderpro.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object ForwardingWorkScheduler {
    private const val FLUSH_WORK_NAME = "sms_forwarding_failed_attempt_flush"
    private const val RETENTION_WORK_NAME = "sms_forwarding_retention_prune"

    fun scheduleForwardingFlush(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DatabaseFlushWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            FLUSH_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelForwardingFlush(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(FLUSH_WORK_NAME)
    }

    fun scheduleRetentionPruning(context: Context) {
        val request = PeriodicWorkRequestBuilder<DataRetentionWorker>(1, TimeUnit.DAYS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RETENTION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
