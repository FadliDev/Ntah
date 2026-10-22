package com.sally.blokku.core

import android.content.Context

/** Batas antara UI Compose dan inti Zalith. Implementasi nyata: [ZalithEngine]. */
interface LauncherEngine {
    suspend fun loadVersions(): List<MinecraftVersion>
    fun currentVersionId(): String?
    fun selectVersion(id: String)
    fun currentAccountName(): String?
    fun createLocalAccount(name: String)
    fun applyPreset(preset: Preset, ramMb: Int)
    fun preflight(req: LaunchRequest, availMb: Int): Preflight
    fun launch(context: Context, req: LaunchRequest)
    /** Buka launcher klasik Zalith (instal versi, unduh mod, login Microsoft, editor kontrol, dll). */
    fun openClassic(context: Context)
}
