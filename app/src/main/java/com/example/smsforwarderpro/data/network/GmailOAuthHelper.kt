package com.example.smsforwarderpro.data.network

import android.os.Build
import android.util.Base64
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GmailOAuthHelper @Inject constructor(
    private val client: OkHttpClient,
    private val prefs: EncryptedPreferencesManager
) {
    // Client ID for Android OAuth
    private val clientId = "527309217132-oipqgtoqcnrr7naldtq2gtuginei8avb.apps.googleusercontent.com"

    /**
     * Obtains a valid access token. If expired, refreshes it using the refresh token.
     */
    suspend fun getValidAccessToken(): String? {
        val currentExpiry = prefs.gmailTokenExpiry
        val currentTime = System.currentTimeMillis()

        // Buffer: if token expires in less than 60 seconds, refresh it now
        if (currentTime + 60_000 >= currentExpiry) {
            return refreshAccessToken()
        }

        return prefs.gmailAccessToken
    }

    private fun refreshAccessToken(): String? {
        val refreshToken = prefs.gmailRefreshToken ?: return null

        try {
            val requestBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()

            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: return null
                    val jsonObject = JsonParser.parseString(responseBody).asJsonObject
                    val newAccessToken = jsonObject.get("access_token")?.asString ?: return null
                    val expiresInSeconds = jsonObject.get("expires_in")?.asLong ?: 3600L

                    prefs.gmailAccessToken = newAccessToken
                    prefs.gmailTokenExpiry = System.currentTimeMillis() + (expiresInSeconds * 1000)
                    return newAccessToken
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Builds a MIME message string and encodes it in base64 URL safe format.
     */
    fun createEncodedMimeMessage(message: Message, senderEmail: String, recipientEmail: String): String {
        val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date(message.timestamp))

        val subject = "[SMS Forwarder] ${message.tag.name} from ${message.sender} at $formattedTime"
        
        val bodyContent = """
            SMS FORWARDER PRO REPORT
            ------------------------
            Sender: ${message.sender}
            SIM Slot: Slot ${message.simSlot + 1}
            Timestamp: $formattedTime
            Category Tag: ${message.tag.name}
            Device: $deviceName
            
            Message Body:
            ${message.body}
        """.trimIndent()

        // Compose RFC 2822 MIME mail
        val mimeMail = """
            From: $senderEmail
            To: $recipientEmail
            Subject: $subject
            Content-Type: text/plain; charset="utf-8"
            Content-Transfer-Encoding: base64

            ${Base64.encodeToString(bodyContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)}
        """.trimIndent().trim()

        return Base64.encodeToString(
            mimeMail.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )
    }
}
