package com.terrasync.app.presentation.addnodeform

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.terrasync.app.domain.model.GeminiInference
import com.terrasync.app.domain.model.RiskLevel
import com.terrasync.app.domain.model.SoilType
import com.terrasync.app.domain.usecase.PermeabilityResult

/**
 * Add Node Form screen — collects geotechnical soil data and fires Gemini AI analysis.
 *
 * State machine:
 *  - [InferenceState.Idle]    → form fully interactive, Submit enabled when valid
 *  - [InferenceState.Loading] → pulsing overlay shown, form inputs remain scrollable
 *  - [InferenceState.Result]  → result card animates in above the form
 *  - [InferenceState.Error]   → snackbar shown, form re-enabled for retry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNodeFormScreen(
    onBack: () -> Unit,
    viewModel: AddNodeFormViewModel = hiltViewModel(),
) {
    val state          by viewModel.formState.collectAsStateWithLifecycle()
    val fieldErrors    by viewModel.fieldErrors.collectAsStateWithLifecycle()
    val inferenceState by viewModel.inferenceState.collectAsStateWithLifecycle()
    val siteName       by viewModel.siteName.collectAsStateWithLifecycle()
    val snackbarHost   = remember { SnackbarHostState() }

    // Error snackbar
    LaunchedEffect(inferenceState) {
        if (inferenceState is InferenceState.Error) {
            snackbarHost.showSnackbar(
                message  = (inferenceState as InferenceState.Error).message,
                duration = SnackbarDuration.Long,
            )
            viewModel.dismissError()
        }
    }

    // Success navigation
    LaunchedEffect(Unit) {
        viewModel.saveSuccess.collect {
            onBack()
        }
    }

    val isLoading = inferenceState is InferenceState.Loading

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = Color(0xFF3D1A1E),
                    contentColor    = Color(0xFFFFB3BA),
                    shape           = RoundedCornerShape(12.dp),
                    modifier        = Modifier.padding(16.dp),
                )
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Add Soil Node",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color      = Color(0xFFEAE1D9),
                            ),
                        )
                        Text(
                            siteName ?: "Site: ${viewModel.siteId.take(8)}…",
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isLoading) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isLoading) Color(0xFF6E635A) else Color(0xFFD97040),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1B1B)),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1C1B1B), Color(0xFF221E1B)),
                        start  = Offset.Zero,
                        end    = Offset(0f, Float.POSITIVE_INFINITY),
                    )
                )
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {

                // ── Gemini Result Card (slides in when result arrives) ────────
                AnimatedVisibility(
                    visible = inferenceState is InferenceState.Result,
                    enter   = fadeIn(tween(400)) + slideInVertically { -it / 3 },
                    exit    = fadeOut(tween(200)),
                ) {
                    if (inferenceState is InferenceState.Result) {
                        InferenceResultCard(
                            inference = (inferenceState as InferenceState.Result).data,
                            onDismiss = viewModel::resetInference,
                        )
                    }
                }

                // ── Section 1 : Grain Size Distribution ─────────────────────
                FormSection(title = "Grain Size Distribution", subtitle = "All values required · mm") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NodeTextField(
                            label = "D10 (mm)", value = state.d10,
                            onValueChange = viewModel::onD10Change,
                            error = fieldErrors[NodeField.D10], hint = "e.g. 0.08",
                            modifier = Modifier.weight(1f), enabled = !isLoading,
                        )
                        NodeTextField(
                            label = "D30 (mm)", value = state.d30,
                            onValueChange = viewModel::onD30Change,
                            error = fieldErrors[NodeField.D30], hint = "e.g. 0.25",
                            modifier = Modifier.weight(1f), enabled = !isLoading,
                        )
                    }
                    NodeTextField(
                        label = "D60 (mm)", value = state.d60,
                        onValueChange = viewModel::onD60Change,
                        error = fieldErrors[NodeField.D60], hint = "e.g. 0.65",
                        modifier = Modifier.fillMaxWidth(0.5f), enabled = !isLoading,
                    )
                    // Live Cu/Cc preview
                    val d10 = state.d10.toDoubleOrNull()
                    val d30 = state.d30.toDoubleOrNull()
                    val d60 = state.d60.toDoubleOrNull()
                    if (d10 != null && d30 != null && d60 != null && d10 > 0 && d60 > 0) {
                        DerivedBadgeRow(cu = d60 / d10, cc = (d30 * d30) / (d60 * d10))
                    }
                }

                // ── Section 2 : Index Properties ────────────────────────────
                FormSection(title = "Index Properties", subtitle = "Required") {
                    NodeTextField(
                        label = "Void Ratio (e)", value = state.voidRatio,
                        onValueChange = viewModel::onVoidRatioChange,
                        error = fieldErrors[NodeField.VOID_RATIO],
                        hint = "e.g. 0.65  (0–10)", modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                    )
                    SoilTypeDropdown(
                        selected = state.soilType,
                        onSelect = viewModel::onSoilTypeChange,
                        error    = fieldErrors[NodeField.SOIL_TYPE],
                        enabled  = !isLoading,
                    )
                }

                // ── Section 3 : Optional Parameters ─────────────────────────
                FormSection(title = "Optional Parameters", subtitle = "Leave blank to skip") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NodeTextField(
                            label = "Moisture (ω, %)", value = state.moistureContent,
                            onValueChange = viewModel::onMoistureContentChange,
                            error = fieldErrors[NodeField.MOISTURE_CONTENT],
                            hint = "e.g. 24.5", modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next, enabled = !isLoading,
                        )
                        NodeTextField(
                            label = "Spec. Gravity (Gs)", value = state.specificGravity,
                            onValueChange = viewModel::onSpecificGravityChange,
                            error = fieldErrors[NodeField.SPECIFIC_GRAVITY],
                            hint = "2.60–2.80", modifier = Modifier.weight(1f),
                            imeAction = ImeAction.Next, enabled = !isLoading,
                        )
                    }
                    NodeTextField(
                        label = "Dry Density (γd, kN/m³)", value = state.dryDensity,
                        onValueChange = viewModel::onDryDensityChange,
                        error = fieldErrors[NodeField.DRY_DENSITY],
                        hint = "e.g. 16.5", modifier = Modifier.fillMaxWidth(0.5f),
                        imeAction = ImeAction.Done, enabled = !isLoading,
                    )
                }

                // ── Local Analysis ─────────────────────────────────────────────────────
                val isValid              = viewModel.isFormValid(state) && !isLoading
                val isEditMode           = viewModel.isEditMode
                val hasUnsavedChanges   by viewModel.hasUnsavedChanges.collectAsStateWithLifecycle()
                val hasSoilParamsChanged by viewModel.hasSoilParamsChanged.collectAsStateWithLifecycle()

                // ── Derivation Card (Math Walkthrough) ────────────────────────
                val localResult = if (isValid) viewModel.computeLocalResult(state) else null
                var showDerivation by rememberSaveable { mutableStateOf(false) }
                if (localResult != null) {
                    CalculationDerivationCard(
                        state        = state,
                        result       = localResult,
                        expanded     = showDerivation,
                        onToggle     = { showDerivation = !showDerivation },
                    )
                }
                var showLocalVisuals by rememberSaveable { mutableStateOf(false) }

                AnimatedVisibility(
                    visible = showLocalVisuals && isValid,
                    enter   = fadeIn() + androidx.compose.animation.expandVertically(),
                    exit    = fadeOut() + androidx.compose.animation.shrinkVertically(),
                ) {
                    LocalAnalysisCard(state = state)
                }

                androidx.compose.material3.Button(
                    onClick  = { showLocalVisuals = !showLocalVisuals },
                    enabled  = isValid,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF2A2927),
                        contentColor           = Color(0xFFD97040),
                        disabledContainerColor = Color(0xFF1A1817),
                        disabledContentColor   = Color(0xFF4A413B),
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    androidx.compose.material3.Text(
                        text = if (showLocalVisuals) "Hide Visualizations" else "Calculate & Visualize (Local)",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Analyze with Gemini Button ────────────────────────────────────────────
                
                Button(
                    onClick  = viewModel::analyzeWithGemini,
                    enabled  = isValid,
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = Color(0xFF3A3330), // Darker, less prominent than save
                        contentColor           = Color(0xFFEAE1D9),
                        disabledContainerColor = Color(0xFF1A1817),
                        disabledContentColor   = Color(0xFF4A413B),
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    AnimatedContent(
                        targetState = isLoading,
                        label       = "submit_btn_anim",
                        transitionSpec = {
                            (fadeIn(tween(200)) + scaleIn(tween(200))).togetherWith(
                                fadeOut(tween(150)) + scaleOut(tween(150))
                            )
                        },
                    ) { loading ->
                        if (loading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GeminiSpinner()
                                Spacer(Modifier.width(10.dp))
                                Text("Analyzing with Gemini AI…", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text("Analyze with Gemini AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Primary Action Button (mode-aware) ───────────────────────
                val geminiRanThisSession by viewModel.geminiRunThisSession.collectAsStateWithLifecycle()
                val showPrimaryButton = !isEditMode || hasUnsavedChanges
                if (showPrimaryButton) {
                    val tallButton = (isEditMode && hasSoilParamsChanged) ||
                                     (!isEditMode && geminiRanThisSession)
                    Button(
                        onClick  = viewModel::saveNode,
                        enabled  = isValid,
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color(0xFFD97040),
                            contentColor           = Color.White,
                            disabledContainerColor = Color(0xFF4A3020),
                            disabledContentColor   = Color(0xFF7A6050),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (tallButton) 62.dp else 54.dp),
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else when {
                            // Edit mode: params changed → auto re-analyze
                            isEditMode && hasSoilParamsChanged -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Update Node", fontWeight = FontWeight.Bold)
                                Text(
                                    "Gemini will re-analyze automatically",
                                    fontSize = 10.sp,
                                    color    = Color.White.copy(alpha = 0.75f),
                                )
                            }
                            // Edit mode: only Gemini changed
                            isEditMode -> Text("Update Node", fontWeight = FontWeight.Bold)
                            // Create mode: Gemini was run
                            geminiRanThisSession -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Save with AI Analysis", fontWeight = FontWeight.Bold)
                                Text(
                                    "AI assessment will be cached with this node",
                                    fontSize = 10.sp,
                                    color    = Color.White.copy(alpha = 0.75f),
                                )
                            }
                            // Create mode: no Gemini
                            else -> Text("Add Node", fontWeight = FontWeight.Bold)
                        }
                    }
                }


                Spacer(Modifier.height(24.dp))

            }
        }
    }
}

// ── Inference Result Card ─────────────────────────────────────────────────────

@Composable
private fun InferenceResultCard(
    inference : GeminiInference,
    onDismiss : () -> Unit,
) {
    val pClass  = inference.riskLevelEnum
    val tint    = Color(pClass.colorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E2A1E))
            .border(1.dp, tint.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Gemini AI Assessment",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFEAE1D9)),
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = Color(0xFF9E8D82), style = MaterialTheme.typography.labelSmall)
            }
        }

        HorizontalDivider(color = tint.copy(alpha = 0.25f))

        // Risk Level badge
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    "${pClass.label} Risk Level",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color      = tint,
                    ),
                )
            }
        }

        // Risk insights
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Reinforcement Suggestions",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)),
            )
            Text(
                inference.reinforcementSuggestions,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = Color(0xFFDAD0C8),
                    lineHeight = 20.sp,
                ),
            )
        }
    }
}

// ── Pulsing Gemini Spinner ────────────────────────────────────────────────────

@Composable
private fun GeminiSpinner() {
    val transition = rememberInfiniteTransition(label = "gemini_spin")
    val angle by transition.animateFloat(
        initialValue   = 0f,
        targetValue    = 360f,
        animationSpec  = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label          = "spin_angle",
    )
    CircularProgressIndicator(
        modifier    = Modifier.size(18.dp).rotate(angle),
        color       = Color.White,
        strokeWidth = 2.dp,
        trackColor  = Color.White.copy(alpha = 0.25f),
    )
}

// ── Shared Sub-components ─────────────────────────────────────────────────────

@Composable
private fun FormSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF252220))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold, color = Color(0xFFD97040)))
            Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
        }
        HorizontalDivider(color = Color(0xFF3A3330))
        content()
    }
}

@Composable
private fun NodeTextField(
    label: String, value: String, onValueChange: (String) -> Unit,
    error: String?, hint: String, modifier: Modifier = Modifier,
    imeAction: ImeAction = ImeAction.Next, enabled: Boolean = true,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            label = { Text(label, fontSize = 12.sp) },
            placeholder = { Text(hint, fontSize = 12.sp, color = Color(0xFF6E635A)) },
            isError = error != null, singleLine = true, enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
            shape = RoundedCornerShape(10.dp),
            colors = nodeFieldColors(error != null),
            modifier = Modifier.fillMaxWidth(),
        )
        AnimatedVisibility(visible = error != null, enter = fadeIn(tween(180)) + scaleIn(tween(180), 0.95f)) {
            Text(error.orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCF6679)),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
private fun SoilTypeDropdown(selected: SoilType?, onSelect: (SoilType) -> Unit, error: String?, enabled: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(if (enabled) Color(0xFF1F1D1B) else Color(0xFF181615))
                .border(1.dp,
                    when { error != null -> Color(0xFFCF6679); expanded -> Color(0xFFD97040); else -> Color(0xFF4A413B) },
                    RoundedCornerShape(10.dp))
                .clickable(enabled = enabled) { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Soil Type", style = MaterialTheme.typography.labelSmall.copy(
                        color = if (expanded) Color(0xFFD97040) else Color(0xFF9E8D82)))
                    Text(selected?.let { "${it.label}  (${it.symbol})" } ?: "Select classification…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (selected != null) Color(0xFFEAE1D9) else Color(0xFF6E635A)))
                }
                Icon(Icons.Default.ArrowDropDown, null, tint = Color(0xFF9E8D82))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF2A2927))) {
            SoilType.entries.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(type.label, style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFEAE1D9),
                                fontWeight = if (type == selected) FontWeight.Bold else FontWeight.Normal))
                            Text("USCS: ${type.symbol}", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
                        }
                    },
                    onClick = { onSelect(type); expanded = false },
                    modifier = if (type == selected) Modifier.background(Color(0xFF3D3530)) else Modifier,
                )
            }
        }
        AnimatedVisibility(visible = error != null, enter = fadeIn(tween(180))) {
            Text(error.orEmpty(), style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFCF6679)),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp))
        }
    }
}

@Composable
private fun DerivedBadgeRow(cu: Double, cc: Double) {
    val cuOk = cu >= 4.0; val ccOk = cc in 1.0..3.0
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF1A1817)).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Cu = ", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
        Text("%.2f".format(cu), style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold, color = if (cuOk) Color(0xFF6BAF72) else Color(0xFFD4A843)))
        Text("·", color = Color(0xFF6E635A))
        Text("Cc = ", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)))
        Text("%.2f".format(cc), style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold, color = if (ccOk) Color(0xFF6BAF72) else Color(0xFFD4A843)))
        Spacer(Modifier.weight(1f))
        Text(if (cuOk && ccOk) "Well-graded ✓" else "Poorly-graded",
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (cuOk && ccOk) Color(0xFF6BAF72) else Color(0xFFD4A843)),
            textAlign = TextAlign.End)
    }
}

@Composable
private fun nodeFieldColors(isError: Boolean = false) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor      = if (isError) Color(0xFFCF6679) else Color(0xFFD97040),
    unfocusedBorderColor    = if (isError) Color(0xFFCF6679) else Color(0xFF4A413B),
    focusedLabelColor       = if (isError) Color(0xFFCF6679) else Color(0xFFD97040),
    unfocusedLabelColor     = Color(0xFF9E8D82),
    cursorColor             = Color(0xFFD97040),
    focusedTextColor        = Color(0xFFEAE1D9),
    unfocusedTextColor      = Color(0xFFEAE1D9),
    disabledTextColor       = Color(0xFF6E635A),
    disabledBorderColor     = Color(0xFF2E2B28),
    disabledLabelColor      = Color(0xFF5E5550),
    focusedContainerColor   = Color(0xFF252220),
    unfocusedContainerColor = Color(0xFF1F1D1B),
    errorLabelColor         = Color(0xFFCF6679),
    errorContainerColor     = Color(0xFF2A1A1C),
    errorTextColor          = Color(0xFFEAE1D9),
)

// ── Calculation Derivation Card ───────────────────────────────────────────────

/**
 * Expandable card showing the step-by-step derivation for Porosity (n) and
 * Permeability (k) using the actual values the user has entered in the form.
 * Collapsed by default to keep the form clean; tap to reveal the math.
 */
@Composable
private fun CalculationDerivationCard(
    state    : NodeFormState,
    result   : PermeabilityResult,
    expanded : Boolean,
    onToggle : () -> Unit,
) {
    val e   = state.voidRatio.toDoubleOrNull() ?: 0.0
    val d10 = state.d10.toDoubleOrNull()
    val isCoarse = state.soilType?.symbol?.let {
        it.startsWith("S") || it.startsWith("G")
    } ?: false

    val riskTint = Color(result.riskLevel.colorHex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F1D1B))
            .border(1.dp, Color(0xFF3A3330), RoundedCornerShape(14.dp))
    ) {
        // ── Header row (always visible) ──────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Science, contentDescription = null, tint = riskTint, modifier = Modifier.size(18.dp))
                Text(
                    "Edge Calculation Derivation",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFFEAE1D9),
                    ),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Local risk badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(riskTint.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        "${result.riskLevel.label} Risk",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color      = riskTint,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color(0xFF9E8D82),
                )
            }
        }

        // ── Derivation body (animated) ────────────────────────────────────────
        AnimatedVisibility(
            visible = expanded,
            enter   = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(250)),
            exit    = fadeOut(tween(150)) + androidx.compose.animation.shrinkVertically(tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HorizontalDivider(color = Color(0xFF2E2B28))

                // ── Porosity ─────────────────────────────────────────────────
                val n = e / (1.0 + e)
                DerivationBlock(
                    title    = "Porosity  (n)",
                    formula  = "n = e / (1 + e)",
                    pluggedIn = "n = ${state.voidRatio} / (1 + ${state.voidRatio})",
                    result   = "n = ${"%.4f".format(n)}  →  ${"%.1f".format(n * 100)} %",
                    note     = "Porosity represents the fraction of total volume occupied by voids.",
                )

                HorizontalDivider(color = Color(0xFF2E2B28))

                // ── Permeability ─────────────────────────────────────────────
                if (isCoarse && d10 != null) {
                    // Hazen's formula
                    DerivationBlock(
                        title    = "Permeability  (k)",
                        formula  = "k = C × D10²    (Hazen, C = 1.0)",
                        pluggedIn = "k = 1.0 × (${state.d10})²",
                        result   = "k ≈ ${"%.2E".format(result.kValue)} cm/s",
                        note     = "Hazen's formula is valid for D10 in 0.1–3 mm range and Cu < 5.",
                    )
                } else {
                    // Kozeny-Carman
                    val eVal = state.voidRatio
                    DerivationBlock(
                        title    = "Permeability  (k)",
                        formula  = "k = Cₖ × e³ / (1 + e)    (Kozeny-Carman, Cₖ = 0.001)",
                        pluggedIn = "k = 0.001 × ($eVal)³ / (1 + $eVal)",
                        result   = "k ≈ ${"%.2E".format(result.kValue)} cm/s",
                        note     = "Cₖ = 0.001 is a generic fine-grained baseline. In production this constant is calibrated per site.",
                    )
                }

                HorizontalDivider(color = Color(0xFF2E2B28))

                // ── Risk Classification ───────────────────────────────────────
                Text(
                    "Risk Classification",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9E8D82)),
                )
                Text(
                    when (result.riskLevel) {
                        RiskLevel.HIGH   -> "k > 1×10⁻² cm/s → HIGH risk. High permeability indicates rapid seepage and potential instability."
                        RiskLevel.MEDIUM -> "10⁻⁵ ≤ k ≤ 10⁻² cm/s → MEDIUM risk. Moderate drainage; monitor settlements."
                        RiskLevel.LOW    -> "k < 1×10⁻⁵ cm/s → LOW risk. Very low permeability; consolidation and drainage concerns."
                        else             -> "Classification unavailable."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = riskTint,
                        lineHeight = 18.sp,
                    ),
                )

                Text(
                    "⚠ This is a local baseline estimate. Run Gemini AI for a full expert assessment.",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color    = Color(0xFF6E635A),
                        fontSize = 10.sp,
                    ),
                )
            }
        }
    }
}

// ── Derivation Block ──────────────────────────────────────────────────────────

@Composable
private fun DerivationBlock(
    title     : String,
    formula   : String,
    pluggedIn : String,
    result    : String,
    note      : String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall.copy(
                color    = Color(0xFF9E8D82),
                fontSize = 11.sp,
            ),
        )
        // Formula
        Text(
            formula,
            style = MaterialTheme.typography.bodySmall.copy(
                color      = Color(0xFFD97040),
                fontWeight = FontWeight.SemiBold,
            ),
        )
        // Substituted values
        Text(
            pluggedIn,
            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFB0A090)),
        )
        // Computed result
        Text(
            result,
            style = MaterialTheme.typography.labelMedium.copy(
                color      = Color(0xFFEAE1D9),
                fontWeight = FontWeight.Bold,
            ),
        )
        if (note.isNotBlank()) {
            Text(
                note,
                style = MaterialTheme.typography.labelSmall.copy(
                    color    = Color(0xFF6E635A),
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

