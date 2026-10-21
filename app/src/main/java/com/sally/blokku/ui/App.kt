package com.sally.blokku.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sally.blokku.R
import com.sally.blokku.core.Preflight

private enum class Tab(val label: String, val icon: ImageVector) {
    Beranda("Beranda", Icons.Filled.Home),
    Versi("Versi", Icons.Filled.List),
    Jelajah("Jelajah", Icons.Filled.Search),
    Atur("Atur", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlokkuApp(vm: AppViewModel = viewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()

    if (!s.onboardingDone) {
        OnboardingScreen(s, vm)
        return
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Beranda) }
    var accountSheet by remember { mutableStateOf(false) }
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { accountSheet = true }) {
                        Icon(Icons.Filled.Person, contentDescription = "Akun")
                    }
                },
            )
        },
        bottomBar = {
            if (!landscape) NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Beranda) ExtendedFloatingActionButton(
                onClick = vm::play,
                icon = { Icon(Icons.Filled.PlayArrow, null) },
                text = { Text("Main") },
            )
        },
    ) { padding ->
        Row(Modifier.padding(padding).fillMaxSize()) {
            if (landscape) NavigationRail {
                Tab.entries.forEach { t ->
                    NavigationRailItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, null) }, label = { Text(t.label) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxHeight()) {
                when (tab) {
                    Tab.Beranda -> HomeScreen(s, vm)
                    Tab.Versi -> VersionsScreen(s, vm)
                    Tab.Jelajah -> ExploreScreen()
                    Tab.Atur -> SettingsScreen(s, vm)
                }
            }
        }
    }

    if (accountSheet) ModalBottomSheet(onDismissRequest = { accountSheet = false }) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Akun", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(s.accountName ?: "Belum ada akun", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            // TODO(zalith): daftar akun dari feature/accounts + tombol login Microsoft
            OutlinedButton(onClick = { accountSheet = false }) { Text("Tutup") }
        }
    }

    s.preflight?.let { p -> PreflightDialog(p, vm) }
}

@Composable
private fun PreflightDialog(p: Preflight, vm: AppViewModel) {
    val (title, body) = when (p) {
        Preflight.NotInstalled -> "Versi belum terpasang" to "Pasang versi Minecraft dulu di tab Versi."
        Preflight.NoAccount -> "Belum ada akun" to "Buat akun lokal atau login dulu lewat ikon akun."
        is Preflight.LowMemory ->
            "Memori tidak cukup" to "Memori kosong ±${p.availMb} MB. Turunkan alokasi game ke ${p.suggestMb} MB?"
        Preflight.Ok -> return
    }
    AlertDialog(
        onDismissRequest = vm::dismissPreflight,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            if (p is Preflight.LowMemory)
                TextButton(onClick = { vm.applySuggestedRam(p.suggestMb) }) { Text("Terapkan") }
            else TextButton(onClick = vm::dismissPreflight) { Text("OK") }
        },
        dismissButton = {
            if (p is Preflight.LowMemory) TextButton(onClick = vm::dismissPreflight) { Text("Batal") }
        },
    )
}
