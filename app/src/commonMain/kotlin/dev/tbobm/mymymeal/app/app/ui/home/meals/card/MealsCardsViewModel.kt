package dev.tbobm.mymymeal.app.app.ui.home.meals.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.tbobm.mymymeal.app.app.ui.food.diary.add.toDiaryFood
import dev.tbobm.mymymeal.app.common.domain.date.DateProvider
import dev.tbobm.mymymeal.app.common.domain.event.EventBus
import dev.tbobm.mymymeal.app.common.domain.userpreferences.UserPreferencesRepository
import dev.tbobm.mymymeal.app.common.result.onSuccess
import dev.tbobm.mymymeal.app.food.domain.entity.RecentFood
import dev.tbobm.mymymeal.app.food.domain.repository.FoodMeasurementSuggestionRepository
import dev.tbobm.mymymeal.app.food.domain.usecase.ObserveFoodUseCase
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.DiaryEntry
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.DiaryFoodRecipe
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.DiaryMeal
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.FoodDiaryEntry
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.ManualDiaryEntry
import dev.tbobm.mymymeal.app.fooddiary.domain.entity.MealsPreferences
import dev.tbobm.mymymeal.app.fooddiary.domain.event.FoodDiaryEntryCreatedEvent
import dev.tbobm.mymymeal.app.fooddiary.domain.repository.FoodDiaryEntryRepository
import dev.tbobm.mymymeal.app.fooddiary.domain.repository.ManualDiaryEntryRepository
import dev.tbobm.mymymeal.app.fooddiary.domain.usecase.CreateFoodDiaryEntryUseCase
import dev.tbobm.mymymeal.app.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

internal class MealsCardsViewModel(
    private val observeDiaryMealsUseCase: ObserveDiaryMealsUseCase,
    private val foodEntryRepository: FoodDiaryEntryRepository,
    private val manualEntryRepository: ManualDiaryEntryRepository,
    private val foodMeasurementSuggestionRepository: FoodMeasurementSuggestionRepository,
    private val observeFoodUseCase: ObserveFoodUseCase,
    private val createFoodDiaryEntryUseCase: CreateFoodDiaryEntryUseCase,
    private val eventBus: EventBus,
    private val dateProvider: DateProvider,
    mealsPreferencesRepository: UserPreferencesRepository<MealsPreferences>,
) : ViewModel() {
    private val dateState = MutableStateFlow<LocalDate?>(null)

    val diaryMeals: StateFlow<List<MealModel>?> =
        dateState
            .filterNotNull()
            .flatMapLatest { date -> observeDiaryMealsUseCase.observe(date) }
            .map { list -> list.map { it.toMealModel() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(60_000),
                initialValue = null,
            )

    private val _layout = mealsPreferencesRepository.observe().map { it.layout }
    val layout =
        _layout.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(2_000),
            initialValue = runBlocking { _layout.first() },
        )

    val recentFoods: StateFlow<List<RecentFood>> =
        foodMeasurementSuggestionRepository
            .observeRecentFoods(RECENT_FOODS_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(60_000),
                initialValue = emptyList(),
            )

    fun setDate(date: LocalDate) {
        viewModelScope.launch { dateState.value = date }
    }

    fun onDeleteEntry(model: MealEntryModel) {
        viewModelScope.launch {
            when (model) {
                is FoodMealEntryModel -> foodEntryRepository.delete(model.id)
                is ManualMealEntryModel -> manualEntryRepository.delete(model.id)
            }
        }
    }

    /** Re-log a recently used food into [mealId] on [date] — one tap, no navigation. */
    fun onRelog(recent: RecentFood, mealId: Long, date: LocalDate) {
        viewModelScope.launch {
            // Origin catalog food may have been deleted since it was last logged; skip silently.
            val food = observeFoodUseCase.observe(recent.foodId).firstOrNull() ?: return@launch

            createFoodDiaryEntryUseCase
                .createDiaryEntry(
                    measurement = recent.measurement,
                    mealId = mealId,
                    date = date,
                    food = food.toDiaryFood(),
                )
                .onSuccess {
                    eventBus.publish(
                        FoodDiaryEntryCreatedEvent(
                            foodId = food.id,
                            timestamp = dateProvider.nowInstant(),
                            measurement = recent.measurement,
                        )
                    )
                }
        }
    }

    private companion object {
        const val RECENT_FOODS_LIMIT = 5
    }
}

private fun DiaryMeal.toMealModel(): MealModel =
    MealModel(
        id = meal.id,
        name = meal.name,
        from = meal.from,
        to = meal.to,
        isAllDay = meal.from == meal.to,
        foods = entries.map { it.toMealEntryModel() },
        energy = nutritionFacts.energy.value?.roundToInt() ?: 0,
        proteins = nutritionFacts.proteins.value ?: 0.0,
        carbohydrates = nutritionFacts.carbohydrates.value ?: 0.0,
        fats = nutritionFacts.fats.value ?: 0.0,
    )

private fun DiaryEntry.toMealEntryModel(): MealEntryModel =
    when (this) {
        is FoodDiaryEntry ->
            FoodMealEntryModel(
                id = id,
                name = food.name,
                energy = nutritionFacts.energy.value?.roundToInt(),
                proteins = nutritionFacts.proteins.value,
                carbohydrates = nutritionFacts.carbohydrates.value,
                fats = nutritionFacts.fats.value,
                measurement = measurement,
                weight = weight,
                isLiquid = food.isLiquid,
                isRecipe = food is DiaryFoodRecipe,
                totalWeight = food.totalWeight,
                servingWeight = food.servingWeight,
            )

        is ManualDiaryEntry ->
            ManualMealEntryModel(
                id = id,
                name = name,
                energy = nutritionFacts.energy.value?.roundToInt(),
                proteins = nutritionFacts.proteins.value,
                carbohydrates = nutritionFacts.carbohydrates.value,
                fats = nutritionFacts.fats.value,
            )
    }
