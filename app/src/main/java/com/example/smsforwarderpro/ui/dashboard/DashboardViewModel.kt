package com.example.smsforwarderpro.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.repository.MessageRepository
import com.example.smsforwarderpro.service.SmsForwardingService
import com.example.smsforwarderpro.worker.ForwardingWorkScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    val prefs: EncryptedPreferencesManager
) : ViewModel() {

    // Calculate start of today to filter today's stats
    private val startOfToday: Long
        get() {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.timeInMillis
        }

    val recentLogs: StateFlow<List<Message>> = messageRepository.getRecentMessageLogsFlow(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val successCount = messageRepository.getSuccessCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount = messageRepository.getFailedCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successTodayCount = messageRepository.getSuccessCountTodayFlow(startOfToday)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun toggleService(isActive: Boolean) {
        prefs.isServiceActive = isActive
        val intent = Intent(context, SmsForwardingService::class.java)
        
        if (isActive) {
            intent.action = SmsForwardingService.ACTION_START_SERVICE
            try {
                context.startForegroundService(intent)
                ForwardingWorkScheduler.scheduleForwardingFlush(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            intent.action = SmsForwardingService.ACTION_STOP_SERVICE
            try {
                context.startService(intent)
                ForwardingWorkScheduler.cancelForwardingFlush(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun triggerTestSms() {
        // Send a mock broadcast to SmsForwardingService for debugging/verification
        val intent = Intent(context, SmsForwardingService::class.java).apply {
            action = SmsForwardingService.ACTION_FORWARD_SMS
            putExtra(SmsForwardingService.EXTRA_SENDER, "+1234567890")
            putExtra(SmsForwardingService.EXTRA_BODY, "TEST SMS: Your verification code is 584931. Expires in 5 minutes.")
            putExtra(SmsForwardingService.EXTRA_TIMESTAMP, System.currentTimeMillis())
            putExtra(SmsForwardingService.EXTRA_SIM_SLOT, 0)
        }
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
