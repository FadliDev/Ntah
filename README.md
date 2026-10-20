# Sallygram TDLib Kotlin compile fix

Patch ini hanya mengganti `app/src/main/java/com/sallygram/app/TdLibManager.kt`.

Memperbaiki:
- Urutan/parameter `TdApi.SetTdlibParameters` sesuai binding Java TDLib yang dibangun workflow.
- Menghapus state database-encryption lama yang tidak tersedia di binding TDLib saat ini.
- Menggunakan tipe generik `TdApi.Function<*>` untuk Kotlin.

Pasang dengan mengekstrak ZIP ke root repository, lalu:

```bash
git add app/src/main/java/com/sallygram/app/TdLibManager.kt
git commit -m "Fix TDLib Kotlin API compatibility"
git push origin main
```
