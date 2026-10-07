package com.markel.flowstate.feature.checkin

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.markel.flowstate.core.designsystem.R as DesignsystemR

/**
 * Colors of the evening check-in flow.
 *
 * Every value is derived from [MaterialTheme.colorScheme] so the check-in
 * wears exactly the app's theme colour, light/dark mode and surfaces — the
 * flow is no longer a fixed lavender-on-black island in a green (or any
 * custom wheel-picked) app theme. Property names keep the design's vocabulary
 * so call sites read the same as before.
 */
internal class CheckinFlowPalette internal constructor(
    /** Screen background — the theme's background (pure black with pure surfaces). */
    val Background: Color,
    /** Accent: buttons, cursor, highlights — the theme's primary. */
    val Accent: Color,
    /** On-accent text/icons — the theme's onPrimary. */
    val OnAccent: Color,
    /** Large headings — the theme's onBackground. */
    val Heading: Color,
    /** Body copy — the theme's onSurface. */
    val Body: Color,
    /** Accent-tinted greeting/label text: primary blended toward onBackground
     *  so it stays legible in light theme with pastel presets. */
    val Greeting: Color,
    /** Secondary/muted text — onSurfaceVariant. */
    val Muted: Color,
    /** The dimmest text tier — outline. */
    val NoteMuted: Color,
    /** Slider/progress track remainder — surfaceContainerHighest. */
    val Track: Color,
    /** Trail dots behind the current position — a faded primary. */
    val DotDim: Color,
    /** Idle segment background — surfaceContainerHigh. */
    val SegmentIdle: Color,
    /** Note/card borders — outlineVariant. */
    val NoteBorder: Color,
    /** The light beam of the "Planning to Plan" screens, tinted with the
     *  theme colour so it can never clash with a custom theme. */
    val Aurora: Color,
)

/**
 * The check-in palette, derived live from the theme. Read it in composable
 * position (`CheckinFlowColors.Accent`); for draw lambdas (Canvas/drawBehind)
 * hoist it into a local first — draw scopes are not composable.
 */
internal val CheckinFlowColors: CheckinFlowPalette
    @Composable get() {
        val scheme = MaterialTheme.colorScheme
        return CheckinFlowPalette(
            Background = scheme.background,
            Accent = scheme.primary,
            OnAccent = scheme.onPrimary,
            Heading = scheme.onBackground,
            Body = scheme.onSurface,
            Greeting = lerp(scheme.onBackground, scheme.primary, 0.6f),
            Muted = scheme.onSurfaceVariant,
            NoteMuted = scheme.outline,
            Track = scheme.surfaceContainerHighest,
            DotDim = scheme.primary.copy(alpha = 0.45f),
            SegmentIdle = scheme.surfaceContainerHigh,
            NoteBorder = scheme.outlineVariant,
            Aurora = scheme.primary,
        )
    }

/**
 * The design uses Figtree at weights 300/400/500. The app ships Roboto Flex
 * as its variable font, so the flow renders it at the same three weights —
 * the light 300 is what carries the design's airy display type.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
internal fun rememberCheckinFonts(): FontFamily = remember {
    fun font(weight: FontWeight, variationWeight: Int) = Font(
        resId = DesignsystemR.font.roboto_flex,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(variationWeight))
    )
    FontFamily(
        font(FontWeight.Light, 300),
        font(FontWeight.Normal, 400),
        font(FontWeight.Medium, 500)
    )
}
