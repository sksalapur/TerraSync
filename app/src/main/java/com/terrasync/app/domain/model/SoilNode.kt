package com.terrasync.app.domain.model

/**
 * A persisted geotechnical node record — the combined result of a field
 * measurement ([SoilNodeData]) and a Gemini AI analysis ([GeminiInference]).
 *
 * Stored in Firestore at: sites/{siteId}/nodes/{nodeId}
 *
 * All fields have defaults so Firestore's [DocumentSnapshot.toObject] can
 * deserialize without a no-arg constructor error.
 */
data class SoilNode(
    val id                  : String  = "",   // Firestore document ID (set after write)

    // ── Lab measurements ──────────────────────────────────────────────────────
    val siteId              : String  = "",
    val recordedAt          : Long    = 0L,
    val soilTypeLabel       : String  = "",   // SoilType.label — stored as String for Firestore
    val soilTypeSymbol      : String  = "",   // SoilType.symbol
    val d10                 : Double  = 0.0,
    val d30                 : Double  = 0.0,
    val d60                 : Double  = 0.0,
    val voidRatio           : Double  = 0.0,
    val moistureContent     : Double? = null,
    val specificGravity     : Double? = null,
    val dryDensity          : Double? = null,

    // ── Edge-computed & Gemini AI predictions ─────────────────────────────────
    val calculatedKValue        : Double? = null,
    val riskLevel               : String? = null,
    val reinforcementSuggestions: String? = null,

    // ── Legacy fields (fallback) ──────────────────────────────────────────────
    val permeabilityClass       : String? = null,
    val estimatedKValue         : String? = null,
    val riskInsights            : String? = null,
) {
    /**
     * Resolves the display risk level with a clear priority chain:
     *  1. Stored Gemini risk level (non-blank, not "Unknown")
     *  2. Legacy permeabilityClass field (backward compat)
     *  3. Derived locally from calculatedKValue using geotechnical thresholds
     *  4. "Unknown" as last resort
     */
    val riskLevelString: String
        get() {
            // 1. Prefer stored Gemini risk (guard against empty strings from old data)
            val stored = riskLevel?.takeIf { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
            if (stored != null) return stored
            // 2. Legacy field fallback
            val legacy = permeabilityClass?.takeIf { it.isNotBlank() }
            if (legacy != null) return legacy
            // 3. Derive from on-device calculated k value
            val k = calculatedKValue
            if (k != null) return when {
                k > 1e-2  -> "High"
                k >= 1e-5 -> "Medium"
                else      -> "Low"
            }
            return "Unknown"
        }

    /** Parses the stored string back to the typed enum for UI color coding. */
    val riskLevelEnum: RiskLevel
        get() = RiskLevel.entries.find {
            it.label.equals(riskLevelString, ignoreCase = true)
        } ?: RiskLevel.UNKNOWN

    val displayKValue: String
        get() = calculatedKValue?.let { "%.2E".format(it) + " cm/s" }
            ?: estimatedKValue
            ?: "Legacy Data"

    val displayReinforcement: String
        get() = reinforcementSuggestions ?: riskInsights ?: "No suggestions available."


    /** Derived Cu on read — not stored to avoid rounding drift. */
    val coefficientOfUniformity: Double get() = if (d10 > 0.0) d60 / d10 else 0.0
    val coefficientOfCurvature : Double get() =
        if (d10 > 0.0 && d60 > 0.0) (d30 * d30) / (d60 * d10) else 0.0
}
