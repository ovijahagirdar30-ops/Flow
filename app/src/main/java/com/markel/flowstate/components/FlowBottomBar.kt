package com.markel.flowstate.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.markel.flowstate.Screen
import com.markel.flowstate.bottomNavItems

@Composable
fun FlowBottomBar(navController: NavHostController, isLandscape: Boolean) {
    // We get the current route to know which item to select
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val barHeight = if (isLandscape) 56.dp else 110.dp  // Reduce height in landscape mode

    Column(modifier = Modifier.fillMaxWidth()) {
        // Divider to separate bottom bar from content, both have the same surface color
        HorizontalDivider(
            thickness = 0.3.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.height(barHeight)
        ) {
            bottomNavItems.forEach { screen ->
                val label = stringResource(screen.labelRes)
                NavigationBarItem(
                    icon = {
                        if (screen == Screen.Calendar) {
                            DynamicCalendarIcon()
                        }
                        else
                            Icon(imageVector = ImageVector.vectorResource(screen.iconRes), contentDescription = label)
                    },
                    label = if (!isLandscape) { { Text(label) } } else null,  // Hide labels in landscape mode
                    selected = currentRoute == screen.route,
                    onClick = {
                        // Navigate to the new screen
                        navController.navigate(screen.route) {
                            // Avoid accumulating screens in the back stack
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.tertiary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}