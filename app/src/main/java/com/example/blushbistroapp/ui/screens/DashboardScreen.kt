package com.example.blushbistroapp.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blushbistroapp.R
import com.example.blushbistroapp.data.FirestoreService
import com.example.blushbistroapp.data.Recipe
import com.example.blushbistroapp.data.RecipeCategory
import com.example.blushbistroapp.data.FirebaseAuthService
import com.example.blushbistroapp.data.SettingsService
import com.example.blushbistroapp.ui.components.ThemeToggle
import com.example.blushbistroapp.ui.components.UserProfileDialog
import com.example.blushbistroapp.ui.components.AddRecipeDialog
import com.example.blushbistroapp.ui.components.RecipeCard
import com.example.blushbistroapp.ui.screens.SettingsScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import com.google.firebase.firestore.FirebaseFirestore

enum class DashboardTab {
    HOME, FAVORITES, PROFILE, SETTINGS
}

data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String
)

private object DashboardScreenConstants {
    const val TAG = "DashboardScreen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onSignOut: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreService = remember { FirestoreService.getInstance(FirebaseFirestore.getInstance()) }
    val authService = remember { FirebaseAuthService.getInstance(context) }
    val settingsService = remember { SettingsService.getInstance(context) }
    
    var recipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var userFavorites by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var isDarkMode by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    
    val currentUser = authService.getCurrentUser()
    
    var showProfileDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf(RecipeCategory.ALL)) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }
    var showRecipeDetails by remember { mutableStateOf<Recipe?>(null) }

    // Load settings when the screen is first displayed
    LaunchedEffect(Unit) {
        try {
            isDarkMode = settingsService.getThemePreference()
            notificationsEnabled = settingsService.getNotificationPreference()
        } catch (e: Exception) {
            error = "Failed to load settings: ${e.message}"
        }
    }
    
    // Function to update favorites
    fun updateFavorites(recipeId: String, isFavorite: Boolean) {
        scope.launch {
            currentUser?.uid?.let { userId ->
                if (isFavorite) {
                    firestoreService.addToFavorites(userId, recipeId)
                        .onFailure { exception ->
                            error = exception.message ?: "Failed to add to favorites"
                        }
                } else {
                    firestoreService.removeFromFavorites(userId, recipeId)
                        .onFailure { exception ->
                            error = exception.message ?: "Failed to remove from favorites"
                        }
                }
            }
        }
    }
    
    // Function to load recipes
    fun loadRecipes() {
        scope.launch {
            isLoading = true
            error = null
            try {
                // Check if user is authenticated
                if (currentUser == null) {
                    error = "You must be logged in to view recipes"
                    isLoading = false
                    return@launch
                }
                
                // Initialize predefined recipes first
                firestoreService.initializePredefinedRecipes().onSuccess {
                    // Load recipes in a coroutine
                    val recipesDeferred = scope.async {
                        firestoreService.getRecipesForCurrentUser()
                    }
                    
                    // Load favorites in a separate coroutine
                    currentUser.uid?.let { userId ->
                        scope.launch {
                            firestoreService.getUserFavorites(userId).collect { result ->
                                result.onSuccess { favorites ->
                                    userFavorites = favorites
                                }.onFailure { exception ->
                                    error = exception.message ?: "Failed to load favorites"
                                }
                            }
                        }
                    }
                    
                    // Wait for recipes to complete
                    recipes = recipesDeferred.await()
                }.onFailure { exception ->
                    error = "Failed to initialize predefined recipes: ${exception.message}"
                }
            } catch (e: Exception) {
                error = "Failed to load recipes: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Load recipes when the screen is first displayed
    LaunchedEffect(Unit) {
        visible = true
        loadRecipes()
        
        // Load favorites
        currentUser?.uid?.let { userId ->
            firestoreService.getUserFavorites(userId).collect { result ->
                result.onSuccess { favorites ->
                    userFavorites = favorites
                }.onFailure { exception ->
                    error = exception.message ?: "Failed to load favorites"
                }
            }
        }
    }
    
    // Only reload recipes when the user changes, not on every recomposition
    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            loadRecipes()
        }
    }
    
    // Show loading indicator
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
    
    // Show error message if there's an error
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Show error message for 3 seconds
            kotlinx.coroutines.delay(3000)
            error = null
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { error = null }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(errorMessage)
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // This is a duplicate loading indicator, remove it
        } else {
            // Debug button for testing Firestore - REMOVED
            if (error != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                when (selectedTab) {
                    DashboardTab.HOME -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp)
                        ) {
                            // Top App Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Blush Bistro",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ThemeToggle()
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                            .clickable { selectedTab = DashboardTab.PROFILE },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = "User Profile",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            Log.d("DashboardScreen", "Top bar logout clicked")
                                            try {
                                                // First sign out from Firebase
                                                authService.signOut()
                                                Log.d("DashboardScreen", "Firebase sign out successful from top bar")
                                                // Then navigate to login screen
                                                onSignOut()
                                                Log.d("DashboardScreen", "Navigation callback called from top bar")
                                            } catch (e: Exception) {
                                                Log.e("DashboardScreen", "Top bar logout failed: ${e.message}", e)
                                                error = "Failed to sign out: ${e.message}"
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = "Sign Out",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            
                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                placeholder = { Text("Search recipes...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )

                            // Categories
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(RecipeCategory.values()) { category ->
                                    FilterChip(
                                        selected = selectedCategories.contains(category),
                                        onClick = {
                                            selectedCategories =
                                                if (category == RecipeCategory.ALL) {
                                                setOf(RecipeCategory.ALL)
                                            } else {
                                                val newSet = selectedCategories.toMutableSet()
                                                if (newSet.contains(RecipeCategory.ALL)) {
                                                    newSet.remove(RecipeCategory.ALL)
                                                }
                                                if (newSet.contains(category)) {
                                                    newSet.remove(category)
                                                } else {
                                                    newSet.add(category)
                                                }
                                                if (newSet.isEmpty()) {
                                                    setOf(RecipeCategory.ALL)
                                                } else {
                                                    newSet
                                                }
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    )
                                }
                            }
                            
                            // Main content with scrolling
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn(animationSpec = tween(1000))
                            ) {
                                val filteredRecipes = when (selectedTab) {
                                    DashboardTab.HOME -> recipes.filter { recipe ->
                                        (selectedCategories.contains(RecipeCategory.ALL) || 
                                         selectedCategories.contains(recipe.category)) &&
                                        (searchQuery.isEmpty() || 
                                         recipe.name.contains(searchQuery, ignoreCase = true) ||
                                         recipe.description.contains(searchQuery, ignoreCase = true))
                                    }
                                    DashboardTab.FAVORITES -> recipes.filter { recipe ->
                                            userFavorites.contains(recipe.id) &&
                                        (selectedCategories.contains(RecipeCategory.ALL) || 
                                         selectedCategories.contains(recipe.category)) &&
                                        (searchQuery.isEmpty() || 
                                         recipe.name.contains(searchQuery, ignoreCase = true) ||
                                         recipe.description.contains(searchQuery, ignoreCase = true))
                                    }
                                    else -> emptyList()
                                }

                                // Grid layout for recipes
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(8.dp)
                                    ) {
                                        if (filteredRecipes.isNotEmpty()) {
                                            items(filteredRecipes) { recipe ->
                                                RecipeCard(
                                                    recipe = recipe,
                                                    isFavorite = userFavorites.contains(recipe.id),
                                                    onFavoriteClick = { 
                                                    scope.launch {
                                                        updateFavorites(recipe.id, !userFavorites.contains(recipe.id))
                                                    }
                                                    },
                                                onClick = {
                                                    showRecipeDetails = recipe
                                                }
                                                )
                                            }
                                        } else {
                                            item {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 32.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "No recipes found",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    DashboardTab.FAVORITES -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp)
                        ) {
                            // Search Bar
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                placeholder = { Text("Search recipes...") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search"
                                    )
                                },
                                singleLine = true,
                                shape = MaterialTheme.shapes.large
                            )
                            
                            // Category Filter
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(RecipeCategory.values()) { category ->
                                    FilterChip(
                                        selected = selectedCategories.contains(category),
                                        onClick = {
                                            selectedCategories =
                                                if (selectedCategories.contains(category)) {
                                                if (selectedCategories.size > 1) {
                                                    selectedCategories - category
                                                } else {
                                                    selectedCategories
                                                }
                                            } else {
                                                selectedCategories + category
                                            }
                                        },
                                        label = { Text(category.name) },
                                        leadingIcon = if (selectedCategories.contains(category)) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Box(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val filteredRecipes = recipes.filter { recipe ->
                                    userFavorites.contains(recipe.id) &&
                                    (searchQuery.isEmpty() || 
                                                    recipe.name.contains(
                                                        searchQuery,
                                                        ignoreCase = true
                                                    ) ||
                                                    recipe.description.contains(
                                                        searchQuery,
                                                        ignoreCase = true
                                                    )) &&
                                    (selectedCategories.contains(RecipeCategory.ALL) || 
                                     selectedCategories.contains(recipe.category))
                                }
                                
                                val scrollState = rememberLazyListState()
                                
                                LazyColumn(
                                    state = scrollState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Show all favorite recipes without separation
                                    if (filteredRecipes.isNotEmpty()) {
                                        items(filteredRecipes) { recipe ->
                                            RecipeCard(
                                                recipe = recipe,
                                                isFavorite = userFavorites.contains(recipe.id),
                                                onFavoriteClick = { 
                                                    scope.launch {
                                                        updateFavorites(recipe.id, !userFavorites.contains(recipe.id))
                                                    }
                                                },
                                                onClick = {
                                                    showRecipeDetails = recipe
                                                }
                                            )
                                        }
                                    } else {
                                        // Show message if no favorite recipes found
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "No favorite recipes yet",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    DashboardTab.SETTINGS -> {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Feedback Section
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Feedback",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "We'd love to hear your thoughts and suggestions to improve Blush Bistro.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        OutlinedTextField(
                                            value = feedbackText,
                                            onValueChange = { feedbackText = it },
                                            label = { Text("Your feedback") },
                                            modifier = Modifier.fillMaxWidth(),
                                            maxLines = 5,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = { feedbackText = "" },
                                                enabled = feedbackText.isNotBlank()
                                            ) {
                                                Text("Clear")
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    if (feedbackText.isNotBlank()) {
                                                        scope.launch {
                                                            try {
                                                                val currentUser = authService.getCurrentUser()
                                                                if (currentUser == null) {
                                                                    error = "Please sign in to submit feedback"
                                                                    return@launch
                                                                }

                                                                firestoreService.saveUserFeedback(
                                                                    currentUser.uid,
                                                                    feedbackText
                                                                ).onSuccess {
                                                                    feedbackText = ""
                                                                }.onFailure { e ->
                                                                    error = "Failed to submit feedback: ${e.message}"
                                                                }
                                                            } catch (e: Exception) {
                                                                error = "Failed to submit feedback: ${e.message}"
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = feedbackText.isNotBlank()
                                            ) {
                                                Text("Submit")
                                            }
                                        }
                                    }
                                }

                                // Notification Settings
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Notifications",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Enable Notifications",
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Switch(
                                                checked = notificationsEnabled,
                                                onCheckedChange = { newValue ->
                                                    scope.launch {
                                                        try {
                                                            settingsService.updateNotificationPreference(newValue)
                                                            notificationsEnabled = newValue
                                                        } catch (e: Exception) {
                                                            error = "Failed to update notifications: ${e.message}"
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }

                                // About Section
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "About",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Blush Bistro v1.0.0\n\n" +
                                                   "A modern recipe management app that helps you organize and discover delicious recipes.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Privacy Policy Section
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Privacy Policy",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Your privacy is important to us. We collect minimal data necessary to provide our services and improve your experience.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    DashboardTab.PROFILE -> {
                    UserProfileDialog(
                            onDismiss = { selectedTab = DashboardTab.HOME },
                        onSignOut = {
                                Log.d("DashboardScreen", "Profile dialog logout clicked")
                                try {
                                    // First sign out from Firebase
                                    authService.signOut()
                                    Log.d("DashboardScreen", "Firebase sign out successful from profile dialog")
                                    // Then navigate to login screen
                            onSignOut()
                                    Log.d("DashboardScreen", "Navigation callback called from profile dialog")
                                } catch (e: Exception) {
                                    Log.e("DashboardScreen", "Profile dialog logout failed: ${e.message}", e)
                                    error = "Failed to sign out: ${e.message}"
                                }
                        }
                        )
                    }
                }
                
                // Bottom Navigation
                NavigationBar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = { Text("Home") },
                        selected = selectedTab == DashboardTab.HOME,
                        onClick = { selectedTab = DashboardTab.HOME }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favorites"
                            )
                        },
                        label = { Text("Favorites") },
                        selected = selectedTab == DashboardTab.FAVORITES,
                        onClick = { selectedTab = DashboardTab.FAVORITES }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        selected = selectedTab == DashboardTab.SETTINGS,
                        onClick = { selectedTab = DashboardTab.SETTINGS }
                    )
                }
                
                // Floating Action Button (only show on HOME and FAVORITES tabs)
                if (selectedTab == DashboardTab.HOME || selectedTab == DashboardTab.FAVORITES) {
                    FloatingActionButton(
                        onClick = { showAddRecipeDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .padding(bottom = 56.dp),
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Recipe",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                // Recipe Details Dialog
                if (showRecipeDetails != null && !showAddRecipeDialog) {
                    RecipeDetailsDialog(
                        recipe = showRecipeDetails!!,
                            onDismiss = { showRecipeDetails = null },
                            onDelete = {
                                scope.launch {
                                    try {
                                    firestoreService.deleteRecipe(showRecipeDetails!!.id)
                                    recipes = recipes.filter { it.id != showRecipeDetails!!.id }
                                            showRecipeDetails = null
                                    } catch (e: Exception) {
                                    error = "Failed to delete recipe: ${e.message}"
                                    }
                                }
                            },
                            isFavorite = userFavorites.contains(showRecipeDetails!!.id),
                            onFavoriteClick = {
                            scope.launch {
                                updateFavorites(
                                    showRecipeDetails!!.id,
                                    !userFavorites.contains(showRecipeDetails!!.id)
                                )
                            }
                            }
                        )
                    }

                    // Add this inside the Scaffold content
                    if (showAddRecipeDialog) {
                        AddRecipeDialog(
                            onDismiss = { showAddRecipeDialog = false },
                            onRecipeAdded = { recipe ->
                                scope.launch {
                                    firestoreService.saveRecipe(recipe).onSuccess {
                                        loadRecipes()
                                    }.onFailure { exception ->
                                        error = exception.message ?: "Failed to save recipe"
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }


@Composable
fun RecipeDetailsDialog(
    recipe: Recipe,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteClick: () -> Unit = {}
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    MaterialTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite button
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Image(
                        painter = painterResource(id = recipe.getActualImageResId()),
                        contentDescription = "Recipe Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Cook Time: ${recipe.cookTime} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Category: ${recipe.category.name}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Ingredients:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    recipe.ingredients.forEach { ingredient ->
                        Text("• $ingredient")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Instructions:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    recipe.cookingInstructions.forEachIndexed { index, instruction ->
                        Text("${index + 1}. $instruction")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Only show delete button for non-predefined recipes
                    if (!recipe.userId.equals("predefined")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showDeleteConfirmation = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete"
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete")
                            }
                        }
                    }
                }
            },
            confirmButton = { }
        )

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Recipe") },
                text = { Text("Are you sure you want to delete this recipe?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun AddRecipeDialog(
    onDismiss: () -> Unit,
    onRecipeAdded: (Recipe) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RecipeCategory.ALL) }
    var ingredients by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    
    // Function to get the appropriate image resource ID based on category
    fun getImageResIdForCategory(category: RecipeCategory): Int {
        return when (category) {
            RecipeCategory.BREAKFAST -> R.drawable.avocado_toast_image
            RecipeCategory.LUNCH -> R.drawable.salad_image
            RecipeCategory.DINNER -> R.drawable.beef_bourguignon_image
            RecipeCategory.DESSERT -> R.drawable.chocolate_lava_cake_image
            RecipeCategory.ITALIAN -> R.drawable.pasta_primavera_image
            RecipeCategory.FRENCH -> R.drawable.ratatouille_image
            else -> R.drawable.cake_image
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Recipe") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Recipe Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cookTime,
                    onValueChange = { cookTime = it },
                    label = { Text("Cook Time") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Category dropdown using stable API
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Close dropdown" else "Open dropdown"
                        )
                    }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RecipeCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                    value = ingredients,
                    onValueChange = { ingredients = it },
                    label = { Text("Ingredients (one per line)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Instructions (one per line)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val imageRes = getImageResIdForCategory(selectedCategory)
                    val recipe = Recipe(
                        name = name,
                        description = description,
                        cookTime = cookTime,
                        imageResId = imageRes,  // Set the image resource based on category
                        category = selectedCategory,
                        rating = 0f,
                        reviewCount = 0,
                        ingredients = ingredients.split("\n").filter { it.isNotBlank() },
                        cookingInstructions = instructions.split("\n")
                            .filter { it.isNotBlank() }
                    )
                    onRecipeAdded(recipe)
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 

@Composable
private fun RecipeCard(
    recipe: Recipe,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Recipe Image
            Image(
                painter = painterResource(id = recipe.getActualImageResId()),
                contentDescription = "Recipe Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
            
            // Recipe Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recipe.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!recipe.userId.equals("predefined")) {
                        IconButton(onClick = onFavoriteClick) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Recipe Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cook Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Cook Time",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${recipe.cookTime} min",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Category",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = recipe.category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Rating
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", recipe.rating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Review Count
                Text(
                    text = "${recipe.reviewCount} reviews",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
} 
