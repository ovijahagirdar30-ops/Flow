package com.markel.flowstate.core.widgets

import androidx.annotation.DrawableRes

// Add this iconmapper to core designsystem with the drawables to be shared between modules
object IconMapper {
    @DrawableRes
    fun getDrawableRes(iconName: String): Int? = when (iconName) {
        "self_improvement" -> R.drawable.self_improvement_24px
        "fitness_center"   -> R.drawable.fitness_center_24px
        "directions_run"   -> R.drawable.directions_run_24px
        "directions_bike"  -> R.drawable.directions_bike_24px
        "book"             -> R.drawable.book_ribbon_24px
        "bedtime"          -> R.drawable.bedtime_24px
        "shower"           -> R.drawable.shower_24px
        "cleaning"         -> R.drawable.cleaning_24px
        "dentistry"        -> R.drawable.dentistry_24px
        "language"         -> R.drawable.language_chinese_dayi_24px
        "laundry"          -> R.drawable.local_laundry_service_24px
        "nutrition"        -> R.drawable.nutrition_24px
        "recycling"        -> R.drawable.recycling_24px
        "shopping"         -> R.drawable.shopping_basket_24px
        "water"            -> R.drawable.water_drop_24px
        "assignment"       -> R.drawable.assignment_24px
        "pets"             -> R.drawable.pets_24px
        "washoku"          -> R.drawable.washoku_24px
        else               -> null
    }
}