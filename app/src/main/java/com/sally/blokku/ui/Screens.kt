package com.sally.blokku.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sally.blokku.core.*
import kotlinx.coroutines.launch

// ---------- Beranda ----------
@Composable
fun HomeScreen(s: UiState, vm: AppViewModel) {
    val v = s.versions.firstOrNull { it.id == s.selectedId }
    var menu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(v?.id ?: "Belum ada versi", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (v != null) "${v.info} · ${s.ramMb} MB · Java ${JavaPicker.forVersion(v.mcVersion)}"
                    else "Buka tab Versi untuk memasang",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Box {
                    AssistChip(
                        onClick = { menu = true },
                        label = { Text(s.preset.label) },
                        trailingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                    )
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        Preset.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.label) }, onClick = { vm.setPreset(p); menu = false })
                        }
                    }
                }
            }
        }
        if (s.preset.experimental) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Mode Eksperimen (Zink) bisa membuat game crash. Kalau gagal, pilih Seimbang.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ---------- Versi ----------
@Composable
fun VersionsScreen(s: UiState, vm: AppViewModel) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { vm.openClassic(context) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text("Pasang versi baru")
        }
        if (s.versions.isEmpty()) {
            Text(
                "Belum ada versi terpasang. Ketuk Pasang versi baru, lalu kembali ke sini.",
                Modifier.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyMedium,
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.versions, key = { it.id }) { v ->
                ElevatedCard(Modifier.fillMaxWidth(), onClick = { vm.select(v.id) }) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(v.id, style = MaterialTheme.typography.titleMedium)
                            Text(v.info, style = MaterialTheme.typography.bodyMedium)
                        }
                        RadioButton(selected = s.selectedId == v.id, onClick = { vm.select(v.id) })
                    }
                }
            }
        }
    }
}

// ---------- Jelajah ----------
@Composable
fun ExploreScreen(vm: AppViewModel) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Mod, modpack, resource pack, shader, dan world", style = MaterialTheme.typography.titleMedium)
        Text(
            "Pencarian dan unduhan Modrinth/CurseForge masih memakai launcher klasik. Layar Jelajah baru menyusul.",
            style = MaterialTheme.typography.bodyMedium,
        )
        FilledTonalButton(onClick = { vm.openClassic(context) }, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Filled.Search, null)
            Spacer(Modifier.width(8.dp))
            Text("Buka di launcher klasik")
        }
    }
}

// ---------- Pengaturan ----------
@Composable
fun SettingsScreen(s: UiState, vm: AppViewModel) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Kinerja", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Preset.entries.forEach { p ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = s.preset == p, onClick = { vm.setPreset(p) })
                            Text(p.label)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Memori game: ${s.ramMb} MB", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = s.ramMb.toFloat(),
                        onValueChange = { vm.setRam(it.toInt()) },
                        valueRange = RamPolicy.MIN_MB.toFloat()..RamPolicy.MAX_SAFE_MB.toFloat(),
                        steps = 3,
                    )
                }
            }
        }
        item { Text("Akun", style = MaterialTheme.typography.titleMedium) }
        item {
            var name by remember(s.accountName) { mutableStateOf("") }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Akun aktif: ${s.accountName ?: "-"}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(name, { name = it }, label = { Text("Tambah akun lokal") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { vm.setAccount(name) }, enabled = name.isNotBlank()) { Text("Simpan") }
                }
            }
        }
        item { Text("Lanjutan", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mode Lanjutan", Modifier.weight(1f))
                        Switch(checked = s.advanced, onCheckedChange = vm::setAdvanced)
                    }
                    if (s.advanced) {
                        Text("Mesin grafis: ${s.preset.renderer}", style = MaterialTheme.typography.bodyMedium)
                        Text("Resolusi render: ${s.preset.resolutionPct}%", style = MaterialTheme.typography.bodyMedium)
                        OutlinedButton(onClick = { vm.openClassic(context) }) {
                            Text("Launcher klasik (file manager, log, kontrol)")
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------- Onboarding ----------
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(s: UiState, vm: AppViewModel) {
    val pager = rememberPagerState { 3 }
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(s.accountName ?: "") }

    Scaffold { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(24.dp)) {
            HorizontalPager(pager, Modifier.weight(1f), userScrollEnabled = false) { page ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (page) {
                        0 -> {
                            Text("Selamat datang", style = MaterialTheme.typography.headlineSmall)
                            Text(
                                "Launcher akan menyiapkan komponen game saat pertama dibuka. " +
                                    "Izin penyimpanan diminta otomatis kalau dibutuhkan.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        1 -> {
                            Text("Pilih kinerja", style = MaterialTheme.typography.headlineSmall)
                            val ps = listOf(Preset.HEMAT, Preset.SEIMBANG)
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ps.forEachIndexed { i, p ->
                                    SegmentedButton(
                                        selected = s.preset == p, onClick = { vm.setPreset(p) },
                                        shape = SegmentedButtonDefaults.itemShape(i, ps.size),
                                    ) { Text(p.label) }
                                }
                            }
                            Text("Bisa diubah kapan saja di tab Atur.", style = MaterialTheme.typography.bodyMedium)
                        }
                        else -> {
                            Text("Buat akun", style = MaterialTheme.typography.headlineSmall)
                            if (s.accountName != null) {
                                Text("Akun terdeteksi: ${s.accountName}", style = MaterialTheme.typography.bodyMedium)
                            } else {
                                OutlinedTextField(name, { name = it }, label = { Text("Nama pemain") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth())
                            }
                            Text(
                                "Login Microsoft ada di ikon akun setelah ini. Beli game resminya untuk mendukung Mojang.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
            val last = pager.currentPage == 2
            Button(
                onClick = {
                    if (last) { if (s.accountName == null) vm.setAccount(name); vm.finishOnboarding() }
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                enabled = !last || s.accountName != null || name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(if (last) "Selesai" else "Lanjut") }
        }
    }
}
