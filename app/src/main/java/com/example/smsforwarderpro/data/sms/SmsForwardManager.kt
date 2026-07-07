package com.example.smsforwarderpro.data.sms

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsForwardManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: EncryptedPreferencesManager
) {
    companion object {
        private const val SENT_TIMEOUT_MS = 60_000L
        private val requestCodes = AtomicInteger(4000)
    }

    suspend fun forward(message: Message): SmsResult {
        val recipient = prefs.smsRecipient ?: return SmsResult.Failure("No recipient phone number configured")
        
        return try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = try {
                subscriptionManager.activeSubscriptionInfoList
            } catch (e: SecurityException) {
                null
            }

            // Dual SIM handling: select SMS Manager for custom Subscription ID
            val targetSimSlot = prefs.smsSimSlot
            var targetSubscriptionId: Int? = null

            if (!activeSubscriptions.isNullOrEmpty()) {
                val matchingSub = activeSubscriptions.find { it.simSlotIndex == targetSimSlot }
                if (matchingSub != null) {
                    targetSubscriptionId = matchingSub.subscriptionId
                }
            }

            val smsManager: SmsManager = if (targetSubscriptionId != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java).createForSubscriptionId(targetSubscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(targetSubscriptionId)
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
            }

            val parts = smsManager.divideMessage(message.body).ifEmpty { listOf(message.body) }
            withTimeout(SENT_TIMEOUT_MS) {
                awaitSentResult(smsManager, recipient, message.body, parts)
            }
        } catch (e: TimeoutCancellationException) {
            SmsResult.Failure("Timed out waiting for SMS sent callback")
        } catch (e: Exception) {
            SmsResult.Failure(e.localizedMessage ?: "Failed to send SMS")
        }
    }

    private suspend fun awaitSentResult(
        smsManager: SmsManager,
        recipient: String,
        body: String,
        parts: List<String>
    ): SmsResult = suspendCancellableCoroutine { continuation ->
        val action = "${context.packageName}.action.SMS_SENT.${System.nanoTime()}"
        val filter = IntentFilter(action)
        var completedParts = 0
        val errors = mutableListOf<String>()

        lateinit var receiver: BroadcastReceiver
        fun unregisterReceiverSafely() {
            runCatching { context.unregisterReceiver(receiver) }
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != action) return

                completedParts++
                if (resultCode != Activity.RESULT_OK) {
                    errors.add("part $completedParts ${describeSmsResult(resultCode)}")
                }

                if (completedParts >= parts.size && continuation.isActive) {
                    unregisterReceiverSafely()
                    val result = if (errors.isEmpty()) {
                        SmsResult.Success
                    } else {
                        SmsResult.Failure("SMS send failed: ${errors.joinToString()}")
                    }
                    continuation.resume(result)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filter)
        }
        continuation.invokeOnCancellation { unregisterReceiverSafely() }

        try {
            val baseRequestCode = requestCodes.getAndAdd(parts.size.coerceAtLeast(1))
            val sentIntents = ArrayList(
                parts.indices.map { index ->
                    PendingIntent.getBroadcast(
                        context,
                        baseRequestCode + index,
                        Intent(action).setPackage(context.packageName).putExtra("part_index", index),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            )

            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(recipient, null, ArrayList(parts), sentIntents, null)
            } else {
                smsManager.sendTextMessage(recipient, null, body, sentIntents.first(), null)
            }
        } catch (e: Exception) {
            unregisterReceiverSafely()
            if (continuation.isActive) {
                continuation.resume(SmsResult.Failure(e.localizedMessage ?: "Failed to send SMS"))
            }
        }
    }

    private fun describeSmsResult(resultCode: Int): String {
        return when (resultCode) {
            SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "generic failure"
            SmsManager.RESULT_ERROR_NO_SERVICE -> "no service"
            SmsManager.RESULT_ERROR_NULL_PDU -> "null PDU"
            SmsManager.RESULT_ERROR_RADIO_OFF -> "radio off"
            else -> "error code $resultCode"
        }
    }
}

sealed interface SmsResult {
    object Success : SmsResult
    data class Failure(val error: String) : SmsResult
}
