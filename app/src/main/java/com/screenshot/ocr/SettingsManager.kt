package com.screenshot.ocr

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
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
        val envApiKey = System.getenv("OPENROUTER_API_KEY") ?: ""
        val storedApiKey = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""

        return AppSettings(
            apiKey = storedApiKey.ifEmpty { envApiKey },
            modelName = encryptedPrefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL,
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
            putString(KEY_MODEL_NAME, settings.modelName)
            putFloat(KEY_OVERLAP_THRESHOLD, settings.overlapThreshold)
            putBoolean(KEY_CHAT_DETECTION, settings.enableChatDetection)
            apply()
        }
    }

    /**
     * Get API key
     */
    fun getApiKey(): String {
        val envApiKey = System.getenv("OPENROUTER_API_KEY") ?: ""
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
     * Get model name
     */
    fun getModelName(): String {
        return encryptedPrefs.getString(KEY_MODEL_NAME, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    /**
     * Save model name
     */
    fun saveModelName(modelName: String) {
        encryptedPrefs.edit().putString(KEY_MODEL_NAME, modelName).apply()
    }

    companion object {
        private const val PREFS_NAME = "encrypted_prefs"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL_NAME = "model_name"
        private const val KEY_OVERLAP_THRESHOLD = "overlap_threshold"
        private const val KEY_CHAT_DETECTION = "chat_detection"

        // Default to a free vision model on OpenRouter
        const val DEFAULT_MODEL = "qwen/qwen2.5-vl-72b-instruct:free"

        // Some suggested models for the UI
        val SUGGESTED_MODELS = listOf(
            "qwen/qwen2.5-vl-72b-instruct:free" to "Qwen 2.5 VL 72B (Free)",
            "google/gemma-3-27b-it:free" to "Gemma 3 27B (Free)",
            "meta-llama/llama-3.2-11b-vision-instruct:free" to "Llama 3.2 11B Vision (Free)",
            "qwen/qwen-vl-plus" to "Qwen VL Plus",
            "openai/gpt-4o" to "GPT-4o",
            "anthropic/claude-3.5-sonnet" to "Claude 3.5 Sonnet",
            "google/gemini-2.0-flash-001" to "Gemini 2.0 Flash"
        )
    }
}
