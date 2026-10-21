package com.sally.blokku.core

/** Pilih versi Java otomatis dari versi Minecraft. Pengguna awam tidak melihat ini. */
object JavaPicker {
    private val release = Regex("""^1\.(\d+)(?:\.(\d+))?""")

    fun forVersion(id: String): Int {
        val m = release.find(id) ?: return 21 // snapshot modern (mis. 24w14a)
        val minor = m.groupValues[1].toInt()
        val patch = m.groupValues[2].toIntOrNull() ?: 0
        return when {
            minor <= 16 -> 8
            minor in 17..19 -> 17
            minor == 20 -> if (patch >= 5) 21 else 17
            else -> 21
        }
    }
}
