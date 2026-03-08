package com.example.shyam_assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.shyam_assignment.navigation.NavGraph
import com.example.shyam_assignment.ui.theme.TwinMindTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the entire app.
 * Uses Jetpack Compose for UI and Navigation Compose for screen routing.
 * @AndroidEntryPoint enables Hilt injection in this activity.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()           // Let content draw behind system bars
        setContent {
            TwinMindTheme {          // Apply the TwinMind light theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()  // Manages screen navigation
                    NavGraph(navController = navController)       // Defines all screens & routes
                }
            }
        }
    }
}
