package com.terrasync.app.presentation.sitedashboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrasync.app.domain.model.SoilNode
import com.terrasync.app.domain.repository.NodeRepository
import com.terrasync.app.domain.repository.SiteRepository
import com.terrasync.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

private const val TAG = "DashboardVM"

sealed interface DashboardUiState {
    data object Loading                            : DashboardUiState
    data class  Success(val nodes: List<SoilNode>) : DashboardUiState
    data class  Error(val message: String)          : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val nodeRepository: NodeRepository,
    private val siteRepository: SiteRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val siteId: String = checkNotNull(savedStateHandle[Screen.SiteDashboard.ARG_SITE_ID])

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Human-readable label for the top bar - site name, invite code, short ID fallback. */
    private val _siteDisplayName = MutableStateFlow("Site")
    val siteDisplayName: StateFlow<String> = _siteDisplayName.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _siteDeletedEvent = MutableSharedFlow<Unit>()
    val siteDeletedEvent = _siteDeletedEvent.asSharedFlow()

    // ── Node multi-select state ────────────────────────────────────────────────
    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode.asStateFlow()

    private val _selectedNodeIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNodeIds: StateFlow<Set<String>> = _selectedNodeIds.asStateFlow()

    private val _isDeletingNodes = MutableStateFlow(false)
    val isDeletingNodes: StateFlow<Boolean> = _isDeletingNodes.asStateFlow()

    init {
        fetchSiteInfo()
        observeNodes()
    }

    private fun fetchSiteInfo() {
        viewModelScope.launch {
            siteRepository.getSiteById(siteId)
                .onSuccess { site ->
                    val uid = auth.currentUser?.uid
                    _isAdmin.value = (uid != null && (site.adminId == uid || site.ownerId == uid))

                    _siteDisplayName.value = when {
                        site.name.isNotBlank()       -> site.name
                        site.inviteCode.isNotBlank() -> "Code: ${site.inviteCode}"
                        else                         -> siteId.take(8)
                    }
                }
                .onFailure {
                    _siteDisplayName.value = siteId.take(8)
                }
        }
    }

    private fun observeNodes() {
        viewModelScope.launch {
            nodeRepository.observeNodes(siteId)
                .catch { cause ->
                    Log.e(TAG, "Firestore snapshot error", cause)
                    _uiState.value = DashboardUiState.Error(
                        cause.localizedMessage ?: "Failed to load nodes."
                    )
                }
                .collect { nodes ->
                    _uiState.value = DashboardUiState.Success(nodes)
                }
        }
    }

    fun renameSite(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            siteRepository.updateSiteName(siteId, newName)
                .onSuccess {
                    _siteDisplayName.value = newName
                }
                .onFailure { cause ->
                    Log.e(TAG, "Failed to rename site", cause)
                }
        }
    }

    fun deleteSite() {
        viewModelScope.launch {
            siteRepository.deleteSite(siteId)
                .onSuccess {
                    _siteDeletedEvent.emit(Unit)
                }
                .onFailure { cause ->
                    Log.e(TAG, "Failed to delete site", cause)
                }
        }
    }

    // ── Node Selection ───────────────────────────────────────────────────────

    fun enterSelectionMode() {
        _selectionMode.value = true
        _selectedNodeIds.value = emptySet()
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedNodeIds.value = emptySet()
    }

    fun toggleNodeSelection(nodeId: String) {
        val current = _selectedNodeIds.value.toMutableSet()
        if (current.contains(nodeId)) current.remove(nodeId) else current.add(nodeId)
        _selectedNodeIds.value = current
    }

    fun deleteSelectedNodes() {
        val ids = _selectedNodeIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _isDeletingNodes.value = true
            nodeRepository.deleteNodes(siteId, ids)
                .onSuccess {
                    exitSelectionMode()
                }
                .onFailure { cause ->
                    Log.e(TAG, "Failed to delete nodes", cause)
                }
            _isDeletingNodes.value = false
        }
    }
}