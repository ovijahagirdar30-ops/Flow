package com.markel.flowstate.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.markel.flowstate.core.designsystem.R

/**
 * Canonical Material 3 typography scale (unmodified).
 *
 * We intentionally do NOT redefine [fontSize], [lineHeight] or [letterSpacing] for the 15 M3
 * styles: those values are already tuned by the Material design team. The "premium" feel comes
 * from swapping [fontFamily] to a custom variable font (Roboto Flex) and activating OpenType
 * features (`ss02`, `dlig`).
 *
 */
private val DefaultTypography = Typography()

/**
 * OpenType features enabled across the whole scale:
 *  - `ss02`: Stylistic Set 02 — alternate glyph variants (e.g. two-storey "a").
 *  - `dlig`: Discretionary Ligatures — refined letter combinations for display contexts.
 */
private const val OPEN_TYPE_FEATURES = "ss02, dlig"

/**
 * Variable-font instances for Roboto Flex.
 *
 * Roboto Flex exposes the following axes (https://fonts.google.com/specimen/Roboto+Flex):
 *  - wght (100–1000): weight
 *  - wdth (25–151): width percentage
 *  - opsz (8–144): optical size
 *  - GRAD (-200–150): grade
 *  - slnt (-10–0): slant
 *  - XOPQ, XTRA, YOPQ, YTAS, YTDE, YTFI, YTLC, YTUC: parametric axes (left untouched)
 *
 * We expose two distinct "personalities" for the same font:
 *  - [displayFont]: heavy, slightly condensed — for hero text (display/headline).
 *  - [textFont]: regular weight, default width — for body/labels (legibility-first).
 */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun displayFont(): FontFamily {
    val family = FontFamily(
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight.Black,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.width(112.5f),
            ),
        ),
    )
    return family
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun textFont(): FontFamily {
    val family = FontFamily(
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight.Normal,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(400),
                FontVariation.width(100f),
            ),
        ),
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight.Medium,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(500),
                FontVariation.width(100f),
            ),
        ),
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(600),
                FontVariation.width(100f),
            ),
        ),
        Font(
            resId = R.font.roboto_flex,
            weight = FontWeight.Bold,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(700),
                FontVariation.width(100f),
            ),
        ),
    )
    return family
}

/**
 * The Material 3 typography for FlowState.
 *
 * Returned as a [Composable] because resolving [R.font.roboto_flex] requires access to the
 * design-system module's resources, which is only safe inside a composition scope.
 */
@Composable
fun FlowStateTypography(): Typography {
    val display = displayFont()
    val text = textFont()
    return remember(display, text) {
        Typography(
            // Hero text — display font (heavy, expanded).
            displayLarge = DefaultTypography.displayLarge.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            displayMedium = DefaultTypography.displayMedium.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            displaySmall = DefaultTypography.displaySmall.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            // Headlines — still display personality, lighter weight via the same family.
            headlineLarge = DefaultTypography.headlineLarge.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            headlineMedium = DefaultTypography.headlineMedium.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            headlineSmall = DefaultTypography.headlineSmall.copy(
                fontFamily = display,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            // Titles — text font (regular width, weights handled by family).
            titleLarge = DefaultTypography.titleLarge.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            titleMedium = DefaultTypography.titleMedium.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            titleSmall = DefaultTypography.titleSmall.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            // Body — text font.
            bodyLarge = DefaultTypography.bodyLarge.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            bodyMedium = DefaultTypography.bodyMedium.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            bodySmall = DefaultTypography.bodySmall.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            // Labels — text font.
            labelLarge = DefaultTypography.labelLarge.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            labelMedium = DefaultTypography.labelMedium.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
            labelSmall = DefaultTypography.labelSmall.copy(
                fontFamily = text,
                fontFeatureSettings = OPEN_TYPE_FEATURES,
            ),
        )
    }
}
