package com.terrasync.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Strongly-typed model for the JSON payload Gemini returns.
 *
 * Expected raw JSON from the model:
 * ```json
 * {
 *   "risk_level": "Low",
 *   "reinforcement_suggestions": "High plasticity clay — susceptible to swelling and consolidation..."
 * }
 * ```
 *
 * [riskLevel] maps to the [RiskLevel] enum for type-safe UI rendering.
 */
@Serializable
data class GeminiInference(
    @SerialName("risk_level")                val riskLevel              : String,
    @SerialName("reinforcement_suggestions") val reinforcementSuggestions : String,
) {
    /** Parses [riskLevel] string to a type-safe enum. Defaults to [RiskLevel.UNKNOWN]. */
    val riskLevelEnum: RiskLevel
        get() = RiskLevel.entries.find {
            it.label.equals(riskLevel, ignoreCase = true)
        } ?: RiskLevel.UNKNOWN
}

enum class RiskLevel(val label: String, val colorHex: Long) {
    LOW("Low",     0xFF6BAF72),   // green — low risk
    MEDIUM("Medium", 0xFFD4A843), // amber — moderate risk
    HIGH("High",   0xFFCF6679),   // red-amber — high risk
    UNKNOWN("Unknown", 0xFF9E8D82),
}
