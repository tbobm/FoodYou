package dev.tbobm.mymymeal.app.app.ui.sponsor

import dev.tbobm.mymymeal.app.common.infrastructure.koin.userPreferencesRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel

fun Module.sponsor() {
    viewModel {
        SponsorViewModel(
            sponsorRepository = get(),
            preferencesRepository = userPreferencesRepository(),
        )
    }
}
