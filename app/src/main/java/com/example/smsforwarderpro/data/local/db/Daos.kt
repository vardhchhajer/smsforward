package com.example.smsforwarderpro.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageLog(log: MessageLog)

    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC")
    fun getAllMessageLogsFlow(): Flow<List<MessageLog>>

    @Query("SELECT * FROM message_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsFlow(limit: Int): Flow<List<MessageLog>>

    @Query("SELECT * FROM message_logs WHERE id = :id")
    suspend fun getMessageLogById(id: String): MessageLog?

    @Query("DELETE FROM message_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteLogsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM message_logs")
    suspend fun clearAll()
}

@Dao
interface ForwardAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardAttempt(attempt: ForwardAttempt)

    @Update
    suspend fun updateForwardAttempt(attempt: ForwardAttempt)

    @Query("SELECT * FROM forward_attempts WHERE messageId = :messageId")
    suspend fun getAttemptsForMessage(messageId: String): List<ForwardAttempt>

    @Query("SELECT * FROM forward_attempts WHERE (status = 'PENDING' OR status = 'FAILED') AND attempts < :maxAttempts")
    suspend fun getPendingOrFailedAttempts(maxAttempts: Int = 3): List<ForwardAttempt>

    @Query("SELECT COUNT(*) FROM forward_attempts WHERE status = 'SUCCESS'")
    fun getSuccessCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM forward_attempts WHERE status = 'FAILED'")
    fun getFailedCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM forward_attempts WHERE status = 'SUCCESS' AND lastAttemptAt >= :startOfDay")
    fun getSuccessCountTodayFlow(startOfDay: Long): Flow<Int>

    @Query("DELETE FROM forward_attempts WHERE lastAttemptAt < :cutoffTimestamp")
    suspend fun deleteAttemptsOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM forward_attempts")
    suspend fun clearAll()
}

@Dao
interface ForwardingRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ForwardingRule)

    @Update
    suspend fun updateRule(rule: ForwardingRule)

    @Delete
    suspend fun deleteRule(rule: ForwardingRule)

    @Query("SELECT * FROM forwarding_rules ORDER BY priority ASC")
    fun getAllRulesFlow(): Flow<List<ForwardingRule>>

    @Query("SELECT * FROM forwarding_rules ORDER BY priority ASC")
    suspend fun getAllRules(): List<ForwardingRule>

    @Query("SELECT MAX(priority) FROM forwarding_rules")
    suspend fun getMaxPriority(): Int?

    @Query("UPDATE forwarding_rules SET priority = :priority WHERE id = :id")
    suspend fun updateRulePriority(id: String, priority: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<ForwardingRule>)
}
