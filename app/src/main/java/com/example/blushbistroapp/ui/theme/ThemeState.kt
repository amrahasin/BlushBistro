package com.example.blushbistroapp.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// Theme state holder
object ThemeState {
    // Use mutableStateOf for state that can be observed by Compose
    private val _isDarkMode = mutableStateOf(false)
    val isDarkMode: Boolean
        get() = _isDarkMode.value
    
    // Custom colors for dark mode
    val darkPrimary = DarkPrimary
    val darkSecondary = DarkSecondary
    val darkTertiary = DarkTertiary
    val darkBackground = DarkBackground
    val darkSurface = DarkSurface
    
    // Custom colors for light mode
    val lightPrimary = LightPrimary
    val lightSecondary = LightSecondary
    val lightTertiary = LightTertiary
    val lightBackground = LightBackground
    val lightSurface = LightSurface
    
    // Function to toggle theme
    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }
} 