package com.terrasync.app.presentation.sitedashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.terrasync.app.core.ui.UiState
import com.terrasync.app.domain.model.Site
import com.terrasync.app.domain.repository.SiteRepository
import com.terrasync.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for the Site Dashboard screen.
 *
 * [SavedStateHandle] is injected by Hilt automatically and provides
 * the [siteId] nav argument from the back-stack entry — no manual
 * argument extraction needed in the ViewModel.
 */
@HiltViewModel
class SiteDashboardViewModel @Inject constructor(
    private val siteRepository: SiteRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val siteId: String = checkNotNull(savedStateHandle[Screen.SiteDashboard.ARG_SITE_ID])

    private val _uiState = MutableStateFlow<UiState<Site>>(UiState.Idle)
    val uiState: StateFlow<UiState<Site>> = _uiState.asStateFlow()

    // Site detail loading logic will be added in the SiteDashboard feature sprint.
}
