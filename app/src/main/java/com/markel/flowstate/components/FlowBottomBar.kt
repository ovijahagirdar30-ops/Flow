package com.markel.flowstate.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
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
 * The bar reads the active tab directly from the [backStack] (last entry that
 * is a [TabKey]) and mutates the stack via [onSwitchTab] when the user taps a
 * different tab. No NavController involved.
 */

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FlowBottomBar(
    backStack: NavBackStack<NavKey>,
    onSwitchTab: (TabKey) -> Unit,
    isLandscape: Boolean,
    items: List<BottomNavScreen> = emptyList(),
) {
    // The active tab is the topmost TabKey in the back stack. When a fullscreen
    // destination is on top, the bar is not visible, so this lookup is only consulted
    // when the bar is actually rendered.
    val currentTab = backStack.lastOrNull { it is TabKey } as? TabKey

    Column(modifier = Modifier.fillMaxWidth()) {
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
                val selected = currentTab == screen.key
                val label = stringResource(screen.labelRes)
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) onSwitchTab(screen.key)
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