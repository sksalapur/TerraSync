package com.terrasync.app.presentation.login

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.terrasync.app.R

// Web client ID from google-services.json (client_type = 3)
private const val WEB_CLIENT_ID =
    "30117677092-n6rtq1aspbg5sinjmojdacimatp1m161.apps.googleusercontent.com"

/**
 * Login screen — a single "Continue with Google" button.
 *
 * Flow:
 *  1. User taps the button → Google Sign-In chooser launched via [ActivityResultContracts].
 *  2. On success, the Google ID token is passed to [LoginViewModel.onGoogleTokenReceived].
 *  3. ViewModel exchanges the token with Firebase Auth → emits navigation event.
 *  4. [onAuthSuccess] is called → NavGraph pops this screen and opens SiteGateway.
 */
@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar   = remember { SnackbarHostState() }
    val context    = LocalContext.current
    val isLoading  = uiState is LoginUiState.Loading

    // Auto-login bypass
    val isAlreadyLoggedIn = viewModel.isAlreadyLoggedIn
    LaunchedEffect(isAlreadyLoggedIn) {
        if (isAlreadyLoggedIn) onAuthSuccess()
    }

    // Navigate on success (SharedFlow — consumed once)
    LaunchedEffect(Unit) {
        viewModel.navigateToGateway.collect { onAuthSuccess() }
    }

    // Show error in snackbar
    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Error) {
            snackbar.showSnackbar(
                message  = (uiState as LoginUiState.Error).message,
                duration = SnackbarDuration.Long,
            )
            viewModel.clearError()
        }
    }

    // Google Sign-In launcher
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task    = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    viewModel.onGoogleTokenReceived(idToken)
                } else {
                    android.util.Log.e("LoginScreen", "Google idToken is null")
                    viewModel.signInFailed("Sign-in failed: no ID token returned.")
                }
            } catch (e: ApiException) {
                val msg = "Google Sign-In error code: ${e.statusCode} — ${e.message}"
                android.util.Log.e("LoginScreen", msg, e)
                val userMsg = when (e.statusCode) {
                    10   -> "Configuration error (code 10). SHA-1 may not be registered in Firebase."
                    7    -> "Network error. Check your internet connection."
                    12501 -> "Sign-in cancelled."
                    else -> "Sign-in failed (code ${e.statusCode})."
                }
                viewModel.signInFailed(userMsg)
            }
        } else {
            android.util.Log.w("LoginScreen", "Google Sign-In cancelled (resultCode=${result.resultCode})")
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1C1B1B), Color(0xFF2A2018), Color(0xFF1C1B1B)),
                    start  = Offset(0f, 0f),
                    end    = Offset(0f, Float.POSITIVE_INFINITY),
                )
            ),
    ) {
        SnackbarHost(
            hostState = snackbar,
            modifier  = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        ) { data ->
            Snackbar(
                snackbarData   = data,
                containerColor = Color(0xFF3D1A1E),
                contentColor   = Color(0xFFFFB3BA),
                shape          = RoundedCornerShape(12.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // ── Wordmark / Logo area ─────────────────────────────────────────
            Text(
                "TerraSync",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFFEAE1D9),
                    letterSpacing = 1.sp,
                ),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Geotechnical AI Platform",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color       = Color(0xFF9E8D82),
                    letterSpacing = 2.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(72.dp))

            // ── Divider label ────────────────────────────────────────────────
            Text(
                "Sign in to continue",
                style = MaterialTheme.typography.labelMedium.copy(color = Color(0xFF6E635A)),
            )

            Spacer(Modifier.height(20.dp))

            // ── Continue with Google button ──────────────────────────────────
            Button(
                onClick = {
                    // Force fresh account chooser every time
                    googleSignInClient.signOut().addOnCompleteListener {
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                },
                enabled  = !isLoading,
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Color(0xFFF2F2F2),
                    contentColor           = Color(0xFF1A1A1A),
                    disabledContainerColor = Color(0xFF2E2A26),
                    disabledContentColor   = Color(0xFF5E5550),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                AnimatedVisibility(
                    visible = isLoading,
                    enter   = fadeIn(tween(150)),
                    exit    = fadeOut(tween(150)),
                ) {
                    CircularProgressIndicator(
                        color       = Color(0xFFD97040),
                        strokeWidth = 2.5.dp,
                        modifier    = Modifier.size(20.dp),
                    )
                }
                AnimatedVisibility(
                    visible = !isLoading,
                    enter   = fadeIn(tween(150)),
                    exit    = fadeOut(tween(150)),
                ) {
                    Row(
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.Center,
                    ) {
                        // Google "G" logo drawn with vectors from the drawable resource
                        // We inline a simple colored circle as a stand-in that is
                        // replaced at runtime — no internet permission needed.
                        GoogleGLogo(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Continue with Google",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize   = 16.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ── Google "G" Logo ───────────────────────────────────────────────────────────

/**
 * Renders the Google multicolour "G" using Canvas paths.
 * Avoids needing a bundled PNG asset.
 */
@Composable
private fun GoogleGLogo(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val r = size.minDimension / 2f
        val cx = size.width / 2f
        val cy = size.height / 2f

        // Blue arc (top)
        drawArc(
            color      = Color(0xFF4285F4),
            startAngle = -135f,
            sweepAngle = 135f,
            useCenter  = false,
            style      = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Red arc (left)
        drawArc(
            color      = Color(0xFFEA4335),
            startAngle = -225f,
            sweepAngle = -90f,
            useCenter  = false,
            style      = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Yellow arc (bottom-left)
        drawArc(
            color      = Color(0xFFFBBC05),
            startAngle = 135f,
            sweepAngle = 90f,
            useCenter  = false,
            style      = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
        // Green arc (right)
        drawArc(
            color      = Color(0xFF34A853),
            startAngle = 45f,
            sweepAngle = 90f,
            useCenter  = false,
            style      = androidx.compose.ui.graphics.drawscope.Stroke(width = r * 0.55f),
        )
    }
}
