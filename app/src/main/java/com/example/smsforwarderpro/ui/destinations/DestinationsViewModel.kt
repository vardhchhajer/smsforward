package com.example.smsforwarderpro.ui.destinations

import android.content.Context
import android.telephony.SubscriptionManager
import androidx.lifecycle.ViewModel
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

data class SimCardInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val phoneNumber: String?
)

@HiltViewModel
class DestinationsViewModel @Inject constructor(
    val prefs: EncryptedPreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val headerNameRegex = Regex("^[A-Za-z0-9-]+$")

    fun getActiveSimCards(): List<SimCardInfo> {
        val list = mutableListOf<SimCardInfo>()
        try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeList = sm.activeSubscriptionInfoList
            if (activeList != null) {
                for (info in activeList) {
                    list.add(
                        SimCardInfo(
                            slotIndex = info.simSlotIndex,
                            subscriptionId = info.subscriptionId,
                            carrierName = info.carrierName.toString(),
                            phoneNumber = null
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // No permission
        } catch (e: Exception) {
            // Ignore
        }
        return list
    }

    fun updateTelegram(token: String, chatId: String, enabled: Boolean) {
        prefs.telegramBotToken = token
        prefs.telegramChatId = chatId
        prefs.isTelegramEnabled = enabled
    }

    fun updateWhatsApp(phoneNumberId: String, accessToken: String, recipientPhone: String, enabled: Boolean) {
        prefs.whatsappPhoneNumberId = phoneNumberId
        prefs.whatsappAccessToken = accessToken
        prefs.whatsappRecipientPhone = recipientPhone
        prefs.isWhatsappEnabled = enabled
    }

    fun updateWebhook(
        url: String,
        method: String,
        headers: String?,
        template: String?,
        authType: String,
        authValue: String?,
        enabled: Boolean
    ) {
        prefs.webhookUrl = url
        prefs.webhookMethod = method
        prefs.webhookHeaders = headers
        prefs.webhookTemplate = template
        prefs.webhookAuthType = authType
        prefs.webhookAuthValue = authValue
        prefs.isWebhookEnabled = enabled
    }

    fun validateWebhookConfig(
        url: String,
        headers: String?,
        authType: String,
        authValue: String?,
        enabled: Boolean
    ): String? {
        if (!enabled) return null
        if (!url.startsWith("https://", ignoreCase = true)) {
            return "Webhook URL must use https://"
        }

        if (!headers.isNullOrBlank()) {
            val jsonObject = runCatching { JsonParser.parseString(headers).asJsonObject }.getOrNull()
                ?: return "Headers must be a JSON object"
            jsonObject.entrySet().forEach { entry ->
                if (!headerNameRegex.matches(entry.key)) {
                    return "Invalid header name: ${entry.key}"
                }
                if (!entry.value.isJsonPrimitive) {
                    return "Header values must be strings"
                }
            }
        }

        if (authType == "API_KEY" && !authValue.isNullOrBlank()) {
            val headerName = authValue.split(":", limit = 2).first().trim()
            if (!headerNameRegex.matches(headerName)) {
                return "Invalid API key header name"
            }
        }

        return null
    }

    fun updateGmail(recipient: String, enabled: Boolean) {
        prefs.gmailRecipient = recipient
        prefs.isGmailEnabled = enabled
    }

    fun saveGmailTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        prefs.gmailAccessToken = accessToken
        if (refreshToken != null) {
            prefs.gmailRefreshToken = refreshToken
        }
        prefs.gmailTokenExpiry = System.currentTimeMillis() + (expiresInSeconds * 1000)
    }

    fun disconnectGmail() {
        prefs.gmailAccessToken = null
        prefs.gmailRefreshToken = null
        prefs.gmailTokenExpiry = 0L
    }

    fun updateSms(recipient: String, simSlot: Int, enabled: Boolean) {
        prefs.smsRecipient = recipient
        prefs.smsSimSlot = simSlot
        prefs.isSmsEnabled = enabled
    }
}
