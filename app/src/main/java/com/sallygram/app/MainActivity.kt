package com.sallygram.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private data class Chat(
    val name: String,
    val message: String,
    val time: String,
    val initials: String,
    val unread: Int = 0,
    val online: Boolean = false
)

private val demoChats = listOf(
    Chat("Saved Messages", "Catatan pribadi dan file tersimpan", "23:18", "★"),
    Chat("Telegram", "Login berhasil. Selamat datang!", "22:54", "T", online = true),
    Chat("Dimas", "Oke, nanti aku kabari lagi", "22:31", "D", 2, true),
    Chat("Keluarga", "Ibu: Jangan lupa makan ya", "21:47", "K", 4),
    Chat("Android Developers", "Compose makes UI development easier", "20:05", "A", 1),
    Chat("Project Sallygram", "Build debug berikutnya segera", "Kemarin", "S", 0)
)

class MainActivity : ComponentActivity() {
    private lateinit var tdLibManager: TdLibManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tdLibManager = TdLibManager(applicationContext)
        tdLibManager.start()
        setContent { SallygramApp(tdLibManager) }
    }

    override fun onDestroy() {
        tdLibManager.close()
        super.onDestroy()
    }
}

@Composable
private fun SallygramApp(tdLibManager: TdLibManager) {
    var dark by remember { mutableStateOf(false) }
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme(
            primary = Color(0xFF65B9F0),
            secondary = Color(0xFF8DD0F5),
            surface = Color(0xFF101A24),
            background = Color(0xFF0B141D)
        ) else lightColorScheme(
            primary = Color(0xFF168ACB),
            secondary = Color(0xFF376B8D),
            surface = Color(0xFFFFFFFF),
            background = Color(0xFFF5F8FA)
        )
    ) {
        ChatHome(
            onToggleTheme = { dark = !dark },
            dark = dark,
            tdLibManager = tdLibManager
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHome(onToggleTheme: () -> Unit, dark: Boolean, tdLibManager: TdLibManager) {
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var showNewChat by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var loginInput by remember { mutableStateOf("") }
    val tdState by tdLibManager.state.collectAsState()
    val filtered = demoChats.filter {
        it.name.contains(query, ignoreCase = true) ||
            it.message.contains(query, ignoreCase = true)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewChat = true }) {
                Icon(Icons.Default.Add, contentDescription = "Chat baru")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.ChatBubble, contentDescription = null) },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Call, contentDescription = null) },
                    label = { Text("Panggilan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
                    label = { Text("Kontak") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Setelan") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (selectedTab) {
                            0 -> "Sallygram"
                            1 -> "Panggilan"
                            2 -> "Kontak"
                            else -> "Setelan"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Terhubung dengan orang-orangmu",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showInfo = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }

            if (selectedTab == 0) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Cari chat") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { showLogin = true },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column {
                            Text(
                                if (tdState.isReady) "Telegram terhubung" else "Koneksi Telegram · TDLib",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                tdState.status,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        top = 8.dp, bottom = 88.dp
                    )
                ) {
                    items(filtered, key = { it.name }) { chat ->
                        ChatRow(chat = chat, onClick = { showInfo = true })
                    }
                    if (filtered.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("Chat tidak ditemukan") }
                        }
                    }
                }
            } else {
                SecondaryPage(
                    tab = selectedTab,
                    onToggleTheme = onToggleTheme,
                    dark = dark,
                    onInfo = { showInfo = true }
                )
            }
        }
    }

    if (showLogin) {
        val step = tdState.step
        val canSubmit = step == TdLibManager.LoginStep.PHONE ||
            step == TdLibManager.LoginStep.CODE || step == TdLibManager.LoginStep.PASSWORD
        val title = when (step) {
            TdLibManager.LoginStep.PHONE -> "Login Telegram"
            TdLibManager.LoginStep.CODE -> "Kode verifikasi"
            TdLibManager.LoginStep.PASSWORD -> "Verifikasi dua langkah"
            TdLibManager.LoginStep.CONFIGURATION -> "Konfigurasi TDLib"
            TdLibManager.LoginStep.READY -> "Akun terhubung"
            else -> "Status Telegram"
        }
        AlertDialog(
            onDismissRequest = { showLogin = false; loginInput = "" },
            title = { Text(title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tdState.status)
                    if (canSubmit) {
                        OutlinedTextField(
                            value = loginInput,
                            onValueChange = { loginInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text(when (step) {
                                    TdLibManager.LoginStep.PHONE -> "Nomor telepon (+62…)"
                                    TdLibManager.LoginStep.CODE -> "Kode login"
                                    else -> "Kata sandi Telegram"
                                })
                            }
                        )
                    }
                    if (step == TdLibManager.LoginStep.CONFIGURATION) {
                        Text("Tambahkan TELEGRAM_API_ID dan TELEGRAM_API_HASH ke GitHub Actions Secrets, lalu build ulang. Dapatkan kredensial aplikasi di my.telegram.org.")
                    }
                    if (step == TdLibManager.LoginStep.REGISTRATION) {
                        Text("Akun baru belum didukung pada tahap ini. Gunakan akun Telegram yang sudah terdaftar.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    when (step) {
                        TdLibManager.LoginStep.PHONE -> tdLibManager.submitPhone(loginInput)
                        TdLibManager.LoginStep.CODE -> tdLibManager.submitCode(loginInput)
                        TdLibManager.LoginStep.PASSWORD -> tdLibManager.submitPassword(loginInput)
                        else -> { }
                    }
                    loginInput = ""
                    if (!canSubmit) showLogin = false
                }, enabled = !canSubmit || loginInput.isNotBlank()) {
                    Text(if (canSubmit) "Kirim" else "Tutup")
                }
            },
            dismissButton = {
                if (canSubmit) TextButton(onClick = { showLogin = false; loginInput = "" }) { Text("Batal") }
            }
        )
    }

    if (showNewChat) {
        AlertDialog(
            onDismissRequest = { showNewChat = false },
            title = { Text("Mulai chat") },
            text = { Text("Fitur kontak dan percakapan asli akan ditambahkan setelah integrasi Telegram API.") },
            confirmButton = {
                TextButton(onClick = { showNewChat = false }) { Text("Mengerti") }
            }
        )
    }
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Tentang Sallygram") },
            text = { Text("Sallygram 0.1.0\nKotlin, AndroidX, Material Design 3, dan TDLib. Integrasi autentikasi sedang disiapkan; daftar chat masih berupa data demo.") },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("Tutup") }
            }
        )
    }
}

@Composable
private fun ChatRow(chat: Chat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(avatarColor(chat.name)),
                contentAlignment = Alignment.Center
            ) {
                Text(chat.initials, color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
            }
            if (chat.online) {
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF35B86B))
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 8.dp)
        ) {
            Text(chat.name, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(chat.message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(chat.time, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (chat.unread > 0) {
                BadgedBox(badge = { Badge { Text(chat.unread.toString()) } }) {
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SecondaryPage(
    tab: Int,
    onToggleTheme: () -> Unit,
    dark: Boolean,
    onInfo: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (tab) {
            1 -> {
                Text("Panggilan", style = MaterialTheme.typography.titleLarge)
                Text("Riwayat panggilan akan muncul setelah layanan Telegram diintegrasikan.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            2 -> {
                Text("Kontak", style = MaterialTheme.typography.titleLarge)
                Text("Daftar kontak Telegram belum tersedia pada kerangka awal.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                Text("Tampilan", style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(if (dark) "Tema gelap aktif" else "Tema terang aktif")
                        Text("Ubah tampilan Sallygram",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onToggleTheme) {
                        Text(if (dark) "Terang" else "Gelap")
                    }
                }
                Text("Versi aplikasi: 0.1.0")
                TextButton(onClick = onInfo) { Text("Tentang Sallygram") }
            }
        }
    }
}

private fun avatarColor(seed: String): Color {
    val colors = listOf(
        Color(0xFF4776C5), Color(0xFF8E5CB7), Color(0xFFCF7852),
        Color(0xFF278B83), Color(0xFFB45E7A), Color(0xFF667A46)
    )
    return colors[(seed.hashCode().and(Int.MAX_VALUE)) % colors.size]
}
