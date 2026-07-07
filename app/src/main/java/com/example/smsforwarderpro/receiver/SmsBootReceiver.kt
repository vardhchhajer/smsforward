package com.example.smsforwarderpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.service.SmsForwardingService
import com.example.smsforwarderpro.worker.ForwardingWorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsBootReceiver : BroadcastReceiver() {

    @Inject lateinit var prefs: EncryptedPreferencesManager

    companion object {
        private const val TAG = "SmsBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Boot receiver triggered with action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            if (prefs.isServiceActive) {
                Log.d(TAG, "Service is active in preferences, launching from boot/update")
                
                // 1. Restart the foreground forwarding service
                val serviceIntent = Intent(context, SmsForwardingService::class.java).apply {
                    this.action = SmsForwardingService.ACTION_START_SERVICE
                }
                try {
                    context.startForegroundService(serviceIntent)
                    ForwardingWorkScheduler.scheduleForwardingFlush(context)
                    Log.d(TAG, "Successfully started SmsForwardingService from boot/update")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start SmsForwardingService from boot/update", e)
                }
            } else {
                Log.d(TAG, "Service is inactive in preferences, not launching on boot")
            }
        }
    }
}
