package com.maksimowiczm.foodyou.app.infrastructure.android.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.maksimowiczm.foodyou.app.infrastructure.android.MainActivity
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.food.NutritionFactsField
import com.maksimowiczm.foodyou.common.domain.food.sum
import com.maksimowiczm.foodyou.fooddiary.domain.usecase.ObserveDiaryMealsUseCase
import com.maksimowiczm.foodyou.goals.domain.repository.GoalsRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.koin.core.context.GlobalContext

/**
 * Read-only home-screen widget: today's energy remaining and macro progress. Data reuses the same
 * Koin-injected use cases as the in-app Goals card; DI resolves from the process-global Koin
 * context (FoodYouApplication.onCreate runs in this process).
 */
class DiaryWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: androidx.glance.GlanceId) {
        val koin = GlobalContext.get()
        val observeDiaryMeals = koin.get<ObserveDiaryMealsUseCase>()
        val goals = koin.get<GoalsRepository>()
        val dateProvider = koin.get<DateProvider>()

        val today = dateProvider.observeDate().first()
        val summary =
            combine(observeDiaryMeals.observe(today), goals.observeDailyGoals(today)) { meals, goal ->
                    val facts = meals.map { it.nutritionFacts }.sum()
                    WidgetSummary(
                        energy = facts.energy.value?.roundToInt() ?: 0,
                        energyGoal = goal[NutritionFactsField.Energy].roundToInt(),
                        proteins = facts.proteins.value?.roundToInt() ?: 0,
                        proteinsGoal = goal[NutritionFactsField.Proteins].roundToInt(),
                        carbohydrates = facts.carbohydrates.value?.roundToInt() ?: 0,
                        carbohydratesGoal = goal[NutritionFactsField.Carbohydrates].roundToInt(),
                        fats = facts.fats.value?.roundToInt() ?: 0,
                        fatsGoal = goal[NutritionFactsField.Fats].roundToInt(),
                    )
                }
                .first()

        provideContent { Content(summary) }
    }

    @Composable
    private fun Content(s: WidgetSummary) {
        GlanceTheme {
            val onBackground = GlanceTheme.colors.onBackground
            val context = LocalContext.current
            Column(
                modifier =
                    GlanceModifier.fillMaxSize()
                        .background(GlanceTheme.colors.background)
                        .padding(12.dp)
                        .clickable(
                            actionStartActivity(Intent(context, MainActivity::class.java))
                        )
            ) {
                val remaining = s.energyGoal - s.energy
                Text(
                    text = "$remaining kcal left",
                    style = TextStyle(color = onBackground, fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "${s.energy} / ${s.energyGoal} kcal",
                    style = TextStyle(color = onBackground),
                )
                Text(
                    text =
                        "P ${s.proteins}/${s.proteinsGoal}  " +
                            "C ${s.carbohydrates}/${s.carbohydratesGoal}  " +
                            "F ${s.fats}/${s.fatsGoal}",
                    style = TextStyle(color = onBackground),
                )
            }
        }
    }
}

private data class WidgetSummary(
    val energy: Int,
    val energyGoal: Int,
    val proteins: Int,
    val proteinsGoal: Int,
    val carbohydrates: Int,
    val carbohydratesGoal: Int,
    val fats: Int,
    val fatsGoal: Int,
)
