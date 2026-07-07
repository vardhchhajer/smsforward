package com.example.smsforwarderpro.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_logs")
data class MessageLog(
    @PrimaryKey val id: String,
    val sender: String,
    val body: String,
    val tag: String, // SMS | MMS | OTP | PROMOTIONAL | TRANSACTIONAL
    val timestamp: Long,
    val simSlot: Int
)

@Entity(tableName = "forward_attempts")
data class ForwardAttempt(
    @PrimaryKey val id: String,
    val messageId: String,
    val destinationId: String,
    val destinationType: String, // GMAIL | TELEGRAM | WHATSAPP | WEBHOOK | SMS
    val status: String, // PENDING | SUCCESS | FAILED
    val attempts: Int,
    val lastAttemptAt: Long,
    val errorMessage: String?
)

@Entity(tableName = "forwarding_rules")
data class ForwardingRule(
    @PrimaryKey val id: String,
    val name: String,
    val conditionsJson: String, // serialized rule conditions JSON
    val destinationIdsJson: String, // serialized target destination IDs JSON
    val priority: Int,
    val isActive: Boolean,
    val breakOnMatch: Boolean
)
