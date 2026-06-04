package com.markel.flowstate.feature.settings.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Provides Material 3 Expressive rounded corner shapes for grouped settings items
 */
object SettingsGroupShapes {
    private val largeRadius = 16.dp
    private val smallRadius = 4.dp

    val leadingItemShape = RoundedCornerShape(
        topStart = largeRadius,
        topEnd = largeRadius,
        bottomStart = smallRadius,
        bottomEnd = smallRadius
    )

    val middleItemShape = RoundedCornerShape(smallRadius)

    val endItemShape = RoundedCornerShape(
        topStart = smallRadius,
        topEnd = smallRadius,
        bottomStart = largeRadius,
        bottomEnd = largeRadius
    )

    val singleItemShape = RoundedCornerShape(largeRadius)
}

/**
 * Returns the appropriate shape for a settings item based on its position
 * in a group of [totalItems] items.
 */
fun settingsItemShape(index: Int, totalItems: Int) = when {
    totalItems == 1 -> SettingsGroupShapes.singleItemShape
    index == 0 -> SettingsGroupShapes.leadingItemShape
    index == totalItems - 1 -> SettingsGroupShapes.endItemShape
    else -> SettingsGroupShapes.middleItemShape
}
