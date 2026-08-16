package com.maksimowiczm.foodyou.goals.domain.entity

import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferences

/**
 * Configuration for the rolling weekly energy budget (PRD 3.1).
 *
 * @property windowLength Number of trailing days (including today) the rolling balance covers.
 * @property carryover When true, a single high day is absorbed by the rolling window rather than
 *   flagged as an individual failure.
 */
data class RollingBudgetPreferences(val windowLength: Int, val carryover: Boolean) :
    UserPreferences {
    companion object {
        const val DEFAULT_WINDOW_LENGTH = 7

        val default = RollingBudgetPreferences(windowLength = DEFAULT_WINDOW_LENGTH, carryover = true)
    }
}
