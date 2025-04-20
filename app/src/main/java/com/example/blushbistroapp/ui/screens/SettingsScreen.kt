package com.example.blushbistroapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blushbistroapp.data.FirebaseAuthService
import com.example.blushbistroapp.data.FirestoreService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val firestoreService = remember { FirestoreService.getInstance(FirebaseFirestore.getInstance()) }
    val authService = remember { FirebaseAuthService.getInstance(context) }
    val currentUser = authService.getCurrentUser()

    var feedbackText by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var showErrorMessage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val scrollState = rememberScrollState()
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
                .verticalScroll(scrollState)
            .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
            // Notifications Section
        Card(
            modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                        text = "Notifications",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                SettingsItem(
                    icon = Icons.Default.Notifications,
                        title = "Push Notifications",
                        subtitle = "Receive updates about new recipes and features",
                        onClick = { /* TODO: Implement notification settings */ }
                    )
                }
            }
            
            // Feedback Section
        Card(
            modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                        text = "Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                    Text(
                        text = "We'd love to hear your thoughts and suggestions to improve the app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Your feedback") },
                        placeholder = { Text("Type your feedback here...") },
                        minLines = 3
                    )
                    
                    if (showSuccessMessage) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(3000)
                            showSuccessMessage = false
                        }
                        Text(
                            text = "Success",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
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
                                            currentUser?.uid?.let { userId ->
                                                firestoreService.saveUserFeedback(userId, feedbackText)
                                                    .onSuccess {
                                                        showSuccessMessage = true
                                                        feedbackText = ""
                                                    }
                                                    .onFailure { exception ->
                                                        errorMessage = exception.message ?: "Failed to send feedback"
                                                        showErrorMessage = true
                                                        kotlinx.coroutines.delay(3000)
                                                        showErrorMessage = false
                                                    }
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to send feedback: ${e.message}"
                                            showErrorMessage = true
                                            kotlinx.coroutines.delay(3000)
                                            showErrorMessage = false
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
            
            // About Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "Privacy Policy",
                        subtitle = "Read our privacy policy",
                        onClick = { /* TODO: Implement privacy policy */ }
                    )
                }
            }
        }
        
        // Error Message
        if (showErrorMessage) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { showErrorMessage = false }) {
                        Text("Dismiss")
                    }
                }
            ) {
                Text(errorMessage)
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
    }
} 