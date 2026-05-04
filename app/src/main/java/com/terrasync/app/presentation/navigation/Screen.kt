package com.terrasync.app.presentation.navigation

/**
 * Sealed class acting as the single source of truth for all navigation routes.
 *
 * Using a sealed class (not enum) allows routes to carry typed arguments
 * in future without breaking the existing navigation graph.
 *
 * Convention: [route] is a stable string constant used by NavHost and
 * [navigate] calls. Never hardcode route strings elsewhere.
 */
sealed class Screen(val route: String) {

    /** Login / Authentication screen. Entry point for unauthenticated users. */
    data object Login : Screen("login")

    /**
     * Site Gateway — lists all geotechnical sites owned by the user.
     * Entry point for authenticated users.
     */
    data object SiteGateway : Screen("site_gateway")

    /**
     * Site Dashboard — detailed view of a specific site.
     * Receives [siteId] as a nav argument.
     */
    data object SiteDashboard : Screen("site_dashboard/{siteId}") {
        const val ARG_SITE_ID = "siteId"
        fun createRoute(siteId: String) = "site_dashboard/$siteId"
    }

    /**
     * Add Node Form — form to record a new geotechnical data node (e.g. SPT bore-log entry).
     * Receives [siteId] and optional [nodeId] to support edit mode.
     */
    data object AddNodeForm : Screen("add_node_form/{siteId}?nodeId={nodeId}") {
        const val ARG_SITE_ID = "siteId"
        const val ARG_NODE_ID = "nodeId"
        fun createRoute(siteId: String, nodeId: String? = null): String =
            if (nodeId != null) "add_node_form/$siteId?nodeId=$nodeId"
            else "add_node_form/$siteId"
    }

    /** Profile screen */
    data object Profile : Screen("profile")
}
