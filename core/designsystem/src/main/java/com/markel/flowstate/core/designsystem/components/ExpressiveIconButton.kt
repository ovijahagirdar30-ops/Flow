package com.markel.flowstate.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Returns a color that is perceptually lighter than [this] by blending it toward
 * [Color.White] at the given [fraction].
 *
 * Note: if [this] is already very light (e.g. `Color.White` or `surface` in light
 * mode), `lighten()` is a no-op because there's no headroom left toward white.
 * In that case use [tonalShift] instead — it picks the direction automatically.
 *
 * @param fraction 0f = no change, 1f = pure white. 0.10–0.16 is the sweet spot
 *                 for visible-but-subtle hierarchy.
 */
fun Color.lighten(fraction: Float = 0.12f): Color =
    lerp(this, Color.White, fraction.coerceIn(0f, 1f))

/**
 * Returns a color that is perceptually darker than [this] by blending it toward
 * [Color.Black] at the given [fraction]. Useful for pressed / hovered states on
 * light surfaces, or as the automatic direction picked by [tonalShift] when the
 * base color is already light.
 */
fun Color.darken(fraction: Float = 0.10f): Color =
    lerp(this, Color.Black, fraction.coerceIn(0f, 1f))

/**
 * Returns a color that is *perceptually distinct* from [this], picking the
 * direction automatically based on the source color's luminance:
 *
 *  - If [this] is dark  (luminance < [threshold]) → blends toward white (lighten)
 *  - If [this] is light (luminance >= [threshold]) → blends toward black (darken)
 *
 * This is the right helper for button backgrounds that sit on a card surface
 * when the card color is not known at compile time — it can be a theme surface
 * (which is near-white in light mode and near-black in dark mode) OR a custom
 * palette color (which can be anything).
 *
 * Why this matters:
 *  - `lighten()` alone fails on a white surface in light mode: `white.lighten(0.12)`
 *    is still `white`, so the button is invisible against the card.
 *  - `darken()` alone fails on a black surface in dark mode: `black.darken(0.12)`
 *    is still `black`.
 *  - `tonalShift()` works in all four cases because it picks the direction that
 *    actually has headroom.
 *
 * The default [fraction] of 0.10 produces a subtle-but-visible shift (~10% of the
 * way to the opposite extreme). 0.12–0.16 if you want it more pronounced.
 *
 * @param threshold Luminance cutoff in [0f, 1f]. 0.5 = neutral midpoint.
 *                  Lower it (e.g. 0.4) if you want more colors to be treated as
 *                  "light" and thus darkened; raise it (e.g. 0.6) for the opposite.
 */
fun Color.tonalShift(
    fraction: Float = 0.12f,
    threshold: Float = 0.55f
): Color {
    val l = luminance()
    return if (l >= threshold) darken(fraction - 0.04f) else lighten(fraction)
}

/**
 * An expressive icon button designed to sit on top of a colored card surface.
 *
 * Visual contract:
 *  - The button's container color is a *tonally-shifted* version of [containerColor],
 *    so the button always appears perceptually ABOVE the card, regardless of
 *    theme (light / dark) and regardless of whether [containerColor] is a palette
 *    color or a theme surface. This is the Google-Tasks look.
 *  - The shift direction is adaptive: if the card is dark, the button lightens;
 *    if the card is light (e.g. white surface in light mode), the button darkens.
 *    This avoids the bug where `white.lighten(0.12) = white` (no visible change).
 *  - Internally uses [Color.tonalShift] with a 0.5 luminance threshold.
 *
 * Motion contract (Material 3 Expressive):
 *  - On click, the button snaps instantly to [pressedScale] (default 0.88) and
 *    springs back to 1f with a bouncy spring (dampingRatio=0.20, stiffness=200).
 *    This is the same pattern used in NumericHabitCard's +/- buttons.
 *
 *    IMPORTANT: We use `Animatable.snapTo()` in the onClick lambda instead of
 *    `collectIsPressedAsState()`. The reason: `collectIsPressedAsState` can
 *    emit a very short `pressed=true` window for quick taps, and a slow spring
 *    doesn't have time to visibly compress before the press ends — so light
 *    taps produce no visible feedback. With `snapTo`, the scale jumps to the
 *    compressed state *immediately* on every tap, guaranteeing visible feedback
 *    no matter how brief the touch.
 *
 * Ripple contract:
 *  - The default `IconButton` ripple paints a gray circle on top of our custom
 *    background and looks out of place. We suppress it with `indication = null`
 *    — the scale animation is the sole press feedback, which is the M3 Expressive
 *    motion-first pattern.
 *
 * Usage:
 * ```
 * ExpressiveIconButton(
 *     onClick = { ... },
 *     imageVector = Icons.Rounded.Palette,
 *     contentDescription = "Change color",
 *     containerColor = cardColor,
 *     tint = onCardColor.copy(alpha = 0.85f),
 * )
 * ```
 *
 * @param containerColor   The card color the button sits on. The button background
 *                         will be computed as `containerColor.tonalShift(shiftFraction)`.
 * @param shiftFraction    How much to shift [containerColor] toward the opposite
 *                         luminance extreme. 0.10–0.16 recommended.
 * @param cornerRadius     Corner radius of the button background. 16dp matches M3
 *                         Expressive's slightly more rounded shape language.
 * @param tint             Icon tint. Defaults to `onSurface`; pass a copy with
 *                         reduced alpha for a softer look.
 * @param pressedScale     Scale factor applied on press. 0.88 matches NumericHabitCard.
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
    shiftFraction: Float = 0.12f,
    pressedScale: Float = 0.88f,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .background(
                color = containerColor.tonalShift(shiftFraction),
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(
                interactionSource = source,
                indication = null,
                onClick = {
                    scope.launch {
                        scale.snapTo(pressedScale)
                        scale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.20f,
                                stiffness = 200f
                            )
                        )
                    }
                    onClick()
                }
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