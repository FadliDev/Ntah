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
    val installing: Map<String, Float> = emptyMap(),
    val preflight: Preflight? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("blokku", Context.MODE_PRIVATE)
    // TODO(zalith): ganti dengan ZalithEngine dari modul :core
    private val engine: LauncherEngine = FakeLauncherEngine()

    private val _state = MutableStateFlow(
        UiState(
            onboardingDone = prefs.getBoolean("onb", false),
            preset = runCatching { Preset.valueOf(prefs.getString("preset", null)!!) }
                .getOrDefault(Preset.SEIMBANG),
            ramMb = prefs.getInt("ram", Preset.SEIMBANG.ramMb),
            selectedId = prefs.getString("sel", null),
            accountName = prefs.getString("acc", null),
            advanced = prefs.getBoolean("adv", false),
        )
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.update { it.copy(versions = engine.fetchVersions()) } }
    }

    fun setPreset(p: Preset) {
        _state.update { it.copy(preset = p, ramMb = p.ramMb) }
        prefs.edit().putString("preset", p.name).putInt("ram", p.ramMb).apply()
    }

    fun setRam(mb: Int) {
        val v = RamPolicy.snap(mb)
        _state.update { it.copy(ramMb = v) }
        prefs.edit().putInt("ram", v).apply()
    }

    fun setAdvanced(on: Boolean) {
        _state.update { it.copy(advanced = on) }
        prefs.edit().putBoolean("adv", on).apply()
    }

    fun setAccount(name: String) {
        _state.update { it.copy(accountName = name.trim()) }
        prefs.edit().putString("acc", name.trim()).apply()
    }

    fun finishOnboarding() {
        _state.update { it.copy(onboardingDone = true) }
        prefs.edit().putBoolean("onb", true).apply()
    }

    fun select(id: String) {
        _state.update { it.copy(selectedId = id) }
        prefs.edit().putString("sel", id).apply()
    }

    fun install(id: String) {
        if (id in _state.value.installing) return
        viewModelScope.launch {
            engine.install(id).collect { p ->
                _state.update { it.copy(installing = it.installing + (id to p)) }
            }
            _state.update { s ->
                s.copy(
                    installing = s.installing - id,
                    versions = s.versions.map { if (it.id == id) it.copy(installed = true) else it },
                    selectedId = s.selectedId ?: id,
                )
            }
        }
    }

    fun play() {
        val s = _state.value
        val id = s.selectedId
        val v = s.versions.firstOrNull { it.id == id }
        if (id == null || v == null) { _state.update { it.copy(preflight = Preflight.NotInstalled) }; return }
        val req = LaunchRequest(id, s.preset, s.ramMb, s.accountName)
        viewModelScope.launch {
            val r = engine.preflight(req, availMb(), v.installed)
            if (r is Preflight.Ok) engine.launch(req) else _state.update { it.copy(preflight = r) }
        }
    }

    fun applySuggestedRam(mb: Int) { setRam(mb); dismissPreflight() }
    fun dismissPreflight() = _state.update { it.copy(preflight = null) }

    private fun availMb(): Int {
        val am = getApplication<Application>().getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return (mi.availMem / (1024 * 1024)).toInt()
    }
}
