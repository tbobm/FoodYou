package com.maksimowiczm.foodyou.food.domain.repository

import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.domain.entity.Recipe
import com.maksimowiczm.foodyou.food.domain.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipe(recipeId: FoodId.Recipe): Flow<Recipe?>

    /** Bulk read of every recipe. For full-data export. */
    fun observeAllRecipes(): Flow<List<Recipe>>

    suspend fun insertRecipe(
        name: String,
        servings: Int,
        note: String?,
        isLiquid: Boolean,
        ingredients: List<RecipeIngredient>,
    ): FoodId.Recipe

    suspend fun updateRecipe(recipe: Recipe)

    suspend fun deleteRecipe(recipe: Recipe)
}
