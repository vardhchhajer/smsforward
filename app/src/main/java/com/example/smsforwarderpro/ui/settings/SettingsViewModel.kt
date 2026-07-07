package com.example.smsforwarderpro.ui.settings

import android.content.Context
import android.text.format.DateFormat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messageRepository: MessageRepository,
    val prefs: EncryptedPreferencesManager
) : ViewModel() {

    init {
        viewModelScope.launch {
            messageRepository.deleteOldLogsAndAttempts(prefs.retentionDays)
            cleanupExportCache()
        }
    }

    fun updateRetentionDays(days: Int) {
        prefs.retentionDays = days
        viewModelScope.launch {
            messageRepository.deleteOldLogsAndAttempts(days)
        }
    }

    fun setPrivacyMode(enabled: Boolean) {
        prefs.isPrivacyMode = enabled
    }

    fun setBiometricLock(enabled: Boolean) {
        prefs.isBiometricLockEnabled = enabled
    }

    fun setAppPin(pin: String?) {
        prefs.setAppPin(pin)
    }

    fun deleteAllAppData(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                messageRepository.clearAllLogs()
                cleanupExportCache(maxAgeMs = 0L)
                prefs.clearAllUserData()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun exportLogsToCsv(
        startDate: Long,
        endDate: Long,
        onComplete: (File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                cleanupExportCache()
                // Fetch all logs from DB
                val logs = messageRepository.getAllMessageLogsFlow().first()
                val filteredLogs = logs.filter { log ->
                    log.timestamp in startDate..endDate
                }

                if (filteredLogs.isEmpty()) {
                    onComplete(null)
                    return@launch
                }

                // Format as CSV
                val csvBuilder = StringBuilder()
                csvBuilder.append("ID,Sender,Body,Tag,Timestamp,SimSlot\n")
                filteredLogs.forEach { log ->
                    val timeStr = DateFormat.format("yyyy-MM-dd HH:mm:ss", Date(log.timestamp)).toString()
                    csvBuilder.append(
                        listOf(
                            log.id,
                            log.sender,
                            log.body,
                            log.tag.name,
                            timeStr,
                            log.simSlot.toString()
                        ).joinToString(",") { escapeCsvField(it) }
                    )
                    csvBuilder.append("\n")
                }

                // Write to cache file
                val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(exportDir, "SMS_Logs_${System.currentTimeMillis()}.csv")
                file.writeText(csvBuilder.toString())
                onComplete(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            }
        }
    }

    private fun escapeCsvField(value: String): String {
        val sanitized = if (value.firstOrNull() in setOf('=', '+', '-', '@', '\t', '\r')) {
            "'$value"
        } else {
            value
        }
        return "\"" + sanitized.replace("\"", "\"\"") + "\""
    }

    private fun cleanupExportCache(maxAgeMs: Long = 60 * 60 * 1000L) {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) return

        val cutoff = System.currentTimeMillis() - maxAgeMs
        exportDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".csv") && it.lastModified() <= cutoff }
            ?.forEach { it.delete() }
    }
}
