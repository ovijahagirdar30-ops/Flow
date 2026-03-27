package com.markel.flowstate.feature.habits.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.markel.flowstate.feature.habits.R


/**
 * Icon mapping (Strings saved in DB) to drawables
 */
val HabitIconList = listOf(
    "none" to null,
    "self_improvement" to R.drawable.self_improvement_24px,
    "fitness_center" to R.drawable.fitness_center_24px,
    "directions_run" to R.drawable.directions_run_24px,
    "directions_bike" to R.drawable.directions_bike_24px,
    "book" to R.drawable.book_ribbon_24px,
    "bedtime" to R.drawable.bedtime_24px,
    "shower" to R.drawable.shower_24px,
    "cleaning" to R.drawable.cleaning_24px,
    "dentistry" to R.drawable.dentistry_24px,
    "language" to R.drawable.language_chinese_dayi_24px,
    "laundry" to R.drawable.local_laundry_service_24px,
    "nutrition" to R.drawable.nutrition_24px,
    "recycling" to R.drawable.recycling_24px,
    "shopping" to R.drawable.shopping_basket_24px,
    "water" to R.drawable.water_drop_24px,
    "assignment" to R.drawable.assignment_24px,
    "pets" to R.drawable.pets_24px,
    "washoku" to R.drawable.washoku_24px

)

@Composable
fun getHabitIcon(iconName: String): ImageVector? {
    if (iconName == "none") return null
    val resId = HabitIconList.toMap()[iconName] ?: R.drawable.asterisk_24px
    return ImageVector.vectorResource(resId)
}