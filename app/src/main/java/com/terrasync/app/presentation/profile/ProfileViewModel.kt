package com.terrasync.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.terrasync.app.core.ui.UiState
import com.terrasync.app.domain.model.Site
import com.terrasync.app.domain.repository.AuthRepository
import com.terrasync.app.domain.repository.SiteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileNavEvent {
    data class NavigateToDashboard(val siteId: String) : ProfileNavEvent
    data object NavigateToLogin : ProfileNavEvent
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    private val authRepository: AuthRepository,
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Site>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<Site>>> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<ProfileNavEvent>()
    val navEvent = _navEvent.asSharedFlow()

    val userName: String? = auth.currentUser?.displayName
    val userPhotoUrl: String? = auth.currentUser?.photoUrl?.toString()

    init {
        loadMySites()
    }

    fun loadMySites() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            siteRepository.getSites()
                .onSuccess { sites ->
                    _uiState.value = UiState.Success(sites)
                }
                .onFailure { cause ->
                    _uiState.value = UiState.Error(
                        message = "Failed to load sites.",
                        cause = cause,
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _navEvent.emit(ProfileNavEvent.NavigateToLogin)
        }
    }

    fun navigateToDashboard(siteId: String) {
        viewModelScope.launch {
            _navEvent.emit(ProfileNavEvent.NavigateToDashboard(siteId))
        }
    }
}
