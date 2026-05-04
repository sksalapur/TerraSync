package com.terrasync.app.core.ui

/**
 * A sealed interface that represents the complete lifecycle of a UI operation.
 *
 * Usage in a ViewModel:
 * ```kotlin
 * private val _uiState = MutableStateFlow<UiState<MyData>>(UiState.Idle)
 * val uiState: StateFlow<UiState<MyData>> = _uiState.asStateFlow()
 * ```
 *
 * Usage in a Composable:
 * ```kotlin
 * when (val state = uiState.collectAsStateWithLifecycle().value) {
 *     is UiState.Idle    -> { /* show empty/initial UI */ }
 *     is UiState.Loading -> { CircularProgressIndicator() }
 *     is UiState.Success -> { MyContent(data = state.data) }
 *     is UiState.Error   -> { ErrorBanner(message = state.message) }
 * }
 * ```
 *
 * @param T The type of data carried on success.
 */
sealed interface UiState<out T> {

    /** Initial state — no operation has been triggered yet. */
    data object Idle : UiState<Nothing>

    /** An async operation is in progress. */
    data object Loading : UiState<Nothing>

    /**
     * The operation completed successfully.
     * @param data The result payload.
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * The operation failed.
     * @param message A human-readable error message safe to display in the UI.
     * @param cause   The original throwable for logging/diagnostics (never shown in UI).
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : UiState<Nothing>
}
