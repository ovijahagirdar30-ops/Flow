package com.markel.flowstate.feature.checkin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.markel.flowstate.core.designsystem.R as DesignsystemR

/**
 * Colors and typography of the redesigned evening check-in, ported verbatim
 * from the "Evening Check-in Flow" design (black board, `#CE93D8` accent,
 * light-weight display type). The design is intentionally a fixed dark
 * palette — the check-in is an evening ritual — so these are constants, not
 * theme tokens, mirroring how the design itself specifies them.
 */
internal object CheckinFlowColors {
    val Background = Color(0xFF000000)
    val Accent = Color(0xFFCE93D8)
    val Heading = Color(0xFFF1ECF3)
    val Body = Color(0xFFEDE7F0)
    val Greeting = Color(0xFFB79AC2)
    val Muted = Color(0xFFA99BB3)
    val NoteMuted = Color(0xFF8A7A94)
    val Track = Color(0xFF46324F)
    val DotDim = Color(0xFF4A3550)
    val SegmentIdle = Color(0xFF2E2236)
    val NoteBorder = Color(0xFF4A3A52)
    val OnAccent = Color(0xFF1D1023)

    /** The violet light beam of the "Planning to Plan" screens. */
    val Aurora = Color(0xFF8B5CF6)
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
