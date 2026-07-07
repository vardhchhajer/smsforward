package com.example.smsforwarderpro.domain.usecase

import com.example.smsforwarderpro.data.local.db.ForwardAttempt
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.domain.repository.MessageRepository
import com.example.smsforwarderpro.domain.repository.RuleRepository
import java.util.UUID
import javax.inject.Inject

class ProcessIncomingMessageUseCase @Inject constructor(
    private val messageRepository: MessageRepository,
    private val ruleRepository: RuleRepository,
    private val ruleEvaluator: RuleEvaluator,
    private val prefs: EncryptedPreferencesManager
) {
    suspend fun execute(message: Message): List<ForwardAttempt> {
        // 1. Save log in DB
        messageRepository.saveMessageLog(message)

        val targetDestinations = mutableSetOf<String>()

        // 2. Default rule: Forward all OTP messages to all enabled destinations
        if (message.tag == MessageTag.OTP) {
            if (prefs.isGmailEnabled) targetDestinations.add("gmail")
            if (prefs.isTelegramEnabled) targetDestinations.add("telegram")
            if (prefs.isWhatsappEnabled) targetDestinations.add("whatsapp")
            if (prefs.isWebhookEnabled) targetDestinations.add("webhook")
            if (prefs.isSmsEnabled) targetDestinations.add("sms")
        }

        // 3. Evaluate user rules in priority order
        val rules = ruleRepository.getAllRules()
        for (rule in rules) {
            if (!rule.isActive) continue
            if (ruleEvaluator.evaluate(rule, message)) {
                targetDestinations.addAll(rule.destinationIds)
                if (rule.breakOnMatch) {
                    break
                }
            }
        }

        if (targetDestinations.isEmpty()) {
            return emptyList()
        }

        // 4. Create and save PENDING ForwardAttempt records in DB
        val attempts = targetDestinations.map { destinationId ->
            ForwardAttempt(
                id = UUID.randomUUID().toString(),
                messageId = message.id,
                destinationId = destinationId,
                destinationType = getDestinationTypeFromId(destinationId),
                status = "PENDING",
                attempts = 0,
                lastAttemptAt = 0L,
                errorMessage = null
            )
        }

        attempts.forEach { attempt ->
            messageRepository.saveForwardAttempt(attempt)
        }

        return attempts
    }

    private fun getDestinationTypeFromId(id: String): String {
        return when {
            id.equals("gmail", ignoreCase = true) -> "GMAIL"
            id.equals("telegram", ignoreCase = true) -> "TELEGRAM"
            id.equals("whatsapp", ignoreCase = true) -> "WHATSAPP"
            id.equals("webhook", ignoreCase = true) -> "WEBHOOK"
            id.equals("sms", ignoreCase = true) -> "SMS"
            else -> "WEBHOOK"
        }
    }
}
