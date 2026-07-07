package com.example.smsforwarderpro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.repository.MessageRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DataRetentionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val prefs: EncryptedPreferencesManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DataRetentionWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            messageRepository.deleteOldLogsAndAttempts(prefs.retentionDays)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Retention cleanup failed", e)
            Result.retry()
        }
    }
}
