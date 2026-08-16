package com.maksimowiczm.foodyou.goals.domain

import com.maksimowiczm.foodyou.common.infrastructure.koin.userPreferencesRepository
import com.maksimowiczm.foodyou.goals.domain.usecase.ObserveRollingEnergyBalanceUseCase
import org.koin.core.module.Module

internal fun Module.goalsDomainModule() {
    factory {
        ObserveRollingEnergyBalanceUseCase(
            observeDiaryMeals = get(),
            goalsRepository = get(),
            preferencesRepository = userPreferencesRepository(),
        )
    }
}
