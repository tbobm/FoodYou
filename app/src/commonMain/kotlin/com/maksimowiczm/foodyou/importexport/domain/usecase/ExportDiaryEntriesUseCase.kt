package com.maksimowiczm.foodyou.importexport.domain.usecase

import com.maksimowiczm.foodyou.common.domain.food.NutritionFacts
import com.maksimowiczm.foodyou.common.domain.food.NutritionFactsField
import com.maksimowiczm.foodyou.common.domain.food.get
import com.maksimowiczm.foodyou.common.domain.measurement.rawValue
import com.maksimowiczm.foodyou.common.domain.measurement.type
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryFoodProduct
import com.maksimowiczm.foodyou.fooddiary.domain.entity.DiaryFoodRecipe
import com.maksimowiczm.foodyou.fooddiary.domain.entity.FoodDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.entity.ManualDiaryEntry
import com.maksimowiczm.foodyou.fooddiary.domain.repository.FoodDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.ManualDiaryEntryRepository
import com.maksimowiczm.foodyou.fooddiary.domain.repository.MealRepository
import com.maksimowiczm.foodyou.importexport.domain.entity.DiaryEntryField
import com.maksimowiczm.foodyou.importexport.domain.entity.csvHeader
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

fun interface ExportDiaryEntriesUseCase {
    /**
     * Exports the full diary entry history (food-backed and manual) to CSV, one row per entry,
     * sorted by date then creation time.
     */
    suspend fun export(): Flow<String>
}

internal class ExportDiaryEntriesUseCaseImpl(
    private val foodDiaryEntryRepository: FoodDiaryEntryRepository,
    private val manualDiaryEntryRepository: ManualDiaryEntryRepository,
    private val mealRepository: MealRepository,
) : ExportDiaryEntriesUseCase {
    override suspend fun export(): Flow<String> = channelFlow {
        val csvWriter = CsvWriter()

        val header =
            (DiaryEntryField.entries.map(DiaryEntryField::csvHeader) +
                    NutritionFactsField.entries.map(NutritionFactsField::name))
                .joinToString(",", transform = csvWriter::writeString)
        send(header)

        val mealNames = mealRepository.observeMeals().first().associate { it.id to it.name }

        val rows =
            foodDiaryEntryRepository.observeAll().first().map { it.toRow(mealNames) } +
                manualDiaryEntryRepository.observeAll().first().map { it.toRow(mealNames) }

        rows.sortedWith(compareBy({ it.date }, { it.createdAt })).forEach { row ->
            val csvLine =
                (DiaryEntryField.entries.map { row.field(it) } +
                        NutritionFactsField.entries.map { row.nutritionFacts[it].value })
                    .joinToString(separator = ",") { csvWriter.write(it) }
            send(csvLine)
        }
    }
}

private data class EntryRow(
    val id: String,
    val entryType: String,
    val mealId: Long,
    val mealName: String?,
    val date: LocalDate,
    val name: String,
    val measurementType: String?,
    val measurementValue: Double?,
    val weightGrams: Double?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val nutritionFacts: NutritionFacts,
) {
    fun field(field: DiaryEntryField): Any? =
        when (field) {
            DiaryEntryField.Id -> id
            DiaryEntryField.EntryType -> entryType
            DiaryEntryField.MealId -> mealId
            DiaryEntryField.MealName -> mealName
            DiaryEntryField.Date -> date.toString()
            DiaryEntryField.Name -> name
            DiaryEntryField.MeasurementType -> measurementType
            DiaryEntryField.MeasurementValue -> measurementValue
            DiaryEntryField.WeightGrams -> weightGrams
            DiaryEntryField.CreatedAt -> createdAt.toString()
            DiaryEntryField.UpdatedAt -> updatedAt.toString()
        }
}

private fun FoodDiaryEntry.toRow(mealNames: Map<Long, String>): EntryRow =
    EntryRow(
        id = "food-${id.value}",
        entryType =
            when (food) {
                is DiaryFoodProduct -> "product"
                is DiaryFoodRecipe -> "recipe"
            },
        mealId = mealId,
        mealName = mealNames[mealId],
        date = date,
        name = name,
        measurementType = measurement.type.name,
        measurementValue = measurement.rawValue,
        weightGrams = weight,
        createdAt = createdAt,
        updatedAt = updatedAt,
        nutritionFacts = nutritionFacts,
    )

private fun ManualDiaryEntry.toRow(mealNames: Map<Long, String>): EntryRow =
    EntryRow(
        id = "manual-${id.value}",
        entryType = "manual",
        mealId = mealId,
        mealName = mealNames[mealId],
        date = date,
        name = name,
        measurementType = null,
        measurementValue = null,
        weightGrams = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        nutritionFacts = nutritionFacts,
    )
