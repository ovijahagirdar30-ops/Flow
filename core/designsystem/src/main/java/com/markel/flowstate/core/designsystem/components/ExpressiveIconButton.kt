package com.markel.flowstate.core.designsystem.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Returns a color that is perceptually lighter than [this] by blending it toward
 * [Color.White] at the given [fraction].
 *
 * This is theme-agnostic: it works for any base color (a custom palette color or a
 * theme surface) and in both light and dark mode. It always produces a color that
 * is visually lighter than the source, which is what we want for buttons sitting
 * on a card surface.
 *
 * Why not use `onSurface.copy(alpha = 0.08f)`
 *   - In dark mode, `onSurface` is white, so 8% white over a dark card lightens it. OK.
 *   - In light mode, `onSurface` is black, so 8% black over a light card DARKENS it. BUG.
 *   Blending toward white works in both cases, which is exactly what Material 3's
 *   tonal elevation does internally (surfaceContainer > surface, etc.).
 *
 * @param fraction 0f = no change, 1f = pure white. 0.10–0.16 is the sweet spot
 *                 for visible-but-subtle hierarchy.
 */
private fun Color.lighten(fraction: Float = 0.12f): Color =
    lerp(this, Color.White, fraction.coerceIn(0f, 1f))

/**
 * Returns a color that is perceptually darker than [this] by blending it toward
 * [Color.Black] at the given [fraction]. Useful for pressed / hovered states on
 * light surfaces.
 */
private fun Color.darken(fraction: Float = 0.10f): Color =
    lerp(this, Color.Black, fraction.coerceIn(0f, 1f))

/**
 * An expressive icon button designed to sit on top of a colored card surface.
 *
 * Visual contract:
 *  - The button's container color is a *lightened* version of [containerColor],
 *    so the button always appears perceptually ABOVE the card, regardless of
 *    theme (light / dark) and regardless of whether [containerColor] is a
 *    palette color or a theme surface. This is the Google-Tasks look.
 *
 * Motion contract (Material 3 Expressive):
 *  - On press, the button scales down to [pressedScale] (default 0.92) using a
 *    bouncy spring (DampingRatioMediumBouncy + StiffnessMediumLow) and snaps
 *    back on release. This gives the button a tactile, expressive feel without
 *    overshooting awkwardly.
 *
 * @param containerColor   The card color the button sits on. The button background
 *                         will be computed as `containerColor.lighten(lightenFraction)`.
 * @param lightenFraction  How much to lighten [containerColor]. 0.10–0.16 recommended.
 * @param cornerRadius     Corner radius of the button background. 16dp matches M3
 *                         Expressive's slightly more rounded shape language.
 * @param tint             Icon tint. Defaults to `onSurface`; pass a copy with
 *                         reduced alpha for a softer look.
 * @param pressedScale     Scale factor applied on press.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveIconButton(
    onClick: () -> Unit,
    imageVector: ImageVector,
    contentDescription: String?,
    containerColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 24.dp,
    cornerRadius: Dp = 16.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    lightenFraction: Float = 0.12f,
    pressedScale: Float = 0.92f,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "expressive_press_scale"
    )

    IconButton(
        onClick = onClick,
        interactionSource = source,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                color = containerColor.lighten(lightenFraction),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
