package com.example.smsforwarderpro.worker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.smsforwarderpro.service.SmsForwardingService

class MmsProcessingWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "MmsProcessingWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val mmsData = queryLatestMms(context)
            if (mmsData != null) {
                val serviceIntent = Intent(context, SmsForwardingService::class.java).apply {
                    action = SmsForwardingService.ACTION_FORWARD_SMS
                    putExtra(SmsForwardingService.EXTRA_SENDER, mmsData.sender)
                    putExtra(SmsForwardingService.EXTRA_BODY, mmsData.body)
                    putExtra(SmsForwardingService.EXTRA_TIMESTAMP, mmsData.timestamp)
                    putExtra(SmsForwardingService.EXTRA_SIM_SLOT, mmsData.simSlot)
                    putExtra(SmsForwardingService.EXTRA_IS_MMS, true)
                }
                context.startForegroundService(serviceIntent)
                Log.d(TAG, "MMS received and dispatched to foreground service")
            } else {
                Log.d(TAG, "MMS arrived but textual body could not be parsed")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing MMS", e)
            Result.retry()
        }
    }

    private data class MmsInfo(
        val sender: String,
        val body: String,
        val timestamp: Long,
        val simSlot: Int
    )

    private fun queryLatestMms(context: Context): MmsInfo? {
        val uri = Uri.parse("content://mms")
        val projection = arrayOf("_id", "date", "sub", "sub_cs")
        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "date DESC LIMIT 1"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val mmsId = it.getString(it.getColumnIndexOrThrow("_id"))
                val dateLong = it.getLong(it.getColumnIndexOrThrow("date")) * 1000L
                return MmsInfo(
                    sender = getMmsSender(context, mmsId),
                    body = getMmsBody(context, mmsId),
                    timestamp = dateLong,
                    simSlot = 0
                )
            }
        }
        return null
    }

    private fun getMmsSender(context: Context, mmsId: String): String {
        val uri = Uri.parse("content://mms/$mmsId/addr")
        val projection = arrayOf("address", "type")

        context.contentResolver.query(uri, projection, "type = 137", null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow("address")) ?: "Unknown Sender"
            }
        }
        return "Unknown MMS Sender"
    }

    private fun getMmsBody(context: Context, mmsId: String): String {
        val partsUri = Uri.parse("content://mms/$mmsId/part")
        val projection = arrayOf("_id", "ct", "text")
        val bodyBuilder = StringBuilder()

        context.contentResolver.query(partsUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val contentType = cursor.getString(cursor.getColumnIndexOrThrow("ct"))
                if (contentType == "text/plain") {
                    val text = cursor.getString(cursor.getColumnIndexOrThrow("text"))
                    if (!text.isNullOrEmpty()) {
                        bodyBuilder.append(text)
                    }
                } else if (contentType.startsWith("image/") || contentType.startsWith("video/")) {
                    bodyBuilder.append("\n[Attachment: Media content ($contentType)]")
                }
            }
        }

        val result = bodyBuilder.toString().trim()
        return result.ifEmpty { "[MMS message without text]" }
    }
}
