package com.terrasync.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.terrasync.app.presentation.addnodeform.AddNodeFormScreen
import com.terrasync.app.presentation.login.LoginScreen
import com.terrasync.app.presentation.sitedashboard.SiteDashboardScreen
import com.terrasync.app.presentation.sitegateway.SiteGatewayScreen

/**
 * Root navigation graph for TerraSync.
 * Each [composable] destination owns its own ViewModel via [hiltViewModel].
 * The NavGraph is purely structural — no logic lives here.
 */
@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route,
    ) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable(route = Screen.Login.route) {
            LoginScreen(
                onAuthSuccess = {
                    navController.navigate(Screen.SiteGateway.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // ── Site Gateway ──────────────────────────────────────────────────────
        composable(route = Screen.SiteGateway.route) {
            SiteGatewayScreen(
                onNavigateToDashboard = { siteId ->
                    navController.navigate(Screen.SiteDashboard.createRoute(siteId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        // ── Site Dashboard ────────────────────────────────────────────────────
        composable(
            route     = Screen.SiteDashboard.route,
            arguments = listOf(
                navArgument(Screen.SiteDashboard.ARG_SITE_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString(Screen.SiteDashboard.ARG_SITE_ID).orEmpty()
            SiteDashboardScreen(
                onAddNode = { navController.navigate(Screen.AddNodeForm.createRoute(siteId)) },
                onEditNode = { nodeId -> navController.navigate(Screen.AddNodeForm.createRoute(siteId, nodeId)) },
                onBack    = { navController.popBackStack() },
            )
        }

        // ── Add Node Form ─────────────────────────────────────────────────────
        composable(
            route     = Screen.AddNodeForm.route,
            arguments = listOf(
                navArgument(Screen.AddNodeForm.ARG_SITE_ID) { type = NavType.StringType },
                navArgument(Screen.AddNodeForm.ARG_NODE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            AddNodeFormScreen(
                onBack = { navController.popBackStack() }
            )
        }
        // ── Profile ───────────────────────────────────────────────────────────
        composable(route = Screen.Profile.route) {
            com.terrasync.app.presentation.profile.ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToDashboard = { siteId ->
                    navController.navigate(Screen.SiteDashboard.createRoute(siteId))
                }
            )
        }
    }
}


