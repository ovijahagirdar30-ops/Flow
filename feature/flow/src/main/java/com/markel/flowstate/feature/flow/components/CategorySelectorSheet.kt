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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 * @param categories        user categories (raw list from the VM; "General"
 *                          entries are filtered out internally).
 * @param selectedCategoryId id of the currently-selected category, or `null`
 *                          for General.
 * @param onCategorySelected invoked with the new category id (null = General).
 * @param onDismiss         invoked when the sheet is dismissed by the user.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectorSheet(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter out any legacy "General" category rows — General is represented
    // by the null-id row at the top, never by a real DB entry.
    val userCategories = remember(categories) {
        categories.filter { !it.name.equals("General", ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                        label = stringResource(R.string.category_general),
                        isSelected = selectedCategoryId == null,
                        onClick = {
                            onCategorySelected(null)
                            onDismiss()
                        }
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
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategorySelectorRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
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
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
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
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}