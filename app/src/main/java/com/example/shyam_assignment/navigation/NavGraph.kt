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

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onStartRecording = {
                    navController.navigate(Screen.Recording.route) {
                        launchSingleTop = true
                    }
                },
                onMeetingClick = { meetingId ->
                    navController.navigate(Screen.Summary.createRoute(meetingId)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.Recording.route) {
            RecordingScreen(
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
                onRecordingComplete = { meetingId ->
                    navController.navigate(Screen.Summary.createRoute(meetingId)) {
                        popUpTo(Screen.Dashboard.route) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Summary.route,
            arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
        ) {
            SummaryScreen(
                onNavigateBack = {
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

