package com.example.shyam_assignment.data.api

import com.google.gson.annotations.SerializedName

// ── Request models ─────────────────────────────────────────────────────

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GenerationConfig? = null
)

data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String = "user"
)

data class GeminiPart(
    val text: String? = null,
    @SerializedName("inline_data")
    val inlineData: InlineData? = null
)

data class InlineData(
    @SerializedName("mime_type")
    val mimeType: String,
    val data: String   // base64-encoded audio
)

data class GenerationConfig(
    val temperature: Float = 0.1f,
    val maxOutputTokens: Int = 8192
)

// ── Response models ────────────────────────────────────────────────────

data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

data class GeminiCandidate(
    val content: GeminiResponseContent? = null,
    val finishReason: String? = null
)

data class GeminiResponseContent(
    val parts: List<GeminiResponsePart>? = null,
    val role: String? = null
)

data class GeminiResponsePart(
    val text: String? = null
)

data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

