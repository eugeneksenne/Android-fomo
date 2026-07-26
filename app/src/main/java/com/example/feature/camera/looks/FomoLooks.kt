package com.example.feature.camera.looks

import androidx.compose.ui.graphics.Color

/**
 * The FOMO Looks catalogue — a single source of truth for colour grading.
 *
 * Previously each Look existed only as an ad-hoc `when (selectedLook)` block
 * returning a translucent overlay colour, duplicated in three places in
 * `CameraScreen`, and applied *only to the on-screen preview*. The saved photo
 * or video never carried the grade, so what the user shared never matched what
 * they framed.
 *
 * A Look is now described by real colour-grading parameters that can drive
 * both the live preview and an actual pixel transform on the captured file.
 */
enum class FomoLook(
    val displayName: String,
    /** Tint applied over the image. */
    val tint: Color,
    /** Tint strength at 100% intensity. */
    val tintAlpha: Float,
    /** Multiplicative contrast; 1f is neutral. */
    val contrast: Float,
    /** Multiplicative saturation; 1f is neutral, 0f is greyscale. */
    val saturation: Float,
    /** Additive brightness in the range -1f..1f; 0f is neutral. */
    val brightness: Float,
) {
    NONE("None", Color.Transparent, 0f, 1f, 1f, 0f),
    PULSE("Pulse", Color(0xFFFF2D55), 0.12f, 1.10f, 1.15f, 0.02f),
    NEON("Neon", Color(0xFF00F0FF), 0.16f, 1.18f, 1.30f, 0.00f),
    GLOW("Glow", Color(0xFFFFD700), 0.14f, 1.05f, 1.10f, 0.06f),
    MIDNIGHT("Midnight", Color(0xFF0033AA), 0.22f, 1.20f, 0.85f, -0.08f),
    VINTAGE_PARTY("Vintage Party", Color(0xFFE28B00), 0.12f, 0.95f, 0.80f, 0.03f),
    ELECTRIC("Electric", Color(0xFFB026FF), 0.18f, 1.15f, 1.35f, 0.00f),
    LUXE("Luxe", Color(0xFFF3E5AB), 0.10f, 1.08f, 0.95f, 0.05f),
    NOIR("Noir", Color(0xFF333333), 0.25f, 1.30f, 0.00f, -0.02f),
    STAGE("Stage", Color(0xFF666666), 0.20f, 1.25f, 1.05f, -0.05f),
    FLASH("Flash", Color(0xFFE0F7FA), 0.08f, 1.02f, 1.00f, 0.10f),
    SUNSET("Sunset", Color(0xFFFF5722), 0.15f, 1.06f, 1.20f, 0.04f),
    ROOFTOP("Rooftop", Color(0xFF3F51B5), 0.12f, 1.10f, 1.08f, -0.03f);

    /** True when this Look leaves pixels untouched. */
    val isIdentity: Boolean get() = this == NONE

    /**
     * Scales this Look's strength.
     *
     * @param intensity 0f..1f, driven by the long-press intensity control the
     *   spec requires.
     */
    fun scaled(intensity: Float): Grade {
        val t = intensity.coerceIn(0f, 1f)
        return Grade(
            tint = tint,
            tintAlpha = tintAlpha * t,
            // Interpolate each parameter from neutral toward the Look.
            contrast = 1f + (contrast - 1f) * t,
            saturation = 1f + (saturation - 1f) * t,
            brightness = brightness * t,
        )
    }

    /** Resolved grading parameters at a given intensity. */
    data class Grade(
        val tint: Color,
        val tintAlpha: Float,
        val contrast: Float,
        val saturation: Float,
        val brightness: Float,
    ) {
        val isIdentity: Boolean
            get() = tintAlpha <= 0.001f &&
                kotlin.math.abs(contrast - 1f) < 0.001f &&
                kotlin.math.abs(saturation - 1f) < 0.001f &&
                kotlin.math.abs(brightness) < 0.001f
    }

    companion object {
        /** Ordered list for the carousel; NONE first so users can clear a Look. */
        val carousel: List<FomoLook> = entries.toList()

        fun fromDisplayName(name: String): FomoLook =
            entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: NONE
    }
}
