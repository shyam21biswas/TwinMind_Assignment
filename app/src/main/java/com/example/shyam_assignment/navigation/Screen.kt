package com.example.shyam_assignment.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Recording : Screen("recording")
    data object Summary : Screen("summary/{meetingId}") {
        fun createRoute(meetingId: String) = "summary/$meetingId"
    }
}

