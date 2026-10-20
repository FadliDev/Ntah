# Sallygram GitHub Actions fix — sdkmanager not found

Log penyebab: `sdkmanager: command not found` (exit code 127).

Patch mengganti `.github/workflows/build.yml` dan menambahkan step `android-actions/setup-android@v3` sebelum `sdkmanager` digunakan. Step berikutnya memverifikasi bahwa `sdkmanager` tersedia, lalu menginstal Android platform/build-tools.

## Terapkan

Ekstrak ZIP ke root repository (folder yang sama dengan `gradlew`, `app/`, dan `scripts/`), timpa `.github/workflows/build.yml`, lalu jalankan:

```bash
git add .github/workflows/build.yml
git commit -m "Fix Android SDK manager setup"
git push origin main
```

Pastikan file berada tepat di `.github/workflows/build.yml`, bukan di `Sallygram/.github/workflows/build.yml`.

Setelah push, buka Actions dan periksa run terbaru. Patch ini memperbaiki error SDK manager yang tampak di log yang dikirim; build TDLib/APK berikutnya tetap perlu diverifikasi dari hasil run baru.
