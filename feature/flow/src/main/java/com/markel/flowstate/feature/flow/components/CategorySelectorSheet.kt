package com.markel.flowstate.feature.flow.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.markel.flowstate.core.domain.Category
import com.markel.flowstate.feature.tasks.R

/**
 * A dismissable bottom sheet that lets the user pick a category for an item
 * being edited (task, idea, checklist…).
 *
 * The sheet always offers, in this order:
 *  1. "General" (null id) — items without a category.
 *  2. One row per user category (filtered, no legacy "General" entries).
 *
 * The currently-selected category is highlighted with a check icon on the
 * right. Tapping any row fires [onCategorySelected] with the chosen id (which
 * may be `null` for General) and dismisses the sheet.
 *
 * Color handling:
 *  - [containerColor] is the sheet background. Defaults to `surfaceContainerLow`,
 *    which matches the task editor (whose surface IS the surface color).
 *  - [contentColor] is used for the title, the row labels and the check icon.
 *    Defaults to `onSurface`.
 *  - Ideas and checklists can have a custom card color; in that case the caller
 *    passes `cardColor` and `onCardColor` so the sheet visually belongs to the
 *    same surface as the editor. When the card has no custom color, `cardColor`
 *    resolves to `surface` and `onCardColor` to `onSurface`, so the sheet ends
 *    up looking identical to the default — no special-casing needed.
 *
 * @param categories        user categories (raw list from the VM; "General"
 *                          entries are filtered out internally).
 * @param selectedCategoryId id of the currently-selected category, or `null`
 *                          for General.
 * @param onCategorySelected invoked with the new category id (null = General).
 * @param onDismiss         invoked when the sheet is dismissed by the user.
 * @param containerColor    background color of the sheet.
 * @param contentColor      text/icon color used inside the sheet. The title and
 *                          unselected rows use a slightly faded version of it;
 *                          the selected row + check use it at full strength.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorSheet(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onDismiss: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    generalTabName: String?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val userCategories = remember(categories) {
        categories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = containerColor,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Title
            Text(
                text = stringResource(R.string.category_selector_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // General (null id) — always first
                item(key = "general") {
                    CategorySelectorRow(
                        label = generalTabName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.category_general),
                        isSelected = selectedCategoryId == null,
                        onClick = {
                            onCategorySelected(null)
                            onDismiss()
                        },
                        contentColor = contentColor
                    )
                }

                // User categories
                items(items = userCategories, key = { it.id }) { category ->
                    CategorySelectorRow(
                        label = category.name,
                        isSelected = selectedCategoryId == category.id,
                        onClick = {
                            onCategorySelected(category.id)
                            onDismiss()
                        },
                        contentColor = contentColor
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CategorySelectorRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = if (isSelected) MaterialTheme.typography.bodyLargeEmphasized else MaterialTheme.typography.bodyLarge,
            color = if (isSelected) contentColor else contentColor.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.size(8.dp))
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.check_24px),
                    contentDescription = null,
                    tint = contentColor
                )
            }
        }
    }
}