package com.example.smsforwarderpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import com.example.smsforwarderpro.service.SmsForwardingService

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
        private var wakeLock: PowerManager.WakeLock? = null

        @Synchronized
        fun acquireWakeLock(context: Context) {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SmsForwarderPro::ReceiverWakeLock"
                ).apply {
                    acquire(10 * 60 * 1000L) // 10 minutes max timeout
                }
                Log.d(TAG, "WakeLock acquired")
            }
        }

        @Synchronized
        fun releaseWakeLock() {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                    Log.d(TAG, "WakeLock released")
                }
            }
            wakeLock = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // 1. Acquire WakeLock to hold CPU
        acquireWakeLock(context)

        // 2. Parse SMS messages from PDU
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) {
            releaseWakeLock()
            return
        }

        // Assemble multipart SMS body
        val sender = messages[0].displayOriginatingAddress ?: "Unknown"
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        // Extract SIM Slot Index (dual-SIM)
        var simSlot = 0
        val extras = intent.extras
        if (extras != null) {
            val subId = extras.getInt("subscription", -1)
            if (subId != -1) {
                val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                try {
                    val info = subscriptionManager.getActiveSubscriptionInfo(subId)
                    if (info != null) {
                        simSlot = info.simSlotIndex
                    }
                } catch (e: SecurityException) {
                    // Fallback to slot 0
                }
            }
        }

        // 3. Start persistent Foreground Service to perform background forwarding
        val serviceIntent = Intent(context, SmsForwardingService::class.java).apply {
            action = SmsForwardingService.ACTION_FORWARD_SMS
            putExtra(SmsForwardingService.EXTRA_SENDER, sender)
            putExtra(SmsForwardingService.EXTRA_BODY, body)
            putExtra(SmsForwardingService.EXTRA_TIMESTAMP, timestamp)
            putExtra(SmsForwardingService.EXTRA_SIM_SLOT, simSlot)
        }

        try {
            context.startForegroundService(serviceIntent)
            Log.d(TAG, "Forward service triggered successfully for SMS from $sender")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start forwarding service", e)
            releaseWakeLock()
        }
    }
}
