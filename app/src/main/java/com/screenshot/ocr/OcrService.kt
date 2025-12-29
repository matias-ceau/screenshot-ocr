package com.screenshot.ocr

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.annotations.SerializedName
import com.screenshot.ocr.models.AiProvider
import com.screenshot.ocr.models.OcrResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Service for performing OCR using AI APIs
 */
class OcrService(
    private val chatDetector: ChatDetector
) {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Extract text from bitmap using specified AI provider
     */
    suspend fun extractText(
        bitmap: Bitmap,
        apiKey: String,
        provider: AiProvider,
        enableChatDetection: Boolean = true
    ): OcrResult = withContext(Dispatchers.IO) {

        val extractedText = when (provider) {
            AiProvider.OPENAI -> extractWithOpenAI(bitmap, apiKey)
            AiProvider.GEMINI -> extractWithGemini(bitmap, apiKey)
            AiProvider.MISTRAL -> extractWithMistral(bitmap, apiKey)
        }

        // Apply chat detection if enabled
        if (enableChatDetection) {
            val (isChat, messages) = chatDetector.detectAndFormat(extractedText)
            OcrResult(
                text = extractedText,
                isChat = isChat,
                chatMessages = messages,
                provider = provider
            )
        } else {
            OcrResult(
                text = extractedText,
                isChat = false,
                chatMessages = emptyList(),
                provider = provider
            )
        }
    }

    /**
     * Extract text using OpenAI Vision API
     */
    private suspend fun extractWithOpenAI(bitmap: Bitmap, apiKey: String): String {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(OpenAIService::interface)
        val base64Image = bitmapToBase64(bitmap)

        val request = OpenAIVisionRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                OpenAIMessage(
                    role = "user",
                    content = listOf(
                        OpenAIContent.Text(
                            text = "Extract all text from this image. Preserve the exact formatting, line breaks, and structure. If it's a chat conversation, maintain the speaker names, timestamps, and messages exactly as they appear."
                        ),
                        OpenAIContent.Image(
                            imageUrl = OpenAIImageUrl(
                                url = "data:image/jpeg;base64,$base64Image"
                            )
                        )
                    )
                )
            ),
            maxTokens = 4096
        )

        val response = service.analyzeImage("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content ?: ""
    }

    /**
     * Extract text using Google Gemini Vision API
     */
    private suspend fun extractWithGemini(bitmap: Bitmap, apiKey: String): String {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(GeminiService::class.java)
        val base64Image = bitmapToBase64(bitmap)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart.Text(
                            text = "Extract all text from this image. Preserve the exact formatting, line breaks, and structure. If it's a chat conversation, maintain the speaker names, timestamps, and messages exactly as they appear."
                        ),
                        GeminiPart.ImageData(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = base64Image
                            )
                        )
                    )
                )
            )
        )

        val response = service.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
    }

    /**
     * Extract text using Mistral Vision API
     */
    private suspend fun extractWithMistral(bitmap: Bitmap, apiKey: String): String {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.mistral.ai/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MistralService::class.java)
        val base64Image = bitmapToBase64(bitmap)

        val request = MistralVisionRequest(
            model = "pixtral-12b-2409",
            messages = listOf(
                MistralMessage(
                    role = "user",
                    content = listOf(
                        MistralContent.Text(
                            text = "Extract all text from this image. Preserve the exact formatting, line breaks, and structure. If it's a chat conversation, maintain the speaker names, timestamps, and messages exactly as they appear."
                        ),
                        MistralContent.Image(
                            imageUrl = "data:image/jpeg;base64,$base64Image"
                        )
                    )
                )
            ),
            maxTokens = 4096
        )

        val response = service.analyzeImage("Bearer $apiKey", request)
        return response.choices.firstOrNull()?.message?.content ?: ""
    }

    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 90): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // ============ OpenAI API Models ============

    interface OpenAIService {
        @POST("v1/chat/completions")
        suspend fun analyzeImage(
            @Header("Authorization") authorization: String,
            @Body request: OpenAIVisionRequest
        ): OpenAIVisionResponse
    }

    data class OpenAIVisionRequest(
        val model: String,
        val messages: List<OpenAIMessage>,
        @SerializedName("max_tokens") val maxTokens: Int
    )

    data class OpenAIMessage(
        val role: String,
        val content: List<OpenAIContent>
    )

    sealed class OpenAIContent {
        data class Text(
            val type: String = "text",
            val text: String
        ) : OpenAIContent()

        data class Image(
            val type: String = "image_url",
            @SerializedName("image_url") val imageUrl: OpenAIImageUrl
        ) : OpenAIContent()
    }

    data class OpenAIImageUrl(
        val url: String
    )

    data class OpenAIVisionResponse(
        val choices: List<OpenAIChoice>
    )

    data class OpenAIChoice(
        val message: OpenAIResponseMessage
    )

    data class OpenAIResponseMessage(
        val content: String
    )

    // ============ Gemini API Models ============

    interface GeminiService {
        @POST("v1beta/models/gemini-1.5-flash:generateContent")
        suspend fun generateContent(
            @retrofit2.http.Query("key") apiKey: String,
            @Body request: GeminiRequest
        ): GeminiResponse
    }

    data class GeminiRequest(
        val contents: List<GeminiContent>
    )

    data class GeminiContent(
        val parts: List<GeminiPart>
    )

    sealed class GeminiPart {
        data class Text(
            val text: String
        ) : GeminiPart()

        data class ImageData(
            @SerializedName("inline_data") val inlineData: GeminiInlineData
        ) : GeminiPart()
    }

    data class GeminiInlineData(
        @SerializedName("mime_type") val mimeType: String,
        val data: String
    )

    data class GeminiResponse(
        val candidates: List<GeminiCandidate>?
    )

    data class GeminiCandidate(
        val content: GeminiContent
    )

    // ============ Mistral API Models ============

    interface MistralService {
        @POST("v1/chat/completions")
        suspend fun analyzeImage(
            @Header("Authorization") authorization: String,
            @Body request: MistralVisionRequest
        ): MistralVisionResponse
    }

    data class MistralVisionRequest(
        val model: String,
        val messages: List<MistralMessage>,
        @SerializedName("max_tokens") val maxTokens: Int
    )

    data class MistralMessage(
        val role: String,
        val content: List<MistralContent>
    )

    sealed class MistralContent {
        data class Text(
            val type: String = "text",
            val text: String
        ) : MistralContent()

        data class Image(
            val type: String = "image_url",
            @SerializedName("image_url") val imageUrl: String
        ) : MistralContent()
    }

    data class MistralVisionResponse(
        val choices: List<MistralChoice>
    )

    data class MistralChoice(
        val message: MistralResponseMessage
    )

    data class MistralResponseMessage(
        val content: String
    )
}
