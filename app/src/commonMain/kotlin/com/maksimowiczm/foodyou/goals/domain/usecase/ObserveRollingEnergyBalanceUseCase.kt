package com.maksimowiczm.foodyou.goals.domain.usecase

import com.maksimowiczm.foodyou.common.domain.food.NutritionFactsField
import com.maksimowiczm.foodyou.common.domain.food.get
import com.maksimowiczm.foodyou.common.domain.food.sum
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import com.maksimowiczm.foodyou.goals.domain.entity.RollingBudgetPreferences
import com.maksimowiczm.foodyou.goals.domain.repository.GoalsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * Rolling weekly energy budget (PRD 3.1): supplements the existing daily energy target with a
 * cumulative surplus/deficit over the last N days.
 *
 * Reuses [ObserveDiaryMealsUseCase] and [GoalsRepository.observeDailyGoals] per day -- the same
 * combine pattern already used by the diary widget and the Goals home card -- rather than adding
 * a new repository method or Room query for date-range aggregation.
 */
class ObserveRollingEnergyBalanceUseCase(
    private val observeDiaryMeals: ObserveDiaryMealsUseCase,
    private val goalsRepository: GoalsRepository,
    private val preferencesRepository: UserPreferencesRepository<RollingBudgetPreferences>,
) {
    /** Observes the rolling balance for the window ending on (and including) [date]. */
    fun observe(date: LocalDate): Flow<RollingEnergyBalance> =
        preferencesRepository.observe().flatMapLatest { preferences ->
            val windowLength = preferences.windowLength.coerceAtLeast(1)

            val dayFlows =
                (0 until windowLength).map { offset ->
                    val day = date.minus(DatePeriod(days = offset))
                    observeDayEnergy(day)
                }

            // ponytail: re-derives all N days on every diary/goal tick (piggybacks on
            // ObserveDiaryMealsUseCase's 1s clock); fine at the default window of 7, revisit with
            // a dedicated range query if windowLength grows much larger.
            combine(dayFlows) { days -> rollingEnergyBalance(days.toList()) }
        }

    private fun observeDayEnergy(date: LocalDate): Flow<DayEnergy> =
        combine(observeDiaryMeals.observe(date), goalsRepository.observeDailyGoals(date)) {
            meals,
            goal,
            ->
            val consumed = meals.map { it.nutritionFacts }.sum()[NutritionFactsField.Energy].value ?: 0.0
            DayEnergy(
                consumedKcal = consumed,
                targetKcal = goal[NutritionFactsField.Energy],
            )
        }
}
