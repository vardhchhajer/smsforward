package com.example.smsforwarderpro.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.smsforwarderpro.MainActivity
import com.example.smsforwarderpro.data.local.db.ForwardAttempt
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.domain.repository.MessageRepository
import com.example.smsforwarderpro.domain.usecase.ForwardMessageUseCase
import com.example.smsforwarderpro.domain.usecase.ForwardResult
import com.example.smsforwarderpro.domain.usecase.ProcessIncomingMessageUseCase
import com.example.smsforwarderpro.receiver.SmsReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SmsForwardingService : Service() {

    companion object {
        private const val TAG = "SmsForwardingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "sms_forwarder_channel"
        private const val JOB_CHANNEL_CAPACITY = 64
        private const val MAX_FORWARD_ATTEMPTS = 3

        const val ACTION_START_SERVICE = "com.example.smsforwarderpro.action.START_SERVICE"
        const val ACTION_FORWARD_SMS = "com.example.smsforwarderpro.action.FORWARD_SMS"
        const val ACTION_STOP_SERVICE = "com.example.smsforwarderpro.action.STOP_SERVICE"
        const val ACTION_FLUSH_QUEUE = "com.example.smsforwarderpro.action.FLUSH_QUEUE"

        const val EXTRA_SENDER = "extra_sender"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_TIMESTAMP = "extra_timestamp"
        const val EXTRA_SIM_SLOT = "extra_sim_slot"
        const val EXTRA_IS_MMS = "extra_is_mms"
    }

    @Inject lateinit var processIncomingMessageUseCase: ProcessIncomingMessageUseCase
    @Inject lateinit var forwardMessageUseCase: ForwardMessageUseCase
    @Inject lateinit var messageRepository: MessageRepository
    @Inject lateinit var prefs: EncryptedPreferencesManager

    private val serviceJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled service coroutine failure", throwable)
    }
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob + exceptionHandler)

    private val jobChannel = Channel<ForwardJob>(JOB_CHANNEL_CAPACITY)
    private var messagesForwardedToday = 0

    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network connection available, triggering failed attempts flush")
            flushFailedQueue()
        }
    }

    private data class ForwardJob(
        val sender: String,
        val body: String,
        val timestamp: Long,
        val simSlot: Int,
        val isMms: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "SmsForwardingService created")
        createNotificationChannel()
        startForegroundServiceWithNotification()

        // Monitor Network Connectivity
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        // Start processing channel sequentially in a coroutine
        serviceScope.launch {
            for (job in jobChannel) {
                processAndForwardJob(job)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_STICKY

        when (intent.action) {
            null, ACTION_START_SERVICE -> {
                Log.d(TAG, "Forwarding service started")
                flushFailedQueue()
            }
            ACTION_FORWARD_SMS -> {
                if (!intent.hasExtra(EXTRA_SENDER) || !intent.hasExtra(EXTRA_BODY)) {
                    Log.w(TAG, "Ignoring forward action without message extras")
                    SmsReceiver.releaseWakeLock()
                    return START_STICKY
                }

                val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty().ifBlank { "Unknown" }
                val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
                val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())
                val simSlot = intent.getIntExtra(EXTRA_SIM_SLOT, 0)
                val isMms = intent.getBooleanExtra(EXTRA_IS_MMS, false)

                serviceScope.launch {
                    jobChannel.send(ForwardJob(sender, body, timestamp, simSlot, isMms))
                }
            }
            ACTION_STOP_SERVICE -> {
                Log.d(TAG, "Action stop received, killing service")
                stopSelf()
            }
            ACTION_FLUSH_QUEUE -> {
                flushFailedQueue()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun processAndForwardJob(job: ForwardJob) {
        try {
            val tag = tagMessage(job.body, job.isMms)
            val message = Message(
                id = UUID.randomUUID().toString(),
                sender = job.sender,
                body = job.body,
                tag = tag,
                timestamp = job.timestamp,
                simSlot = job.simSlot
            )

            // 1. Evaluate rules and store attempts in Room
            val attempts = processIncomingMessageUseCase.execute(message)

            // 2. Perform sequential forwarding attempts
            attempts.forEach { attempt ->
                forwardWithRetries(attempt, message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error running forwarding queue job", e)
        } finally {
            // Guarantee WakeLock is released
            SmsReceiver.releaseWakeLock()
        }
    }

    private suspend fun forwardWithRetries(attempt: ForwardAttempt, message: Message) {
        var currentAttempts = attempt.attempts.coerceAtLeast(0)
        var success = false
        var lastError = "Delivery failed"
        
        val backoffs = listOf(2000L, 8000L, 32000L) // 2s, 8s, 32s backoffs

        var dbAttempt = attempt

        while (currentAttempts < MAX_FORWARD_ATTEMPTS && !success) {
            currentAttempts++
            dbAttempt = dbAttempt.copy(
                attempts = currentAttempts,
                lastAttemptAt = System.currentTimeMillis()
            )
            messageRepository.saveForwardAttempt(dbAttempt)

            Log.d(TAG, "Forwarding message ${message.id} to ${attempt.destinationId} (Attempt $currentAttempts)")
            
            when (val result = forwardMessageUseCase.execute(dbAttempt, message)) {
                is ForwardResult.Success -> {
                    success = true
                    dbAttempt = dbAttempt.copy(status = "SUCCESS", errorMessage = null)
                    messageRepository.updateForwardAttempt(dbAttempt)
                    
                    messagesForwardedToday++
                    updateStickyNotification()
                    Log.d(TAG, "Forwarding success to ${attempt.destinationId}")
                }
                is ForwardResult.Failure -> {
                    lastError = result.error
                    Log.e(TAG, "Forwarding failed (Attempt $currentAttempts): $lastError")
                    if (currentAttempts < MAX_FORWARD_ATTEMPTS) {
                        delay(backoffs[currentAttempts - 1])
                    }
                }
            }
        }

        if (!success) {
            dbAttempt = dbAttempt.copy(status = "FAILED", errorMessage = lastError)
            messageRepository.updateForwardAttempt(dbAttempt)
        }
    }

    private fun tagMessage(body: String, isMms: Boolean): MessageTag {
        if (isMms) return MessageTag.MMS
        
        val otpKeywords = listOf("otp", "verification code", "one-time password", "is your code", "use code", "expires in", "activation code", "passcode")
        val hasOtpKeyword = otpKeywords.any { body.contains(it, ignoreCase = true) }
        val hasCode = Regex("\\b\\d{6,8}\\b").containsMatchIn(body)
        
        if (hasOtpKeyword || hasCode) return MessageTag.OTP

        val promoKeywords = listOf("sale", "offer", "discount", "promo", "coupon", "buy 1", "limited time", "subscribe")
        if (promoKeywords.any { body.contains(it, ignoreCase = true) }) return MessageTag.PROMOTIONAL

        val txnKeywords = listOf("debited", "credited", "charged", "balance", "txn", "transaction", "bank", "payment", "received")
        if (txnKeywords.any { body.contains(it, ignoreCase = true) }) return MessageTag.TRANSACTIONAL

        return MessageTag.SMS
    }

    private fun flushFailedQueue() {
        serviceScope.launch {
            val pendingAttempts = messageRepository.getPendingOrFailedAttempts()
            if (pendingAttempts.isEmpty()) return@launch

            Log.d(TAG, "Flushing ${pendingAttempts.size} offline/failed messages from queue")
            pendingAttempts.forEach { attempt ->
                val message = messageRepository.getMessageById(attempt.messageId)
                if (message != null) {
                    forwardWithRetries(attempt, message)
                } else {
                    Log.w(TAG, "Message not found for attempt: ${attempt.id}")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "SMS Forwarder Service"
        val descriptionText = "Monitors and forwards incoming SMS/MMS messages"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildServiceNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SmsForwardingService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SMS Forwarder Pro is Active")
            .setContentText("Status: Listening - $messagesForwardedToday messages forwarded today")
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Monitoring", stopPendingIntent)
            .build()
    }

    private fun updateStickyNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildServiceNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "SmsForwardingService destroyed")
        connectivityManager.unregisterNetworkCallback(networkCallback)
        jobChannel.close()
        serviceJob.cancel()
    }
}
