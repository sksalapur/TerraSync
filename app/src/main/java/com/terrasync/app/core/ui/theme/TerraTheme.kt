package com.terrasync.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Palette ───────────────────────────────────────────────────────────────────
// Earthy, geotechnical-inspired tones: clay terracotta + dark slate.

private val TerraOrange    = Color(0xFFD97040)   // primary — burnt clay
private val TerraOrangeVar = Color(0xFFF0956A)   // primary variant
private val TerraDark      = Color(0xFF1C1B1B)   // background dark
private val TerraSurface   = Color(0xFF2A2927)   // card surface
private val TerraContainer = Color(0xFF3D3A36)   // elevated surface
private val TerraOnPrimary = Color(0xFFFFFFFF)
private val TerraError     = Color(0xFFCF6679)
private val TerraOnSurface = Color(0xFFEAE1D9)
private val TerraOnBg      = Color(0xFFDDD4CB)
private val TerraOutline   = Color(0xFF6E635A)

private val DarkColors = darkColorScheme(
    primary          = TerraOrange,
    onPrimary        = TerraOnPrimary,
    primaryContainer = Color(0xFF7A3B1E),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary        = Color(0xFFB8A89A),
    onSecondary      = Color(0xFF2E211A),
    background       = TerraDark,
    onBackground     = TerraOnBg,
    surface          = TerraSurface,
    onSurface        = TerraOnSurface,
    surfaceVariant   = TerraContainer,
    onSurfaceVariant = Color(0xFFCFBFB5),
    outline          = TerraOutline,
    error            = TerraError,
    onError          = Color(0xFF680020),
)

@Composable
fun TerraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content,
    )
}
