package com.sally.blokku.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.movtery.zalithlauncher.feature.accounts.AccountType
import com.movtery.zalithlauncher.feature.accounts.AccountsManager
import com.movtery.zalithlauncher.feature.accounts.LocalAccountUtils
import com.movtery.zalithlauncher.feature.version.VersionsManager
import com.movtery.zalithlauncher.launch.LaunchGame
import com.movtery.zalithlauncher.setting.AllSettings
import net.kdt.pojavlaunch.LauncherActivity
import net.kdt.pojavlaunch.value.MinecraftAccount
import kotlinx.coroutines.delay

/** Jembatan nyata ke inti ZalithLauncher (VersionsManager, AccountsManager, LaunchGame, AllSettings). */
class ZalithEngine : LauncherEngine {

    override suspend fun loadVersions(): List<MinecraftVersion> {
        VersionsManager.refresh("Blokku", true)
        // refresh berjalan async di coroutine VersionsManager; beri waktu lalu baca hasilnya
        delay(700)
        return VersionsManager.getVersions().filter { it.isValid() }.map { v ->
            val info = v.getVersionInfo()
            MinecraftVersion(
                id = v.getVersionName(),
                mcVersion = info?.minecraftVersion ?: v.getVersionName(),
                info = info?.getInfoString() ?: "Vanilla",
            )
        }
    }

    override fun currentVersionId(): String? = VersionsManager.getCurrentVersion()?.getVersionName()

    override fun selectVersion(id: String) = VersionsManager.saveCurrentVersion(id)

    override fun currentAccountName(): String? = AccountsManager.currentAccount?.username

    override fun createLocalAccount(name: String) {
        val acc = MinecraftAccount().apply {
            username = name
            accountType = AccountType.LOCAL.type
        }
        runCatching { acc.save() }
        AccountsManager.doneListener.onLoginDone(acc)
        runCatching { AccountsManager.currentAccount = acc } // jadikan akun aktif
    }

    override fun applyPreset(preset: Preset, ramMb: Int) {
        // Zink hanya untuk mode Eksperimen; selain itu GL4ES ("opengles2")
        val rendererId = if (preset.experimental) "vulkan_zink" else "opengles2"
        AllSettings.renderer.put(rendererId).save()
        AllSettings.ramAllocation.value.put(ramMb).save()
        AllSettings.resolutionRatio.put(preset.resolutionPct).save()
        AllSettings.sustainedPerformance.put(preset == Preset.HEMAT).save()
        AllSettings.gameMenuShowFPS.put(preset.showFps).save()
    }

    override fun preflight(req: LaunchRequest, availMb: Int): Preflight = when {
        VersionsManager.getCurrentVersion() == null -> Preflight.NotInstalled
        AccountsManager.allAccounts.isEmpty() -> Preflight.NoAccount
        req.ramMb + 1024 > availMb -> Preflight.LowMemory(availMb, RamPolicy.snap(availMb - 1024))
        else -> Preflight.Ok
    }

    override fun launch(context: Context, req: LaunchRequest) {
        val version = VersionsManager.getCurrentVersion() ?: return
        applyPreset(req.preset, req.ramMb)
        val activity = context as? Activity ?: return
        // Sama dengan alur LauncherActivity.launchGame (pengingat akun lokal + login + unduh + start MainActivity)
        LocalAccountUtils.checkUsageAllowed(object : LocalAccountUtils.CheckResultListener {
            override fun onUsageAllowed() = LaunchGame.preLaunch(activity, version)
            override fun onUsageDenied() {
                if (!AllSettings.localAccountReminders.getValue()) {
                    LaunchGame.preLaunch(activity, version)
                } else {
                    LocalAccountUtils.openDialog(
                        activity,
                        { checked ->
                            LocalAccountUtils.saveReminders(checked)
                            LaunchGame.preLaunch(activity, version)
                        },
                        activity.getString(com.movtery.zalithlauncher.R.string.account_no_microsoft_account) +
                            activity.getString(com.movtery.zalithlauncher.R.string.account_purchase_minecraft_account_tip),
                        com.movtery.zalithlauncher.R.string.account_continue_to_launch_the_game,
                    )
                }
            }
        })
    }

    override fun openClassic(context: Context) {
        context.startActivity(Intent(context, LauncherActivity::class.java))
    }
}
