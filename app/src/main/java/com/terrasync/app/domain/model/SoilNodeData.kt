package com.terrasync.app.domain.model

/**
 * Immutable data class representing one geotechnical soil node record.
 *
 * A "node" corresponds to a single test point (e.g. a borehole or trial pit)
 * within a [Site]. All numeric fields are [Double] to accommodate decimal
 * precision from laboratory instruments.
 *
 * ── Required fields ──────────────────────────────────────────────────────────
 * @param siteId        Firestore document ID of the parent [Site].
 * @param soilType      USCS classification selected by the field engineer.
 * @param d10           Grain size at 10% passing (mm) — effective size.
 * @param d30           Grain size at 30% passing (mm) — used for Cu/Cc calc.
 * @param d60           Grain size at 60% passing (mm) — used for uniformity.
 * @param voidRatio     Ratio of void volume to solid volume (dimensionless, e > 0).
 *
 * ── Optional fields ──────────────────────────────────────────────────────────
 * @param moistureContent  Water content as % of dry soil mass (ω, %).
 * @param specificGravity  Gs — ratio of soil particle density to water density.
 *                         Typical range: 2.60–2.80 for most mineral soils.
 * @param dryDensity       γd — mass per unit volume of dry soil (kN/m³ or g/cm³).
 *
 * ── Derived properties ───────────────────────────────────────────────────────
 * Coefficient of Uniformity (Cu) and Curvature (Cc) are computed on demand
 * from D10/D30/D60, not stored — avoids data duplication and rounding drift.
 */
data class SoilNodeData(
    // Context
    val siteId          : String   = "",
    val recordedAt      : Long     = System.currentTimeMillis(),

    // Required — grain size distribution
    val d10             : Double,   // mm
    val d30             : Double,   // mm
    val d60             : Double,   // mm

    // Required — index properties
    val voidRatio       : Double,   // e, dimensionless
    val soilType        : SoilType,

    // Optional — index properties
    val moistureContent : Double?  = null,  // ω, %
    val specificGravity : Double?  = null,  // Gs
    val dryDensity      : Double?  = null,  // γd, kN/m³

    // Edge-computed math
    val calculatedKValue: Double?  = null,
) {
    /** Cu = D60 / D10  — well-graded if Cu > 4 (gravel) or > 6 (sand). */
    val coefficientOfUniformity: Double get() = if (d10 > 0.0) d60 / d10 else 0.0

    /** Cc = D30² / (D60 × D10)  — well-graded if 1 ≤ Cc ≤ 3. */
    val coefficientOfCurvature: Double  get() =
        if (d10 > 0.0 && d60 > 0.0) (d30 * d30) / (d60 * d10) else 0.0
}
