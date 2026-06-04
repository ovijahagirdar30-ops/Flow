package com.markel.flowstate.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.markel.flowstate.feature.settings.components.settingsItemShape

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appVersion: String,
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val handleItemClick: (SettingsItemData) -> Unit = { item ->
        when (item) {
            SettingsItemData.Notifications -> onNavigateToNotifications()
            SettingsItemData.Appearance -> onNavigateToAppearance()
            SettingsItemData.Language -> onNavigateToLanguage()
            SettingsItemData.About -> onNavigateToAbout()
        }
    }

    Scaffold(
        modifier = Modifier.Companion
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 60.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── General group ──────────────────────────────────
            item {
                SettingsGroupLabel(stringResource(R.string.settings_general))
            }

            item {
                val generalItems = listOf(
                    SettingsItemData.Notifications,
                    SettingsItemData.Appearance,
                    SettingsItemData.Language
                )
                SettingsGroup(
                    items = generalItems,
                    appVersion = appVersion,
                    onItemClick = handleItemClick
                )
            }

            // ── Info group ─────────────────────────────────────
            item {
                val infoItems = listOf(SettingsItemData.About)
                SettingsGroup(
                    items = infoItems,
                    appVersion = appVersion,
                    onItemClick = handleItemClick
                )
            }
        }
    }
}

/**
 * Section label above a group of settings items.
 */
@Composable
private fun SettingsGroupLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.Companion.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/**
 * Data representing a navigable settings item.
 */
private sealed interface SettingsItemData {
    data object Notifications : SettingsItemData
    data object Appearance : SettingsItemData
    data object Language : SettingsItemData
    data object About : SettingsItemData
}

/**
 * A vertically grouped set of settings items with M3 Expressive rounded shapes.
 * Items are clipped based on their position.
 */
@Composable
private fun SettingsGroup(
    items: List<SettingsItemData>,
    appVersion: String,
    onItemClick: (SettingsItemData) -> Unit
) {
    Column(
        modifier = Modifier.Companion.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, item ->
            val shape = settingsItemShape(index, items.size)
            val itemColors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
            val onClick = { onItemClick(item) }

            when (item) {
                is SettingsItemData.Notifications -> {
                    SettingsNavigationItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.Companion.vectorResource(
                                    R.drawable.notifications_24px
                                ),
                                contentDescription = stringResource(R.string.settings_notifications)
                            )
                        },
                        headline = stringResource(R.string.settings_notifications),
                        supporting = stringResource(R.string.settings_notifications_description),
                        shape = shape,
                        colors = itemColors,
                        onClick = onClick
                    )
                }

                is SettingsItemData.Appearance -> {
                    SettingsNavigationItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.Companion.vectorResource(
                                    R.drawable.palette_24px
                                ),
                                contentDescription = stringResource(R.string.settings_appearance)
                            )
                        },
                        headline = stringResource(R.string.settings_appearance),
                        supporting = stringResource(R.string.settings_appearance_description),
                        shape = shape,
                        colors = itemColors,
                        onClick = onClick
                    )
                }

                is SettingsItemData.Language -> {
                    SettingsNavigationItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.Companion.vectorResource(
                                    R.drawable.language_24px
                                ),
                                contentDescription = stringResource(R.string.settings_language)
                            )
                        },
                        headline = stringResource(R.string.settings_language),
                        supporting = stringResource(R.string.settings_language_description),
                        shape = shape,
                        colors = itemColors,
                        onClick = onClick
                    )
                }

                is SettingsItemData.About -> {
                    SettingsNavigationItem(
                        icon = {
                            Icon(
                                imageVector = ImageVector.Companion.vectorResource(
                                    R.drawable.info_24px
                                ),
                                contentDescription = stringResource(R.string.settings_about)
                            )
                        },
                        headline = stringResource(R.string.settings_about),
                        supporting = stringResource(
                            R.string.settings_about_description,
                            appVersion
                        ),
                        shape = shape,
                        colors = itemColors,
                        onClick = onClick
                    )
                }
            }
        }
    }
}

/**
 * A single [ListItem] styled as a navigable settings row.
 * Uses the provided [shape] for clipping and [colors] for background.
 * Shows an arrow icon as trailing content to indicate navigation.
 */
@Composable
private fun SettingsNavigationItem(
    icon: @Composable () -> Unit,
    headline: String,
    supporting: String,
    shape: Shape,
    colors: androidx.compose.material3.ListItemColors,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = icon,
        trailingContent = {
            Icon(
                imageVector = ImageVector.Companion.vectorResource(
                    R.drawable.arrow_forward_24px
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = colors,
        modifier = Modifier.Companion
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
        ,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    )
}