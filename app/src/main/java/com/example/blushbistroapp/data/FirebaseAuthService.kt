package com.example.blushbistroapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await

class FirebaseAuthService private constructor(private val context: Context) {
    private val auth: FirebaseAuth by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            Firebase.auth
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Auth", e)
            throw IllegalStateException("Failed to initialize Firebase Auth: ${e.message}")
        }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Sign in failed", e)
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> Result.failure(Exception("Invalid email format"))
                "ERROR_WRONG_PASSWORD" -> Result.failure(Exception("Incorrect password"))
                "ERROR_USER_NOT_FOUND" -> Result.failure(Exception("No account found with this email"))
                "ERROR_USER_DISABLED" -> Result.failure(Exception("This account has been disabled"))
                else -> Result.failure(Exception("Authentication failed: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed", e)
            Result.failure(Exception("Authentication failed: ${e.message}"))
        }
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Registration failed", e)
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> Result.failure(Exception("Invalid email format"))
                "ERROR_WEAK_PASSWORD" -> Result.failure(Exception("Password is too weak"))
                "ERROR_EMAIL_ALREADY_IN_USE" -> Result.failure(Exception("Email is already registered"))
                else -> Result.failure(Exception("Registration failed: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            // Try to send reset email directly
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully to $email")
            Result.success(Unit)
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "Password reset failed with FirebaseAuthException", e)
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> Result.failure(Exception("Please enter a valid email address"))
                "ERROR_USER_NOT_FOUND" -> Result.failure(Exception("No account found with this email address"))
                "ERROR_TOO_MANY_REQUESTS" -> Result.failure(Exception("Too many attempts. Please try again later"))
                else -> Result.failure(Exception("Failed to send reset email. Please try again"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Password reset failed with unexpected error", e)
            Result.failure(Exception("Failed to send reset email. Please try again"))
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }

    suspend fun isEmailRegistered(email: String): Boolean {
        return try {
            val result = auth.fetchSignInMethodsForEmail(email).await()
            result.signInMethods?.isNotEmpty() ?: false
        } catch (e: FirebaseAuthException) {
            when (e.errorCode) {
                "ERROR_INVALID_EMAIL" -> {
                    Log.e(TAG, "Invalid email format")
                    false
                }
                else -> {
                    Log.e(TAG, "Error checking email registration: ${e.message}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking email registration: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthService"
        
        @Volatile
        private var instance: FirebaseAuthService? = null

        @JvmStatic
        fun getInstance(context: Context): FirebaseAuthService {
            return instance ?: synchronized(this) {
                instance ?: FirebaseAuthService(context.applicationContext).also { instance = it }
            }
        }
    }
} 