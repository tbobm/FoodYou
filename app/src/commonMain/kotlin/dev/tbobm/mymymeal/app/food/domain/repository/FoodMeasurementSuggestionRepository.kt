package dev.tbobm.mymymeal.app.food.domain.repository

import dev.tbobm.mymymeal.app.common.domain.measurement.Measurement
import dev.tbobm.mymymeal.app.food.domain.entity.FoodId
import dev.tbobm.mymymeal.app.food.domain.entity.RecentFood
import kotlinx.coroutines.flow.Flow

interface FoodMeasurementSuggestionRepository {
    suspend fun insert(foodId: FoodId, measurement: Measurement)

    fun observeByFoodId(foodId: FoodId, limit: Int): Flow<List<Measurement>>

    /** Most recently logged foods, each with the measurement it was last logged with. */
    fun observeRecentFoods(limit: Int): Flow<List<RecentFood>>
}
