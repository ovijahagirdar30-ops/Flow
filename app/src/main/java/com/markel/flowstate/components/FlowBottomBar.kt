package com.markel.flowstate.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.markel.flowstate.navigation.BottomNavScreen
import com.markel.flowstate.navigation.TabKey

/**
 * Bottom navigation bar.
 *
 * In the nav3 architecture this composable lives INSIDE the Scene Decorator
 * (see [com.markel.flowstate.navigation.FlowStateSceneDecoratorStrategy]). The
 * decorator wraps non-fullscreen scenes with this bar; fullscreen scenes pass
 * through unwrapped and cover the bar visually.
 *
 * The bar receives the current [topLevelRoute] directly from
 * [com.markel.flowstate.navigation.NavigationState] and routes every tap
 * through [onNavigate], which calls [com.markel.flowstate.navigation.FlowStateNavigator.navigate].
 * The navigator decides whether to switch tabs or push a detail based on the
 * key type.
 */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlowBottomBar(
    topLevelRoute: NavKey,
    onNavigate: (NavKey) -> Unit,
    isLandscape: Boolean,
    items: List<BottomNavScreen> = emptyList(),
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
    ) {
        HorizontalDivider(
            thickness = 0.3.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        ShortNavigationBar(
            arrangement = if (isLandscape) {
                ShortNavigationBarArrangement.Centered
            } else {
                ShortNavigationBarArrangement.EqualWeight
            },
            containerColor = Color.Transparent,
            windowInsets = ShortNavigationBarDefaults.windowInsets,
        ) {
            items.forEach { screen ->
                val selected = topLevelRoute == screen.key
                val label = stringResource(screen.labelRes)
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) onNavigate(screen.key)
                    },
                    icon = {
                        val iconDrawable = if (selected) screen.iconSelectedRes else screen.iconRes
                        Icon(
                            imageVector = ImageVector.vectorResource(iconDrawable),
                            contentDescription = label
                        )
                    },
                    label = { Text(label) },
                    // Portrait: icon above label (Top)
                    // Landscape: icon beside label (Start)
                    iconPosition = if (isLandscape) {
                        NavigationItemIconPosition.Start
                    } else {
                        NavigationItemIconPosition.Top
                    },
                    colors = ShortNavigationBarItemDefaults.colors(
                        selectedIndicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.7f),
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.9f)
                    )
                )
            }
        }
    }
}