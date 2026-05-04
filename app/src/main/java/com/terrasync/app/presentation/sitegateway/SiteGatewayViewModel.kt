package com.terrasync.app.presentation.sitegateway

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.terrasync.app.core.ui.UiState
import com.terrasync.app.domain.model.Site
import com.terrasync.app.domain.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class SiteGatewayViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    // ── Create-site state ─────────────────────────────────────────────────────
    private val _createState = MutableStateFlow<UiState<CreatedSiteResult>>(UiState.Idle)
    val createState: StateFlow<UiState<CreatedSiteResult>> = _createState.asStateFlow()

    // ── Join-site state ───────────────────────────────────────────────────────
    private val _joinState = MutableStateFlow<UiState<String>>(UiState.Idle)  // String = siteId
    val joinState: StateFlow<UiState<String>> = _joinState.asStateFlow()

    // ── Navigation events (one-shot) ──────────────────────────────────────────
    private val _navEvent = MutableSharedFlow<GatewayNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    // ── User Photo ─────────────────────────────────────────────────────────────
    val userPhotoUrl: String? = auth.currentUser?.photoUrl?.toString()

    // ── Create Site ───────────────────────────────────────────────────────────

    fun createSite(siteName: String) {
        val uid = auth.currentUser?.uid ?: run {
            _createState.value = UiState.Error("You must be logged in to create a site.")
            return
        }
        if (siteName.isBlank()) {
            _createState.value = UiState.Error("Site name cannot be empty.")
            return
        }
        viewModelScope.launch {
            _createState.value = UiState.Loading
            val code = generateInviteCode()
            val site = Site(
                name       = siteName.trim(),
                ownerId    = uid,
                adminId    = uid,
                inviteCode = code,
                memberIds  = listOf(uid),
                createdAt  = System.currentTimeMillis(),
            )
            siteRepository.createSite(site)
                .onSuccess { docId ->
                    _createState.value = UiState.Success(
                        CreatedSiteResult(siteId = docId, inviteCode = code)
                    )
                }
                .onFailure { cause ->
                    _createState.value = UiState.Error(
                        message = cause.localizedMessage ?: "Failed to create site.",
                        cause   = cause,
                    )
                }
        }
    }

    // ── Join Site (manual code entry) ─────────────────────────────────────────

    fun joinByCode(code: String) {
        if (code.length != 6) {
            _joinState.value = UiState.Error("Invite code must be exactly 6 characters.")
            return
        }
        lookupCode(code.uppercase())
    }

    // ── Join Site (QR scan result callback) ───────────────────────────────────

    // Removed onQrScanned

    private fun lookupCode(code: String) {
        viewModelScope.launch {
            _joinState.value = UiState.Loading
            siteRepository.findSiteByInviteCode(code)
                .onSuccess { site ->
                    siteRepository.joinSite(site.id)
                        .onSuccess {
                            _joinState.value = UiState.Success(site.id)
                            _navEvent.emit(GatewayNavEvent.NavigateToDashboard(site.id))
                        }
                        .onFailure { cause ->
                            _joinState.value = UiState.Error(
                                message = "Found site, but failed to join.",
                                cause = cause,
                            )
                        }
                }
                .onFailure { cause ->
                    _joinState.value = UiState.Error(
                        message = "Invalid code. No site found.",
                        cause   = cause,
                    )
                }
        }
    }

    fun navigateToDashboard(siteId: String) {
        viewModelScope.launch {
            _navEvent.emit(GatewayNavEvent.NavigateToDashboard(siteId))
        }
    }

    fun navigateToProfile() {
        viewModelScope.launch {
            _navEvent.emit(GatewayNavEvent.NavigateToProfile)
        }
    }

    fun clearCreateState() { _createState.value = UiState.Idle }
    fun clearJoinError()   { if (_joinState.value is UiState.Error) _joinState.value = UiState.Idle }

    /** Resets all state to initial values. Called when the user backs away from this screen. */
    fun resetAll() {
        _createState.value = UiState.Idle
        _joinState.value   = UiState.Idle
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Generates a random 6-character uppercase alphanumeric code. */
    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }


}

/** Carries the result of a successful site creation. */
data class CreatedSiteResult(
    val siteId: String,
    val inviteCode: String,
)

/** One-shot navigation events emitted by [SiteGatewayViewModel]. */
sealed interface GatewayNavEvent {
    data class NavigateToDashboard(val siteId: String) : GatewayNavEvent
    data object NavigateToProfile : GatewayNavEvent
}
