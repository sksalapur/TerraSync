package com.terrasync.app.presentation.addnodeform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.log10

@Composable
fun LocalAnalysisCard(state: NodeFormState) {
    val d10 = state.d10.toDoubleOrNull()
    val d30 = state.d30.toDoubleOrNull()
    val d60 = state.d60.toDoubleOrNull()
    val e   = state.voidRatio.toDoubleOrNull() ?: return
    val w   = state.moistureContent.toDoubleOrNull()?.div(100.0) // % to decimal
    val gs  = state.specificGravity.toDoubleOrNull()
    val yd  = state.dryDensity.toDoubleOrNull()

    // Derived parameters
    val n = e / (1.0 + e)
    val sRaw = if (w != null && gs != null) (w * gs) / e else null
    val s = sRaw?.coerceIn(0.0, 1.0)
    
    val bulkDensity = if (yd != null && w != null) yd * (1.0 + w) else null
    val satDensity  = if (gs != null) ((gs + e) / (1.0 + e)) * 9.81 else null // assuming water density ~9.81 kN/m3

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF252220))
            .border(1.dp, Color(0xFF3A3330), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Local Geotechnical Analysis",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFEAE1D9)
            )
        )

        // ── Derived Parameters Grid ──────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ParamBox("Porosity (n)", "%.1f %%".format(n * 100), Modifier.weight(1f))
            ParamBox("Saturation (S)", s?.let { "%.1f %%".format(it * 100) } ?: "—", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ParamBox("Bulk γ", bulkDensity?.let { "%.2f kN/m³".format(it) } ?: "—", Modifier.weight(1f))
            ParamBox("Sat. γ", satDensity?.let { "%.2f kN/m³".format(it) } ?: "—", Modifier.weight(1f))
        }

        HorizontalDivider(color = Color(0xFF3A3330))

        // ── Phase Diagram & PSD Curve ────────────────────────────────────────
        Text(
            "Volumetric Phase Diagram",
            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF9E8D82))
        )
        PhaseDiagramBlock(e = e, s = s ?: 0.0)

        if (d10 != null && d30 != null && d60 != null && d10 > 0 && d60 > 0) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF3A3330))
            Text(
                "Particle Size Distribution (PSD)",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF9E8D82))
            )
            PsdCurveGraph(d10 = d10, d30 = d30, d60 = d60)
        }
    }
}

@Composable
private fun ParamBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1817), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFEAE1D9), fontWeight = FontWeight.SemiBold))
    }
}

/**
 * Renders a 3-phase soil block diagram (Air, Water, Solid).
 */
@Composable
private fun PhaseDiagramBlock(e: Double, s: Double) {
    val vSolid = 1.0
    val vWater = e * s
    val vAir   = e * (1.0 - s)
    val vTotal = vSolid + vWater + vAir

    val pSolid = (vSolid / vTotal).toFloat()
    val pWater = (vWater / vTotal).toFloat()
    val pAir   = (vAir / vTotal).toFloat()

    Row(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        // Labels
        Column(
            modifier = Modifier.weight(0.3f).height(160.dp),
            verticalArrangement = Arrangement.Center
        ) {
            if (pAir > 0.01f) PhaseLabel("Air", pAir)
            if (pWater > 0.01f) PhaseLabel("Water", pWater)
            PhaseLabel("Solid", pSolid)
        }
        Spacer(Modifier.width(8.dp))
        // Blocks
        Column(
            modifier = Modifier
                .weight(0.7f)
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF4A413B), RoundedCornerShape(8.dp))
        ) {
            if (pAir > 0.01f) Box(Modifier.fillMaxWidth().weight(pAir).background(Color(0xFF8BA6B5)))
            if (pWater > 0.01f) Box(Modifier.fillMaxWidth().weight(pWater).background(Color(0xFF4A90E2)))
            Box(Modifier.fillMaxWidth().weight(pSolid).background(Color(0xFF8D6E63)))
        }
    }
}

@Composable
private fun PhaseLabel(name: String, proportion: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(160.dp * proportion), contentAlignment = Alignment.CenterEnd) {
        Text(name, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
    }
}

/**
 * Draws a semi-logarithmic PSD curve based on D10, D30, D60.
 */
@Composable
private fun PsdCurveGraph(d10: Double, d30: Double, d60: Double) {
    val points = listOf(
        Pair(d10, 0.10f),
        Pair(d30, 0.30f),
        Pair(d60, 0.60f)
    )
    val minD = log10(0.01) // 0.01 mm
    val maxD = log10(100.0) // 100 mm

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1A1817))
            .border(1.dp, Color(0xFF2E2B28), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Grid lines
            for (i in 1..4) {
                val y = h - (h * (i * 0.25f))
                drawLine(Color(0xFF2E2B28), Offset(0f, y), Offset(w, y), 1f)
            }

            val path = Path()
            points.forEachIndexed { index, (d, pct) ->
                val logD = log10(d)
                val normalizedX = ((logD - minD) / (maxD - minD)).toFloat().coerceIn(0f, 1f)
                val px = w * (1f - normalizedX) // Log scales usually go right-to-left in geotechnical graphs (finer to the right) Wait, standard is coarser to left? Actually coarser to the left (100 -> 0.01).
                val pxCorrected = w * (1f - normalizedX) // larger particle sizes on left.

                val py = h - (h * pct)

                if (index == 0) path.moveTo(pxCorrected, py) else path.lineTo(pxCorrected, py)
                drawCircle(Color(0xFFD97040), radius = 6f, center = Offset(pxCorrected, py))
            }
            
            drawPath(
                path = path,
                color = Color(0xFFD97040),
                style = Stroke(width = 3f, cap = StrokeCap.Round, pathEffect = PathEffect.cornerPathEffect(20f))
            )
        }
    }
}
