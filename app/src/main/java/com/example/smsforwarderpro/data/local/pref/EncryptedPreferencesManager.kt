package com.example.smsforwarderpro.data.local.pref

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "sms_forwarder_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_SERVICE_ACTIVE = "service_active"
        private const val KEY_PRIVACY_MODE = "privacy_mode"
        private const val KEY_BIOMETRIC_LOCK = "biometric_lock"
        private const val KEY_APP_PIN = "app_pin"
        private const val KEY_FAILED_PIN_ATTEMPTS = "failed_pin_attempts"
        private const val KEY_PIN_LOCKOUT_UNTIL = "pin_lockout_until"
        private const val KEY_RETENTION_DAYS = "retention_days"
        private const val KEY_RETRY_INTERVAL_SEC = "retry_interval_sec"
        private const val PIN_HASH_PREFIX = "pin:v1"
        private const val PIN_HASH_ITERATIONS = 120_000
        private const val PIN_SALT_BYTES = 16
        private const val PIN_HASH_BYTES = 32
        private const val MAX_PIN_ATTEMPTS = 5
        private const val PIN_LOCKOUT_MS = 5 * 60 * 1000L

        // Telegram Bot API
        private const val KEY_TG_BOT_TOKEN = "tg_bot_token"
        private const val KEY_TG_CHAT_ID = "tg_chat_id"
        private const val KEY_TG_ENABLED = "tg_enabled"

        // WhatsApp Business Cloud API
        private const val KEY_WA_PHONE_NUMBER_ID = "wa_phone_number_id"
        private const val KEY_WA_ACCESS_TOKEN = "wa_access_token"
        private const val KEY_WA_RECIPIENT_PHONE = "wa_recipient_phone"
        private const val KEY_WA_ENABLED = "wa_enabled"

        // Webhook Configuration
        private const val KEY_WH_URL = "wh_url"
        private const val KEY_WH_METHOD = "wh_method"
        private const val KEY_WH_HEADERS = "wh_headers" // JSON key-value
        private const val KEY_WH_TEMPLATE = "wh_template"
        private const val KEY_WH_AUTH_TYPE = "wh_auth_type" // NONE, BEARER, BASIC, API_KEY
        private const val KEY_WH_AUTH_VALUE = "wh_auth_value"
        private const val KEY_WH_ENABLED = "wh_enabled"

        // Gmail Configuration (OAuth 2.0)
        private const val KEY_GMAIL_RECIPIENT = "gmail_recipient"
        private const val KEY_GMAIL_REFRESH_TOKEN = "gmail_refresh_token"
        private const val KEY_GMAIL_ACCESS_TOKEN = "gmail_access_token"
        private const val KEY_GMAIL_TOKEN_EXPIRY = "gmail_token_expiry"
        private const val KEY_GMAIL_ENABLED = "gmail_enabled"

        // Direct SMS Destination Configuration
        private const val KEY_SMS_RECIPIENT = "sms_recipient"
        private const val KEY_SMS_SIM_SLOT = "sms_sim_slot"
        private const val KEY_SMS_ENABLED = "sms_enabled"

        // Database Encryption
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
    }

    var isServiceActive: Boolean
        get() = sharedPreferences.getBoolean(KEY_SERVICE_ACTIVE, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_SERVICE_ACTIVE, value) }

    var isPrivacyMode: Boolean
        get() = sharedPreferences.getBoolean(KEY_PRIVACY_MODE, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_PRIVACY_MODE, value) }

    var isBiometricLockEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_BIOMETRIC_LOCK, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_BIOMETRIC_LOCK, value) }

    val hasAppPin: Boolean
        get() = !sharedPreferences.getString(KEY_APP_PIN, null).isNullOrBlank()

    val isPinLockedOut: Boolean
        get() = System.currentTimeMillis() < pinLockoutUntil

    val pinLockoutRemainingMillis: Long
        get() = (pinLockoutUntil - System.currentTimeMillis()).coerceAtLeast(0L)

    private val pinLockoutUntil: Long
        get() = sharedPreferences.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)

    fun setAppPin(pin: String?) {
        sharedPreferences.edit {
            if (pin.isNullOrBlank()) {
                remove(KEY_APP_PIN)
            } else {
                putString(KEY_APP_PIN, hashPin(pin))
            }
            putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
            putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        }
    }

    fun verifyAppPin(pin: String): Boolean {
        if (isPinLockedOut) return false

        val stored = sharedPreferences.getString(KEY_APP_PIN, null) ?: return false
        val isValid = if (stored.startsWith(PIN_HASH_PREFIX)) {
            verifyHashedPin(pin, stored)
        } else {
            stored == pin
        }

        if (isValid) {
            sharedPreferences.edit {
                if (!stored.startsWith(PIN_HASH_PREFIX)) {
                    putString(KEY_APP_PIN, hashPin(pin))
                }
                putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
                putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
            }
        } else {
            recordFailedPinAttempt()
        }

        return isValid
    }

    var retentionDays: Int
        get() = sharedPreferences.getInt(KEY_RETENTION_DAYS, 30)
        set(value) = sharedPreferences.edit { putInt(KEY_RETENTION_DAYS, value) }

    var retryIntervalSec: Int
        get() = sharedPreferences.getInt(KEY_RETRY_INTERVAL_SEC, 5)
        set(value) = sharedPreferences.edit { putInt(KEY_RETRY_INTERVAL_SEC, value) }

    // Telegram getters and setters
    var telegramBotToken: String?
        get() = sharedPreferences.getString(KEY_TG_BOT_TOKEN, "8615658203:AAGnV97L9V9fiCVHGuPkuyUp6tUS1zxebkg")
        set(value) = sharedPreferences.edit { putString(KEY_TG_BOT_TOKEN, value) }

    var telegramChatId: String?
        get() = sharedPreferences.getString(KEY_TG_CHAT_ID, null)
        set(value) = sharedPreferences.edit { putString(KEY_TG_CHAT_ID, value) }

    var isTelegramEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_TG_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_TG_ENABLED, value) }

    // WhatsApp getters and setters
    var whatsappPhoneNumberId: String?
        get() = sharedPreferences.getString(KEY_WA_PHONE_NUMBER_ID, null)
        set(value) = sharedPreferences.edit { putString(KEY_WA_PHONE_NUMBER_ID, value) }

    var whatsappAccessToken: String?
        get() = sharedPreferences.getString(KEY_WA_ACCESS_TOKEN, null)
        set(value) = sharedPreferences.edit { putString(KEY_WA_ACCESS_TOKEN, value) }

    var whatsappRecipientPhone: String?
        get() = sharedPreferences.getString(KEY_WA_RECIPIENT_PHONE, null)
        set(value) = sharedPreferences.edit { putString(KEY_WA_RECIPIENT_PHONE, value) }

    var isWhatsappEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_WA_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_WA_ENABLED, value) }

    // Webhook getters and setters
    var webhookUrl: String?
        get() = sharedPreferences.getString(KEY_WH_URL, null)
        set(value) = sharedPreferences.edit { putString(KEY_WH_URL, value) }

    var webhookMethod: String
        get() = sharedPreferences.getString(KEY_WH_METHOD, "POST") ?: "POST"
        set(value) = sharedPreferences.edit { putString(KEY_WH_METHOD, value) }

    var webhookHeaders: String?
        get() = sharedPreferences.getString(KEY_WH_HEADERS, null)
        set(value) = sharedPreferences.edit { putString(KEY_WH_HEADERS, value) }

    var webhookTemplate: String?
        get() = sharedPreferences.getString(KEY_WH_TEMPLATE, null)
        set(value) = sharedPreferences.edit { putString(KEY_WH_TEMPLATE, value) }

    var webhookAuthType: String
        get() = sharedPreferences.getString(KEY_WH_AUTH_TYPE, "NONE") ?: "NONE"
        set(value) = sharedPreferences.edit { putString(KEY_WH_AUTH_TYPE, value) }

    var webhookAuthValue: String?
        get() = sharedPreferences.getString(KEY_WH_AUTH_VALUE, null)
        set(value) = sharedPreferences.edit { putString(KEY_WH_AUTH_VALUE, value) }

    var isWebhookEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_WH_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_WH_ENABLED, value) }

    // Gmail getters and setters

    var gmailRecipient: String?
        get() = sharedPreferences.getString(KEY_GMAIL_RECIPIENT, null)
        set(value) = sharedPreferences.edit { putString(KEY_GMAIL_RECIPIENT, value) }

    var gmailRefreshToken: String?
        get() = sharedPreferences.getString(KEY_GMAIL_REFRESH_TOKEN, null)
        set(value) = sharedPreferences.edit { putString(KEY_GMAIL_REFRESH_TOKEN, value) }

    var gmailAccessToken: String?
        get() = sharedPreferences.getString(KEY_GMAIL_ACCESS_TOKEN, null)
        set(value) = sharedPreferences.edit { putString(KEY_GMAIL_ACCESS_TOKEN, value) }

    var gmailTokenExpiry: Long
        get() = sharedPreferences.getLong(KEY_GMAIL_TOKEN_EXPIRY, 0L)
        set(value) = sharedPreferences.edit { putLong(KEY_GMAIL_TOKEN_EXPIRY, value) }

    var isGmailEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_GMAIL_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_GMAIL_ENABLED, value) }

    // SMS Forwarding getters and setters
    var smsRecipient: String?
        get() = sharedPreferences.getString(KEY_SMS_RECIPIENT, null)
        set(value) = sharedPreferences.edit { putString(KEY_SMS_RECIPIENT, value) }

    var smsSimSlot: Int
        get() = sharedPreferences.getInt(KEY_SMS_SIM_SLOT, 0)
        set(value) = sharedPreferences.edit { putInt(KEY_SMS_SIM_SLOT, value) }

    var isSmsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SMS_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_SMS_ENABLED, value) }

    // Database Encryption Key
    val databasePassphrase: ByteArray
        get() {
            var storedBase64 = sharedPreferences.getString(KEY_DB_PASSPHRASE, null)
            if (storedBase64 == null) {
                val newKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
                storedBase64 = Base64.encodeToString(newKey, Base64.NO_WRAP)
                sharedPreferences.edit { putString(KEY_DB_PASSPHRASE, storedBase64) }
            }
            return Base64.decode(storedBase64, Base64.NO_WRAP)
        }

    fun clearAllUserData() {
        sharedPreferences.edit { clear() }
    }

    private fun recordFailedPinAttempt() {
        val attempts = sharedPreferences.getInt(KEY_FAILED_PIN_ATTEMPTS, 0) + 1
        sharedPreferences.edit {
            if (attempts >= MAX_PIN_ATTEMPTS) {
                putInt(KEY_FAILED_PIN_ATTEMPTS, 0)
                putLong(KEY_PIN_LOCKOUT_UNTIL, System.currentTimeMillis() + PIN_LOCKOUT_MS)
            } else {
                putInt(KEY_FAILED_PIN_ATTEMPTS, attempts)
            }
        }
    }

    private fun hashPin(pin: String): String {
        val salt = ByteArray(PIN_SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derivePinHash(pin, salt, PIN_HASH_ITERATIONS)
        return listOf(
            PIN_HASH_PREFIX,
            PIN_HASH_ITERATIONS.toString(),
            Base64.encodeToString(salt, Base64.NO_WRAP),
            Base64.encodeToString(hash, Base64.NO_WRAP)
        ).joinToString(":")
    }

    private fun verifyHashedPin(pin: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 5) return false
        val iterations = parts[2].toIntOrNull() ?: return false
        val salt = runCatching { Base64.decode(parts[3], Base64.NO_WRAP) }.getOrNull() ?: return false
        val expected = runCatching { Base64.decode(parts[4], Base64.NO_WRAP) }.getOrNull() ?: return false
        val actual = derivePinHash(pin, salt, iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derivePinHash(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_HASH_BYTES * 8)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }
}
