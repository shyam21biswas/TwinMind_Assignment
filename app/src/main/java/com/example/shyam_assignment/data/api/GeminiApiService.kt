package com.example.shyam_assignment.data.api

import android.util.Base64
import android.util.Log
import com.example.shyam_assignment.BuildConfig
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple HTTP wrapper for the Gemini REST API.
 * Uses inline audio (base64) for chunk transcription.
 */
@Singleton
class GeminiApiService @Inject constructor() {

    companion object {
        private const val TAG = "GeminiApiService"
        private const val MODEL = "gemini-2.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

        private const val TRANSCRIPTION_PROMPT =
            "Transcribe this audio chunk accurately. " +
            "Preserve the original spoken language. " +
            "Do not summarize. Do not add explanations. " +
            "Return only the transcript text."

        private const val SUMMARY_PROMPT =
            "You are generating a clean meeting summary for a note-taking app. " +
            "Based on the transcript below, return structured JSON with these fields only: " +
            "title, summary, actionItems, keyPoints.\n\n" +
            "Rules:\n" +
            "- title: short and specific\n" +
            "- summary: concise but informative paragraph\n" +
            "- actionItems: concrete follow-up tasks (array of strings)\n" +
            "- keyPoints: important discussion highlights (array of strings)\n" +
            "- do not include any extra keys\n" +
            "- do not include markdown\n" +
            "- do not include commentary outside JSON\n\n" +
            "Return ONLY valid JSON. Example format:\n" +
            "{\"title\":\"...\",\"summary\":\"...\",\"actionItems\":[\"...\"],\"keyPoints\":[\"...\"]}\n\n" +
            "Transcript:\n"
    }

    private val gson = Gson()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)   // transcription can be slow
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribes an audio WAV file using Gemini 2.5 Flash.
     *
     * @return the transcript text on success
     * @throws IOException on network / API errors
     * @throws IllegalStateException if API key is missing
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("GEMINI_API_KEY is not configured in local.properties"))
        }

        return try {
            // Read and base64-encode the audio file
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(
                            GeminiPart(text = TRANSCRIPTION_PROMPT),
                            GeminiPart(
                                inlineData = InlineData(
                                    mimeType = "audio/wav",
                                    data = base64Audio
                                )
                            )
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.1f,
                    maxOutputTokens = 8192
                )
            )

            val jsonBody = gson.toJson(request)
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            // Execute synchronously (called from a coroutine on Dispatchers.IO)
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini API error ${response.code}: $responseBody")
                val geminiResp = try { gson.fromJson(responseBody, GeminiResponse::class.java) } catch (_: Exception) { null }
                val errMsg = geminiResp?.error?.message ?: "HTTP ${response.code}"
                return Result.failure(IOException("Gemini API error: $errMsg"))
            }

            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
            val transcript = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (transcript.isNullOrBlank()) {
                Log.w(TAG, "Empty transcript from Gemini")
                Result.failure(IOException("Empty transcript returned by Gemini"))
            } else {
                Log.d(TAG, "Transcription successful: ${transcript.take(100)}...")
                Result.success(transcript)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generates a structured meeting summary from transcript text.
     *
     * @return parsed [SummaryJsonResponse] on success
     */
    suspend fun generateSummary(transcriptText: String): Result<SummaryJsonResponse> {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("GEMINI_API_KEY is not configured"))
        }

        return try {
            val fullPrompt = SUMMARY_PROMPT + transcriptText

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = fullPrompt))
                    )
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.2f,
                    maxOutputTokens = 4096
                )
            )

            val jsonBody = gson.toJson(request)
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

            val httpRequest = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Gemini summary API error ${response.code}: $responseBody")
                val geminiResp = try {
                    gson.fromJson(responseBody, GeminiResponse::class.java)
                } catch (_: Exception) { null }
                val errMsg = geminiResp?.error?.message ?: "HTTP ${response.code}"
                return Result.failure(IOException("Gemini API error: $errMsg"))
            }

            val geminiResponse = gson.fromJson(responseBody, GeminiResponse::class.java)
            val rawText = geminiResponse.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (rawText.isNullOrBlank()) {
                return Result.failure(IOException("Empty summary returned by Gemini"))
            }

            // Strip markdown code fences if present
            val cleanJson = rawText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            Log.d(TAG, "Summary raw JSON: ${cleanJson.take(200)}")

            val summaryResponse = gson.fromJson(cleanJson, SummaryJsonResponse::class.java)
            if (summaryResponse.title.isBlank() && summaryResponse.summary.isBlank()) {
                Result.failure(IOException("Parsed summary is empty"))
            } else {
                Log.d(TAG, "Summary generated: title='${summaryResponse.title}'")
                Result.success(summaryResponse)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Summary generation failed", e)
            Result.failure(e)
        }
    }
}

