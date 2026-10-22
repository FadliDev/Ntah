package com.sally.blokku.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sally.blokku.core.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UiState(
    val onboardingDone: Boolean = false,
    val preset: Preset = Preset.SEIMBANG,
    val ramMb: Int = Preset.SEIMBANG.ramMb,
    val selectedId: String? = null,
    val accountName: String? = null,
    val advanced: Boolean = false,
    val versions: List<MinecraftVersion> = emptyList(),
    val preflight: Preflight? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("blokku", Context.MODE_PRIVATE)
    val engine: LauncherEngine = ZalithEngine()

    private val _state = MutableStateFlow(
        UiState(
            onboardingDone = prefs.getBoolean("onb", false),
            preset = runCatching { Preset.valueOf(prefs.getString("preset", null)!!) }
                .getOrDefault(Preset.SEIMBANG),
            ramMb = prefs.getInt("ram", Preset.SEIMBANG.ramMb),
            advanced = prefs.getBoolean("adv", false),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    /** Dipanggil saat layar kembali aktif (mis. setelah memasang versi di launcher klasik). */
    fun refresh() {
        viewModelScope.launch {
            val versions = engine.loadVersions()
            _state.update {
                it.copy(
                    versions = versions,
                    selectedId = engine.currentVersionId(),
                    accountName = engine.currentAccountName(),
                )
            }
        }
    }

    fun setPreset(p: Preset) {
        _state.update { it.copy(preset = p, ramMb = p.ramMb) }
        prefs.edit().putString("preset", p.name).putInt("ram", p.ramMb).apply()
        engine.applyPreset(p, p.ramMb)
    }

    fun setRam(mb: Int) {
        val v = RamPolicy.snap(mb)
        _state.update { it.copy(ramMb = v) }
        prefs.edit().putInt("ram", v).apply()
        engine.applyPreset(_state.value.preset, v)
    }

    fun setAdvanced(on: Boolean) {
        _state.update { it.copy(advanced = on) }
        prefs.edit().putBoolean("adv", on).apply()
    }

    fun setAccount(name: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        engine.createLocalAccount(n)
        _state.update { it.copy(accountName = n) }
    }

    fun finishOnboarding() {
        _state.update { it.copy(onboardingDone = true) }
        prefs.edit().putBoolean("onb", true).apply()
        engine.applyPreset(_state.value.preset, _state.value.ramMb)
    }

    fun select(id: String) {
        engine.selectVersion(id)
        _state.update { it.copy(selectedId = id) }
    }

    fun play(context: Context) {
        val s = _state.value
        val id = s.selectedId ?: engine.currentVersionId()
        if (id == null) { _state.update { it.copy(preflight = Preflight.NotInstalled) }; return }
        val req = LaunchRequest(id, s.preset, s.ramMb)
        val r = engine.preflight(req, availMb())
        if (r is Preflight.Ok) engine.launch(context, req) else _state.update { it.copy(preflight = r) }
    }

    fun openClassic(context: Context) = engine.openClassic(context)
    fun applySuggestedRam(mb: Int) { setRam(mb); dismissPreflight() }
    fun dismissPreflight() = _state.update { it.copy(preflight = null) }

    private fun availMb(): Int {
        val am = getApplication<Application>().getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return (mi.availMem / (1024 * 1024)).toInt()
    }
}
