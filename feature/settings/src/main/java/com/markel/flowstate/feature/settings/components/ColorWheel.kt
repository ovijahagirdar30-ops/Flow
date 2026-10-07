package com.markel.flowstate.feature.settings.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * HSV colour wheel for picking the app's theme colour.
 *
 * The ring is hue (starting at the top, increasing counter-clockwise to match
 * [Brush.sweepGradient]'s native direction) and saturation fades from the
 * centre (white) to the rim (fully saturated). Brightness is inherited from
 * the incoming colour so re-editing a colour never shifts it.
 *
 * @param selectedColor current colour as an ARGB int
 * @param onColorChange reports every colour picked on the wheel (drag or tap)
 */
@Composable
internal fun ColorWheel(
    selectedColor: Int,
    onColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Seed once per composition — the dialog disposes/recreates this composable
    // each time it opens, so a fresh seed picks up the latest applied colour.
    val seeded = remember { hsvFromArgb(selectedColor) }
    var hue by remember { mutableFloatStateOf(seeded[0]) }
    var saturation by remember { mutableFloatStateOf(seeded[1]) }
    val value = remember(seeded[2]) { seeded[2].coerceIn(0.35f, 1f) }
    val hueColors = remember(value) { List(13) { i -> Color.hsv(i * 30f, 1f, value) } }

    fun select(offset: Offset, wheelRadius: Float, center: Offset) {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        saturation = (hypot(dx, dy) / wheelRadius).coerceIn(0f, 1f)
        // Hue measured counter-clockwise from the top: top=0, left=90,
        // bottom=180, right=270 — matching the rotated sweep gradient.
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
        hue = ((270.0 - angle + 360.0) % 360.0).toFloat()
        onColorChange(Color.hsv(hue, saturation, value).toArgbInt())
    }

    Canvas(
        modifier = modifier
            .size(240.dp)
            .pointerInput(value) {
                val wheelRadius = minOf(size.width, size.height) / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                detectTapGestures { offset -> select(offset, wheelRadius, center) }
            }
            .pointerInput(value) {
                val wheelRadius = minOf(size.width, size.height) / 2f - 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                detectDragGestures(
                    onDragStart = { offset -> select(offset, wheelRadius, center) },
                    onDrag = { change, _ ->
                        change.consume()
                        select(change.position, wheelRadius, center)
                    }
                )
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val markerRadius = 10.dp.toPx()
        // Inset the wheel so the selection marker never clips at the rim.
        val wheelRadius = size.minDimension / 2f - markerRadius

        // Hue ring: sweep gradient rotated so red (hue 0) starts at the top and
        // the hues then run counter-clockwise, same as the marker math.
        rotate(degrees = -90f, pivot = center) {
            drawCircle(brush = Brush.sweepGradient(hueColors), radius = wheelRadius)
        }
        // Saturation: white at the centre fading out to reveal full saturation
        // at the rim — alpha(d) = 1 - d/r means effective saturation = d/r,
        // exactly the radius the marker maps saturation to.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                center = center,
                radius = wheelRadius
            ),
            radius = wheelRadius
        )

        // Selection marker: white ring around the picked colour.
        val angleRad = Math.toRadians(hue.toDouble())
        val markerCenter = Offset(
            x = center.x - (saturation * wheelRadius * sin(angleRad)).toFloat(),
            y = center.y - (saturation * wheelRadius * cos(angleRad)).toFloat()
        )
        drawCircle(color = Color.White, radius = markerRadius, center = markerCenter)
        drawCircle(
            color = Color.Black.copy(alpha = 0.18f),
            radius = markerRadius,
            center = markerCenter,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = Color.hsv(hue, saturation, value),
            radius = markerRadius - 3.dp.toPx(),
            center = markerCenter
        )
    }
}

/** Decomposes an ARGB int into HSV components (hue 0..360, sat/val 0..1). */
private fun hsvFromArgb(argb: Int): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return hsv
}

/** ARGB int for a Compose color — mirrors `Color.toArgb()` without the import. */
private fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255f + 0.5f).toInt(),
        (red * 255f + 0.5f).toInt(),
        (green * 255f + 0.5f).toInt(),
        (blue * 255f + 0.5f).toInt()
    )
