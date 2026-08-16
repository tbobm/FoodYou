package com.maksimowiczm.foodyou.goals.infrastructure

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.maksimowiczm.foodyou.common.infrastructure.datastore.AbstractDataStoreUserPreferencesRepository
import com.maksimowiczm.foodyou.goals.domain.entity.RollingBudgetPreferences

internal class DataStoreRollingBudgetPreferencesRepository(dataStore: DataStore<Preferences>) :
    AbstractDataStoreUserPreferencesRepository<RollingBudgetPreferences>(dataStore) {
    override fun Preferences.toUserPreferences(): RollingBudgetPreferences =
        RollingBudgetPreferences(
            windowLength = this[RollingBudgetPreferencesDataStoreKeys.windowLength]
                ?: RollingBudgetPreferences.default.windowLength,
            carryover = this[RollingBudgetPreferencesDataStoreKeys.carryover]
                ?: RollingBudgetPreferences.default.carryover,
        )

    override fun MutablePreferences.applyUserPreferences(updated: RollingBudgetPreferences) {
        this[RollingBudgetPreferencesDataStoreKeys.windowLength] = updated.windowLength
        this[RollingBudgetPreferencesDataStoreKeys.carryover] = updated.carryover
    }
}

private object RollingBudgetPreferencesDataStoreKeys {
    val windowLength = intPreferencesKey("goals:rolling_budget_preferences:window_length")
    val carryover = booleanPreferencesKey("goals:rolling_budget_preferences:carryover")
}
