package com.markel.flowstate.feature.calendar.components.calendarview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import java.time.YearMonth
import java.time.format.TextStyle

sealed interface MonthPickerItem {
    data class Month(val yearMonth: YearMonth) : MonthPickerItem
    data class Year(val year: Int) : MonthPickerItem
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPicker(
    currentMonth: YearMonth,
    onMonthClick: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickerItems = remember {
        val items = mutableListOf<MonthPickerItem>()
        val startMonth = YearMonth.now().minusYears(2).withMonth(1)
        val endMonth = YearMonth.now().plusYears(2).withMonth(12)
        var curr = startMonth
        while (!curr.isAfter(endMonth)) {
            if (curr.monthValue == 1) items.add(MonthPickerItem.Year(curr.year))
            items.add(MonthPickerItem.Month(curr))
            curr = curr.plusMonths(1)
        }
        items
    }

    val listState = rememberLazyListState()

    // Scroll to the current month
    LaunchedEffect(Unit) {
        val index = pickerItems.indexOfFirst { it is MonthPickerItem.Month && it.yearMonth == currentMonth }
        if (index >= 0) listState.scrollToItem(maxOf(0, index - 2))
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(pickerItems) { item ->
            when (item) {
                is MonthPickerItem.Month -> {
                    val isSelected = item.yearMonth == currentMonth
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMonthClick(item.yearMonth) },
                        label = {
                            Text(
                                item.yearMonth.month.getDisplayName(TextStyle.SHORT, LocalLocale.current.platformLocale)
                                    .replaceFirstChar { it.uppercase() }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiary,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        shape = if (isSelected) CircleShape else FilterChipDefaults.shape,
                        border = null
                    )
                }
                is MonthPickerItem.Year -> {
                    Surface(
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(
                                text = item.year.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}