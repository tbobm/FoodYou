package com.maksimowiczm.foodyou.food.domain.entity

import com.maksimowiczm.foodyou.common.domain.measurement.Measurement

/**
 * A food that was recently logged, with the measurement it was last logged with. Used to power
 * one-tap re-log of recent foods (PRD 3.3).
 */
data class RecentFood(val foodId: FoodId, val headline: String, val measurement: Measurement)
