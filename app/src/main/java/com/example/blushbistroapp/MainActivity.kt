package com.example.blushbistroapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
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
                                onLoginSuccess = { navController.navigate("dashboard") },
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
                                    FirebaseAuthService.getInstance(context).signOut()
                                    navController.navigate("login") {
                                        popUpTo("welcome") { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}