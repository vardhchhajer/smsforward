package com.example.smsforwarderpro.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.smsforwarderpro.worker.MmsProcessingWorker
import java.util.concurrent.TimeUnit

class MmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.provider.Telephony.WAP_PUSH_RECEIVED") return
        if (intent.type != "application/vnd.wap.mms-message") return

        val request = OneTimeWorkRequestBuilder<MmsProcessingWorker>()
            .setInitialDelay(8, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        Log.d(TAG, "MMS processing handed off to WorkManager")
    }
}
