package com.terrasync.app.domain.usecase

import com.terrasync.app.domain.model.RiskLevel
import com.terrasync.app.domain.model.SoilNodeData
import javax.inject.Inject
import kotlin.math.pow

/**
 * Holds the edge-computed results for a single soil node.
 *
 * @param kValue   Hydraulic conductivity in cm/s derived from empirical formulas.
 * @param riskLevel Local risk classification based purely on the k value,
 *                  using standard geotechnical thresholds. This is a baseline
 *                  estimate — Gemini's AI classification will overwrite it if run.
 */
data class PermeabilityResult(
    val kValue    : Double,
    val riskLevel : RiskLevel,
)

class CalculatePermeabilityUseCase @Inject constructor() {

    /**
     * Calculates the permeability (k) and a baseline local risk level.
     *
     * Formulas:
     *  - Sand/Gravel: Hazen's formula  →  k = C × D10²   (C = 1.0)
     *  - Silt/Clay:   Kozeny-Carman    →  k = Cₖ × e³/(1+e)   (Cₖ = 0.001)
     *
     * Risk thresholds (geotechnical standards):
     *  - k > 1×10⁻² cm/s  → HIGH  (high seepage / instability risk)
     *  - 1×10⁻⁵ ≤ k ≤ 1×10⁻² cm/s → MEDIUM
     *  - k < 1×10⁻⁵ cm/s  → LOW   (poor drainage / consolidation risk)
     *
     * @return [PermeabilityResult] containing the k value and a local risk level.
     */
    operator fun invoke(data: SoilNodeData): PermeabilityResult {
        val k = when {
            // Coarse soils: Hazen's formula
            data.soilType.symbol.startsWith("S") || data.soilType.symbol.startsWith("G") -> {
                val c = 1.0
                c * data.d10.pow(2)
            }
            // Fine soils: Kozeny-Carman approximation
            else -> {
                val ck = 0.001
                val e  = data.voidRatio
                ck * (e.pow(3) / (1.0 + e))
            }
        }

        val risk = when {
            k > 1e-2  -> RiskLevel.HIGH
            k >= 1e-5 -> RiskLevel.MEDIUM
            else      -> RiskLevel.LOW
        }

        return PermeabilityResult(kValue = k, riskLevel = risk)
    }
}
