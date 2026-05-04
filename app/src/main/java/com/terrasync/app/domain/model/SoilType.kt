package com.terrasync.app.domain.model

/**
 * Soil classification types per USCS (Unified Soil Classification System).
 * Used as the dropdown values in [SoilNodeData].
 *
 * @param label  Human-readable display label shown in the UI dropdown.
 * @param symbol USCS symbol for engineering reports.
 */
enum class SoilType(val label: String, val symbol: String) {
    GRAVEL("Gravel", "GW/GP"),
    SAND("Sand",     "SW/SP"),
    SILT("Silt",     "ML/MH"),
    CLAY("Clay",     "CL/CH"),
}
