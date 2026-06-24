package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Request/Response Data Classes using Moshi ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Double? = 0.2
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

// --- Retrofit Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun analyzeImage(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Gemini client helper ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService: GeminiApiService by lazy {
        retrofit.create(GeminiApiService::class.java)
    }

    // Helper to convert Bitmap to Base64
    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        // Resize bitmap if too large to save bandwidth and stay within limits
        val maxDimension = 1024
        val resized = if (width > maxDimension || height > maxDimension) {
            val aspectRatio = width.toFloat() / height.toFloat()
            val (newWidth, newHeight) = if (width > height) {
                maxDimension to (maxDimension / aspectRatio).toInt()
            } else {
                (maxDimension * aspectRatio).toInt() to maxDimension
            }
            Bitmap.createScaledBitmap(this, newWidth, newHeight, true)
        } else {
            this
        }
        resized.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeCropLeaf(bitmap: Bitmap): CropAnalysisResult {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured. Please add your key via the AI Studio Secrets panel.")
        }

        val prompt = """
            You are an expert agricultural botanist and plant pathologist specializing in crop diseases.
            Analyze the provided image of a crop leaf carefully. 
            Identify if there is any disease or if the crop is healthy.
            
            You MUST return a valid JSON object matching the following structure:
            {
              "crop": "Crop Name (e.g., Tomato, Potato, Corn, Pepper)",
              "disease": "Disease Name (e.g., Early Blight, Late Blight, Common Rust, or Healthy)",
              "confidence": confidence_percentage_as_double (between 0.0 and 100.0),
              "description": "A precise explanation of the disease or health status",
              "symptoms": ["Symptom 1", "Symptom 2", ...],
              "causes": ["Cause 1", "Cause 2", ...],
              "treatment": ["Treatment 1", "Treatment 2", ...],
              "prevention": ["Prevention method 1", "Prevention method 2", ...],
              "topPredictions": [
                {"label": "Disease A", "confidence": conf_a},
                {"label": "Disease B", "confidence": conf_b},
                {"label": "Disease C", "confidence": conf_c}
              ]
            }
            
            Make sure the predictions list includes the current identified disease as the top prediction, followed by 2 other plausible crop diseases/health statuses with their respective confidence scores.
            
            Return ONLY the raw JSON block. Do not wrap it in markdown backticks or other text.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmap.toBase64()))
                    )
                )
            ),
            generationConfig = GenerationConfig()
        )

        val response = apiService.analyzeImage(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from Gemini API.")

        // Clean response text in case it wrapped it in ```json ```
        val cleanedText = responseText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return try {
            val resultAdapter = moshi.adapter(CropAnalysisResult::class.java)
            resultAdapter.fromJson(cleanedText)
                ?: throw IllegalStateException("Failed to parse analysis result from API response.")
        } catch (e: Exception) {
            // Attempt a fallback parse in case of minor structure issues, or rethrow
            throw IllegalStateException("Failed to parse Gemini response: ${e.message}\nRaw response: $responseText", e)
        }
    }
}
