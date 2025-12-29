package com.screenshot.ocr.models

import android.graphics.Bitmap
import android.net.Uri

/**
 * Data models for the Screenshot OCR app
 */

data class Screenshot(
    val uri: Uri,
    val bitmap: Bitmap? = null,
    val order: Int
)

data class StitchedResult(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val overlapRegions: List<OverlapRegion> = emptyList()
)

data class OverlapRegion(
    val imageIndex: Int,
    val yOffset: Int,
    val height: Int,
    val confidence: Float
)

data class OcrResult(
    val text: String,
    val isChat: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val provider: AiProvider
)

data class ChatMessage(
    val speaker: String,
    val message: String,
    val timestamp: String? = null,
    val lineNumber: Int = 0
)

enum class AiProvider {
    OPENAI,
    GEMINI,
    MISTRAL;

    companion object {
        fun fromString(value: String): AiProvider {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: OPENAI
        }
    }
}

data class AppSettings(
    val apiKey: String = "",
    val aiProvider: AiProvider = AiProvider.OPENAI,
    val overlapThreshold: Float = 0.8f,
    val enableChatDetection: Boolean = true
)

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Stitching(val progress: Int, val total: Int) : ProcessingState()
    object ExtractingText : ProcessingState()
    data class Success(val result: OcrResult, val stitchedImage: Bitmap) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
