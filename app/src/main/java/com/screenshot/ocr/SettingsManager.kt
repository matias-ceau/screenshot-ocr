package com.screenshot.ocr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.screenshot.ocr.models.AiProvider
import com.screenshot.ocr.models.AppSettings

/**
 * Manages app settings with encrypted storage for API keys
 */
class SettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /**
     * Get current app settings
     */
    fun getSettings(): AppSettings {
        // Try environment variable first for API key
        val envApiKey = System.getenv("OPENAI_API_KEY") ?: ""
        val storedApiKey = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""

        return AppSettings(
            apiKey = storedApiKey.ifEmpty { envApiKey },
            aiProvider = AiProvider.fromString(
                encryptedPrefs.getString(KEY_AI_PROVIDER, AiProvider.OPENAI.name) ?: AiProvider.OPENAI.name
            ),
            overlapThreshold = encryptedPrefs.getFloat(KEY_OVERLAP_THRESHOLD, 0.8f),
            enableChatDetection = encryptedPrefs.getBoolean(KEY_CHAT_DETECTION, true)
        )
    }

    /**
     * Save app settings
     */
    fun saveSettings(settings: AppSettings) {
        encryptedPrefs.edit().apply {
            putString(KEY_API_KEY, settings.apiKey)
            putString(KEY_AI_PROVIDER, settings.aiProvider.name)
            putFloat(KEY_OVERLAP_THRESHOLD, settings.overlapThreshold)
            putBoolean(KEY_CHAT_DETECTION, settings.enableChatDetection)
            apply()
        }
    }

    /**
     * Get API key
     */
    fun getApiKey(): String {
        val envApiKey = System.getenv("OPENAI_API_KEY") ?: ""
        val storedApiKey = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        return storedApiKey.ifEmpty { envApiKey }
    }

    /**
     * Save API key
     */
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /**
     * Get AI provider
     */
    fun getAiProvider(): AiProvider {
        return AiProvider.fromString(
            encryptedPrefs.getString(KEY_AI_PROVIDER, AiProvider.OPENAI.name) ?: AiProvider.OPENAI.name
        )
    }

    /**
     * Save AI provider
     */
    fun saveAiProvider(provider: AiProvider) {
        encryptedPrefs.edit().putString(KEY_AI_PROVIDER, provider.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "encrypted_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_AI_PROVIDER = "ai_provider"
        private const val KEY_OVERLAP_THRESHOLD = "overlap_threshold"
        private const val KEY_CHAT_DETECTION = "chat_detection"
    }
}
