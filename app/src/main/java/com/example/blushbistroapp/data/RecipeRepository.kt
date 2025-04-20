package com.example.blushbistroapp.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RecipeRepository(private val firestoreService: FirestoreService) {
    companion object {
        private const val TAG = "RecipeRepository"
        @Volatile
        private var instance: RecipeRepository? = null

        fun getInstance(firestoreService: FirestoreService): RecipeRepository {
            return instance ?: synchronized(this) {
                instance ?: RecipeRepository(firestoreService).also { instance = it }
            }
        }
    }

    suspend fun saveRecipe(recipe: Recipe): Result<Unit> {
        return try {
            firestoreService.saveRecipe(recipe).map { Unit }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving recipe", e)
            Result.failure(e)
        }
    }

    fun getRecipe(recipeId: String): Flow<Result<Recipe>> = flow {
        try {
            firestoreService.getRecipe(recipeId).collect { result ->
                emit(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipe", e)
            emit(Result.failure(e))
        }
    }

    fun getRecipes(userId: String? = null): Flow<Result<List<Recipe>>> = flow {
        try {
            if (userId != null) {
                firestoreService.getRecipes(userId).collect { result ->
                    emit(result)
                }
            } else {
                // If userId is null, use getRecipesForCurrentUser instead
                val recipes = firestoreService.getRecipesForCurrentUser()
                emit(Result.success(recipes))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipes", e)
            emit(Result.failure(e))
        }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Recipe> {
        return try {
            firestoreService.updateRecipe(recipe).map { recipe }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating recipe", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> {
        return try {
            firestoreService.deleteRecipe(recipeId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recipe", e)
            Result.failure(e)
        }
    }

    suspend fun saveUserFavorites(userId: String, recipeIds: List<String>): Result<Unit> {
        return try {
            firestoreService.saveUserFavorites(userId, recipeIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user favorites", e)
            Result.failure(e)
        }
    }

    fun getUserFavorites(userId: String): Flow<Result<List<String>>> = flow {
        try {
            firestoreService.getUserFavorites(userId).collect { result ->
                emit(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user favorites", e)
            emit(Result.failure(e))
        }
    }
} 