package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.tasks.R

/**
 * Sentinel id used for the trailing "+ New category" tab.
 *
 * It must never collide with a real [com.markel.flowstate.core.domain.Category.id]
 * (Room ids start at 1), so [Int.MIN_VALUE] is safe.
 */
private const val NEW_CATEGORY_TAB_ID = Int.MIN_VALUE

/**
 * Primary scrollable tab row for categories
 * Tabs change ONLY by click (no swipe) to avoid conflict with swipe-to-delete.
 *
 * The row always renders, in this order:
 *  1. "General" tab (icon-only, null categoryId → shows items without a category)
 *  2. One tab per user category (text-only)
 *  3. "+ New category" trailing tab (text-only, opens the creation dialog)
 *
 * Long-pressing any tab (except "+ New") fires [onCategoryLongPress], which the
 * caller typically uses to open the "Reorder categories" sheet. Long-pressing
 * "General" (null id) is also reported so the caller can open the same sheet —
 * the sheet itself only shows reorderable real categories.
 *
 * @param onAddCategoryClick invoked when the trailing "+ New category" tab is pressed.
 * @param onCategoryLongPress invoked when any non-"+ New" tab is long-pressed.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryTabRow(
    categories: List<com.markel.flowstate.core.domain.Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onAddCategoryClick: () -> Unit,
    onCategoryLongPress: () -> Unit
) {
    // Build the list of tabs: "General" (null id) + user categories + "New"
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
            // Trailing "+ New category" action tab — never selectable
            add(NEW_CATEGORY_TAB_ID to null)
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
            // Only draw the indicator when the selected tab is a real category
            // (i.e. not the trailing "+ New category" action tab).
            if (selectedIndex in tabItems.indices && tabItems[selectedIndex].first != NEW_CATEGORY_TAB_ID) {
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
        }
    ) {
        tabItems.forEachIndexed { index, (catId, name) ->
            val isNewCategoryTab = catId == NEW_CATEGORY_TAB_ID
            val isSelected = !isNewCategoryTab && selectedIndex == index

            val interactionSource = remember { MutableInteractionSource() }
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            val clickAction: (() -> Unit)? = if (isNewCategoryTab) {
                onAddCategoryClick
            } else {
                { onCategorySelected(catId) }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .then(
                        if (catId == null) {
                            // Icon-only "General" tab: fill the parent's minTabWidth
                            // slot so the ripple covers the whole tab area.
                            Modifier.height(46.dp)
                        } else {
                            Modifier
                                .wrapContentWidth()
                                .height(46.dp)
                        }
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = { clickAction?.invoke() },
                        onLongClick = if (!isNewCategoryTab) {
                            { onCategoryLongPress() }
                        } else null
                    )
                    .padding(horizontal = 16.dp)
            ) {
                if (catId == null) {
                    // Icon-only "General" tab
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.home_23px),
                        contentDescription = "index",
                        tint = contentColor
                    )
                } else {
                    // Text tab (user category OR "+ New category")
                    Text(
                        text = if (isNewCategoryTab) {
                            stringResource(R.string.categories_trail)
                        } else {
                            name!!
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = contentColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
