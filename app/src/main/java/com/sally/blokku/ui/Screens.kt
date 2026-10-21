package com.sally.blokku.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                Text(
                    if (v != null) "Minecraft ${v.id}" else "Belum ada versi",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (v != null) "${v.loader ?: "Vanilla"} · ${s.ramMb} MB · Java ${JavaPicker.forVersion(v.id)}"
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
                            DropdownMenuItem(
                                text = { Text(p.label) },
                                onClick = { vm.setPreset(p); menu = false },
                            )
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
                        "Mode Eksperimen bisa membuat game crash. Otomatis kembali ke Seimbang jika gagal start dua kali.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

// ---------- Versi ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionsScreen(s: UiState, vm: AppViewModel) {
    var filter by remember { mutableIntStateOf(0) }
    val labels = listOf("Terpasang", "Rilis", "Snapshot")
    val list = s.versions.filter {
        when (filter) {
            0 -> it.installed
            1 -> it.type == VersionType.RELEASE
            else -> it.type == VersionType.SNAPSHOT
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, l ->
                SegmentedButton(
                    selected = filter == i, onClick = { filter = i },
                    shape = SegmentedButtonDefaults.itemShape(i, labels.size),
                ) { Text(l) }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (list.isEmpty()) {
            Text("Belum ada versi terpasang. Buka tab Rilis lalu ketuk Pasang.",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { v ->
                val progress = s.installing[v.id]
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(v.id, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (v.type == VersionType.RELEASE) "Rilis" else "Snapshot",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            when {
                                v.installed -> RadioButton(selected = s.selectedId == v.id, onClick = { vm.select(v.id) })
                                progress != null -> Text("${(progress * 100).toInt()}%")
                                else -> FilledTonalButton(onClick = { vm.install(v.id) }) { Text("Pasang") }
                            }
                        }
                        if (progress != null) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

// ---------- Jelajah ----------
@Composable
fun ExploreScreen() {
    var query by remember { mutableStateOf("") }
    var chip by remember { mutableIntStateOf(0) }
    val chips = listOf("Mod", "Modpack", "Resource", "Shader", "World")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Cari di Modrinth") },
            shape = MaterialTheme.shapes.extraLarge,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chips.size) { i ->
                FilterChip(selected = chip == i, onClick = { chip = i }, label = { Text(chips[i]) })
            }
        }
        // TODO(zalith): hubungkan ke feature/download (Modrinth utama, CurseForge opsional)
        Card(Modifier.fillMaxWidth()) {
            Text(
                "Hasil pencarian ${chips[chip].lowercase()} akan muncul di sini setelah engine dihubungkan.",
                Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ---------- Pengaturan ----------
@Composable
fun SettingsScreen(s: UiState, vm: AppViewModel) {
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
            var name by remember(s.accountName) { mutableStateOf(s.accountName ?: "") }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nama akun lokal") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { vm.setAccount(name) }, enabled = name.isNotBlank()) { Text("Simpan") }
                }
            }
        }
        item { Text("Lanjutan", style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Mode Lanjutan", Modifier.weight(1f))
                        Switch(checked = s.advanced, onCheckedChange = vm::setAdvanced)
                    }
                    if (s.advanced) {
                        Spacer(Modifier.height(8.dp))
                        Text("Mesin grafis: ${s.preset.renderer}", style = MaterialTheme.typography.bodyMedium)
                        Text("Resolusi render: ${s.preset.resolutionPct}%", style = MaterialTheme.typography.bodyMedium)
                        // TODO(zalith): file manager, log, runtime (AllSettings, FilesFragment)
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
                            Text("Blokku butuh izin penyimpanan dan notifikasi untuk mengunduh dan menjalankan game. " +
                                "Izin akan diminta saat dibutuhkan.", style = MaterialTheme.typography.bodyMedium)
                            // TODO(zalith): minta izin lewat ActivityResultContracts.RequestPermission
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
                            OutlinedTextField(name, { name = it }, label = { Text("Nama pemain") },
                                singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text("Login Microsoft menyusul. Beli game resminya untuk mendukung Mojang.",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            val last = pager.currentPage == 2
            Button(
                onClick = {
                    if (last) { vm.setAccount(name); vm.finishOnboarding() }
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                enabled = !last || name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(if (last) "Selesai" else "Lanjut") }
        }
    }
}
