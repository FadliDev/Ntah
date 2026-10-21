package com.sally.blokku.core

enum class VersionType { RELEASE, SNAPSHOT }

data class MinecraftVersion(
    val id: String,
    val type: VersionType,
    val installed: Boolean = false,
    val loader: String? = null,
)

data class LaunchRequest(
    val versionId: String,
    val preset: Preset,
    val ramMb: Int,
    val accountName: String?,
)

sealed interface Preflight {
    data object Ok : Preflight
    data object NotInstalled : Preflight
    data object NoAccount : Preflight
    data class LowMemory(val availMb: Int, val suggestMb: Int) : Preflight
}
