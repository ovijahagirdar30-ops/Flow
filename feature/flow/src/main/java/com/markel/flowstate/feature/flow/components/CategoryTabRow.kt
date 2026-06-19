package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.tasks.R

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
            add(null to null) // null id + null name = General (icon-only tab)
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
        edgePadding = 8.dp,
        minTabWidth = 52.dp,
        divider = {}, // No divider
        indicator = {
            TabRowDefaults.PrimaryIndicator(
                modifier = Modifier.tabIndicatorOffset(
                    selectedTabIndex = selectedIndex,
                    matchContentSize = true
                ),
                height = 3.dp,
                width = Dp.Unspecified,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            )
        }
    ) {
        tabItems.forEachIndexed { index, (catId, name) ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onCategorySelected(catId) },
                modifier = Modifier
                    .wrapContentWidth()
                    .height(46.dp),
                icon = if (catId == null) {
                    {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.home_23px) ,
                            contentDescription = "index"
                        )
                    }
                }
                else null,
                text = if (catId != null) {
                    {
                        Text(
                            text = name!!,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                } else null,
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

