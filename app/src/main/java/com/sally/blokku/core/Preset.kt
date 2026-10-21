package com.sally.blokku.core

/** Preset kinerja untuk Snapdragon 710 / Adreno 616 / RAM 6 GB (lihat PRD). */
enum class Preset(
    val label: String,
    val ramMb: Int,
    val resolutionPct: Int,
    val renderer: String,
    val showFps: Boolean,
    val experimental: Boolean = false,
) {
    HEMAT("Hemat", 1280, 75, "gl4es", false),
    SEIMBANG("Seimbang", 1536, 100, "gl4es", false),
    EKSPERIMEN("Eksperimen", 1536, 100, "zink+turnip", false, experimental = true),
}

object RamPolicy {
    const val MIN_MB = 1024
    const val MAX_SAFE_MB = 2048
    const val STEP_MB = 256

    /** Bulatkan ke kelipatan 256 dalam rentang aman 1024–2048. */
    fun snap(mb: Int): Int = ((mb / STEP_MB) * STEP_MB).coerceIn(MIN_MB, MAX_SAFE_MB)
}
