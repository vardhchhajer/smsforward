package com.example.smsforwarderpro.domain.repository

import com.example.smsforwarderpro.data.local.db.ForwardAttempt
import com.example.smsforwarderpro.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun saveMessageLog(message: Message)
    fun getAllMessageLogsFlow(): Flow<List<Message>>
    fun getRecentMessageLogsFlow(limit: Int): Flow<List<Message>>
    suspend fun getMessageById(id: String): Message?
    suspend fun saveForwardAttempt(attempt: ForwardAttempt)
    suspend fun updateForwardAttempt(attempt: ForwardAttempt)
    suspend fun getPendingOrFailedAttempts(): List<ForwardAttempt>
    fun getSuccessCountFlow(): Flow<Int>
    fun getFailedCountFlow(): Flow<Int>
    fun getSuccessCountTodayFlow(startOfDay: Long): Flow<Int>
    suspend fun deleteOldLogsAndAttempts(retentionDays: Int)
    suspend fun clearAllLogs()
}
