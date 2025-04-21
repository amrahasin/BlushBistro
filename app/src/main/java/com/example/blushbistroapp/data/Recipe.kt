package com.example.blushbistroapp.data

import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.example.blushbistroapp.R
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Recipe(
    @DocumentId
    val id: String = "",
    
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("name")
    val name: String = "",
    
    @PropertyName("description")
    val description: String = "",
    
    @PropertyName("cookTime")
    val cookTime: String = "",
    
    @PropertyName("imageUrl")
    val imageUrl: String = "",
    
    @PropertyName("category")
    val category: RecipeCategory = RecipeCategory.ALL,
    
    @PropertyName("rating")
    val rating: Float = 0f,
    
    @PropertyName("reviewCount")
    val reviewCount: Int = 0,
    
    @PropertyName("ingredients")
    val ingredients: List<String> = emptyList(),
    
    @PropertyName("cookingInstructions")
    val cookingInstructions: List<String> = emptyList(),
    
    @PropertyName("createdAt")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @Exclude
    val imageResId: Int = R.drawable.cake_image
) {
    companion object {
        fun createEmpty(): Recipe = Recipe(
            id = "",
            userId = "",
            name = "",
            description = "",
            cookTime = "",
            imageUrl = "",
            category = RecipeCategory.ALL,
            rating = 0f,
            reviewCount = 0,
            ingredients = emptyList(),
            cookingInstructions = emptyList(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            imageResId = R.drawable.cake_image
        )

        // Function to get the appropriate image resource ID based on recipe name
        fun getImageResourceId(recipeName: String): Int {
            return when (recipeName.lowercase()) {
                "classic margherita pizza" -> R.drawable.margherita_pizza_image
                "greek salad" -> R.drawable.greek_salad_image
                "chocolate cake" -> R.drawable.cake_image
                "spaghetti bolognese" -> R.drawable.pasta_image
                "caesar salad" -> R.drawable.caesar_salad_image
                "classic caesar salad" -> R.drawable.caesar_salad_image
                "berry smoothie" -> R.drawable.berry_smoothie
                "chicken parmesan" -> R.drawable.chicken_parmesan_image
                "vegetable curry" -> R.drawable.vegetable_curry_image
                "stir fry" -> R.drawable.stir_fry_image
                "grilled salmon" -> R.drawable.grilled_salmon_image
                "stuffed pepper" -> R.drawable.stuffed_pepper
                "vegetable lasagna" -> R.drawable.vegetable_lasagna
                "tacos" -> R.drawable.tacos_image
                "salmon bowl" -> R.drawable.salmon_bowl_image
                "risotto" -> R.drawable.risotto_image
                "ratatouille" -> R.drawable.ratatouille_image
                "pasta primavera" -> R.drawable.pasta_primavera_image
                "pancakes" -> R.drawable.pancakes_image
                "chocolate mousse" -> R.drawable.chocolate_mousse_image
                "chocolate lava cake" -> R.drawable.chocolate_lava_cake_image
                "buddha bowl" -> R.drawable.buddha_bowl_image
                "beef bourguignon" -> R.drawable.beef_bourguignon_image
                "banana bread" -> R.drawable.banana_bread_image
                "avocado toast" -> R.drawable.avocado_toast_image
                // Additional mappings for exact recipe names
                "blueberry pancakes" -> R.drawable.pancakes_image
                "grilled salmon bowl" -> R.drawable.salmon_bowl_image
                "mushroom risotto" -> R.drawable.risotto_image
                "vegan buddha bowl" -> R.drawable.buddha_bowl_image
                "gluten-free pasta primavera" -> R.drawable.pasta_primavera_image
                "vegan chocolate mousse" -> R.drawable.chocolate_mousse_image
                "gluten-free banana bread" -> R.drawable.banana_bread_image
                "stuffed bell peppers" -> R.drawable.stuffed_pepper
                "vegetable stir fry" -> R.drawable.stir_fry_image
                else -> R.drawable.cake_image
            }
        }
    }

    // Function to get the actual image resource ID for this recipe
    fun getActualImageResId(): Int {
        // If the imageResId is not the default value, use it
        if (imageResId != R.drawable.cake_image) {
            return imageResId
        }
        
        // Try to get image based on category first
        val categoryImage = when (category) {
            RecipeCategory.BREAKFAST -> R.drawable.avocado_toast_image
            RecipeCategory.LUNCH -> R.drawable.salad_image
            RecipeCategory.DINNER -> R.drawable.beef_bourguignon_image
            RecipeCategory.DESSERT -> R.drawable.chocolate_lava_cake_image
            RecipeCategory.ITALIAN -> R.drawable.pasta_primavera_image
            RecipeCategory.FRENCH -> R.drawable.ratatouille_image
            else -> null
        }
        
        if (categoryImage != null) {
            return categoryImage
        }
        
        // If no category image, try name-based matching
        return getImageResourceId(name)
    }
} 