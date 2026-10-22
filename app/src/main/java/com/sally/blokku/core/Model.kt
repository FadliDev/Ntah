package com.sally.blokku.core

/** Versi Minecraft yang sudah terpasang (dari VersionsManager Zalith). */
data class MinecraftVersion(
    val id: String,        // nama folder versi
    val mcVersion: String, // versi Minecraft murni, dipakai untuk memilih Java
    val info: String,      // mis. "1.20.1, Fabric - 0.15"
)

data class LaunchRequest(val versionId: String, val preset: Preset, val ramMb: Int)

sealed interface Preflight {
    data object Ok : Preflight
    data object NotInstalled : Preflight
    data object NoAccount : Preflight
    data class LowMemory(val availMb: Int, val suggestMb: Int) : Preflight
}
