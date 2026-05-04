package com.terrasync.app.presentation.addnodeform

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrasync.app.data.remote.GeotechInferenceRepository
import com.terrasync.app.domain.model.GeminiInference
import com.terrasync.app.domain.model.SoilNode
import com.terrasync.app.domain.model.SoilNodeData
import com.terrasync.app.domain.model.SoilType
import com.terrasync.app.domain.repository.NodeRepository
import com.terrasync.app.domain.repository.SiteRepository
import com.terrasync.app.presentation.navigation.Screen
import com.terrasync.app.domain.usecase.PermeabilityResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "AddNodeFormVM"

// ── Form State ────────────────────────────────────────────────────────────────

/**
 * Represents every field in the Add Node form as raw [String] values.
 * Raw strings preserve mid-entry states like "1." without premature parsing.
 * The ViewModel converts to [Double] only on submit.
 */
data class NodeFormState(
    val d10             : String  = "",
    val d30             : String  = "",
    val d60             : String  = "",
    val voidRatio       : String  = "",
    val soilType        : SoilType? = null,
    val moistureContent : String  = "",
    val specificGravity : String  = "",
    val dryDensity      : String  = "",
    val fieldErrors     : Map<NodeField, String> = emptyMap(),
)

/** Identifies each form field for per-field error display. */
enum class NodeField {
    D10, D30, D60, VOID_RATIO, SOIL_TYPE,
    MOISTURE_CONTENT, SPECIFIC_GRAVITY, DRY_DENSITY,
}

// ── Inference State ───────────────────────────────────────────────────────────

/**
 * Separate from [NodeFormState] so the form stays visible while Gemini processes.
 * The UI collapses the form and shows the result card on [InferenceState.Result].
 */
sealed interface InferenceState {
    data object Idle    : InferenceState
    data object Loading : InferenceState
    data class  Result(val data: GeminiInference) : InferenceState
    data class  Error(val message: String)         : InferenceState
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class AddNodeFormViewModel @Inject constructor(
    @Suppress("unused") private val siteRepository: SiteRepository,
    private val inferenceRepo: GeotechInferenceRepository,
    private val nodeRepository: NodeRepository,
    private val calculatePermeabilityUseCase: com.terrasync.app.domain.usecase.CalculatePermeabilityUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val siteId: String = checkNotNull(savedStateHandle[Screen.AddNodeForm.ARG_SITE_ID])
    val nodeId: String? = savedStateHandle[Screen.AddNodeForm.ARG_NODE_ID]

    private val _siteName = MutableStateFlow<String?>(null)
    val siteName: StateFlow<String?> = _siteName.asStateFlow()

    // Event to signal the UI to navigate back after successful save
    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess = _saveSuccess.asSharedFlow()

    private val _formState      = MutableStateFlow(NodeFormState())
    val formState: StateFlow<NodeFormState> = _formState.asStateFlow()

    // Real-time validation flow
    val fieldErrors: StateFlow<Map<NodeField, String>> = _formState
        .map { validate(it) }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _inferenceState = MutableStateFlow<InferenceState>(InferenceState.Idle)
    val inferenceState: StateFlow<InferenceState> = _inferenceState.asStateFlow()

    // ── Edit mode tracking ────────────────────────────────────────────────────

    val isEditMode: Boolean get() = nodeId != null

    /** Snapshot of the form state as loaded from Firestore. Null in create mode. */
    private var originalFormState: NodeFormState? = null

    /** True when the displayed inference came from Firestore cache, not a fresh API call. */
    private var inferenceIsFromCache = false

    /** True once the user manually triggers Gemini in this session. */
    private val _geminiRunThisSession = MutableStateFlow(false)
    /** Exposed so the UI can update the button label even in create mode. */
    val geminiRunThisSession: StateFlow<Boolean> = _geminiRunThisSession.asStateFlow()

    /** True when any soil parameter differs from the originally loaded values (edit mode only). */
    val hasSoilParamsChanged: StateFlow<Boolean> = _formState
        .map { c ->
            val o = originalFormState ?: return@map false
            c.d10 != o.d10 || c.d30 != o.d30 || c.d60 != o.d60 ||
            c.voidRatio != o.voidRatio || c.soilType != o.soilType ||
            c.moistureContent != o.moistureContent ||
            c.specificGravity != o.specificGravity || c.dryDensity != o.dryDensity
        }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    /** True when there is something worth saving: param changes OR a fresh Gemini result. */
    val hasUnsavedChanges: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(
        hasSoilParamsChanged, _geminiRunThisSession
    ) { paramsChanged, geminiRun -> paramsChanged || geminiRun }
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), false)

    // All properties declared above; init blocks below are safe to reference any of them.
    init {
        // ── Load site name + existing node data (edit mode) ───────────────────
        viewModelScope.launch {
            siteRepository.getSiteById(siteId).onSuccess { site ->
                _siteName.value = site.name
            }
            if (nodeId != null) {
                nodeRepository.getNodeById(siteId, nodeId).onSuccess { node ->
                    val loaded = NodeFormState(
                        d10             = node.d10.toString(),
                        d30             = node.d30.toString(),
                        d60             = node.d60.toString(),
                        voidRatio       = node.voidRatio.toString(),
                        soilType        = SoilType.entries.find { it.symbol == node.soilTypeSymbol },
                        moistureContent = node.moistureContent?.toString() ?: "",
                        specificGravity = node.specificGravity?.toString() ?: "",
                        dryDensity      = node.dryDensity?.toString() ?: "",
                    )
                    _formState.value  = loaded
                    originalFormState = loaded   // snapshot for diff tracking

                    // Restore cached Gemini result — only riskLevel needs to be valid
                    val rl = node.riskLevel
                    val rs = node.reinforcementSuggestions
                    if (!rl.isNullOrBlank() && !rl.equals("Unknown", ignoreCase = true)) {
                        _inferenceState.value = InferenceState.Result(
                            GeminiInference(
                                riskLevel                = rl,
                                reinforcementSuggestions = rs ?: "",
                            )
                        )
                        inferenceIsFromCache = true
                    }
                }
            }
        }

        // ── Invalidate stale cached inference when soil params change ─────────
        viewModelScope.launch {
            hasSoilParamsChanged.collect { changed ->
                if (changed && isEditMode && inferenceIsFromCache) {
                    _inferenceState.value = InferenceState.Idle
                    inferenceIsFromCache   = false
                }
            }
        }
    }


    // ── Field updates ─────────────────────────────────────────────────────────

    fun onD10Change(v: String)             = updateField { copy(d10 = v) }
    fun onD30Change(v: String)             = updateField { copy(d30 = v) }
    fun onD60Change(v: String)             = updateField { copy(d60 = v) }
    fun onVoidRatioChange(v: String)       = updateField { copy(voidRatio = v) }
    fun onSoilTypeChange(t: SoilType)      = updateField { copy(soilType = t) }
    fun onMoistureContentChange(v: String) = updateField { copy(moistureContent = v) }
    fun onSpecificGravityChange(v: String) = updateField { copy(specificGravity = v) }
    fun onDryDensityChange(v: String)      = updateField { copy(dryDensity = v) }

    // ── Saving & Analysis ─────────────────────────────────────────────────────

    fun saveNode() {
        val state  = _formState.value
        val errors = fieldErrors.value
        if (errors.isNotEmpty()) return
        val node = buildNodeData(state)
        logNode(node)

        viewModelScope.launch {
            if (isEditMode && hasSoilParamsChanged.value) {
                // Soil parameters changed → auto re-analyze before saving
                _inferenceState.value = InferenceState.Loading
                inferenceRepo.analyze(node)
                    .onSuccess { inference ->
                        Log.i(TAG, "Auto re-analysis: $inference")
                        _inferenceState.value    = InferenceState.Result(inference)
                        inferenceIsFromCache     = false
                        saveNodeToFirestore(node, inference)
                    }
                    .onFailure { cause ->
                        Log.e(TAG, "Auto re-analysis failed", cause)
                        _inferenceState.value = InferenceState.Error(
                            cause.localizedMessage ?: "Re-analysis failed. Check your connection."
                        )
                    }
            } else {
                // Create mode OR edit mode with no soil-param changes → save as-is
                val inference = (_inferenceState.value as? InferenceState.Result)?.data
                saveNodeToFirestore(node, inference)
            }
        }
    }

    fun analyzeWithGemini() {
        val state  = _formState.value
        val errors = fieldErrors.value
        if (errors.isNotEmpty()) return
        val node = buildNodeData(state)
        logNode(node)
        callGemini(node)
    }

    private fun buildNodeData(state: NodeFormState): SoilNodeData {
        val baseData = SoilNodeData(
            siteId          = siteId,
            d10             = state.d10.toDouble(),
            d30             = state.d30.toDouble(),
            d60             = state.d60.toDouble(),
            voidRatio       = state.voidRatio.toDouble(),
            soilType        = state.soilType!!,
            moistureContent = state.moistureContent.toDoubleOrNull(),
            specificGravity = state.specificGravity.toDoubleOrNull(),
            dryDensity      = state.dryDensity.toDoubleOrNull(),
        )
        val result: PermeabilityResult = calculatePermeabilityUseCase(baseData)
        return baseData.copy(calculatedKValue = result.kValue)
    }

    /** Returns the edge-computed result for the current form state, if valid. */
    fun computeLocalResult(state: NodeFormState): PermeabilityResult? {
        if (!isFormValid(state)) return null
        return calculatePermeabilityUseCase(buildNodeData(state))
    }

    fun dismissError() { _inferenceState.value = InferenceState.Idle }

    fun resetInference() { _inferenceState.value = InferenceState.Idle }

    // ── Gemini call ───────────────────────────────────────────────────────────

    private fun callGemini(node: SoilNodeData) {
        viewModelScope.launch {
            _inferenceState.value = InferenceState.Loading
            inferenceRepo.analyze(node)
                .onSuccess { inference ->
                    Log.i(TAG, "Gemini inference: $inference")
                    _inferenceState.value    = InferenceState.Result(inference)
                    inferenceIsFromCache     = false
                    _geminiRunThisSession.value = true
                }
                .onFailure { cause ->
                    Log.e(TAG, "Gemini inference failed", cause)
                    _inferenceState.value = InferenceState.Error(
                        cause.localizedMessage ?: "AI analysis failed. Check your API key."
                    )
                }
        }
    }

    private suspend fun saveNodeToFirestore(node: SoilNodeData, inference: GeminiInference?) {
        val soilNode = SoilNode(
            id                = nodeId ?: "",
            siteId            = node.siteId,
            recordedAt        = node.recordedAt,
            soilTypeLabel     = node.soilType.label,
            soilTypeSymbol    = node.soilType.symbol,
            d10               = node.d10,
            d30               = node.d30,
            d60               = node.d60,
            voidRatio         = node.voidRatio,
            moistureContent   = node.moistureContent,
            specificGravity   = node.specificGravity,
            dryDensity        = node.dryDensity,
            permeabilityClass = null,
            estimatedKValue   = null,
            riskInsights      = null,
            calculatedKValue  = node.calculatedKValue,
            // Gemini risk overrides local baseline; if no Gemini run, use local classification
            riskLevel         = inference?.riskLevel
                ?: calculatePermeabilityUseCase(node).riskLevel.label,
            reinforcementSuggestions = inference?.reinforcementSuggestions ?: "",
        )
        
        val result = if (nodeId != null) {
            nodeRepository.updateNode(soilNode)
        } else {
            nodeRepository.saveNode(soilNode).map { }
        }

        result.onSuccess {
            Log.i(TAG, "Node persisted to Firestore")
            _saveSuccess.emit(Unit)
        }.onFailure { cause ->
            Log.e(TAG, "Firestore write failed", cause)
            _inferenceState.value = InferenceState.Error("Failed to save to database.")
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate(s: NodeFormState): Map<NodeField, String> {
        val errors = mutableMapOf<NodeField, String>()

        val d10 = s.d10.toDoubleOrNull()
        when {
            s.d10.isBlank() -> errors[NodeField.D10] = "D10 is required."
            d10 == null     -> errors[NodeField.D10] = "Enter a valid decimal number."
            d10 <= 0.0      -> errors[NodeField.D10] = "D10 must be > 0 mm."
        }
        val d30 = s.d30.toDoubleOrNull()
        when {
            s.d30.isBlank()          -> errors[NodeField.D30] = "D30 is required."
            d30 == null              -> errors[NodeField.D30] = "Enter a valid decimal number."
            d30 <= 0.0               -> errors[NodeField.D30] = "D30 must be > 0 mm."
            d10 != null && d30 < d10 -> errors[NodeField.D30] = "D30 must be ≥ D10 (${s.d10} mm)."
        }
        val d60 = s.d60.toDoubleOrNull()
        when {
            s.d60.isBlank()          -> errors[NodeField.D60] = "D60 is required."
            d60 == null              -> errors[NodeField.D60] = "Enter a valid decimal number."
            d60 <= 0.0               -> errors[NodeField.D60] = "D60 must be > 0 mm."
            d30 != null && d60 < d30 -> errors[NodeField.D60] = "D60 must be ≥ D30 (${s.d30} mm)."
        }
        val e = s.voidRatio.toDoubleOrNull()
        when {
            s.voidRatio.isBlank() -> errors[NodeField.VOID_RATIO] = "Void ratio (e) is required."
            e == null             -> errors[NodeField.VOID_RATIO] = "Enter a valid decimal number."
            e <= 0.0              -> errors[NodeField.VOID_RATIO] = "Void ratio must be > 0."
            e > 10.0              -> errors[NodeField.VOID_RATIO] = "Void ratio > 10 is physically implausible."
        }
        if (s.soilType == null) errors[NodeField.SOIL_TYPE] = "Select a soil type."

        s.moistureContent.toDoubleOrNull()?.let { mc ->
            if (mc < 0.0)   errors[NodeField.MOISTURE_CONTENT] = "Cannot be negative."
            if (mc > 500.0) errors[NodeField.MOISTURE_CONTENT] = "Value > 500% is implausible."
        } ?: run {
            if (s.moistureContent.isNotBlank()) errors[NodeField.MOISTURE_CONTENT] = "Enter a valid number."
        }

        s.specificGravity.toDoubleOrNull()?.let { gs ->
            if (gs < 2.0 || gs > 3.0) errors[NodeField.SPECIFIC_GRAVITY] = "Gs is typically 2.0–3.0."
        } ?: run {
            if (s.specificGravity.isNotBlank()) errors[NodeField.SPECIFIC_GRAVITY] = "Enter a valid number."
        }

        s.dryDensity.toDoubleOrNull()?.let { γd ->
            if (γd <= 0.0) errors[NodeField.DRY_DENSITY] = "Must be > 0."
            if (γd > 25.0) errors[NodeField.DRY_DENSITY] = "Value > 25 kN/m³ is implausible."
        } ?: run {
            if (s.dryDensity.isNotBlank()) errors[NodeField.DRY_DENSITY] = "Enter a valid number."
        }

        return errors
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun updateField(transform: NodeFormState.() -> NodeFormState) =
        _formState.update { it.transform() }

    fun isFormValid(state: NodeFormState): Boolean {
        val d10 = state.d10.toDoubleOrNull()
        val d30 = state.d30.toDoubleOrNull()
        val d60 = state.d60.toDoubleOrNull()
        val e   = state.voidRatio.toDoubleOrNull()
        return d10 != null && d10 > 0.0
            && d30 != null && d30 > 0.0 && d30 >= d10
            && d60 != null && d60 > 0.0 && d60 >= d30
            && e   != null && e   > 0.0 && e <= 10.0
            && state.soilType != null
    }

    private fun logNode(node: SoilNodeData) {
        Log.i(TAG, "═══════════════════════════════════════════")
        Log.i(TAG, "  SOIL NODE — Site: ${node.siteId}")
        Log.i(TAG, "  Soil Type      : ${node.soilType.label} (${node.soilType.symbol})")
        Log.i(TAG, "  D10/D30/D60    : ${node.d10}/${node.d30}/${node.d60} mm")
        Log.i(TAG, "  Cu / Cc        : ${"%.3f".format(node.coefficientOfUniformity)} / ${"%.3f".format(node.coefficientOfCurvature)}")
        Log.i(TAG, "  Void Ratio (e) : ${node.voidRatio}")
        Log.i(TAG, "  Moisture (ω)   : ${node.moistureContent ?: "—"} %")
        Log.i(TAG, "  Spec. Gravity  : ${node.specificGravity ?: "—"}")
        Log.i(TAG, "  Dry Density γd : ${node.dryDensity ?: "—"} kN/m³")
        Log.i(TAG, "═══════════════════════════════════════════")
    }
}
