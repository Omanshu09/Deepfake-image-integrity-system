package com.example.deepfakedetectioncts.core

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// Root request object matching Hive API requirements
data class ModelRequest(
    @SerializedName("media_metadata") val mediaMetadata: Boolean = true,
    @SerializedName("input") val input: List<ModelInput>
)

// Input object containing the image data
data class ModelInput(
    @SerializedName("media_base64") val mediaBase64: String
)

// Response mapping
data class ModelResponse(
    @SerializedName("task_id") val taskId: String?,
    @SerializedName("output") val output: List<ModelOutput>?
)

data class ModelOutput(
    @SerializedName("classes") val classes: List<ModelClass>?
)

data class ModelClass(
    @SerializedName("class") val label: String,
    @SerializedName("value") val confidence: Double
)

// Gemini API Request/Response
data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface ModelService {
    @POST("v3/hive/ai-generated-and-deepfake-content-detection")
    suspend fun processContent(
        @Header("authorization") token: String,
        @Body request: ModelRequest
    ): ModelResponse

    @POST("https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent")
    suspend fun getExplanation(
        @Header("X-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

data class AnalysisResult(
    val resultText: String,
    val explanation: String? = null
)

class DeepfakeModelEngine {

    private val hiveSecretKey = "Bearer pjAVhFshbLF/DmQ7LF8L7g=="
    private val geminiApiKey = "AIzaSyBVDI_YBM_d01MHgIAC8HI-Oj4bVdeAjNY"

    private val service: ModelService by lazy {
        val interceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.thehive.ai/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModelService::class.java)
    }

    private val geminiService: ModelService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModelService::class.java)
    }

    suspend fun analyze(bitmap: Bitmap, shouldExplain: Boolean = false): AnalysisResult {
        return try {
            val scaled = scaleDown(bitmap)
            val base64Image = bitmapToBase64(scaled)
            val request = ModelRequest(input = listOf(ModelInput(base64Image)))
            val response = service.processContent(hiveSecretKey, request)
            
            val classes = response.output?.firstOrNull()?.classes ?: return AnalysisResult("Result inconclusive.")
            val aiScore = classes.find { it.label == "ai_generated" }?.confidence ?: 0.0
            val deepfakeScore = classes.find { it.label == "deepfake" }?.confidence ?: 0.0
            
            val resultText = when {
                deepfakeScore > aiScore && deepfakeScore >= 0.5 -> "⚠️ Deepfake Detected (${(deepfakeScore * 100).toInt()}%)"
                aiScore >= 0.5 -> "🤖 Synthetic Content (${(aiScore * 100).toInt()}%)"
                else -> "✅ Authentic Content"
            }

            var explanation: String? = null
            if (shouldExplain && (deepfakeScore >= 0.5 || aiScore >= 0.5)) {
                explanation = fetchGeminiExplanation(resultText)
            }

            AnalysisResult(resultText, explanation)
        } catch (e: Exception) {
            Log.e("CoreEngine", "Analysis error", e)
            AnalysisResult("Connection Error: Please verify your internet and try again.")
        }
    }

    private suspend fun fetchGeminiExplanation(result: String): String? {
        return try {
            val prompt = "The image was detected as '$result'. Provide a short, professional explanation (max 50 words) of why an image might be classified this way and what to look for (e.g., facial inconsistencies, skin texture, background artifacts)."
            val request = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(prompt)))))
            val response = geminiService.getExplanation(geminiApiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        } catch (e: Exception) {
            Log.e("CoreEngine", "Gemini error: ${e.message}", e)
            if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("CoreEngine", "Gemini API Error Body: $errorBody")
            }
            null // Return null so we don't save error text to history
        }
    }

    private fun scaleDown(bitmap: Bitmap): Bitmap {
        val maxSize = 1024
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
        val newHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
