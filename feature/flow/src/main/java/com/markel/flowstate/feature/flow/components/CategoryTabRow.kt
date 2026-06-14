package com.markel.flowstate.feature.flow.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Primary scrollable tab row for categories
 * Tabs change ONLY by click (no swipe) to avoid conflict with swipe-to-delete.
 */
@Composable
fun CategoryTabRow(
    categories: List<com.markel.flowstate.core.domain.Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    // Build the list of tabs: "General" (null id) + user categories
    // "General" tab = null categoryId (shows items without a category)
    val tabItems = remember(categories) {
        buildList {
            // General tab is implicit — represents null categoryId
            // Only add it if there are no categories with the name "General"
            add(null to "General") // null = General (no category)
            categories
                .filter { !it.name.equals("General", ignoreCase = true) }
                .forEach { cat ->
                    add(cat.id to cat.name)
                }
        }
    }

    val selectedIndex = tabItems.indexOfFirst { it.first == selectedCategoryId }
        .coerceAtLeast(0)

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        edgePadding = 16.dp,
        divider = {} // No divider
    ) {
        tabItems.forEachIndexed { index, (catId, name) ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onCategorySelected(catId) },
                text = {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

