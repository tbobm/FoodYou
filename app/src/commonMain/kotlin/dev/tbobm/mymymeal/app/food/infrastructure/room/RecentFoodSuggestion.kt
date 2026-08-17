package dev.tbobm.mymymeal.app.food.infrastructure.room

import dev.tbobm.mymymeal.app.common.domain.measurement.MeasurementType

/** Row shape for [MeasurementSuggestionDao.observeRecentFoods]. */
data class RecentFoodSuggestion(
    val productId: Long?,
    val recipeId: Long?,
    val headline: String,
    val type: MeasurementType,
    val value: Double,
    val epochSeconds: Long,
)
