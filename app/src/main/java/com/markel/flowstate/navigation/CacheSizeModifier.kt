package com.markel.flowstate.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize

/**
 * Caches the measured size of the component and optionally reuses it.
 *
 * Used by [FlowStateSceneDecoratorStrategy] on the Box that hosts the movable
 * bottom bar. While the bar is being rendered in the *other* scene during a
 * transition, this Box uses the previously-measured size as a placeholder so
 * the shared-element animation has a stable target.
 *
 * Pulled verbatim from the official nav3-recipes Modifiers.kt:
 * https://github.com/android/nav3-recipes/blob/main/app/src/main/java/com/example/nav3recipes/navscenedecorator/Modifiers.kt
 */
fun Modifier.cacheSize(useCachedSize: Boolean): Modifier =
    this.then(CacheSizeElement(useCachedSize))

private data class CacheSizeElement(
    val useCachedSize: Boolean,
) : ModifierNodeElement<CacheSizeNode>() {
    override fun create(): CacheSizeNode = CacheSizeNode(useCachedSize)
    override fun update(node: CacheSizeNode) {
        node.useCachedSize = useCachedSize
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "cacheSize"
        properties["useCachedSize"] = useCachedSize
    }
}

private class CacheSizeNode(
    var useCachedSize: Boolean,
) : Modifier.Node(), LayoutModifierNode {

    private var cachedSize: IntSize? = null

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val cached = if (useCachedSize) cachedSize else null
        val placeable = if (cached != null) {
            measurable.measure(Constraints.fixed(cached.width, cached.height))
        } else {
            measurable.measure(constraints)
        }
        if (!useCachedSize) {
            cachedSize = IntSize(placeable.width, placeable.height)
        }
        return layout(placeable.width, placeable.height) {
            placeable.place(0, 0)
        }
    }
}
