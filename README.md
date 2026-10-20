# Sallygram

Kerangka awal aplikasi klien Telegram Android menggunakan Kotlin, AndroidX, Jetpack Compose, dan Material Design 3.

## Status saat ini

- UI awal daftar chat, pencarian lokal, navigasi Chat/Panggilan/Kontak/Setelan.
- Tema terang dan gelap.
- Data chat adalah data demo lokal.
- Belum login Telegram, belum mengirim pesan, dan belum sinkron dengan akun Telegram.

## Build APK melalui GitHub Actions

1. Buat repository GitHub dan upload/push seluruh isi proyek ini.
2. Buka tab **Actions** dan aktifkan workflow bila diminta.
3. Jalankan workflow **Build Sallygram APK** atau push commit ke `main`/`master`.
4. Buka hasil run workflow dan unduh artifact **Sallygram-debug**.
5. Ekstrak ZIP artifact untuk mendapatkan APK debug.

## Build lokal

Butuh JDK 17, Android SDK Platform 35, dan Gradle wrapper yang valid:

```bash
./gradlew assembleDebug
```

APK debug akan berada di `app/build/outputs/apk/debug/app-debug.apk`.

## Langkah berikutnya

Integrasi akun Telegram memerlukan Telegram API (`api_id` dan `api_hash`) dan implementasi protokol melalui TDLib atau solusi klien yang sesuai. Jangan menaruh kredensial rahasia di repository publik. Untuk distribusi, konfigurasi signing release dengan GitHub Actions Secrets.

## Teknologi

- Kotlin
- AndroidX
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- GitHub Actions
