package com.example.shyam_assignment.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.shyam_assignment.ui.screens.dashboard.DashboardScreen
import com.example.shyam_assignment.ui.screens.recording.RecordingScreen
import com.example.shyam_assignment.ui.screens.summary.SummaryScreen

/**
 * Navigation graph — connects all screens and handles navigation between them.
 *
 * Routes:
 *   Dashboard → Recording (start new recording)
 *   Dashboard → Summary   (view meeting details)
 *   Recording → Summary   (after recording stops)
 *   Summary   → Dashboard (go back)
 */
@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route   // App always starts at Dashboard
    ) {
        // ── Dashboard screen ──
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStartRecording = {
                    // Navigate to recording screen (don't create duplicate)
                    navController.navigate(Screen.Recording.route) {
                        launchSingleTop = true
                    }
                },
                onMeetingClick = { meetingId ->
                    // Navigate to summary/details for a specific meeting
                    navController.navigate(Screen.Summary.createRoute(meetingId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Recording screen ──
        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateBack = {
                    // Go back to dashboard (or recreate it if stack is empty)
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onRecordingComplete = { meetingId ->
                    // After stopping, go to summary screen (keep dashboard in stack)
                    navController.navigate(Screen.Summary.createRoute(meetingId)) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ── Summary screen (receives meetingId as argument) ──
        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) {
            SummaryScreen(
                onNavigateBack = {
                    // Go back to dashboard
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )
        }
    }
}
