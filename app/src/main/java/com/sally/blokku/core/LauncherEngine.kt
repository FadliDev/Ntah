package com.sally.blokku.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Batas antara UI baru (Compose) dan inti ZalithLauncher.
 *
 * Saat integrasi, buat `ZalithEngine : LauncherEngine` di modul :core yang membungkus:
 *  - fetchVersions -> feature/version + MinecraftDownloader
 *  - install       -> MinecraftDownloader
 *  - launch        -> JREUtils / MultiRTUtils, lalu start MainActivity (layar game)
 * UI tidak perlu diubah sama sekali.
 */
interface LauncherEngine {
    suspend fun fetchVersions(): List<MinecraftVersion>
    fun install(id: String): Flow<Float>
    suspend fun preflight(req: LaunchRequest, availMb: Int, installed: Boolean): Preflight
    suspend fun launch(req: LaunchRequest)
}

/** Implementasi palsu supaya UI bisa dijalankan dan diuji tanpa inti Zalith. */
class FakeLauncherEngine : LauncherEngine {
    override suspend fun fetchVersions() = listOf(
        MinecraftVersion("1.20.1", VersionType.RELEASE),
        MinecraftVersion("1.16.5", VersionType.RELEASE),
        MinecraftVersion("1.12.2", VersionType.RELEASE),
        MinecraftVersion("1.8.9", VersionType.RELEASE),
        MinecraftVersion("24w14a", VersionType.SNAPSHOT),
    )

    override fun install(id: String): Flow<Float> = flow {
        for (i in 1..20) { delay(120); emit(i / 20f) }
    }

    override suspend fun preflight(req: LaunchRequest, availMb: Int, installed: Boolean): Preflight = when {
        !installed -> Preflight.NotInstalled
        req.accountName.isNullOrBlank() -> Preflight.NoAccount
        req.ramMb + 1024 > availMb -> Preflight.LowMemory(availMb, RamPolicy.snap(availMb - 1024))
        else -> Preflight.Ok
    }

    override suspend fun launch(req: LaunchRequest) {
        // TODO(zalith): JREUtils.launchJavaVM(...) dengan renderer req.preset.renderer
    }
}
