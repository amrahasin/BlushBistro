package com.example.blushbistroapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.blushbistroapp.R
import com.example.blushbistroapp.data.FirebaseAuthService
import com.example.blushbistroapp.ui.components.ThemeToggle
import com.example.blushbistroapp.ui.components.UserProfileDialog
import androidx.compose.foundation.Image
import com.example.blushbistroapp.ui.screens.SettingsScreen

enum class RecipeCategory {
    ALL, BREAKFAST, LUNCH, DINNER, DESSERT, VEGETARIAN, VEGAN, GLUTEN_FREE, ITALIAN, FRENCH
}

enum class DashboardTab {
    HOME, FAVORITES, PROFILE, SETTINGS
}

data class Ingredient(
    val name: String,
    val quantity: String,
    val unit: String
)

data class Recipe(
    val name: String,
    val description: String,
    val cookTime: String,
    val imageResId: Int,
    val category: RecipeCategory,
    val rating: Float,
    val reviewCount: Int,
    val ingredients: List<Ingredient>,
    val cookingInstructions: String
)

@Composable
fun DashboardScreen(
    onSignOut: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategories by remember { mutableStateOf(setOf(RecipeCategory.ALL)) }
    var showAddRecipeDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }
    var showRecipeDetails by remember { mutableStateOf<Recipe?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var favoriteRecipes by remember { mutableStateOf(setOf<String>()) }
    var showCookingMode by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    // Sample recipe data
    val recipes = remember {
        listOf(
            Recipe(
                "Chocolate Dream Cake",
                "Rich chocolate cake with ganache and fresh berries",
                "60 min",
                R.drawable.cake_image,
                RecipeCategory.DESSERT,
                4.9f,
                150,
                listOf(
                    Ingredient("Dark chocolate", "8", "oz"),
                    Ingredient("All-purpose flour", "1 1/2", "cups"),
                    Ingredient("Eggs", "3", "large"),
                    Ingredient("Butter", "1", "cup"),
                    Ingredient("Sugar", "1", "cup"),
                    Ingredient("Cocoa powder", "3/4", "cup"),
                    Ingredient("Baking powder", "2", "tsp"),
                    Ingredient("Baking soda", "1", "tsp"),
                    Ingredient("Salt", "1/2", "tsp"),
                    Ingredient("Milk", "1", "cup"),
                    Ingredient("Vanilla extract", "2", "tsp"),
                    Ingredient("Fresh berries", "2", "cups"),
                    Ingredient("Whipped cream", "2", "cups")
                ),
                """
                    Preparation:
                    1. Preheat oven to 350°F (175°C)
                    2. Grease and flour two 9-inch round cake pans
                    3. Line bottoms with parchment paper
                    
                    Chocolate Mixture:
                    1. Chop dark chocolate into small pieces
                    2. In a double boiler, melt chocolate and butter together
                    3. Stir until smooth and glossy
                    4. Remove from heat and let cool slightly
                    
                    Dry Ingredients:
                    1. Sift together flour, cocoa powder, baking powder, baking soda, and salt
                    2. Whisk to ensure even distribution
                    
                    Wet Ingredients:
                    1. In a large bowl, beat eggs and sugar until light and fluffy
                    2. Add vanilla extract and mix well
                    3. Slowly pour in melted chocolate mixture while mixing
                    4. Alternate adding dry ingredients and milk
                    5. Mix until just combined (do not overmix)
                    
                    Baking:
                    1. Divide batter evenly between prepared pans
                    2. Smooth tops with a spatula
                    3. Bake for 30-35 minutes
                    4. Test with toothpick - should come out with moist crumbs
                    5. Let cool in pans for 10 minutes
                    6. Transfer to wire racks to cool completely
                    
                    Ganache:
                    1. Heat 1 cup heavy cream until steaming
                    2. Pour over 8 oz chopped chocolate
                    3. Let sit for 2 minutes
                    4. Stir until smooth and glossy
                    5. Let cool until spreadable consistency
                    
                    Assembly:
                    1. Place first cake layer on serving plate
                    2. Spread 1/3 of ganache evenly
                    3. Top with second cake layer
                    4. Cover entire cake with remaining ganache
                    5. Decorate with fresh berries
                    6. Pipe whipped cream around edges
                    7. Chill for at least 1 hour before serving
                """.trimIndent()
            ),
            Recipe(
                "Chocolate Lava Cake",
                "Decadent chocolate cake with a molten center",
                "25 min",
                R.drawable.chocolate_lava_cake_image,
                RecipeCategory.DESSERT,
                4.9f,
                120,
                listOf(
                    Ingredient("Dark chocolate", "6", "oz"),
                    Ingredient("Butter", "1/2", "cup"),
                    Ingredient("Eggs", "2", "large"),
                    Ingredient("Egg yolks", "2", "large"),
                    Ingredient("Sugar", "1/4", "cup"),
                    Ingredient("All-purpose flour", "1/4", "cup"),
                    Ingredient("Cocoa powder", "2", "tbsp"),
                    Ingredient("Vanilla extract", "1", "tsp"),
                    Ingredient("Salt", "1/4", "tsp"),
                    Ingredient("Powdered sugar", "for", "dusting"),
                    Ingredient("Fresh berries", "for", "garnish"),
                    Ingredient("Vanilla ice cream", "for", "serving")
                ),
                """
                    Preparation:
                    1. Preheat oven to 425°F (220°C)
                    2. Butter 4 (6-ounce) ramekins
                    3. Dust with cocoa powder, tapping out excess
                    4. Place ramekins on a baking sheet
                    
                    Chocolate Mixture:
                    1. Chop chocolate into small pieces
                    2. In a double boiler, melt chocolate and butter together
                    3. Stir until smooth and glossy
                    4. Remove from heat and let cool slightly
                    
                    Egg Mixture:
                    1. In a large bowl, whisk eggs, egg yolks, and sugar
                    2. Beat until light and pale yellow
                    3. Add vanilla extract and mix well
                    
                    Combining:
                    1. Slowly pour chocolate mixture into egg mixture
                    2. Whisk constantly to prevent eggs from cooking
                    3. Sift in flour, cocoa powder, and salt
                    4. Gently fold until just combined
                    
                    Baking:
                    1. Divide batter evenly among prepared ramekins
                    2. Bake for 12-14 minutes
                    3. Edges should be set but center still jiggly
                    4. Remove from oven and let rest for 1 minute
                    
                    Serving:
                    1. Run a knife around edges of each ramekin
                    2. Invert onto serving plates
                    3. Dust with powdered sugar
                    4. Garnish with fresh berries
                    5. Serve immediately with vanilla ice cream
                    
                    Tips:
                    - Do not overbake - center should be molten
                    - Can be prepared ahead and refrigerated
                    - Add 1-2 minutes to baking time if chilled
                    - Serve immediately after baking
                """.trimIndent()
            ),
            Recipe(
                "Classic Margherita Pizza",
                "Traditional Italian pizza with fresh basil and mozzarella",
                "30 min",
                R.drawable.margherita_pizza_image,
                RecipeCategory.ITALIAN,
                4.8f,
                120,
                listOf(
                    Ingredient("Pizza dough", "1", "ball"),
                    Ingredient("Tomato sauce", "1/2", "cup"),
                    Ingredient("Fresh mozzarella", "8", "oz"),
                    Ingredient("Fresh basil leaves", "10", "leaves"),
                    Ingredient("Extra virgin olive oil", "2", "tbsp"),
                    Ingredient("Salt", "1", "tsp"),
                    Ingredient("Black pepper", "1/2", "tsp"),
                    Ingredient("Garlic powder", "1/2", "tsp"),
                    Ingredient("Dried oregano", "1", "tsp")
                ),
                """
                    Preparation:
                    1. Preheat oven to 475°F (245°C)
                    2. Place pizza stone in oven to heat
                    3. Let dough rest at room temperature for 30 minutes
                    
                    Dough:
                    1. On a floured surface, roll out dough
                    2. Stretch to 12-inch circle
                    3. Transfer to pizza peel or baking sheet
                    
                    Sauce:
                    1. Spread tomato sauce evenly over dough
                    2. Leave 1/2-inch border for crust
                    3. Season with salt, pepper, and garlic powder
                    
                    Toppings:
                    1. Tear mozzarella into pieces
                    2. Distribute evenly over sauce
                    3. Sprinkle with dried oregano
                    4. Drizzle with olive oil
                    
                    Baking:
                    1. Transfer pizza to hot stone
                    2. Bake for 10-12 minutes
                    3. Rotate halfway through
                    4. Crust should be golden brown
                    
                    Finishing:
                    1. Remove from oven
                    2. Top with fresh basil leaves
                    3. Drizzle with more olive oil
                    4. Let cool for 2 minutes
                    5. Slice and serve
                    
                    Tips:
                    - Use fresh, high-quality ingredients
                    - Don't overload with toppings
                    - Can use store-bought dough
                    - Add red pepper flakes for heat
                """.trimIndent()
            ),
            Recipe(
                "Blueberry Pancakes",
                "Fluffy pancakes with fresh blueberries",
                "20 min",
                R.drawable.pancakes_image,
                RecipeCategory.BREAKFAST,
                4.8f,
                90,
                listOf(
                    Ingredient("All-purpose flour", "1 1/2", "cups"),
                    Ingredient("Baking powder", "3 1/2", "tsp"),
                    Ingredient("Salt", "1", "tsp"),
                    Ingredient("Sugar", "1", "tbsp"),
                    Ingredient("Milk", "1 1/4", "cups"),
                    Ingredient("Egg", "1", "large"),
                    Ingredient("Butter", "3", "tbsp"),
                    Ingredient("Fresh blueberries", "1", "cup"),
                    Ingredient("Maple syrup", "for", "serving"),
                    Ingredient("Powdered sugar", "for", "dusting")
                ),
                """
                    Preparation:
                    1. Heat griddle or large skillet over medium heat
                    2. Melt 1 tbsp butter for cooking
                    
                    Dry Ingredients:
                    1. In a large bowl, whisk together flour, baking powder, salt, and sugar
                    2. Make a well in the center
                    
                    Wet Ingredients:
                    1. In a separate bowl, whisk milk and egg
                    2. Melt remaining butter and add to milk mixture
                    3. Pour wet ingredients into well of dry ingredients
                    4. Stir until just combined (lumps are okay)
                    5. Gently fold in blueberries
                    
                    Cooking:
                    1. Pour 1/4 cup batter for each pancake
                    2. Cook until bubbles form on surface
                    3. Flip and cook until golden brown
                    4. Keep warm in 200°F oven
                    
                    Serving:
                    1. Stack pancakes on plates
                    2. Dust with powdered sugar
                    3. Drizzle with maple syrup
                    4. Top with extra blueberries if desired
                    
                    Tips:
                    - Don't overmix the batter
                    - Keep heat at medium to prevent burning
                    - Use fresh blueberries for best results
                    - Can substitute frozen blueberries
                """.trimIndent()
            ),
            Recipe(
                "Grilled Salmon Bowl",
                "Healthy lunch bowl with grilled salmon and quinoa",
                "30 min",
                R.drawable.salmon_bowl_image,
                RecipeCategory.LUNCH,
                4.9f,
                75,
                listOf(
                    Ingredient("Salmon fillet", "2", "6-oz pieces"),
                    Ingredient("Quinoa", "1", "cup"),
                    Ingredient("Mixed greens", "4", "cups"),
                    Ingredient("Avocado", "1", "medium"),
                    Ingredient("Cherry tomatoes", "1", "cup"),
                    Ingredient("Cucumber", "1/2", "medium"),
                    Ingredient("Lemon", "1", "large"),
                    Ingredient("Olive oil", "3", "tbsp"),
                    Ingredient("Dijon mustard", "1", "tbsp"),
                    Ingredient("Honey", "1", "tsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Rinse quinoa under cold water
                    2. Cook quinoa according to package instructions
                    3. Let cool to room temperature
                    
                    Salmon:
                    1. Preheat grill to medium-high heat
                    2. Pat salmon dry with paper towels
                    3. Season with salt and pepper
                    4. Drizzle with 1 tbsp olive oil
                    5. Grill skin-side down for 4-5 minutes
                    6. Flip and grill for 3-4 minutes more
                    7. Squeeze lemon juice over salmon
                    
                    Dressing:
                    1. Whisk together olive oil, Dijon mustard, and honey
                    2. Add juice of 1/2 lemon
                    3. Season with salt and pepper
                    4. Whisk until emulsified
                    
                    Assembly:
                    1. Divide mixed greens among bowls
                    2. Top with cooked quinoa
                    3. Add sliced avocado
                    4. Place grilled salmon on top
                    5. Scatter cherry tomatoes and cucumber
                    6. Drizzle with dressing
                    7. Garnish with lemon wedges
                    
                    Tips:
                    - Can use any type of salmon
                    - Substitute brown rice for quinoa
                    - Add other vegetables as desired
                    - Dressing can be made ahead
                """.trimIndent()
            ),
            Recipe(
                "Beef Bourguignon",
                "Classic French beef stew in red wine sauce",
                "3 hours",
                R.drawable.beef_bourguignon_image,
                RecipeCategory.FRENCH,
                4.7f,
                85,
                listOf(
                    Ingredient("Beef chuck", "3", "lbs"),
                    Ingredient("Bacon", "6", "slices"),
                    Ingredient("Red wine", "2", "cups"),
                    Ingredient("Beef stock", "2", "cups"),
                    Ingredient("Carrots", "3", "medium"),
                    Ingredient("Onions", "2", "large"),
                    Ingredient("Garlic", "4", "cloves"),
                    Ingredient("Mushrooms", "1", "lb"),
                    Ingredient("Tomato paste", "2", "tbsp"),
                    Ingredient("Fresh thyme", "4", "sprigs"),
                    Ingredient("Bay leaves", "2", "leaves"),
                    Ingredient("Butter", "2", "tbsp"),
                    Ingredient("Flour", "2", "tbsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Cut beef into 2-inch cubes
                    2. Pat dry with paper towels
                    3. Season with salt and pepper
                    
                    Bacon:
                    1. Cook bacon in Dutch oven until crisp
                    2. Remove and set aside
                    3. Reserve fat in pot
                    
                    Beef:
                    1. Brown beef in batches in bacon fat
                    2. Remove and set aside
                    3. Add more oil if needed
                    
                    Vegetables:
                    1. Sauté onions and carrots until soft
                    2. Add garlic and cook 1 minute
                    3. Add tomato paste and cook 2 minutes
                    4. Add mushrooms and cook until browned
                    
                    Braising:
                    1. Return beef and bacon to pot
                    2. Add wine and stock
                    3. Add thyme and bay leaves
                    4. Bring to simmer
                    5. Cover and transfer to oven
                    6. Cook at 325°F for 2.5 hours
                    
                    Finishing:
                    1. Remove from oven
                    2. Skim fat from surface
                    3. Make beurre manié (butter and flour paste)
                    4. Whisk into stew to thicken
                    5. Season with salt and pepper
                    
                    Serving:
                    1. Remove bay leaves and thyme stems
                    2. Serve hot with crusty bread
                    3. Garnish with fresh parsley
                    
                    Tips:
                    - Use good quality red wine
                    - Can make ahead and reheat
                    - Serve with mashed potatoes
                    - Freezes well
                """.trimIndent()
            ),
            Recipe(
                "Mushroom Risotto",
                "Creamy Italian risotto with wild mushrooms",
                "45 min",
                R.drawable.risotto_image,
                RecipeCategory.ITALIAN,
                4.6f,
                95,
                listOf(
                    Ingredient("Arborio rice", "1.5", "cups"),
                    Ingredient("Mixed mushrooms", "2", "cups"),
                    Ingredient("Vegetable broth", "4", "cups"),
                    Ingredient("White wine", "1/2", "cup"),
                    Ingredient("Parmesan cheese", "1/2", "cup"),
                    Ingredient("Onion", "1", "medium"),
                    Ingredient("Garlic", "3", "cloves"),
                    Ingredient("Butter", "3", "tbsp"),
                    Ingredient("Olive oil", "2", "tbsp"),
                    Ingredient("Fresh thyme", "2", "tsp"),
                    Ingredient("Salt and pepper", "to", "taste"),
                    Ingredient("Fresh parsley", "2", "tbsp")
                ),
                """
                    Preparation:
                    1. Heat broth and keep warm
                    2. Clean and slice mushrooms
                    3. Mince onion and garlic
                    
                    Mushrooms:
                    1. Heat 1 tbsp butter and 1 tbsp oil
                    2. Sauté mushrooms until golden
                    3. Season with salt and pepper
                    4. Remove and set aside
                    
                    Base:
                    1. Heat remaining butter and oil
                    2. Sauté onion until translucent
                    3. Add garlic and cook 1 minute
                    4. Add rice and toast 2 minutes
                    
                    Cooking:
                    1. Add wine and cook until absorbed
                    2. Add broth 1/2 cup at a time
                    3. Stir constantly until absorbed
                    4. Continue until rice is al dente
                    5. This should take about 20 minutes
                    
                    Finishing:
                    1. Stir in cooked mushrooms
                    2. Add parmesan cheese
                    3. Season with salt and pepper
                    4. Add fresh thyme
                    5. Let rest 2 minutes
                    
                    Serving:
                    1. Garnish with fresh parsley
                    2. Serve immediately
                    3. Pass extra parmesan
                    
                    Tips:
                    - Use good quality broth
                    - Keep broth at simmer
                    - Stir constantly for creaminess
                    - Can add other vegetables
                """.trimIndent()
            ),
            Recipe(
                "Vegan Buddha Bowl",
                "Colorful bowl packed with plant-based goodness",
                "25 min",
                R.drawable.buddha_bowl_image,
                RecipeCategory.VEGAN,
                4.7f,
                110,
                listOf(
                    Ingredient("Quinoa", "1", "cup"),
                    Ingredient("Chickpeas", "1", "can"),
                    Ingredient("Sweet potato", "1", "large"),
                    Ingredient("Avocado", "1", "medium"),
                    Ingredient("Kale", "2", "cups"),
                    Ingredient("Red cabbage", "1/4", "head"),
                    Ingredient("Carrot", "1", "large"),
                    Ingredient("Cucumber", "1/2", "medium"),
                    Ingredient("Tahini", "2", "tbsp"),
                    Ingredient("Lemon juice", "1", "tbsp"),
                    Ingredient("Olive oil", "2", "tbsp"),
                    Ingredient("Cumin", "1", "tsp"),
                    Ingredient("Paprika", "1", "tsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Cook quinoa according to package
                    2. Preheat oven to 400°F (200°C)
                    3. Drain and rinse chickpeas
                    
                    Sweet Potato:
                    1. Peel and cube sweet potato
                    2. Toss with 1 tbsp oil, cumin, paprika
                    3. Roast for 20 minutes
                    4. Turn halfway through
                    
                    Chickpeas:
                    1. Pat chickpeas dry
                    2. Toss with remaining oil and spices
                    3. Roast for 15 minutes
                    4. Shake pan occasionally
                    
                    Vegetables:
                    1. Massage kale with lemon juice
                    2. Shred cabbage and carrot
                    3. Slice cucumber and avocado
                    
                    Dressing:
                    1. Whisk tahini with lemon juice
                    2. Add water to thin if needed
                    3. Season with salt and pepper
                    
                    Assembly:
                    1. Divide quinoa among bowls
                    2. Arrange vegetables around quinoa
                    3. Add roasted sweet potatoes
                    4. Top with roasted chickpeas
                    5. Drizzle with tahini dressing
                    
                    Tips:
                    - Can use any grain instead of quinoa
                    - Add other vegetables as desired
                    - Dressing can be made ahead
                    - Can add nuts or seeds for crunch
                """.trimIndent()
            ),
            Recipe(
                "Gluten-Free Pasta Primavera",
                "Fresh vegetable pasta with gluten-free noodles",
                "30 min",
                R.drawable.pasta_primavera_image,
                RecipeCategory.GLUTEN_FREE,
                4.5f,
                65,
                listOf(
                    Ingredient("Gluten-free pasta", "8", "oz"),
                    Ingredient("Broccoli florets", "2", "cups"),
                    Ingredient("Bell peppers", "2", "medium"),
                    Ingredient("Zucchini", "1", "medium"),
                    Ingredient("Cherry tomatoes", "1", "cup"),
                    Ingredient("Olive oil", "1/4", "cup"),
                    Ingredient("Garlic", "4", "cloves"),
                    Ingredient("Fresh basil", "1/4", "cup"),
                    Ingredient("Parmesan cheese", "1/2", "cup"),
                    Ingredient("Lemon zest", "1", "tbsp"),
                    Ingredient("Red pepper flakes", "1/2", "tsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Bring large pot of salted water to boil
                    2. Wash and prepare all vegetables
                    3. Mince garlic
                    4. Grate parmesan cheese
                    
                    Vegetables:
                    1. Heat 2 tbsp oil in large skillet
                    2. Sauté garlic until fragrant
                    3. Add broccoli and bell peppers
                    4. Cook for 3-4 minutes
                    5. Add zucchini and tomatoes
                    6. Cook until vegetables are crisp-tender
                    
                    Pasta:
                    1. Cook pasta according to package
                    2. Reserve 1/2 cup pasta water
                    3. Drain pasta
                    
                    Combining:
                    1. Add pasta to vegetables
                    2. Toss with remaining oil
                    3. Add pasta water as needed
                    4. Stir in basil and lemon zest
                    5. Season with salt and pepper
                    
                    Serving:
                    1. Top with parmesan cheese
                    2. Sprinkle with red pepper flakes
                    3. Garnish with fresh basil
                    
                    Tips:
                    - Don't overcook gluten-free pasta
                    - Can use any vegetables
                    - Add protein if desired
                    - Can make ahead and reheat
                """.trimIndent()
            ),
            Recipe(
                "Vegan Chocolate Mousse",
                "Rich and creamy dairy-free chocolate dessert",
                "20 min",
                R.drawable.chocolate_mousse_image,
                RecipeCategory.VEGAN,
                4.9f,
                75,
                listOf(
                    Ingredient("Dark chocolate", "8", "oz"),
                    Ingredient("Silken tofu", "12", "oz"),
                    Ingredient("Maple syrup", "1/4", "cup"),
                    Ingredient("Vanilla extract", "1", "tsp"),
                    Ingredient("Coconut cream", "1/2", "cup"),
                    Ingredient("Cocoa powder", "2", "tbsp"),
                    Ingredient("Salt", "1/4", "tsp"),
                    Ingredient("Fresh berries", "1/2", "cup"),
                    Ingredient("Mint leaves", "4", "leaves"),
                    Ingredient("Shaved chocolate", "2", "tbsp")
                ),
                """
                    Preparation:
                    1. Chill coconut cream overnight
                    2. Drain silken tofu
                    3. Chop chocolate into small pieces
                    
                    Chocolate:
                    1. Melt chocolate in double boiler
                    2. Stir until smooth
                    3. Let cool slightly
                    
                    Mousse Base:
                    1. Blend tofu until smooth
                    2. Add maple syrup and vanilla
                    3. Blend until combined
                    4. Add melted chocolate
                    5. Blend until smooth
                    
                    Coconut Cream:
                    1. Whip chilled coconut cream
                    2. Fold into chocolate mixture
                    3. Add cocoa powder and salt
                    4. Mix until combined
                    
                    Chilling:
                    1. Divide into serving glasses
                    2. Chill for at least 2 hours
                    3. Can be made day ahead
                    
                    Serving:
                    1. Top with fresh berries
                    2. Garnish with mint leaves
                    3. Sprinkle with shaved chocolate
                    
                    Tips:
                    - Use high-quality chocolate
                    - Can use agave instead of maple syrup
                    - Add espresso powder for mocha flavor
                    - Can top with coconut whipped cream
                """.trimIndent()
            ),
            Recipe(
                "Gluten-Free Banana Bread",
                "Moist and delicious gluten-free banana bread",
                "60 min",
                R.drawable.banana_bread_image,
                RecipeCategory.GLUTEN_FREE,
                4.8f,
                85,
                listOf(
                    Ingredient("Gluten-free flour", "2", "cups"),
                    Ingredient("Ripe bananas", "3", "medium"),
                    Ingredient("Eggs", "2", "large"),
                    Ingredient("Coconut oil", "1/2", "cup"),
                    Ingredient("Maple syrup", "1/3", "cup"),
                    Ingredient("Baking soda", "1", "tsp"),
                    Ingredient("Baking powder", "1/2", "tsp"),
                    Ingredient("Cinnamon", "1", "tsp"),
                    Ingredient("Vanilla extract", "1", "tsp"),
                    Ingredient("Salt", "1/2", "tsp"),
                    Ingredient("Walnuts", "1/2", "cup"),
                    Ingredient("Dark chocolate chips", "1/2", "cup")
                ),
                """
                    Preparation:
                    1. Preheat oven to 350°F (175°C)
                    2. Grease 9x5-inch loaf pan
                    3. Line with parchment paper
                    
                    Wet Ingredients:
                    1. Mash bananas in large bowl
                    2. Add eggs and whisk
                    3. Add melted coconut oil
                    4. Add maple syrup and vanilla
                    5. Mix until combined
                    
                    Dry Ingredients:
                    1. Whisk flour, baking soda, baking powder
                    2. Add cinnamon and salt
                    3. Mix well
                    
                    Combining:
                    1. Add dry ingredients to wet
                    2. Mix until just combined
                    3. Fold in walnuts and chocolate chips
                    4. Don't overmix
                    
                    Baking:
                    1. Pour batter into prepared pan
                    2. Smooth top with spatula
                    3. Bake for 50-60 minutes
                    4. Test with toothpick
                    5. Let cool in pan 10 minutes
                    6. Transfer to wire rack
                    
                    Tips:
                    - Use very ripe bananas
                    - Can use other nuts
                    - Can omit chocolate chips
                    - Freezes well
                """.trimIndent()
            ),
            Recipe(
                "Ratatouille",
                "Classic French vegetable stew",
                "60 min",
                R.drawable.ratatouille_image,
                RecipeCategory.FRENCH,
                4.8f,
                75,
                listOf(
                    Ingredient("Eggplant", "1", "large"),
                    Ingredient("Zucchini", "2", "medium"),
                    Ingredient("Bell peppers", "2", "medium"),
                    Ingredient("Tomatoes", "4", "large"),
                    Ingredient("Onion", "1", "large"),
                    Ingredient("Garlic", "4", "cloves"),
                    Ingredient("Fresh thyme", "4", "sprigs"),
                    Ingredient("Fresh basil", "1/4", "cup"),
                    Ingredient("Olive oil", "1/4", "cup"),
                    Ingredient("Salt and pepper", "to", "taste"),
                    Ingredient("Red wine vinegar", "1", "tbsp")
                ),
                """
                    Preparation:
                    1. Preheat oven to 375°F (190°C)
                    2. Wash and prepare all vegetables
                    3. Mince garlic and herbs
                    
                    Vegetables:
                    1. Slice eggplant, zucchini, and bell peppers
                    2. Dice tomatoes and onion
                    3. Toss vegetables with olive oil
                    4. Season with salt and pepper
                    
                    Cooking:
                    1. Heat 2 tbsp oil in large pot
                    2. Sauté onion until soft
                    3. Add garlic and cook 1 minute
                    4. Add tomatoes and cook 5 minutes
                    5. Layer remaining vegetables
                    6. Add thyme and basil
                    7. Cover and simmer 30 minutes
                    
                    Finishing:
                    1. Remove from heat
                    2. Add vinegar
                    3. Adjust seasoning
                    4. Let rest 10 minutes
                    
                    Serving:
                    1. Garnish with fresh basil
                    2. Drizzle with olive oil
                    3. Serve with crusty bread
                    
                    Tips:
                    - Can be served hot or cold
                    - Better the next day
                    - Freezes well
                    - Add olives for extra flavor
                """.trimIndent()
            ),
            Recipe(
                "Vegetable Lasagna",
                "Layered pasta with rich tomato sauce and vegetables",
                "60 min",
                R.drawable.pasta_image,
                RecipeCategory.VEGETARIAN,
                4.8f,
                95,
                listOf(
                    Ingredient("Lasagna noodles", "12", "sheets"),
                    Ingredient("Ricotta cheese", "15", "oz"),
                    Ingredient("Mozzarella cheese", "2", "cups"),
                    Ingredient("Parmesan cheese", "1", "cup"),
                    Ingredient("Eggplant", "1", "medium"),
                    Ingredient("Zucchini", "2", "medium"),
                    Ingredient("Bell peppers", "2", "medium"),
                    Ingredient("Spinach", "2", "cups"),
                    Ingredient("Tomato sauce", "3", "cups"),
                    Ingredient("Garlic", "4", "cloves"),
                    Ingredient("Fresh basil", "1/4", "cup"),
                    Ingredient("Olive oil", "2", "tbsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Preheat oven to 375°F (190°C)
                    2. Cook lasagna noodles according to package
                    3. Prepare all vegetables
                    
                    Vegetables:
                    1. Slice eggplant, zucchini, and bell peppers
                    2. Sauté garlic in olive oil
                    3. Add vegetables and cook until tender
                    4. Add spinach and cook until wilted
                    5. Season with salt and pepper
                    
                    Cheese Mixture:
                    1. Mix ricotta with 1/2 cup parmesan
                    2. Add chopped basil
                    3. Season with salt and pepper
                    
                    Assembly:
                    1. Spread tomato sauce in baking dish
                    2. Layer noodles, vegetables, cheese mixture
                    3. Repeat layers
                    4. Top with mozzarella and remaining parmesan
                    
                    Baking:
                    1. Cover with foil
                    2. Bake for 30 minutes
                    3. Remove foil and bake 15 more minutes
                    4. Let rest 10 minutes before serving
                    
                    Tips:
                    - Can be made ahead
                    - Freezes well
                    - Add mushrooms for extra flavor
                    - Use fresh herbs for best taste
                """.trimIndent()
            ),
            Recipe(
                "Stuffed Bell Peppers",
                "Colorful bell peppers filled with quinoa and vegetables",
                "45 min",
                R.drawable.salad_image,
                RecipeCategory.VEGETARIAN,
                4.7f,
                80,
                listOf(
                    Ingredient("Bell peppers", "4", "large"),
                    Ingredient("Quinoa", "1", "cup"),
                    Ingredient("Black beans", "1", "can"),
                    Ingredient("Corn", "1", "cup"),
                    Ingredient("Onion", "1", "medium"),
                    Ingredient("Garlic", "3", "cloves"),
                    Ingredient("Tomato sauce", "1", "cup"),
                    Ingredient("Cheddar cheese", "1", "cup"),
                    Ingredient("Cumin", "1", "tsp"),
                    Ingredient("Chili powder", "1", "tsp"),
                    Ingredient("Olive oil", "2", "tbsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Preheat oven to 375°F (190°C)
                    2. Cook quinoa according to package
                    3. Prepare bell peppers
                    
                    Filling:
                    1. Sauté onion and garlic in oil
                    2. Add black beans and corn
                    3. Stir in cooked quinoa
                    4. Add tomato sauce and spices
                    5. Mix in half the cheese
                    
                    Assembly:
                    1. Cut tops off peppers
                    2. Remove seeds and membranes
                    3. Fill with quinoa mixture
                    4. Top with remaining cheese
                    
                    Baking:
                    1. Place in baking dish
                    2. Cover with foil
                    3. Bake for 30 minutes
                    4. Remove foil and bake 10 more minutes
                    
                    Tips:
                    - Can use any color peppers
                    - Add other vegetables as desired
                    - Can be made ahead
                    - Serve with sour cream
                """.trimIndent()
            ),
            Recipe(
                "Grilled Salmon with Asparagus",
                "Perfectly grilled salmon with roasted vegetables",
                "30 min",
                R.drawable.salmon_bowl_image,
                RecipeCategory.DINNER,
                4.9f,
                110,
                listOf(
                    Ingredient("Salmon fillets", "4", "6-oz pieces"),
                    Ingredient("Asparagus", "1", "bunch"),
                    Ingredient("Lemon", "2", "medium"),
                    Ingredient("Garlic", "3", "cloves"),
                    Ingredient("Fresh dill", "1/4", "cup"),
                    Ingredient("Olive oil", "3", "tbsp"),
                    Ingredient("Butter", "2", "tbsp"),
                    Ingredient("White wine", "1/4", "cup"),
                    Ingredient("Salt and pepper", "to", "taste"),
                    Ingredient("Red pepper flakes", "1/4", "tsp")
                ),
                """
                    Preparation:
                    1. Preheat grill to medium-high
                    2. Prepare asparagus
                    3. Mince garlic and chop dill
                    
                    Salmon:
                    1. Pat salmon dry
                    2. Season with salt and pepper
                    3. Drizzle with 1 tbsp oil
                    4. Let rest 10 minutes
                    
                    Asparagus:
                    1. Toss with 1 tbsp oil
                    2. Season with salt and pepper
                    3. Add minced garlic
                    
                    Grilling:
                    1. Place salmon skin-side down
                    2. Grill for 4-5 minutes
                    3. Flip and grill 3-4 minutes
                    4. Add asparagus to grill
                    5. Cook until tender
                    
                    Sauce:
                    1. Melt butter in pan
                    2. Add wine and lemon juice
                    3. Stir in dill and red pepper
                    4. Cook until slightly reduced
                    
                    Serving:
                    1. Plate salmon and asparagus
                    2. Drizzle with sauce
                    3. Garnish with lemon wedges
                    
                    Tips:
                    - Don't overcook salmon
                    - Can use other fish
                    - Add other vegetables
                    - Serve with rice
                """.trimIndent()
            ),
            Recipe(
                "Chicken Parmesan",
                "Crispy breaded chicken with marinara and cheese",
                "40 min",
                R.drawable.caesar_salad_image,
                RecipeCategory.DINNER,
                4.8f,
                95,
                listOf(
                    Ingredient("Chicken breasts", "4", "boneless"),
                    Ingredient("Breadcrumbs", "1", "cup"),
                    Ingredient("Parmesan cheese", "1/2", "cup"),
                    Ingredient("Mozzarella cheese", "1", "cup"),
                    Ingredient("Eggs", "2", "large"),
                    Ingredient("Flour", "1/2", "cup"),
                    Ingredient("Marinara sauce", "2", "cups"),
                    Ingredient("Fresh basil", "1/4", "cup"),
                    Ingredient("Olive oil", "1/4", "cup"),
                    Ingredient("Garlic powder", "1", "tsp"),
                    Ingredient("Italian seasoning", "1", "tbsp"),
                    Ingredient("Salt and pepper", "to", "taste")
                ),
                """
                    Preparation:
                    1. Preheat oven to 400°F (200°C)
                    2. Pound chicken to even thickness
                    3. Set up breading station
                    
                    Breading:
                    1. Mix breadcrumbs with parmesan
                    2. Add garlic powder and Italian seasoning
                    3. Beat eggs in separate bowl
                    4. Place flour in third bowl
                    
                    Chicken:
                    1. Season chicken with salt and pepper
                    2. Dredge in flour
                    3. Dip in egg
                    4. Coat with breadcrumb mixture
                    
                    Cooking:
                    1. Heat oil in large skillet
                    2. Cook chicken until golden
                    3. Transfer to baking dish
                    4. Top with sauce and cheese
                    5. Bake until cheese melts
                    
                    Serving:
                    1. Garnish with fresh basil
                    2. Serve with pasta
                    3. Add extra sauce if desired
                    
                    Tips:
                    - Use fresh breadcrumbs
                    - Can make ahead
                    - Add extra cheese if desired
                    - Serve with garlic bread
                """.trimIndent()
            )
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
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
                        .clickable { showProfileDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "User Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                IconButton(onClick = onSignOut) {
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
                                    selectedCategories = if (category == RecipeCategory.ALL) {
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
                                favoriteRecipes.contains(recipe.name) &&
                                (selectedCategories.contains(RecipeCategory.ALL) || 
                                 selectedCategories.contains(recipe.category)) &&
                                (searchQuery.isEmpty() || 
                                 recipe.name.contains(searchQuery, ignoreCase = true) ||
                                 recipe.description.contains(searchQuery, ignoreCase = true))
                            }
                            else -> emptyList()
                        }

                        val scrollState = rememberLazyListState()

                        Box(
                            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                                items(filteredRecipes) { recipe ->
                                    RecipeCard(
                                        recipe = recipe,
                                        isFavorite = favoriteRecipes.contains(recipe.name),
                                        onFavoriteClick = { 
                                            favoriteRecipes = if (favoriteRecipes.contains(recipe.name)) {
                                                favoriteRecipes - recipe.name
                                            } else {
                                                favoriteRecipes + recipe.name
                                            }
                                        },
                                        onClick = { showRecipeDetails = recipe }
                                    )
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
                                    selectedCategories = if (selectedCategories.contains(category)) {
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
                            (searchQuery.isEmpty() || 
                             recipe.name.contains(searchQuery, ignoreCase = true) || 
                             recipe.description.contains(searchQuery, ignoreCase = true)) &&
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
                            val favoriteRecipesList = filteredRecipes.filter { favoriteRecipes.contains(it.name) }
                            items(favoriteRecipesList) { recipe ->
                                RecipeCard(
                                    recipe = recipe,
                                    isFavorite = true,
                                    onFavoriteClick = { 
                                        favoriteRecipes = favoriteRecipes - recipe.name
                                    },
                                    onClick = { showRecipeDetails = recipe }
                                )
                            }
                        }
                        
                        if (filteredRecipes.none { favoriteRecipes.contains(it.name) }) {
                            Text(
                                text = "No favorite recipes yet",
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            DashboardTab.SETTINGS -> {
                SettingsScreen()
            }
            DashboardTab.PROFILE -> {
            UserProfileDialog(
                    onDismiss = { selectedTab = DashboardTab.HOME },
                onSignOut = {
                        selectedTab = DashboardTab.HOME
                    onSignOut()
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
                onDismiss = { showRecipeDetails = null }
            )
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background Image
            Image(
                painter = painterResource(id = recipe.imageResId),
                contentDescription = recipe.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recipe.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else Color.White
                        )
                    }
                }
                
                Column {
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                )
                    Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = recipe.cookTime,
                        style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color.Yellow,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "%.1f".format(recipe.rating),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Text(
                                text = "(${recipe.reviewCount})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailsDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    var isFavorite by remember { mutableStateOf(false) }
    var showCookingInstructions by remember { mutableStateOf(false) }
    
    if (showCookingInstructions) {
        CookingInstructionsDialog(
            recipe = recipe,
            onDismiss = { showCookingInstructions = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(recipe.name) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Recipe Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        Image(
                            painter = painterResource(id = recipe.imageResId),
                            contentDescription = recipe.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    // Description
                    Text(
                        text = recipe.description,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Rating and Reviews
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = recipe.rating.toString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "(${recipe.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Cook Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Cook time",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = recipe.cookTime,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    // Ingredients
                    Text(
                        text = "Ingredients:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    recipe.ingredients.forEach { ingredient ->
                        Text(
                            text = "• ${ingredient.quantity} ${ingredient.unit} ${ingredient.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isFavorite = !isFavorite }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Add to favorites",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = { 
                            showCookingInstructions = true
                        }
                    ) {
                        Text("Start Cooking")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun CookingInstructionsDialog(
    recipe: Recipe,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${recipe.name} - Cooking Instructions") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = recipe.cookingInstructions,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Back to Recipe")
            }
        }
    )
}

@Composable
fun AddRecipeDialog(
    onDismiss: () -> Unit,
    onAddRecipe: (Recipe) -> Unit
) {
    var recipeName by remember { mutableStateOf("") }
    var recipeDescription by remember { mutableStateOf("") }
    var cookTime by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RecipeCategory.DINNER) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var ingredients by remember { mutableStateOf(listOf<Ingredient>()) }
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientQuantity by remember { mutableStateOf("") }
    var newIngredientUnit by remember { mutableStateOf("") }
    var cookingInstructions by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Recipe") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recipe Name
                OutlinedTextField(
                    value = recipeName,
                    onValueChange = { recipeName = it },
                    label = { Text("Recipe Name") },
                    singleLine = true,
                    isError = showValidationError && recipeName.isBlank(),
                    supportingText = {
                        if (showValidationError && recipeName.isBlank()) {
                            Text("Recipe name is required")
                        }
                    }
                )
                
                // Description
                OutlinedTextField(
                    value = recipeDescription,
                    onValueChange = { recipeDescription = it },
                    label = { Text("Description") },
                    minLines = 2,
                    isError = showValidationError && recipeDescription.isBlank(),
                    supportingText = {
                        if (showValidationError && recipeDescription.isBlank()) {
                            Text("Description is required")
                        }
                    }
                )
                
                // Cook Time
                OutlinedTextField(
                    value = cookTime,
                    onValueChange = { cookTime = it },
                    label = { Text("Cook Time (e.g., 30 min)") },
                    singleLine = true,
                    isError = showValidationError && cookTime.isBlank(),
                    supportingText = {
                        if (showValidationError && cookTime.isBlank()) {
                            Text("Cook time is required")
                        }
                    }
                )
                
                // Category Selection
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { showCategoryMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = selectedCategory.name,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start
                        )
                        Icon(
                            imageVector = if (showCategoryMenu) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Select Category"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        RecipeCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Ingredients
                Column {
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // List of added ingredients
                    ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• ${ingredient.quantity} ${ingredient.unit} ${ingredient.name}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(
                                onClick = {
                                    ingredients = ingredients.filter { it != ingredient }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove ingredient",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Add new ingredient
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newIngredientName,
                            onValueChange = { newIngredientName = it },
                            label = { Text("Ingredient Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newIngredientQuantity,
                                onValueChange = { newIngredientQuantity = it },
                                label = { Text("Quantity") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newIngredientUnit,
                                onValueChange = { newIngredientUnit = it },
                                label = { Text("Unit") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                        Button(
                            onClick = {
                                if (newIngredientName.isNotBlank() && 
                                    newIngredientQuantity.isNotBlank() && 
                                    newIngredientUnit.isNotBlank()) {
                                    ingredients = ingredients + Ingredient(
                                        name = newIngredientName,
                                        quantity = newIngredientQuantity,
                                        unit = newIngredientUnit
                                    )
                                    newIngredientName = ""
                                    newIngredientQuantity = ""
                                    newIngredientUnit = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Add Ingredient")
                        }
                    }
                }
                
                // Cooking Instructions
                OutlinedTextField(
                    value = cookingInstructions,
                    onValueChange = { cookingInstructions = it },
                    label = { Text("Cooking Instructions") },
                    minLines = 3,
                    isError = showValidationError && cookingInstructions.isBlank(),
                    supportingText = {
                        if (showValidationError && cookingInstructions.isBlank()) {
                            Text("Cooking instructions are required")
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (recipeName.isBlank() || recipeDescription.isBlank() || 
                        cookTime.isBlank() || cookingInstructions.isBlank() || 
                        ingredients.isEmpty()) {
                        showValidationError = true
                    } else {
                        val newRecipe = Recipe(
                            name = recipeName,
                            description = recipeDescription,
                            cookTime = cookTime,
                            imageResId = R.drawable.logo,
                            category = selectedCategory,
                            rating = 0f,
                            reviewCount = 0,
                            ingredients = ingredients,
                            cookingInstructions = cookingInstructions
                        )
                        onAddRecipe(newRecipe)
                        onDismiss()
                    }
                }
            ) {
                Text("Add Recipe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
} 