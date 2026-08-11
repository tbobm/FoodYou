package com.maksimowiczm.foodyou.food.infrastructure.room

import com.maksimowiczm.foodyou.common.domain.measurement.MeasurementType

/** Row shape for [MeasurementSuggestionDao.observeRecentFoods]. */
data class RecentFoodSuggestion(
    val productId: Long?,
    val recipeId: Long?,
    val headline: String,
    val type: MeasurementType,
    val value: Double,
    val epochSeconds: Long,
)
