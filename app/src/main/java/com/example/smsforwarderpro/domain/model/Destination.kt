package com.example.smsforwarderpro.domain.model

enum class DestinationType {
    GMAIL, TELEGRAM, WHATSAPP, WEBHOOK, SMS
}

data class Destination(
    val id: String,
    val type: DestinationType,
    val name: String,
    val isEnabled: Boolean
)
