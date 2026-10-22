package com.sally.blokku

import android.os.Bundle
import androidx.activity.compose.setContent
import com.movtery.zalithlauncher.ui.activity.BaseActivity
import com.sally.blokku.ui.BlokkuApp
import com.sally.blokku.ui.BlokkuTheme

/** Layar utama baru (Compose + M3). Launcher klasik tetap ada lewat tombol "Launcher klasik". */
class BlokkuActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BlokkuTheme { BlokkuApp() } }
    }
}
