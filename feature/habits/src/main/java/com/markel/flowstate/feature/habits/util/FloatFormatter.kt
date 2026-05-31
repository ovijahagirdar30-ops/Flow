package com.markel.flowstate.feature.habits.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Formats a Float value for display in the UI.
 *
 * - Whole numbers (or values very close to a whole number due to floating-point
 *   precision artifacts) are shown without decimals: `2.0f` → `"2"`
 * - True decimal values are shown with up to 2 decimal places, trimming
 *   unnecessary trailing zeros: `2.50f` → `"2.5"`, `1.33f` → `"1.33"`
 *
 * This function exists because Kotlin's `Float.toString()` can produce
 * representations like `"1.0"` or `"2.0000001"`, and the naive pattern
 * `if (value % 1 == 0f) value.toInt().toString() else value.toString()`
 * fails when floating-point arithmetic leaves the value just above or below
 * an integer (e.g. `1.9999998f` or `2.0000002f`).
 */
fun formatFloat(value: Float): String {
    val rounded = round(value)
    return if (abs(value - rounded) < 0.001f) {
        // The value is effectively a whole number
        rounded.toInt().toString()
    } else {
        // True decimal – format to 2 dp and strip trailing zeros
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }
}