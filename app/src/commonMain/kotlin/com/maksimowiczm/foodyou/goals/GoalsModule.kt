package com.maksimowiczm.foodyou.goals

import com.maksimowiczm.foodyou.goals.domain.goalsDomainModule
import com.maksimowiczm.foodyou.goals.infrastructure.goalsInfrastructureModule
import org.koin.dsl.module

val goalsModule = module {
    goalsInfrastructureModule()
    goalsDomainModule()
}
