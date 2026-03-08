package com.example.shyam_assignment.navigation

/**
 * Defines all navigation routes in the app.
 * Each screen has a unique route string used by NavGraph.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")           // Home screen — list of meetings
    data object Recording : Screen("recording")           // Live recording screen
    data object Summary : Screen("summary/{meetingId}") { // Meeting details + summary screen
        /** Builds the route with a specific meeting ID */
        fun createRoute(meetingId: String) = "summary/$meetingId"
    }
}
