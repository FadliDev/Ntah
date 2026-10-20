package com.sally.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import com.sally.keyboard.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var b: ActivitySettingsBinding
    private val imm get() = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

    override fun onCreate(s: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)   // Material You
        super.onCreate(s)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(b.root)
        // Padding mengikuti status bar, nav bar, dan keyboard (field tidak tertutup)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, i ->
            val bars = i.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = i.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom)); i
        }
        b.enable.setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        b.choose.setOnClickListener { imm.showInputMethodPicker() }
    }

    override fun onResume() { super.onResume(); refreshState() }
    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) refreshState() }

    /** Status langkah setup ikut berubah otomatis. */
    private fun refreshState() {
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val selected = current?.startsWith("$packageName/") == true
        b.enable.text = getString(if (enabled) R.string.step1_done else R.string.step1)
        b.choose.isEnabled = enabled
        b.choose.text = getString(if (selected) R.string.step2_done else R.string.step2)
        b.status.setText(when {
            selected -> R.string.status_ready
            enabled -> R.string.status_enabled
            else -> R.string.setup_desc
        })
    }
}
