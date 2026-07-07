package com.example.smsforwarderpro.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsforwarderpro.domain.repository.MessageRepository
import com.example.smsforwarderpro.domain.usecase.ForwardMessageUseCase
import com.example.smsforwarderpro.domain.usecase.ForwardResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DatabaseFlushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val messageRepository: MessageRepository,
    private val forwardMessageUseCase: ForwardMessageUseCase
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DatabaseFlushWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting periodic offline/failed logs flush")
        
        try {
            val pendingAttempts = messageRepository.getPendingOrFailedAttempts()
            if (pendingAttempts.isEmpty()) {
                Log.d(TAG, "No pending/failed attempts to flush")
                return Result.success()
            }

            Log.d(TAG, "Found ${pendingAttempts.size} attempts to retry")
            
            pendingAttempts.forEach { attempt ->
                val message = messageRepository.getMessageById(attempt.messageId)
                if (message != null) {
                    // Update attempts count
                    val currentCount = attempt.attempts + 1
                    var updatedAttempt = attempt.copy(
                        attempts = currentCount,
                        lastAttemptAt = System.currentTimeMillis()
                    )
                    messageRepository.saveForwardAttempt(updatedAttempt)

                    Log.d(TAG, "Retrying attempt ${attempt.id} to destination ${attempt.destinationId}")
                    when (val result = forwardMessageUseCase.execute(updatedAttempt, message)) {
                        is ForwardResult.Success -> {
                            updatedAttempt = updatedAttempt.copy(status = "SUCCESS", errorMessage = null)
                            messageRepository.updateForwardAttempt(updatedAttempt)
                            Log.d(TAG, "Successfully flushed attempt ${attempt.id}")
                        }
                        is ForwardResult.Failure -> {
                            updatedAttempt = updatedAttempt.copy(status = "FAILED", errorMessage = result.error)
                            messageRepository.updateForwardAttempt(updatedAttempt)
                            Log.w(TAG, "Flush attempt failed for ${attempt.id}: ${result.error}")
                        }
                    }
                } else {
                    Log.w(TAG, "No message record found for attempt ${attempt.id}, marking failed")
                    val updatedAttempt = attempt.copy(
                        status = "FAILED",
                        errorMessage = "Associated message record missing in local database"
                    )
                    messageRepository.updateForwardAttempt(updatedAttempt)
                }
            }
            
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error executing DatabaseFlushWorker", e)
            return Result.retry()
        }
    }
}
