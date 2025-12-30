package com.screenshot.ocr

import android.graphics.Bitmap
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.screenshot.ocr.models.OcrResult
import com.screenshot.ocr.models.ProcessingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Service for performing OCR using OpenRouter API
 * Supports any vision model available on OpenRouter
 */
class OcrService(
    private val chatDetector: ChatDetector
) {

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(OpenRouterService::class.java)

    /**
     * Extract text from bitmap using OpenRouter
     * @param stateFlow Optional flow to emit status updates
     */
    suspend fun extractText(
        bitmap: Bitmap,
        apiKey: String,
        modelName: String,
        enableChatDetection: Boolean = true,
        stateFlow: MutableStateFlow<ProcessingState>? = null
    ): OcrResult = withContext(Dispatchers.IO) {

        try {
            // Status: Connecting
            stateFlow?.emit(ProcessingState.Connecting(modelName))

            val base64Image = bitmapToBase64(bitmap)

            // Status: Sending image
            stateFlow?.emit(ProcessingState.SendingImage(modelName))

            val request = OpenRouterRequest(
                model = modelName,
                messages = listOf(
                    OpenRouterMessage(
                        role = "user",
                        content = listOf(
                            ContentPart.Text(
                                text = """Extract all text from this image.
                                    |Preserve the exact formatting, line breaks, and structure.
                                    |If it's a chat conversation, maintain the speaker names, timestamps, and messages exactly as they appear.
                                    |Output ONLY the extracted text, no explanations.""".trimMargin()
                            ),
                            ContentPart.Image(
                                imageUrl = ImageUrl(
                                    url = "data:image/jpeg;base64,$base64Image"
                                )
                            )
                        )
                    )
                ),
                maxTokens = 4096
            )

            // Status: Waiting for response
            stateFlow?.emit(ProcessingState.WaitingForResponse(modelName))

            val response = service.chatCompletion(
                authorization = "Bearer $apiKey",
                request = request
            )

            // Status: Parsing response
            stateFlow?.emit(ProcessingState.ParsingResponse(modelName))

            val extractedText = response.choices?.firstOrNull()?.message?.content ?: ""

            if (extractedText.isEmpty() && response.error != null) {
                throw Exception("API Error: ${response.error.message} (code: ${response.error.code})")
            }

            // Apply chat detection if enabled
            if (enableChatDetection) {
                val (isChat, messages) = chatDetector.detectAndFormat(extractedText)
                OcrResult(
                    text = extractedText,
                    isChat = isChat,
                    chatMessages = messages,
                    model = modelName
                )
            } else {
                OcrResult(
                    text = extractedText,
                    isChat = false,
                    chatMessages = emptyList(),
                    model = modelName
                )
            }
        } catch (e: Exception) {
            throw Exception("OCR failed: ${e.message}")
        }
    }

    /**
     * Convert bitmap to base64 string
     */
    private fun bitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    // ============ OpenRouter API Models ============

    interface OpenRouterService {
        @POST("v1/chat/completions")
        suspend fun chatCompletion(
            @Header("Authorization") authorization: String,
            @Header("HTTP-Referer") referer: String = "https://github.com/screenshot-ocr",
            @Header("X-Title") title: String = "Screenshot OCR",
            @Body request: OpenRouterRequest
        ): OpenRouterResponse
    }

    data class OpenRouterRequest(
        val model: String,
        val messages: List<OpenRouterMessage>,
        @SerializedName("max_tokens") val maxTokens: Int = 4096
    )

    data class OpenRouterMessage(
        val role: String,
        val content: List<ContentPart>
    )

    sealed class ContentPart {
        abstract val type: String

        data class Text(
            override val type: String = "text",
            val text: String
        ) : ContentPart()

        data class Image(
            override val type: String = "image_url",
            @SerializedName("image_url") val imageUrl: ImageUrl
        ) : ContentPart()
    }

    data class ImageUrl(
        val url: String
    )

    data class OpenRouterResponse(
        val id: String? = null,
        val choices: List<Choice>? = null,
        val error: ApiError? = null
    )

    data class Choice(
        val message: ResponseMessage
    )

    data class ResponseMessage(
        val role: String,
        val content: String
    )

    data class ApiError(
        val code: Int?,
        val message: String
    )
}
