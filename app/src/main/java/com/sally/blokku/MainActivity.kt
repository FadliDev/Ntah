package com.sally.blokku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sally.blokku.ui.BlokkuApp
import com.sally.blokku.ui.BlokkuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BlokkuTheme { BlokkuApp() } }
    }
}
