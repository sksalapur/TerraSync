package com.terrasync.app.presentation.sitegateway

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.Person
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.terrasync.app.core.ui.UiState

/**
 * SiteGateway screen — the authenticated user's home.
 *
 * Provides two paths:
 *  1. **Create Site** — user enters a name, gets a Firestore site + QR code.
 *  2. **Join Site**   — user enters a 6-char code OR scans a QR code to find & join a site.
 *
 * @param onNavigateToDashboard Called with [siteId] to navigate to SiteDashboard.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SiteGatewayScreen(
    onNavigateToDashboard: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: SiteGatewayViewModel = hiltViewModel(),
) {
    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val joinState   by viewModel.joinState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }

    var siteName by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }

    // ── Reset all state when user leaves this screen ───────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            siteName = ""
            joinCode = ""
            viewModel.resetAll()
        }
    }

    // ── One-shot navigation event ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is GatewayNavEvent.NavigateToDashboard -> onNavigateToDashboard(event.siteId)
                is GatewayNavEvent.NavigateToProfile -> onNavigateToProfile()
            }
        }
    }

    // ── Error snackbars ───────────────────────────────────────────────────────
    LaunchedEffect(createState) {
        if (createState is UiState.Error) {
            snackbarHost.showSnackbar(
                message  = (createState as UiState.Error).message,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearCreateState()
        }
    }
    LaunchedEffect(joinState) {
        if (joinState is UiState.Error) {
            snackbarHost.showSnackbar(
                message  = (joinState as UiState.Error).message,
                duration = SnackbarDuration.Short,
            )
            viewModel.clearJoinError()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = { viewModel.navigateToProfile() }) {
                        if (viewModel.userPhotoUrl != null) {
                            AsyncImage(
                                model = viewModel.userPhotoUrl,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFFD97040),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF3A3330))
                                    .padding(4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onError,
                    shape          = RoundedCornerShape(12.dp),
                    modifier       = Modifier.padding(16.dp),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1C1B1B), Color(0xFF221E1B)),
                        start  = Offset(0f, 0f),
                        end    = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                    )
                )
                .imePadding()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Header ────────────────────────────────────────────────────
                Text(
                    text  = "Site Gateway",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight    = FontWeight.Bold,
                        color         = Color(0xFFD97040),
                        letterSpacing = 0.5.sp,
                    ),
                )
                Text(
                    text  = "Create a new geotechnical site or join an existing one.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E8D82)),
                )

                Spacer(Modifier.height(4.dp))

                // ── Create Site Card ──────────────────────────────────────────
                CreateSiteCard(
                    createState = createState,
                    siteName = siteName,
                    onSiteNameChange = { siteName = it },
                    onCreateSite = viewModel::createSite,
                    onNavigateToDashboard = { siteId ->
                        viewModel.navigateToDashboard(siteId)
                    },
                )

                // ── Divider ───────────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(Modifier.weight(1f), color = Color(0xFF3A3330))
                    Text(
                        "  OR  ",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6E635A)),
                    )
                    HorizontalDivider(Modifier.weight(1f), color = Color(0xFF3A3330))
                }

                // ── Join Site Card ────────────────────────────────────────────
                JoinSiteCard(
                    joinState       = joinState,
                    code            = joinCode,
                    onCodeChange    = { joinCode = it },
                    onJoinByCode    = viewModel::joinByCode,
                )

            }
        }
    }
}

// ── Create Site Card ──────────────────────────────────────────────────────────

@Composable
private fun CreateSiteCard(
    createState: UiState<CreatedSiteResult>,
    siteName: String,
    onSiteNameChange: (String) -> Unit,
    onCreateSite: (String) -> Unit,
    onNavigateToDashboard: (String) -> Unit,
) {
    TerraCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(icon = "🏗", title = "Create New Site")

            OutlinedTextField(
                value       = siteName,
                onValueChange = onSiteNameChange,
                label       = { Text("Site Name") },
                placeholder = { Text("e.g. NTPC Sipat Block-A") },
                singleLine  = true,
                enabled     = createState !is UiState.Loading,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done,
                ),
                shape   = RoundedCornerShape(12.dp),
                colors  = gatewayFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick  = { onCreateSite(siteName) },
                enabled  = createState !is UiState.Loading,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97040),
                    contentColor   = Color.White,
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (createState is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create New Site", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── QR Result Panel ───────────────────────────────────────────────
            AnimatedVisibility(
                visible = createState is UiState.Success,
                enter   = fadeIn(tween(400)) + slideInVertically { it / 4 },
                exit    = fadeOut(tween(200)) + slideOutVertically { it / 4 },
            ) {
                if (createState is UiState.Success) {
                    val result = createState.data
                    QrResultPanel(
                        siteId     = result.siteId,
                        inviteCode = result.inviteCode,
                        onOpen     = { onNavigateToDashboard(result.siteId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QrResultPanel(
    siteId: String,
    inviteCode: String,
    onOpen: () -> Unit,
) {
    Column(
        modifier            = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(color = Color(0xFF3A3330))

        Text(
            text  = "Share this code with your team",
            style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF9E8D82)),
        )

        // Invite code badge
        Box(
            modifier = Modifier
                .background(Color(0xFF2A2927), RoundedCornerShape(8.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text      = inviteCode,
                style     = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight    = FontWeight.Bold,
                    color         = Color(0xFFD97040),
                    letterSpacing = 6.sp,
                ),
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick  = onOpen,
            shape    = RoundedCornerShape(12.dp),
            colors   = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3D3A36),
                contentColor   = Color(0xFFEAE1D9),
            ),
            modifier = Modifier.fillMaxWidth().height(44.dp),
        ) {
            Text("Open Site Dashboard →", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Join Site Card ────────────────────────────────────────────────────────────

@Composable
private fun JoinSiteCard(
    joinState: UiState<String>,
    code: String,
    onCodeChange: (String) -> Unit,
    onJoinByCode: (String) -> Unit,
) {
    TerraCard {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionLabel(icon = "🔗", title = "Join Existing Site")

            OutlinedTextField(
                value       = code,
                onValueChange = { if (it.length <= 6) onCodeChange(it.uppercase()) },
                label       = { Text("Invite Code") },
                placeholder = { Text("Enter 6-char code") },
                singleLine  = true,
                enabled     = joinState !is UiState.Loading,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction    = ImeAction.Done,
                ),
                shape   = RoundedCornerShape(12.dp),
                colors  = gatewayFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick  = { onJoinByCode(code) },
                    enabled  = code.length == 6 && joinState !is UiState.Loading,
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97040),
                        contentColor   = Color.White,
                    ),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    AnimatedContent(
                        targetState = joinState is UiState.Loading,
                        label       = "join_btn",
                    ) { loading ->
                        if (loading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Join", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ── Shared sub-components ─────────────────────────────────────────────────────

@Composable
private fun TerraCard(content: @Composable () -> Unit) {
    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFF252220)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        content  = { content() },
    )
}

@Composable
private fun SectionLabel(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            text  = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFEAE1D9),
            ),
        )
    }
}

@Composable
private fun gatewayFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor     = Color(0xFFD97040),
    unfocusedBorderColor   = Color(0xFF4A413B),
    focusedLabelColor      = Color(0xFFD97040),
    unfocusedLabelColor    = Color(0xFF9E8D82),
    cursorColor            = Color(0xFFD97040),
    focusedTextColor       = Color(0xFFEAE1D9),
    unfocusedTextColor     = Color(0xFFEAE1D9),
    focusedContainerColor  = Color(0xFF1F1D1B),
    unfocusedContainerColor = Color(0xFF1A1817),
)
