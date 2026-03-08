package com.example.shyam_assignment.data.api

import com.google.gson.annotations.SerializedName

// ── Request models for Gemini API ──────────────────────────────────────

/** Top-level request body sent to Gemini API */
data class GeminiRequest(
    val contents: List<GeminiContent>,          // List of content blocks (text + audio)
    val generationConfig: GenerationConfig? = null  // Controls temperature, max tokens, etc.
)

/** A content block — contains parts (text or audio) and the role (always "user") */
data class GeminiContent(
    val parts: List<GeminiPart>,                // Text parts and/or inline audio data
    val role: String = "user"
)

/** A single part — either text or inline base64 audio data */
data class GeminiPart(
    val text: String? = null,                   // Text prompt (e.g., "Transcribe this audio...")
    @SerializedName("inline_data")
    val inlineData: InlineData? = null           // Base64-encoded audio file
)

/** Inline audio data sent directly in the request (no file upload needed) */
data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,                       // e.g., "audio/wav"
    val data: String                            // Base64-encoded audio bytes
)

/** Controls how Gemini generates its response */
data class GenerationConfig(
    val temperature: Float = 0.1f,              // Low = more deterministic output
    val maxOutputTokens: Int = 8192             // Maximum response length
)

// ── Response models from Gemini API ────────────────────────────────────

/** Top-level response from Gemini API */
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,  // List of generated responses
    val error: GeminiError? = null                   // Error details if request failed
)

/** A single candidate response */
data class GeminiCandidate(
    val content: GeminiResponseContent? = null,     // The generated content
    val finishReason: String? = null                 // Why generation stopped (e.g., "STOP")
)

/** The content of a candidate response */
data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null,    // List of text parts in the response
    val role: String? = null                         // Always "model"
)

/** A single part of the response — contains the generated text */
data class GeminiResponsePart(
    val text: String? = null                        // The transcript or summary text
)

/** Error details from Gemini API */
data class GeminiError(
    val code: Int? = null,                          // HTTP-like error code
    val message: String? = null,                    // Human-readable error message
    val status: String? = null                      // Error status string
)

// ── Summary structured output ──────────────────────────────────────────

data class SummaryJsonResponse(
    val title: String = "",
    val summary: String = "",
    val actionItems: List<String> = emptyList(),
    val keyPoints: List<String> = emptyList()
)
