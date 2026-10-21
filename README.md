# Blokku (nama kerja)

Lapisan UI baru (Jetpack Compose + Material 3) untuk fork ZalithLauncher, sesuai PRD:
arm64-v8a, target Snapdragon 710 / Adreno 616 / RAM 6 GB, preset Hemat/Seimbang/Eksperimen.

## Status
- UI jalan sendiri dengan `FakeLauncherEngine` (data palsu): onboarding, Beranda, Versi, Jelajah, Pengaturan, dialog pra-cek memori.
- Logika murni sudah ada + unit test: `JavaPicker` (Java 8/17/21 otomatis), `RamPolicy` (1024–2048 MB), `Preset`.
- **Belum** tersambung ke inti Zalith (native, JRE, renderer, login). Itu langkah integrasi di bawah.
- Belum pernah dikompilasi (tidak ada Android SDK/jaringan di sini). Build pertama lewat GitHub Actions.

## Build
GitHub Actions: push repo, workflow `Build APK` menghasilkan artifact `Blokku-debug`.
Lokal/Termux: `gradle :app:assembleDebug` (butuh JDK 17 + Android SDK 35).

## Integrasi dengan ZalithLauncher (GPL-3.0)
1. Fork https://github.com/ZalithLauncher/ZalithLauncher (repo sudah archived; Fase 0 PRD membandingkan dengan ZalithLauncher2).
2. Pindahkan `jni/`, `jniLibs/arm64-v8a`, `assets/components`, renderer, `JREUtils`, `MultiRTUtils`,
   `MinecraftGLSurface`, `MainActivity` (layar game) ke modul `:engine`; build dengan `-Darch=arm64`.
3. Di `:core`, tulis `ZalithEngine : LauncherEngine` (lihat komentar di `LauncherEngine.kt`) yang membungkus
   `feature/version`, `MinecraftDownloader`, `feature/accounts`, `feature/download`.
4. Di `AppViewModel`, ganti `FakeLauncherEngine()` dengan `ZalithEngine`.
5. Naikkan compileSdk/targetSdk ke 35, R8 tetap mati, pertahankan `AllSettings` dulu.
6. Ganti nama, ikon, package, dan pertahankan notice GPL-3.0 + halaman Tentang (jangan pakai nama "ZalithLauncher"/"ZL").
