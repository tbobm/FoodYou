package com.maksimowiczm.foodyou.importexport.domain.usecase

import com.maksimowiczm.foodyou.food.domain.entity.Recipe
import com.maksimowiczm.foodyou.food.domain.repository.RecipeRepository
import com.maksimowiczm.foodyou.importexport.domain.entity.RecipeField
import com.maksimowiczm.foodyou.importexport.domain.entity.csvHeader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first

fun interface ExportRecipesUseCase {
    /** Exports recipe metadata (not ingredients, see [ExportRecipeIngredientsUseCase]) to CSV. */
    suspend fun export(): Flow<String>
}

internal class ExportRecipesUseCaseImpl(private val recipeRepository: RecipeRepository) :
    ExportRecipesUseCase {
    override suspend fun export(): Flow<String> = channelFlow {
        val csvWriter = CsvWriter()

        val header =
            RecipeField.entries
                .map(RecipeField::csvHeader)
                .joinToString(",", transform = csvWriter::writeString)
        send(header)

        val recipes = recipeRepository.observeAllRecipes().first()
        for (recipe in recipes) {
            val csvLine =
                RecipeField.entries.joinToString(separator = ",") { field ->
                    csvWriter.write(recipe.field(field))
                }
            send(csvLine)
        }
    }
}

private fun Recipe.field(field: RecipeField): Any? =
    when (field) {
        RecipeField.Id -> id.id
        RecipeField.Name -> name
        RecipeField.Servings -> servings.toLong()
        RecipeField.Note -> note
        RecipeField.IsLiquid -> isLiquid
    }
