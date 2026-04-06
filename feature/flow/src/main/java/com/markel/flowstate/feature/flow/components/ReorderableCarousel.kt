package com.markel.flowstate.feature.flow.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun <T : Any> ReorderableCarousel(
    items: List<T>,
    key: (T) -> Any,
    onReorder: (from: Int, to: Int) -> Unit,
    content: @Composable (T) -> Unit
) {
    val rowState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(rowState) { from, to ->
        onReorder(from.index, to.index)
    }

    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(items, key = { key(it) }) { item ->
            ReorderableItem(reorderableState, key = key(item)) { isDragging ->
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.04f else 1f,
                    label = "carousel_drag_scale"
                )
                Box(
                    modifier = Modifier
                        .longPressDraggableHandle(
                            interactionSource = remember { MutableInteractionSource() }
                        )
                        .graphicsLayer {
                            scaleX = scale; scaleY = scale
                            alpha = if (isDragging) 0.92f else 1f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                ) {
                    content(item)
                }
            }
        }
    }
}