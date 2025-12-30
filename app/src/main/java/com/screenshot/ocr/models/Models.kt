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
    val model: String
)

data class ChatMessage(
    val speaker: String,
    val message: String,
    val timestamp: String? = null,
    val lineNumber: Int = 0,
    val alignment: MessageAlignment = MessageAlignment.UNKNOWN,
    val quotedSpeaker: String? = null,
    val quotedMessage: String? = null
)

enum class MessageAlignment {
    LEFT,    // Message aligned to the left
    RIGHT,   // Message aligned to the right
    UNKNOWN  // Alignment not detected
}

data class AppSettings(
    val apiKey: String = "",
    val modelName: String = "qwen/qwen2.5-vl-72b-instruct:free",
    val overlapThreshold: Float = 0.8f,
    val enableChatDetection: Boolean = true
)

sealed class ProcessingState {
    object Idle : ProcessingState()
    data class Stitching(val progress: Int, val total: Int) : ProcessingState()
    data class Connecting(val model: String) : ProcessingState()
    data class SendingImage(val model: String) : ProcessingState()
    data class WaitingForResponse(val model: String) : ProcessingState()
    data class ParsingResponse(val model: String) : ProcessingState()
    data class Success(val result: OcrResult, val stitchedImage: Bitmap) : ProcessingState()
    data class Error(val message: String) : ProcessingState()
}
