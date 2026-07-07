package com.example.smsforwarderpro.domain.model

enum class MessageTag {
    SMS, MMS, OTP, PROMOTIONAL, TRANSACTIONAL
}

data class Message(
    val id: String,
    val sender: String,
    val body: String,
    val tag: MessageTag,
    val timestamp: Long,
    val simSlot: Int
)
