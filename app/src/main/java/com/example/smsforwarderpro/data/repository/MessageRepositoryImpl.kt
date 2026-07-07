package com.example.smsforwarderpro.data.repository

import com.example.smsforwarderpro.data.local.db.ForwardAttempt
import com.example.smsforwarderpro.data.local.db.ForwardAttemptDao
import com.example.smsforwarderpro.data.local.db.MessageLog
import com.example.smsforwarderpro.data.local.db.MessageLogDao
import com.example.smsforwarderpro.data.local.crypto.MessageCrypto
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageLogDao: MessageLogDao,
    private val forwardAttemptDao: ForwardAttemptDao,
    private val messageCrypto: MessageCrypto
) : MessageRepository {

    override suspend fun saveMessageLog(message: Message) {
        messageLogDao.insertMessageLog(
            MessageLog(
                id = message.id,
                sender = messageCrypto.encrypt(message.sender),
                body = messageCrypto.encrypt(message.body),
                tag = message.tag.name,
                timestamp = message.timestamp,
                simSlot = message.simSlot
            )
        )
    }

    override fun getAllMessageLogsFlow(): Flow<List<Message>> {
        return messageLogDao.getAllMessageLogsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecentMessageLogsFlow(limit: Int): Flow<List<Message>> {
        return messageLogDao.getRecentLogsFlow(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getMessageById(id: String): Message? {
        return messageLogDao.getMessageLogById(id)?.toDomain()
    }

    override suspend fun saveForwardAttempt(attempt: ForwardAttempt) {
        forwardAttemptDao.insertForwardAttempt(attempt)
    }

    override suspend fun updateForwardAttempt(attempt: ForwardAttempt) {
        forwardAttemptDao.updateForwardAttempt(attempt)
    }

    override suspend fun getPendingOrFailedAttempts(): List<ForwardAttempt> {
        return forwardAttemptDao.getPendingOrFailedAttempts()
    }

    override fun getSuccessCountFlow(): Flow<Int> {
        return forwardAttemptDao.getSuccessCountFlow()
    }

    override fun getFailedCountFlow(): Flow<Int> {
        return forwardAttemptDao.getFailedCountFlow()
    }

    override fun getSuccessCountTodayFlow(startOfDay: Long): Flow<Int> {
        return forwardAttemptDao.getSuccessCountTodayFlow(startOfDay)
    }

    override suspend fun deleteOldLogsAndAttempts(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
        messageLogDao.deleteLogsOlderThan(cutoff)
        forwardAttemptDao.deleteAttemptsOlderThan(cutoff)
    }

    override suspend fun clearAllLogs() {
        messageLogDao.clearAll()
        forwardAttemptDao.clearAll()
    }

    private fun MessageLog.toDomain(): Message {
        val domainTag = try {
            MessageTag.valueOf(this.tag)
        } catch (e: Exception) {
            MessageTag.SMS
        }
        return Message(
            id = this.id,
            sender = messageCrypto.decrypt(this.sender),
            body = messageCrypto.decrypt(this.body),
            tag = domainTag,
            timestamp = this.timestamp,
            simSlot = this.simSlot
        )
    }
}
