package com.example.smsforwarderpro.domain.usecase

import android.os.Build
import com.example.smsforwarderpro.data.local.db.ForwardAttempt
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.data.network.GmailApi
import com.example.smsforwarderpro.data.network.GmailOAuthHelper
import com.example.smsforwarderpro.data.network.TelegramApi
import com.example.smsforwarderpro.data.network.WhatsAppApi
import com.example.smsforwarderpro.data.network.WebhookDispatcher
import com.example.smsforwarderpro.data.network.WebhookResult
import com.example.smsforwarderpro.data.sms.SmsForwardManager
import com.example.smsforwarderpro.data.sms.SmsResult
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.google.gson.JsonObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ForwardMessageUseCase @Inject constructor(
    private val telegramApi: TelegramApi,
    private val whatsAppApi: WhatsAppApi,
    private val gmailApi: GmailApi,
    private val gmailOAuthHelper: GmailOAuthHelper,
    private val webhookDispatcher: WebhookDispatcher,
    private val smsForwardManager: SmsForwardManager,
    private val prefs: EncryptedPreferencesManager
) {
    suspend fun execute(attempt: ForwardAttempt, message: Message): ForwardResult {
        return when (attempt.destinationType) {
            "GMAIL" -> forwardToGmail(message)
            "TELEGRAM" -> forwardToTelegram(message)
            "WHATSAPP" -> forwardToWhatsApp(message)
            "WEBHOOK" -> forwardToWebhook(message)
            "SMS" -> forwardToSms(message)
            else -> ForwardResult.Failure("Unknown destination type: ${attempt.destinationType}")
        }
    }

    private suspend fun forwardToGmail(message: Message): ForwardResult {
        val recipient = prefs.gmailRecipient ?: return ForwardResult.Failure("Gmail recipient not configured")
        
        // Since we removed SMTP, we solely rely on OAuth 2.0.

        // Mock authorization switch for local debugging (fallback)
        if (prefs.gmailRefreshToken == "MOCK_TOKEN") {
            // Simulated Success
            return ForwardResult.Success
        }

        val accessToken = gmailOAuthHelper.getValidAccessToken()
            ?: return ForwardResult.Failure("Failed to obtain a valid Gmail OAuth access token")

        try {
            // Find user's Gmail sender address (defaults to me)
            val oauthSender = "me"
            val encodedMime = gmailOAuthHelper.createEncodedMimeMessage(message, oauthSender, recipient)

            val jsonBody = JsonObject().apply {
                addProperty("raw", encodedMime)
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = gmailApi.sendEmail("Bearer $accessToken", requestBody)

            return if (response.isSuccessful) {
                ForwardResult.Success
            } else {
                ForwardResult.Failure("Gmail API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            return ForwardResult.Failure(e.localizedMessage ?: "Gmail forwarding crashed")
        }
    }



    private suspend fun forwardToTelegram(message: Message): ForwardResult {
        val token = prefs.telegramBotToken ?: return ForwardResult.Failure("Telegram bot token not configured")
        val chatId = prefs.telegramChatId ?: return ForwardResult.Failure("Telegram Chat ID not configured")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date(message.timestamp))

        // Monospace OTP parsing
        val otpCode = if (message.tag == MessageTag.OTP) {
            val regex = Regex("\\b\\d{6,8}\\b")
            regex.find(message.body)?.value
        } else null

        val escapedBody = escapeHtml(message.body)
        val formattedBody = if (otpCode != null) {
            escapedBody.replace(escapeHtml(otpCode), "<code>${escapeHtml(otpCode)}</code>")
        } else {
            escapedBody
        }

        val htmlText = """
            <b>[SMS Intercepted]</b>
            <b>From:</b> <code>${escapeHtml(message.sender)}</code>
            <b>SIM Slot:</b> Slot ${message.simSlot + 1}
            <b>Time:</b> <i>$formattedTime</i>
            <b>Category:</b> <u>${escapeHtml(message.tag.name)}</u>
            
            $formattedBody
        """.trimIndent()

        try {
            val response = telegramApi.sendMessage(
                token = token,
                chatId = chatId,
                text = htmlText
            )
            return if (response.isSuccessful) {
                ForwardResult.Success
            } else {
                ForwardResult.Failure("Telegram Bot API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            return ForwardResult.Failure(e.localizedMessage ?: "Telegram forwarding failed")
        }
    }

    private suspend fun forwardToWhatsApp(message: Message): ForwardResult {
        val phoneId = prefs.whatsappPhoneNumberId ?: return ForwardResult.Failure("WhatsApp Phone Number ID not configured")
        val token = prefs.whatsappAccessToken ?: return ForwardResult.Failure("WhatsApp Access Token not configured")
        val recipient = prefs.whatsappRecipientPhone ?: return ForwardResult.Failure("WhatsApp Recipient Phone not configured")

        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date(message.timestamp))

        val textBody = """
            *SMS Intercepted (Slot ${message.simSlot + 1})*
            *From:* ${message.sender}
            *Time:* $formattedTime
            *Tag:* ${message.tag.name}
            *Device:* $deviceName
            
            ${message.body}
        """.trimIndent()

        try {
            // Build meta JSON structure
            val jsonBody = JsonObject().apply {
                addProperty("messaging_product", "whatsapp")
                addProperty("recipient_type", "individual")
                addProperty("to", recipient)
                addProperty("type", "text")
                add("text", JsonObject().apply {
                    addProperty("preview_url", false)
                    addProperty("body", textBody)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val response = whatsAppApi.sendMessage(
                phoneNumberId = phoneId,
                authHeader = "Bearer $token",
                body = requestBody
            )

            return if (response.isSuccessful) {
                ForwardResult.Success
            } else {
                ForwardResult.Failure("WhatsApp Cloud API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            return ForwardResult.Failure(e.localizedMessage ?: "WhatsApp forwarding crashed")
        }
    }

    private suspend fun forwardToWebhook(message: Message): ForwardResult {
        return when (val result = webhookDispatcher.dispatch(message)) {
            is WebhookResult.Success -> ForwardResult.Success
            is WebhookResult.Failure -> ForwardResult.Failure(result.error)
        }
    }

    private suspend fun forwardToSms(message: Message): ForwardResult {
        return when (val result = smsForwardManager.forward(message)) {
            is SmsResult.Success -> ForwardResult.Success
            is SmsResult.Failure -> ForwardResult.Failure(result.error)
        }
    }

    private fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

sealed interface ForwardResult {
    object Success : ForwardResult
    data class Failure(val error: String) : ForwardResult
}
