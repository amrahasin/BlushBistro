package com.example.blushbistroapp.data

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
import com.example.blushbistroapp.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class FirestoreService @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val TAG = "FirestoreService"
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val recipesCollection = firestore.collection("recipes")
    private val usersCollection = firestore.collection("users")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    suspend fun saveRecipe(recipe: Recipe): Result<String> = try {
        val docRef = firestore.collection("recipes").document()
        val recipeWithId = recipe.copy(id = docRef.id)
        docRef.set(recipeWithId).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Log.e(TAG, "Error saving recipe", e)
        Result.failure(e)
    }

    fun getRecipe(recipeId: String): Flow<Result<Recipe>> = callbackFlow {
        val subscription = firestore.collection("recipes")
                .document(recipeId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                    Log.e(TAG, "Error getting recipe", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                    try {
                        val recipe = snapshot.toObject(Recipe::class.java)
                        if (recipe != null) {
                            trySend(Result.success(recipe))
                        } else {
                            trySend(Result.failure(Exception("Failed to deserialize recipe")))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing recipe", e)
                        trySend(Result.failure(e))
                        }
                    } else {
                        trySend(Result.failure(Exception("Recipe not found")))
                    }
                }
            awaitClose { subscription.remove() }
    }

    fun getRecipes(userId: String): Flow<Result<List<Recipe>>> = callbackFlow {
        val subscription = firestore.collection("recipes")
                .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                    Log.e(TAG, "Error getting recipes", error)
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                    try {
                        val recipes = snapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(Recipe::class.java)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error deserializing recipe ${doc.id}", e)
                                null
                            }
                        }
                        trySend(Result.success(recipes))
        } catch (e: Exception) {
                        Log.e(TAG, "Error processing recipes", e)
            trySend(Result.failure(e))
        }
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateRecipe(recipe: Recipe): Result<Unit> = try {
        firestore.collection("recipes")
            .document(recipe.id)
            .set(recipe)
            .await()
        Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating recipe", e)
            Result.failure(e)
        }

    suspend fun deleteRecipe(recipeId: String): Result<Unit> = try {
        firestore.collection("recipes")
            .document(recipeId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting recipe", e)
        Result.failure(e)
    }

    suspend fun addToFavorites(userId: String, recipeId: String): Result<Unit> = try {
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId)
            .set(mapOf("recipeId" to recipeId, "addedAt" to System.currentTimeMillis()))
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error adding to favorites", e)
        Result.failure(e)
    }

    suspend fun removeFromFavorites(userId: String, recipeId: String): Result<Unit> = try {
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId)
            .delete()
            .await()
            Result.success(Unit)
        } catch (e: Exception) {
        Log.e(TAG, "Error removing from favorites", e)
            Result.failure(e)
        }

    fun getFavorites(userId: String): Flow<Result<List<Recipe>>> = callbackFlow {
        val subscription = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting favorites", error)
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    coroutineScope.launch {
                        try {
                            val recipeIds = snapshot.documents.map { it.id }
                            if (recipeIds.isEmpty()) {
                                trySend(Result.success(emptyList()))
                                return@launch
                            }

                            val recipes = firestore.collection("recipes")
                                .whereIn("id", recipeIds)
                                .get()
                                .await()
                                .documents
                                .mapNotNull { doc ->
                                    try {
                                        doc.toObject(Recipe::class.java)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error deserializing favorite recipe ${doc.id}", e)
                                        null
                                    }
                                }
                            trySend(Result.success(recipes))
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing favorites", e)
                            trySend(Result.failure(e))
                        }
                    }
                } else {
                    trySend(Result.success(emptyList()))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun saveUserFavorites(userId: String, favorites: List<String>): Result<Unit> = try {
        usersCollection.document(userId).set(mapOf("favorites" to favorites)).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun getUserFavorites(userId: String): Flow<Result<List<String>>> = callbackFlow {
            val subscription = usersCollection
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val favorites = snapshot.get("favorites") as? List<String> ?: emptyList()
                        trySend(Result.success(favorites))
                    } else {
                        // If document doesn't exist, create it with empty favorites
                        usersCollection.document(userId).set(mapOf("favorites" to emptyList<String>()))
                            .addOnSuccessListener {
                                trySend(Result.success(emptyList()))
                            }
                            .addOnFailureListener { e ->
                                trySend(Result.failure(e))
                            }
                    }
                }
            awaitClose { subscription.remove() }
    }

    fun getRecipes(): Flow<Result<List<Recipe>>> = flow {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            val snapshot = recipesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
            
            val recipes = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Recipe::class.java)?.copy(id = doc.id)
            }
            emit(Result.success(recipes))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getUserFavorites(): Flow<Result<List<String>>> = flow {
        try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            val userDoc = usersCollection.document(currentUser.uid).get().await()
            val favorites = userDoc.get("favorites") as? List<String> ?: emptyList()
            emit(Result.success(favorites))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun saveUserFavorites(favorites: List<String>): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: throw Exception("User not authenticated")
            
            // Verify that all recipes exist before saving favorites
            val recipesToVerify = favorites.filter { it.isNotEmpty() }
            if (recipesToVerify.isNotEmpty()) {
                val recipesSnapshot = recipesCollection
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), recipesToVerify)
                    .get()
                    .await()
                
                // Only keep valid recipe IDs
                val validRecipeIds = recipesSnapshot.documents.map { it.id }
                val filteredFavorites = favorites.filter { validRecipeIds.contains(it) }
                
                // Save the filtered favorites
                usersCollection.document(currentUser.uid)
                    .set(mapOf("favorites" to filteredFavorites), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                Result.success(Unit)
            } else {
                // If no favorites to save, just clear the favorites field
                usersCollection.document(currentUser.uid)
                    .set(mapOf("favorites" to emptyList<String>()), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecipesForCurrentUser(): List<Recipe> {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "User must be logged in to view recipes")
                throw Exception("User must be logged in to view recipes")
            }

            // Get user's recipes
            val userRecipesSnapshot = recipesCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            // Get predefined recipes
            val predefinedRecipesSnapshot = recipesCollection
                .whereEqualTo("userId", "predefined")
                .get()
                .await()

            // Combine and deduplicate recipes
            val allRecipes = mutableListOf<Recipe>()
            val seenNames = mutableSetOf<String>()

            // Process user recipes first
            userRecipesSnapshot.documents.forEach { doc ->
                try {
                    val recipe = doc.toObject(Recipe::class.java)?.copy(id = doc.id)
                    if (recipe != null && !seenNames.contains(recipe.name)) {
                        seenNames.add(recipe.name)
                        allRecipes.add(recipe)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to Recipe: ${e.message}", e)
                }
            }

            // Then process predefined recipes
            predefinedRecipesSnapshot.documents.forEach { doc ->
                try {
                    val recipe = doc.toObject(Recipe::class.java)?.copy(id = doc.id)
                    if (recipe != null && !seenNames.contains(recipe.name)) {
                        seenNames.add(recipe.name)
                        allRecipes.add(recipe)
                }
                } catch (e: Exception) {
                    Log.e(TAG, "Error converting document to Recipe: ${e.message}", e)
                }
            }

            // Ensure each recipe has the correct image
            return allRecipes.map { recipe ->
                recipe.copy(imageResId = Recipe.getImageResourceId(recipe.name))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recipes: ${e.message}", e)
            throw Exception("Failed to get recipes: ${e.message}")
        }
    }

    suspend fun saveUserFeedback(userId: String, feedback: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                return Result.failure(Exception("User must be logged in to send feedback"))
            }

            val feedbackData = hashMapOf(
                "userId" to currentUser.uid,
                "feedback" to feedback,
                "timestamp" to FieldValue.serverTimestamp()
            )
            
            firestore.collection("feedback")
                .add(feedbackData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving feedback: ${e.message}", e)
            Result.failure(e)
        }
    }

    // Predefined recipes to be added to Firestore
    private val predefinedRecipes = listOf(
        Recipe(
            name = "Chocolate Dream Cake",
            description = "Rich chocolate cake with ganache and fresh berries",
            cookTime = "60",
            category = RecipeCategory.DESSERT,
            rating = 4.9f,
            reviewCount = 150,
            ingredients = listOf(
                "Dark chocolate, 8 oz",
                "All-purpose flour, 1 1/2 cups",
                "Eggs, 3 large",
                "Butter, 1 cup",
                "Sugar, 1 cup",
                "Cocoa powder, 3/4 cup",
                "Baking powder, 2 tsp",
                "Baking soda, 1 tsp",
                "Salt, 1/2 tsp",
                "Milk, 1 cup",
                "Vanilla extract, 2 tsp",
                "Fresh berries, 2 cups",
                "Whipped cream, 2 cups"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 350°F (175°C)",
                "Grease and flour two 9-inch round cake pans",
                "Line bottoms with parchment paper",
                "Chop dark chocolate into small pieces",
                "In a double boiler, melt chocolate and butter together",
                "Stir until smooth and glossy",
                "Remove from heat and let cool slightly",
                "Sift together flour, cocoa powder, baking powder, baking soda, and salt",
                "Whisk to ensure even distribution",
                "In a large bowl, beat eggs and sugar until light and fluffy",
                "Add vanilla extract and mix well",
                "Slowly pour in melted chocolate mixture while mixing",
                "Alternate adding dry ingredients and milk",
                "Mix until just combined (do not overmix)",
                "Divide batter evenly between prepared pans",
                "Smooth tops with a spatula",
                "Bake for 30-35 minutes",
                "Test with toothpick - should come out with moist crumbs",
                "Let cool in pans for 10 minutes",
                "Transfer to wire racks to cool completely",
                "Heat 1 cup heavy cream until steaming",
                "Pour over 8 oz chopped chocolate",
                "Let sit for 2 minutes",
                "Stir until smooth and glossy",
                "Let cool until spreadable consistency",
                "Place first cake layer on serving plate",
                "Spread 1/3 of ganache evenly",
                "Top with second cake layer",
                "Cover entire cake with remaining ganache",
                "Decorate with fresh berries",
                "Pipe whipped cream around edges",
                "Chill for at least 1 hour before serving"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Chocolate Dream Cake")
        ),
        Recipe(
            name = "Chocolate Lava Cake",
            description = "Decadent chocolate cake with a molten center",
            cookTime = "25",
            category = RecipeCategory.DESSERT,
            rating = 4.9f,
            reviewCount = 120,
            ingredients = listOf(
                "Dark chocolate, 6 oz",
                "Butter, 1/2 cup",
                "Eggs, 2 large",
                "Egg yolks, 2 large",
                "Sugar, 1/4 cup",
                "All-purpose flour, 1/4 cup",
                "Cocoa powder, 2 tbsp",
                "Vanilla extract, 1 tsp",
                "Salt, 1/4 tsp",
                "Powdered sugar, for dusting",
                "Fresh berries, for garnish",
                "Vanilla ice cream, for serving"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 425°F (220°C)",
                "Butter 4 (6-ounce) ramekins",
                "Dust with cocoa powder, tapping out excess",
                "Place ramekins on a baking sheet",
                "Chop chocolate into small pieces",
                "In a double boiler, melt chocolate and butter together",
                "Stir until smooth and glossy",
                "Remove from heat and let cool slightly",
                "In a large bowl, whisk eggs, egg yolks, and sugar",
                "Beat until light and pale yellow",
                "Add vanilla extract and mix well",
                "Slowly pour chocolate mixture into egg mixture",
                "Whisk constantly to prevent eggs from cooking",
                "Sift in flour, cocoa powder, and salt",
                "Gently fold until just combined",
                "Divide batter evenly among prepared ramekins",
                "Bake for 12-14 minutes",
                "Edges should be set but center still jiggly",
                "Remove from oven and let rest for 1 minute",
                "Run a knife around edges of each ramekin",
                "Invert onto serving plates",
                "Dust with powdered sugar",
                "Garnish with fresh berries",
                "Serve immediately with vanilla ice cream"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Chocolate Lava Cake")
        ),
        Recipe(
            name = "Classic Margherita Pizza",
            description = "Traditional Italian pizza with fresh basil and mozzarella",
            cookTime = "30",
            category = RecipeCategory.ITALIAN,
            rating = 4.8f,
            reviewCount = 120,
            ingredients = listOf(
                "Pizza dough, 1 ball",
                "Tomato sauce, 1/2 cup",
                "Fresh mozzarella, 8 oz",
                "Fresh basil leaves, 10 leaves",
                "Extra virgin olive oil, 2 tbsp",
                "Salt, 1 tsp",
                "Black pepper, 1/2 tsp",
                "Garlic powder, 1/2 tsp",
                "Dried oregano, 1 tsp"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 475°F (245°C)",
                "Place pizza stone in oven to heat",
                "Let dough rest at room temperature for 30 minutes",
                "On a floured surface, roll out dough",
                "Stretch to 12-inch circle",
                "Transfer to pizza peel or baking sheet",
                "Spread tomato sauce evenly over dough",
                "Leave 1/2-inch border for crust",
                "Season with salt, pepper, and garlic powder",
                "Tear mozzarella into pieces",
                "Distribute evenly over sauce",
                "Sprinkle with dried oregano",
                "Drizzle with olive oil",
                "Transfer pizza to hot stone",
                "Bake for 10-12 minutes",
                "Rotate halfway through",
                "Crust should be golden brown",
                "Remove from oven",
                "Top with fresh basil leaves",
                "Drizzle with more olive oil",
                "Let cool for 2 minutes",
                "Slice and serve"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Classic Margherita Pizza")
        ),
        Recipe(
            name = "Blueberry Pancakes",
            description = "Fluffy pancakes with fresh blueberries",
            cookTime = "20",
            category = RecipeCategory.BREAKFAST,
            rating = 4.8f,
            reviewCount = 90,
            ingredients = listOf(
                "All-purpose flour, 1 1/2 cups",
                "Baking powder, 3 1/2 tsp",
                "Salt, 1 tsp",
                "Sugar, 1 tbsp",
                "Milk, 1 1/4 cups",
                "Egg, 1 large",
                "Butter, 3 tbsp",
                "Fresh blueberries, 1 cup",
                "Maple syrup, for serving",
                "Powdered sugar, for dusting"
            ),
            cookingInstructions = listOf(
                "Heat griddle or large skillet over medium heat",
                "Melt 1 tbsp butter for cooking",
                "In a large bowl, whisk together flour, baking powder, salt, and sugar",
                "Make a well in the center",
                "In a separate bowl, whisk milk and egg",
                "Melt remaining butter and add to milk mixture",
                "Pour wet ingredients into well of dry ingredients",
                "Stir until just combined (lumps are okay)",
                "Gently fold in blueberries",
                "Pour 1/4 cup batter for each pancake",
                "Cook until bubbles form on surface",
                "Flip and cook until golden brown",
                "Keep warm in 200°F oven",
                "Stack pancakes on plates",
                "Dust with powdered sugar",
                "Drizzle with maple syrup",
                "Top with extra blueberries if desired"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Blueberry Pancakes")
        ),
        Recipe(
            name = "Grilled Salmon Bowl",
            description = "Healthy lunch bowl with grilled salmon and quinoa",
            cookTime = "30",
            category = RecipeCategory.LUNCH,
            rating = 4.9f,
            reviewCount = 75,
            ingredients = listOf(
                "Salmon fillet, 2 6-oz pieces",
                "Quinoa, 1 cup",
                "Mixed greens, 4 cups",
                "Avocado, 1 medium",
                "Cherry tomatoes, 1 cup",
                "Cucumber, 1/2 medium",
                "Lemon, 1 large",
                "Olive oil, 3 tbsp",
                "Dijon mustard, 1 tbsp",
                "Honey, 1 tsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Rinse quinoa under cold water",
                "Cook quinoa according to package instructions",
                "Let cool to room temperature",
                "Preheat grill to medium-high heat",
                "Pat salmon dry with paper towels",
                "Season with salt and pepper",
                "Drizzle with 1 tbsp olive oil",
                "Grill skin-side down for 4-5 minutes",
                "Flip and grill for 3-4 minutes more",
                "Squeeze lemon juice over salmon",
                "Whisk together olive oil, Dijon mustard, and honey",
                "Add juice of 1/2 lemon",
                "Season with salt and pepper",
                "Whisk until emulsified",
                "Divide mixed greens among bowls",
                "Top with cooked quinoa",
                "Add sliced avocado",
                "Place grilled salmon on top",
                "Scatter cherry tomatoes and cucumber",
                "Drizzle with dressing",
                "Garnish with lemon wedges"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Grilled Salmon Bowl")
        ),
        Recipe(
            name = "Beef Bourguignon",
            description = "Classic French beef stew in red wine sauce",
            cookTime = "180",
            category = RecipeCategory.FRENCH,
            rating = 4.7f,
            reviewCount = 85,
            ingredients = listOf(
                "Beef chuck, 3 lbs",
                "Bacon, 6 slices",
                "Red wine, 2 cups",
                "Beef stock, 2 cups",
                "Carrots, 3 medium",
                "Onions, 2 large",
                "Garlic, 4 cloves",
                "Mushrooms, 1 lb",
                "Tomato paste, 2 tbsp",
                "Fresh thyme, 4 sprigs",
                "Bay leaves, 2 leaves",
                "Butter, 2 tbsp",
                "Flour, 2 tbsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Cut beef into 2-inch cubes",
                "Pat dry with paper towels",
                "Season with salt and pepper",
                "Cook bacon in Dutch oven until crisp",
                "Remove and set aside",
                "Reserve fat in pot",
                "Brown beef in batches in bacon fat",
                "Remove and set aside",
                "Add more oil if needed",
                "Sauté onions and carrots until soft",
                "Add garlic and cook 1 minute",
                "Add tomato paste and cook 2 minutes",
                "Add mushrooms and cook until browned",
                "Return beef and bacon to pot",
                "Add wine and stock",
                "Add thyme and bay leaves",
                "Bring to simmer",
                "Cover and transfer to oven",
                "Cook at 325°F for 2.5 hours",
                "Remove from oven",
                "Skim fat from surface",
                "Make beurre manié (butter and flour paste)",
                "Whisk into stew to thicken",
                "Season with salt and pepper",
                "Remove bay leaves and thyme stems",
                "Serve hot with crusty bread",
                "Garnish with fresh parsley"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Beef Bourguignon")
        ),
        Recipe(
            name = "Mushroom Risotto",
            description = "Creamy Italian risotto with wild mushrooms",
            cookTime = "45",
            category = RecipeCategory.ITALIAN,
            rating = 4.6f,
            reviewCount = 95,
            ingredients = listOf(
                "Arborio rice, 1.5 cups",
                "Mixed mushrooms, 2 cups",
                "Vegetable broth, 4 cups",
                "White wine, 1/2 cup",
                "Parmesan cheese, 1/2 cup",
                "Onion, 1 medium",
                "Garlic, 3 cloves",
                "Butter, 3 tbsp",
                "Olive oil, 2 tbsp",
                "Fresh thyme, 2 tsp",
                "Salt and pepper, to taste",
                "Fresh parsley, 2 tbsp"
            ),
            cookingInstructions = listOf(
                "Heat broth and keep warm",
                "Clean and slice mushrooms",
                "Mince onion and garlic",
                "Heat 1 tbsp butter and 1 tbsp oil",
                "Sauté mushrooms until golden",
                "Season with salt and pepper",
                "Remove and set aside",
                "Heat remaining butter and oil",
                "Sauté onion until translucent",
                "Add garlic and cook 1 minute",
                "Add rice and toast 2 minutes",
                "Add wine and cook until absorbed",
                "Add broth 1/2 cup at a time",
                "Stir constantly until absorbed",
                "Continue until rice is al dente",
                "This should take about 20 minutes",
                "Stir in cooked mushrooms",
                "Add parmesan cheese",
                "Season with salt and pepper",
                "Add fresh thyme",
                "Let rest 2 minutes",
                "Garnish with fresh parsley",
                "Serve immediately",
                "Pass extra parmesan"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Mushroom Risotto")
        ),
        Recipe(
            name = "Vegan Buddha Bowl",
            description = "Colorful bowl packed with plant-based goodness",
            cookTime = "25",
            category = RecipeCategory.VEGAN,
            rating = 4.7f,
            reviewCount = 110,
            ingredients = listOf(
                "Quinoa, 1 cup",
                "Chickpeas, 1 can",
                "Sweet potato, 1 large",
                "Avocado, 1 medium",
                "Kale, 2 cups",
                "Red cabbage, 1/4 head",
                "Carrot, 1 large",
                "Cucumber, 1/2 medium",
                "Tahini, 2 tbsp",
                "Lemon juice, 1 tbsp",
                "Olive oil, 2 tbsp",
                "Cumin, 1 tsp",
                "Paprika, 1 tsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Cook quinoa according to package",
                "Preheat oven to 400°F (200°C)",
                "Drain and rinse chickpeas",
                "Peel and cube sweet potato",
                "Toss with 1 tbsp oil, cumin, paprika",
                "Roast for 20 minutes",
                "Turn halfway through",
                "Pat chickpeas dry",
                "Toss with remaining oil and spices",
                "Roast for 15 minutes",
                "Shake pan occasionally",
                "Massage kale with lemon juice",
                "Shred cabbage and carrot",
                "Slice cucumber and avocado",
                "Whisk tahini with lemon juice",
                "Add water to thin if needed",
                "Season with salt and pepper",
                "Divide quinoa among bowls",
                "Arrange vegetables around quinoa",
                "Add roasted sweet potatoes",
                "Top with roasted chickpeas",
                "Drizzle with tahini dressing"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Vegan Buddha Bowl")
        ),
        Recipe(
            name = "Gluten-Free Pasta Primavera",
            description = "Fresh vegetable pasta with gluten-free noodles",
            cookTime = "30",
            category = RecipeCategory.GLUTEN_FREE,
            rating = 4.5f,
            reviewCount = 65,
            ingredients = listOf(
                "Gluten-free pasta, 8 oz",
                "Broccoli florets, 2 cups",
                "Bell peppers, 2 medium",
                "Zucchini, 1 medium",
                "Cherry tomatoes, 1 cup",
                "Olive oil, 1/4 cup",
                "Garlic, 4 cloves",
                "Fresh basil, 1/4 cup",
                "Parmesan cheese, 1/2 cup",
                "Lemon zest, 1 tbsp",
                "Red pepper flakes, 1/2 tsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Bring large pot of salted water to boil",
                "Wash and prepare all vegetables",
                "Mince garlic",
                "Grate parmesan cheese",
                "Heat 2 tbsp oil in large skillet",
                "Sauté garlic until fragrant",
                "Add broccoli and bell peppers",
                "Cook for 3-4 minutes",
                "Add zucchini and tomatoes",
                "Cook until vegetables are crisp-tender",
                "Cook pasta according to package",
                "Reserve 1/2 cup pasta water",
                "Drain pasta",
                "Add pasta to vegetables",
                "Toss with remaining oil",
                "Add pasta water as needed",
                "Stir in basil and lemon zest",
                "Season with salt and pepper",
                "Top with parmesan cheese",
                "Sprinkle with red pepper flakes",
                "Garnish with fresh basil"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Gluten-Free Pasta Primavera")
        ),
        Recipe(
            name = "Vegan Chocolate Mousse",
            description = "Rich and creamy dairy-free chocolate dessert",
            cookTime = "20",
            category = RecipeCategory.VEGAN,
            rating = 4.9f,
            reviewCount = 75,
            ingredients = listOf(
                "Dark chocolate, 8 oz",
                "Silken tofu, 12 oz",
                "Maple syrup, 1/4 cup",
                "Vanilla extract, 1 tsp",
                "Coconut cream, 1/2 cup",
                "Cocoa powder, 2 tbsp",
                "Salt, 1/4 tsp",
                "Fresh berries, 1/2 cup",
                "Mint leaves, 4 leaves",
                "Shaved chocolate, 2 tbsp"
            ),
            cookingInstructions = listOf(
                "Chill coconut cream overnight",
                "Drain silken tofu",
                "Chop chocolate into small pieces",
                "Melt chocolate in double boiler",
                "Stir until smooth",
                "Let cool slightly",
                "Blend tofu until smooth",
                "Add maple syrup and vanilla",
                "Blend until combined",
                "Add melted chocolate",
                "Blend until smooth",
                "Whip chilled coconut cream",
                "Fold into chocolate mixture",
                "Add cocoa powder and salt",
                "Mix until combined",
                "Divide into serving glasses",
                "Chill for at least 2 hours",
                "Can be made day ahead",
                "Top with fresh berries",
                "Garnish with mint leaves",
                "Sprinkle with shaved chocolate"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Vegan Chocolate Mousse")
        ),
        Recipe(
            name = "Gluten-Free Banana Bread",
            description = "Moist and delicious gluten-free banana bread",
            cookTime = "60",
            category = RecipeCategory.GLUTEN_FREE,
            rating = 4.8f,
            reviewCount = 85,
            ingredients = listOf(
                "Gluten-free flour, 2 cups",
                "Ripe bananas, 3 medium",
                "Eggs, 2 large",
                "Coconut oil, 1/2 cup",
                "Maple syrup, 1/3 cup",
                "Baking soda, 1 tsp",
                "Baking powder, 1/2 tsp",
                "Cinnamon, 1 tsp",
                "Vanilla extract, 1 tsp",
                "Salt, 1/2 tsp",
                "Walnuts, 1/2 cup",
                "Dark chocolate chips, 1/2 cup"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 350°F (175°C)",
                "Grease 9x5-inch loaf pan",
                "Line with parchment paper",
                "Mash bananas in large bowl",
                "Add eggs and whisk",
                "Add melted coconut oil",
                "Add maple syrup and vanilla",
                "Mix until combined",
                "Whisk flour, baking soda, baking powder",
                "Add cinnamon and salt",
                "Mix well",
                "Add dry ingredients to wet",
                "Mix until just combined",
                "Fold in walnuts and chocolate chips",
                "Don't overmix",
                "Pour batter into prepared pan",
                "Smooth top with spatula",
                "Bake for 50-60 minutes",
                "Test with toothpick",
                "Let cool in pan 10 minutes",
                "Transfer to wire rack"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Gluten-Free Banana Bread")
        ),
        Recipe(
            name = "Ratatouille",
            description = "Classic French vegetable stew",
            cookTime = "60",
            category = RecipeCategory.FRENCH,
            rating = 4.8f,
            reviewCount = 75,
            ingredients = listOf(
                "Eggplant, 1 large",
                "Zucchini, 2 medium",
                "Bell peppers, 2 medium",
                "Tomatoes, 4 large",
                "Onion, 1 large",
                "Garlic, 4 cloves",
                "Fresh thyme, 4 sprigs",
                "Fresh basil, 1/4 cup",
                "Olive oil, 1/4 cup",
                "Salt and pepper, to taste",
                "Red wine vinegar, 1 tbsp"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 375°F (190°C)",
                "Wash and prepare all vegetables",
                "Mince garlic and herbs",
                "Slice eggplant, zucchini, and bell peppers",
                "Dice tomatoes and onion",
                "Toss vegetables with olive oil",
                "Season with salt and pepper",
                "Heat 2 tbsp oil in large pot",
                "Sauté onion until soft",
                "Add garlic and cook 1 minute",
                "Add tomatoes and cook 5 minutes",
                "Layer remaining vegetables",
                "Add thyme and basil",
                "Cover and simmer 30 minutes",
                "Remove from heat",
                "Add vinegar",
                "Adjust seasoning",
                "Let rest 10 minutes",
                "Garnish with fresh basil",
                "Drizzle with olive oil",
                "Serve with crusty bread"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Ratatouille")
        ),
        Recipe(
            name = "Vegetable Lasagna",
            description = "Layered pasta with rich tomato sauce and vegetables",
            cookTime = "60",
            category = RecipeCategory.VEGETARIAN,
            rating = 4.8f,
            reviewCount = 95,
            ingredients = listOf(
                "Lasagna noodles, 12 sheets",
                "Ricotta cheese, 15 oz",
                "Mozzarella cheese, 2 cups",
                "Parmesan cheese, 1 cup",
                "Eggplant, 1 medium",
                "Zucchini, 2 medium",
                "Bell peppers, 2 medium",
                "Spinach, 2 cups",
                "Tomato sauce, 3 cups",
                "Garlic, 4 cloves",
                "Fresh basil, 1/4 cup",
                "Olive oil, 2 tbsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 375°F (190°C)",
                "Cook lasagna noodles according to package",
                "Prepare all vegetables",
                "Slice eggplant, zucchini, and bell peppers",
                "Sauté garlic in olive oil",
                "Add vegetables and cook until tender",
                "Add spinach and cook until wilted",
                "Season with salt and pepper",
                "Mix ricotta with 1/2 cup parmesan",
                "Add chopped basil",
                "Season with salt and pepper",
                "Spread tomato sauce in baking dish",
                "Layer noodles, vegetables, cheese mixture",
                "Repeat layers",
                "Top with mozzarella and remaining parmesan",
                "Cover with foil",
                "Bake for 30 minutes",
                "Remove foil and bake 15 more minutes",
                "Let rest 10 minutes before serving"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Vegetable Lasagna")
        ),
        Recipe(
            name = "Stuffed Bell Peppers",
            description = "Colorful bell peppers filled with quinoa and vegetables",
            cookTime = "45",
            category = RecipeCategory.VEGETARIAN,
            rating = 4.7f,
            reviewCount = 80,
            ingredients = listOf(
                "Bell peppers, 4 large",
                "Quinoa, 1 cup",
                "Black beans, 1 can",
                "Corn, 1 cup",
                "Onion, 1 medium",
                "Garlic, 3 cloves",
                "Tomato sauce, 1 cup",
                "Cheddar cheese, 1 cup",
                "Cumin, 1 tsp",
                "Chili powder, 1 tsp",
                "Olive oil, 2 tbsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 375°F (190°C)",
                "Cook quinoa according to package",
                "Prepare bell peppers",
                "Sauté onion and garlic in oil",
                "Add black beans and corn",
                "Stir in cooked quinoa",
                "Add tomato sauce and spices",
                "Mix in half the cheese",
                "Cut tops off peppers",
                "Remove seeds and membranes",
                "Fill with quinoa mixture",
                "Top with remaining cheese",
                "Place in baking dish",
                "Cover with foil",
                "Bake for 30 minutes",
                "Remove foil and bake 10 more minutes"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Stuffed Bell Peppers")
        ),
        Recipe(
            name = "Grilled Salmon with Asparagus",
            description = "Perfectly grilled salmon with roasted vegetables",
            cookTime = "30",
            category = RecipeCategory.DINNER,
            rating = 4.9f,
            reviewCount = 110,
            ingredients = listOf(
                "Salmon fillets, 4 6-oz pieces",
                "Asparagus, 1 bunch",
                "Lemon, 2 medium",
                "Garlic, 3 cloves",
                "Fresh dill, 1/4 cup",
                "Olive oil, 3 tbsp",
                "Butter, 2 tbsp",
                "White wine, 1/4 cup",
                "Salt and pepper, to taste",
                "Red pepper flakes, 1/4 tsp"
            ),
            cookingInstructions = listOf(
                "Preheat grill to medium-high",
                "Prepare asparagus",
                "Mince garlic and chop dill",
                "Pat salmon dry",
                "Season with salt and pepper",
                "Drizzle with 1 tbsp oil",
                "Let rest 10 minutes",
                "Toss with 1 tbsp oil",
                "Season with salt and pepper",
                "Add minced garlic",
                "Place salmon skin-side down",
                "Grill for 4-5 minutes",
                "Flip and grill 3-4 minutes",
                "Add asparagus to grill",
                "Cook until tender",
                "Melt butter in pan",
                "Add wine and lemon juice",
                "Stir in dill and red pepper",
                "Cook until slightly reduced",
                "Plate salmon and asparagus",
                "Drizzle with sauce",
                "Garnish with lemon wedges"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Grilled Salmon with Asparagus")
        ),
        Recipe(
            name = "Chicken Parmesan",
            description = "Crispy breaded chicken with marinara and cheese",
            cookTime = "40",
            category = RecipeCategory.DINNER,
            rating = 4.8f,
            reviewCount = 95,
            ingredients = listOf(
                "Chicken breasts, 4 boneless",
                "Breadcrumbs, 1 cup",
                "Parmesan cheese, 1/2 cup",
                "Mozzarella cheese, 1 cup",
                "Eggs, 2 large",
                "Flour, 1/2 cup",
                "Marinara sauce, 2 cups",
                "Fresh basil, 1/4 cup",
                "Olive oil, 1/4 cup",
                "Garlic powder, 1 tsp",
                "Italian seasoning, 1 tbsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Preheat oven to 400°F (200°C)",
                "Pound chicken to even thickness",
                "Set up breading station",
                "Mix breadcrumbs with parmesan",
                "Add garlic powder and Italian seasoning",
                "Beat eggs in separate bowl",
                "Place flour in third bowl",
                "Season chicken with salt and pepper",
                "Dredge in flour",
                "Dip in egg",
                "Coat with breadcrumb mixture",
                "Heat oil in large skillet",
                "Cook chicken until golden",
                "Transfer to baking dish",
                "Top with sauce and cheese",
                "Bake until cheese melts",
                "Garnish with fresh basil",
                "Serve with pasta",
                "Add extra sauce if desired"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Chicken Parmesan")
        ),
        Recipe(
            name = "Classic Caesar Salad",
            description = "Traditional Caesar salad with homemade dressing and croutons",
            cookTime = "20",
            category = RecipeCategory.SALAD,
            rating = 4.7f,
            reviewCount = 85,
            ingredients = listOf(
                "Romaine lettuce, 2 heads",
                "Parmesan cheese, 1/2 cup",
                "Croutons, 2 cups",
                "Anchovy fillets, 4 pieces",
                "Garlic, 2 cloves",
                "Dijon mustard, 1 tsp",
                "Egg yolk, 1 large",
                "Lemon juice, 2 tbsp",
                "Olive oil, 1/2 cup",
                "Worcestershire sauce, 1 tsp",
                "Black pepper, 1/2 tsp",
                "Salt, to taste"
            ),
            cookingInstructions = listOf(
                "Wash and dry romaine lettuce",
                "Chop into bite-sized pieces",
                "Make dressing: blend anchovies and garlic",
                "Add egg yolk and mustard",
                "Whisk in lemon juice",
                "Slowly drizzle in olive oil",
                "Add Worcestershire sauce",
                "Season with salt and pepper",
                "Toss lettuce with dressing",
                "Add croutons and parmesan",
                "Serve immediately"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Classic Caesar Salad")
        ),
        Recipe(
            name = "Vegetable Stir Fry",
            description = "Quick and healthy Asian-style vegetable stir fry",
            cookTime = "25",
            category = RecipeCategory.VEGETARIAN,
            rating = 4.6f,
            reviewCount = 70,
            ingredients = listOf(
                "Broccoli, 2 cups",
                "Bell peppers, 2 medium",
                "Carrots, 2 medium",
                "Snap peas, 1 cup",
                "Mushrooms, 8 oz",
                "Ginger, 1 tbsp",
                "Garlic, 3 cloves",
                "Soy sauce, 3 tbsp",
                "Sesame oil, 2 tbsp",
                "Rice vinegar, 1 tbsp",
                "Honey, 1 tsp",
                "Cornstarch, 1 tsp",
                "Water, 1/4 cup"
            ),
            cookingInstructions = listOf(
                "Prepare all vegetables",
                "Cut into uniform pieces",
                "Mince garlic and ginger",
                "Mix sauce ingredients",
                "Heat wok or large skillet",
                "Add sesame oil",
                "Stir-fry garlic and ginger",
                "Add hard vegetables first",
                "Cook for 2-3 minutes",
                "Add softer vegetables",
                "Pour in sauce mixture",
                "Cook until sauce thickens",
                "Serve hot with rice"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Vegetable Stir Fry")
        ),
        Recipe(
            name = "Greek Salad",
            description = "Fresh Mediterranean salad with feta and olives",
            cookTime = "15",
            category = RecipeCategory.SALAD,
            rating = 4.8f,
            reviewCount = 95,
            ingredients = listOf(
                "Cucumber, 1 large",
                "Tomatoes, 4 medium",
                "Red onion, 1/2 medium",
                "Bell pepper, 1 medium",
                "Kalamata olives, 1/2 cup",
                "Feta cheese, 1/2 cup",
                "Extra virgin olive oil, 1/4 cup",
                "Red wine vinegar, 2 tbsp",
                "Dried oregano, 1 tsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Wash all vegetables",
                "Chop cucumber into chunks",
                "Cut tomatoes into wedges",
                "Slice red onion thinly",
                "Cut bell pepper into chunks",
                "Combine all vegetables",
                "Add olives and feta",
                "Mix oil and vinegar",
                "Add oregano and seasonings",
                "Toss with dressing",
                "Serve immediately"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Greek Salad")
        ),
        Recipe(
            name = "Vegetable Curry",
            description = "Spicy Indian vegetable curry with coconut milk",
            cookTime = "45",
            category = RecipeCategory.VEGETARIAN,
            rating = 4.7f,
            reviewCount = 80,
            ingredients = listOf(
                "Mixed vegetables, 4 cups",
                "Onion, 1 large",
                "Garlic, 4 cloves",
                "Ginger, 2 tbsp",
                "Tomatoes, 2 medium",
                "Coconut milk, 1 can",
                "Curry powder, 2 tbsp",
                "Garam masala, 1 tsp",
                "Turmeric, 1/2 tsp",
                "Cumin, 1 tsp",
                "Coriander, 1 tsp",
                "Vegetable oil, 2 tbsp",
                "Salt and pepper, to taste"
            ),
            cookingInstructions = listOf(
                "Prepare all vegetables",
                "Chop into uniform pieces",
                "Mince garlic and ginger",
                "Heat oil in large pot",
                "Sauté onions until soft",
                "Add garlic and ginger",
                "Cook until fragrant",
                "Add spices and cook",
                "Add vegetables",
                "Cook for 5 minutes",
                "Add tomatoes",
                "Pour in coconut milk",
                "Simmer until vegetables are tender",
                "Season to taste",
                "Serve with rice"
            ),
            userId = "predefined",
            imageResId = Recipe.getImageResourceId("Vegetable Curry")
        )
    )

    suspend fun initializePredefinedRecipes(): Result<Unit> {
        return try {
            // Check if predefined recipes already exist
            val existingPredefinedRecipes = recipesCollection
                .whereEqualTo("userId", "predefined")
                .limit(1)
                .get()
                .await()
            
            // If predefined recipes already exist, don't add them again
            if (!existingPredefinedRecipes.isEmpty) {
                return Result.success(Unit)
            }
            
            // Add each predefined recipe to Firestore
            var successCount = 0
            for (recipe in predefinedRecipes) {
                try {
                    recipesCollection.add(recipe).await()
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding recipe ${recipe.name}: ${e.message}", e)
                }
            }
            
            if (successCount > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to add any predefined recipes"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing predefined recipes", e)
            Result.failure(e)
        }
    }

    suspend fun checkFirestorePermissions(): Result<Unit> {
        return try {
            Log.d(TAG, "Checking Firestore permissions...")
            
            // Try to read from the recipes collection to check permissions
            val testDoc = recipesCollection.limit(1).get().await()
            Log.d(TAG, "Successfully accessed Firestore. Found ${testDoc.size()} documents.")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Firestore permissions: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun addTestRecipe(): Result<Unit> {
        return try {
            Log.d(TAG, "Adding test recipe...")
            
            val testRecipe = Recipe(
                name = "Test Recipe",
                description = "This is a test recipe to check if Firestore is working correctly.",
                cookTime = "10",
                category = RecipeCategory.BREAKFAST,
                rating = 4.0f,
                reviewCount = 1,
                ingredients = listOf("Test ingredient 1", "Test ingredient 2"),
                cookingInstructions = listOf("Test instruction 1", "Test instruction 2"),
                userId = "test"
            )
            
            recipesCollection.add(testRecipe).await()
            Log.d(TAG, "Successfully added test recipe")
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding test recipe: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: FirestoreService? = null

        @JvmStatic
        fun getInstance(firestore: FirebaseFirestore): FirestoreService {
            return instance ?: synchronized(this) {
                instance ?: FirestoreService(firestore).also { instance = it }
            }
        }
    }
} 