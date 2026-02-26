package com.markel.flowstate.feature.flow.ideas.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Sentinel value used as the default "no color" selection. */

const val IDEA_COLOR_TRANSPARENT = 0x00000000L

/**
 * Pastel palette for light theme and their darker (but still pastel) counterparts for dark theme.
 * Each entry is a Pair(lightColor, darkColor).
 */
private val ideaColorPairs: List<Pair<Long, Long>> = listOf(
    0xFFFFF59DL to 0xFF5C561AL, // yellow
    0xFFFFCC80L to 0xFF5C3D14L, // orange
    0xFFEF9A9AL to 0xFF5C3236L, // red
    0xFFF48FB1L to 0xFF5C2E46L, // pink
    0xFFCE93D8L to 0xFF46305CL, // purple
    0xFF90CAF9L to 0xFF1E3F5CL, // blue
    0xFF80DEEFL to 0xFF1A4C54L, // cyan
    0xFFA5D6A7L to 0xFF244A28L, // green
    0xFFC5E1A5L to 0xFF324A1EL, // lime green
    0xFFBCAAA4L to 0xFF3D2E26L, // brown
    0xFFB0BEC5L to 0xFF2C363DL, // blue-gray

)

/**
 * Returns the resolved color long for the current theme.
 * Transparent stays transparent in both themes.
 */
@Composable
fun Long.resolveIdeaColor(): Long {
    if (this == IDEA_COLOR_TRANSPARENT) return IDEA_COLOR_TRANSPARENT
    val dark = isSystemInDarkTheme()
    val pair = ideaColorPairs.find { it.first == this || it.second == this }
    return when {
        pair == null -> this  // unknown color, use as-is
        dark -> pair.second
        else -> pair.first
    }
}

/**
 * The palette exposed to the rest of the app.
 * Always use the light-theme longs as canonical IDs (stored in DB),
 * then call [resolveIdeaColor] at render time.
 */

val ideaColorPalette: List<Long> = listOf(IDEA_COLOR_TRANSPARENT) + ideaColorPairs.map { it.first }

@Composable
fun IdeaColorPicker(
    selectedColor: Long,
    onColorSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(ideaColorPalette) { colorLong ->
            val isSelected = colorLong == selectedColor
            val isTransparent = colorLong == IDEA_COLOR_TRANSPARENT
            val resolvedColor = colorLong.resolveIdeaColor()
            val animatedBorderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = tween(150),
                label = "border_color"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .then(
                        if (isTransparent) {
                            // Draw a light circle + red diagonal slash to represent "no color"
                            Modifier.drawBehind {
                                drawCircle(Color.LightGray.copy(alpha = 0.25f))
                                drawLine(
                                    color = Color(0xFFE57373),
                                    start = Offset(size.width * 0.2f, size.height * 0.8f),
                                    end = Offset(size.width * 0.8f, size.height * 0.2f),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                            }
                        } else {
                            Modifier.background(Color(resolvedColor))
                        }
                    )

                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = animatedBorderColor,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(colorLong) }
            ) {
                if (isSelected && !isTransparent) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}