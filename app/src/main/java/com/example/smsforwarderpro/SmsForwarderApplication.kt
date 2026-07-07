package com.example.smsforwarderpro

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.worker.ForwardingWorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmsForwarderApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var prefs: EncryptedPreferencesManager

    override fun onCreate() {
        super.onCreate()
        ForwardingWorkScheduler.scheduleRetentionPruning(this)
        if (prefs.isServiceActive) {
            ForwardingWorkScheduler.scheduleForwardingFlush(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
