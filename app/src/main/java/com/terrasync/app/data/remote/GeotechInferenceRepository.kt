package com.terrasync.app.data.remote

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.terrasync.app.domain.model.GeminiInference
import com.terrasync.app.domain.model.SoilNodeData
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GeotechInference"

/**
 * Repository that wraps the Gemini generativeai SDK.
 *
 * Responsibility: build the domain-specific prompt, call the model,
 * extract and parse the JSON response into [GeminiInference].
 *
 * The [GenerativeModel] is injected (provided by Hilt's [AppModule])
 * so it can be swapped in tests without touching this class.
 */
@Singleton
class GeotechInferenceRepository @Inject constructor(
    private val model: GenerativeModel,
) {
    // Lenient JSON parser — ignores unknown keys Gemini might add
    private val json = Json {
        ignoreUnknownKeys    = true
        isLenient            = true
        coerceInputValues    = true
    }

    /**
     * Sends [node] to Gemini and parses the structured JSON response.
     *
     * @return [Result.success] with [GeminiInference] on success,
     *         [Result.failure] if the network call fails or JSON is malformed.
     */
    suspend fun analyze(node: SoilNodeData): Result<GeminiInference> = runCatching {
        val prompt = buildPrompt(node)
        Log.d(TAG, "Sending prompt to Gemini…")

        val response = model.generateContent(prompt)
        val raw      = response.text
            ?: error("Gemini returned an empty response.")

        Log.d(TAG, "Raw Gemini response:\n$raw")

        val jsonString = extractJsonBlock(raw)
        json.decodeFromString<GeminiInference>(jsonString)
    }

    // ── Prompt Engineering ────────────────────────────────────────────────────

    /**
     * Constructs a highly specific, constrained prompt.
     *
     * Key design decisions:
     * - Role-plays as a licensed geotechnical engineer (improves response quality).
     * - Provides all available soil parameters in a labelled block.
     * - Explicitly requests ONLY a JSON object — no markdown, no explanation.
     * - Specifies the exact field names and allowed enum values to prevent drift.
     */
    private fun buildPrompt(node: SoilNodeData): String = """
        You are a licensed geotechnical engineer with 20 years of field experience.
        Analyze the following soil test data from a borehole node and return a
        structured assessment. You MUST respond with ONLY a raw JSON object — no
        markdown fences, no preamble, no explanation outside the JSON.

        === SOIL TEST DATA ===
        Soil Classification (USCS): ${node.soilType.label} (${node.soilType.symbol})
        On-Site Empirical Permeability (k): ${node.calculatedKValue?.let { "%.2E".format(it) + " cm/s" } ?: "Not calculated"}
        Grain Size Distribution:
          D10 = ${node.d10} mm  (effective size)
          D30 = ${node.d30} mm
          D60 = ${node.d60} mm
        Coefficient of Uniformity  Cu = ${"%.3f".format(node.coefficientOfUniformity)}
        Coefficient of Curvature   Cc = ${"%.3f".format(node.coefficientOfCurvature)}
        Void Ratio (e)             = ${node.voidRatio}
        ${node.moistureContent?.let { "Moisture Content (ω)       = $it %" } ?: "Moisture Content: Not provided"}
        ${node.specificGravity?.let  { "Specific Gravity (Gs)      = $it"  } ?: "Specific Gravity: Not provided"}
        ${node.dryDensity?.let       { "Dry Density (γd)           = $it kN/m³" } ?: "Dry Density: Not provided"}

        === REQUIRED OUTPUT FORMAT ===
        Return exactly this JSON structure with these exact field names:
        {
          "risk_level": "<exactly one of: Low | Medium | High>",
          "reinforcement_suggestions": "<a short paragraph of actionable engineering advice based on the data and k-value>"
        }

        Rules:
        - risk_level MUST be exactly one of: Low, Medium, High
        - reinforcement_suggestions must be technically precise and actionable
        - Do NOT include any text before or after the JSON object
    """.trimIndent()

    // ── JSON Extraction ───────────────────────────────────────────────────────

    /**
     * Strips any markdown code fences Gemini might insert despite instructions.
     * Falls back to the raw string if no JSON block is found.
     */
    private fun extractJsonBlock(raw: String): String {
        // Try to extract content between first { and last }
        val start = raw.indexOf('{')
        val end   = raw.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start) {
            raw.substring(start, end + 1)
        } else {
            raw.trim()
        }
    }
}
