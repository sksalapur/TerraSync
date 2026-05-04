package com.terrasync.app.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.terrasync.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle    : LoginUiState
    data object Loading : LoginUiState
    data class  Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val isAlreadyLoggedIn: Boolean = authRepository.currentUser() != null

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigateToGateway = MutableSharedFlow<Unit>()
    val navigateToGateway: SharedFlow<Unit> = _navigateToGateway.asSharedFlow()

    /** Called by the screen after it receives a Google ID token from the sign-in flow. */
    fun onGoogleTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _navigateToGateway.emit(Unit) }
                .onFailure { cause ->
                    _uiState.value = LoginUiState.Error(
                        cause.localizedMessage ?: "Google sign-in failed."
                    )
                }
        }
    }

    /** Called when the Google chooser itself fails (e.g. no account, network, SHA-1 mismatch). */
    fun signInFailed(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    fun clearError() { _uiState.value = LoginUiState.Idle }
}
