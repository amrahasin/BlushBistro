package com.example.blushbistroapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun BackgroundGradient(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    // Create a beautiful gradient background
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF121212), // Dark background
            Color(0xFF1A1A1A), // Slightly lighter dark
            Color(0xFF880E4F), // Dark pink
            Color(0xFF4A148C)  // Dark purple
        )
    } else {
        listOf(
            Color(0xFFFCE4EC), // Very light pink
            Color(0xFFFFF0F5), // Lavender blush
            Color(0xFFE1BEE7), // Light purple
            Color(0xFFBBDEFB)  // Light blue
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors,
                    startY = 0f,
                    endY = screenHeight.value
                )
            )
    ) {
        content()
    }
}

@Composable
fun AnimatedBackgroundGradient(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Create a beautiful animated gradient background
    val gradientColors = if (isDarkTheme) {
        listOf(
            Color(0xFF121212), // Dark background
            Color(0xFF1A1A1A), // Slightly lighter dark
            Color(0xFF880E4F), // Dark pink
            Color(0xFF4A148C), // Dark purple
            Color(0xFF121212)  // Back to dark background for seamless loop
        )
    } else {
        listOf(
            Color(0xFFFCE4EC), // Very light pink
            Color(0xFFFFF0F5), // Lavender blush
            Color(0xFFE1BEE7), // Light purple
            Color(0xFFBBDEFB), // Light blue
            Color(0xFFFCE4EC)  // Back to very light pink for seamless loop
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = gradientColors,
                    tileMode = TileMode.Mirror
                )
            )
    ) {
        content()
    }
} 