package com.example.smsforwarderpro.data.network

import android.os.Build
import com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
import com.example.smsforwarderpro.domain.model.Message
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebhookDispatcher @Inject constructor(
    private val client: OkHttpClient,
    private val prefs: EncryptedPreferencesManager
) {
    private val gson = Gson()
    private val headerNameRegex = Regex("^[A-Za-z0-9-]+$")

    suspend fun dispatch(message: Message): WebhookResult {
        val url = prefs.webhookUrl ?: return WebhookResult.Failure("Webhook URL is not configured")
        val method = prefs.webhookMethod
        val headersJson = prefs.webhookHeaders
        val template = prefs.webhookTemplate ?: getDefaultTemplate()
        val authType = prefs.webhookAuthType
        val authValue = prefs.webhookAuthValue

        try {
            val httpUrl = url.toHttpUrlOrNull()
                ?: return WebhookResult.Failure("Webhook URL must be a valid HTTPS URL")
            if (httpUrl.scheme != "https") {
                return WebhookResult.Failure("Webhook URL must use https://")
            }

            // Replace placeholders in template body
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedTime = sdf.format(Date(message.timestamp))

            // Escape values for JSON safety if template looks like JSON
            val isJson = url.contains(".json") || headersJson?.contains("application/json") == true || template.trim().startsWith("{")
            
            val cleanSender = if (isJson) escapeJson(message.sender) else message.sender
            val cleanBody = if (isJson) escapeJson(message.body) else message.body
            val cleanTag = message.tag.name

            val parsedBody = template
                .replace("{{sender}}", cleanSender)
                .replace("{{body}}", cleanBody)
                .replace("{{timestamp}}", formattedTime)
                .replace("{{tag}}", cleanTag)
                .replace("{{sim_slot}}", message.simSlot.toString())
                .replace("{{device_name}}", deviceName)

            val requestBuilder = Request.Builder().url(httpUrl)

            // 1. Add Auth Headers
            when (authType) {
                "BEARER" -> {
                    if (!authValue.isNullOrEmpty()) {
                        requestBuilder.addHeader("Authorization", "Bearer $authValue")
                    }
                }
                "BASIC" -> {
                    if (!authValue.isNullOrEmpty()) {
                        val base64Auth = android.util.Base64.encodeToString(
                            authValue.toByteArray(),
                            android.util.Base64.NO_WRAP
                        )
                        requestBuilder.addHeader("Authorization", "Basic $base64Auth")
                    }
                }
                "API_KEY" -> {
                    val keyParts = authValue?.split(":", limit = 2)
                    if (keyParts != null && keyParts.size == 2) {
                        val headerName = keyParts[0].trim()
                        if (!isValidHeaderName(headerName)) {
                            return WebhookResult.Failure("Invalid API key header name")
                        }
                        requestBuilder.addHeader(headerName, keyParts[1].trim())
                    }
                }
            }

            // 2. Add Custom Headers from SharedPreferences
            if (!headersJson.isNullOrEmpty()) {
                val headerType = object : TypeToken<Map<String, String>>() {}.type
                val customHeaders: Map<String, String>? = try {
                    gson.fromJson(headersJson, headerType)
                } catch (e: Exception) {
                    return WebhookResult.Failure("Webhook headers must be valid JSON")
                }
                customHeaders?.forEach { (key, value) ->
                    if (!isValidHeaderName(key)) {
                        return WebhookResult.Failure("Invalid webhook header name: $key")
                    }
                    requestBuilder.addHeader(key, value)
                }
            }

            // 3. Set Request Method and Body
            val mediaType = if (isJson) "application/json; charset=utf-8" else "application/x-www-form-urlencoded"
            val requestBody = parsedBody.toRequestBody(mediaType.toMediaTypeOrNull())

            if (method.equals("PUT", ignoreCase = true)) {
                requestBuilder.put(requestBody)
            } else {
                requestBuilder.post(requestBody)
            }

            val request = requestBuilder.build()
            client.newCall(request).execute().use { response ->
                return if (response.isSuccessful) {
                    WebhookResult.Success
                } else {
                    WebhookResult.Failure("HTTP Status: ${response.code} - ${response.message}")
                }
            }
        } catch (e: Exception) {
            return WebhookResult.Failure(e.localizedMessage ?: "Unknown network error")
        }
    }

    private fun isValidHeaderName(name: String): Boolean {
        return name.isNotBlank() && headerNameRegex.matches(name)
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun getDefaultTemplate(): String {
        return """
            {
              "sender": "{{sender}}",
              "body": "{{body}}",
              "timestamp": "{{timestamp}}",
              "tag": "{{tag}}",
              "sim_slot": {{sim_slot}},
              "device_name": "{{device_name}}"
            }
        """.trimIndent()
    }
}

sealed interface WebhookResult {
    object Success : WebhookResult
    data class Failure(val error: String) : WebhookResult
}
