package com.example.blushbistroapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blushbistroapp.data.FirebaseAuthService
import com.example.blushbistroapp.ui.screens.*
import com.example.blushbistroapp.ui.theme.BlushBistroTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase Auth Service
        FirebaseAuthService.getInstance(this)
        
        setContent {
            BlushBistroTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current
                    
                    NavHost(
                        navController = navController,
                        startDestination = "welcome"
                    ) {
                        composable("welcome") {
                            WelcomeScreen(
                                onLoginClick = { navController.navigate("login") },
                                onRegisterClick = { navController.navigate("register") }
                            )
                        }
                        
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = {
                                    navController.navigate("dashboard") {
                                        popUpTo("welcome") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onRegisterClick = { navController.navigate("register") },
                                onForgotPasswordClick = { navController.navigate("forgot_password") },
                                onBackClick = { navController.navigate("welcome") }
                            )
                        }
                        
                        composable("register") {
                            RegisterScreen(
                                onNavigateToLogin = { navController.navigate("login") },
                                onNavigateToDashboard = { navController.navigate("dashboard") }
                            )
                        }
                        
                        composable("forgot_password") {
                            ForgotPasswordScreen(
                                onBackToLogin = { navController.navigate("login") }
                            )
                        }
                        
                        composable("dashboard") {
                            DashboardScreen(
                                onSignOut = {
                                    Log.d("MainActivity", "Starting logout process")
                                    try {
                                        // First sign out from Firebase
                                        FirebaseAuthService.getInstance(context).signOut()
                                        Log.d("MainActivity", "Firebase sign out successful")
                                        // Then navigate to login screen
                                        navController.navigate("login") {
                                            popUpTo("dashboard") { inclusive = true }
                                        }
                                        Log.d("MainActivity", "Navigation to login screen successful")
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "Logout failed: ${e.message}", e)
                                    }
                                },
                                onNavigateToProfile = { /* No navigation needed as profile is handled within DashboardScreen */ },
                                onNavigateToSettings = { /* No navigation needed as settings is handled within DashboardScreen */ }
                            )
                        }
                    }
                }
            }
        }
    }
}