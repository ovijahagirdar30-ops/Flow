package com.markel.flowstate.core.data

/**
 * Preset app color options. Stores ARGB integer values to avoid
 * a Compose dependency in this module. The [core:designsystem] module
 * converts these to Compose [Color] objects and generates full
 * Material 3 color schemes from them.
 */
enum class AppColor(
    val displayName: String,
    val lightArgb: Int,
    val darkArgb: Int,
) {
    GREEN(
        displayName = "Green",
        lightArgb = 0xFF4CAF50.toInt(),
        darkArgb = 0xFF81C784.toInt(),
    ),
    ROSE(
        displayName = "Rose",
        lightArgb = 0xFFE57373.toInt(),
        darkArgb = 0xFFEF9A9A.toInt(),
    ),
    LAVENDER(
        displayName = "Lavender",
        lightArgb = 0xFF9575CD.toInt(),
        darkArgb = 0xFFCE93D8.toInt(),
    ),
    SKY(
        displayName = "Sky",
        lightArgb = 0xFF64B5F6.toInt(),
        darkArgb = 0xFF90CAF9.toInt(),
    ),
    PEACH(
        displayName = "Peach",
        lightArgb = 0xFFFFAB91.toInt(),
        darkArgb = 0xFFFFCCBC.toInt(),
    ),
    MINT(
        displayName = "Mint",
        lightArgb = 0xFF80CBC4.toInt(),
        darkArgb = 0xFFB2DFDB.toInt(),
    ),
    LILAC(
        displayName = "Lilac",
        lightArgb = 0xFFC5CAE9.toInt(),
        darkArgb = 0xFFD1C4E9.toInt(),
    ),
    SAND(
        displayName = "Sand",
        lightArgb = 0xFFD7CCC8.toInt(),
        darkArgb = 0xFFEFEBE9.toInt(),
    ),
    /**
     * User-picked seed chosen on the colour wheel in Appearance settings.
     * The actual value lives in DataStore ([UserPreferencesRepository.customThemeColor]);
     * the argb fields here are only the fallback used before DataStore emits.
     * Must stay last so existing stored ordinals keep matching their preset.
     */
    CUSTOM(
        displayName = "Custom",
        lightArgb = 0xFF81C784.toInt(),
        darkArgb = 0xFF81C784.toInt(),
    ),
}
