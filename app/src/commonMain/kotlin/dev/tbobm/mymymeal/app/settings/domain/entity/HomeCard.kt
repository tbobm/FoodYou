package dev.tbobm.mymymeal.app.settings.domain.entity

enum class HomeCard {
    Calendar,
    Goals,
    Meals;

    companion object {
        val defaultOrder: List<HomeCard> = listOf(Calendar, Goals, Meals)
    }
}
