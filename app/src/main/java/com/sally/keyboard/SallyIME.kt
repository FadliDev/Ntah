package com.sally.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.material.color.DynamicColors

class SallyIME : InputMethodService(), SallyKeyboardView.Listener {
    private lateinit var kb: SallyKeyboardView

    override fun onCreateInputView(): View {
        // Material You: warna ikut wallpaper (Android 12+), fallback ke tema statis
        val themed = DynamicColors.wrapContextIfAvailable(ContextThemeWrapper(this, R.style.Theme_Keyboard))
        kb = SallyKeyboardView(themed, this)
        return kb
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val caps = info.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 ||
                info.inputType and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0
        kb.setShift(if (caps) 1 else 0)
        kb.setSuggestionsEnabled(wantsSuggestions(info))
        updateSuggestions()
    }

    override fun onUpdateSelection(a: Int, b: Int, c: Int, d: Int, e: Int, f: Int) {
        super.onUpdateSelection(a, b, c, d, e, f); updateSuggestions()
    }

    override fun onText(t: String) {
        currentInputConnection?.commitText(t, 1)
        if (kb.shiftState() == 1) kb.setShift(0)
        if (t == "." ) kb.setShift(1)
    }

    override fun onDelete() {
        val ic = currentInputConnection ?: return
        if (ic.getSelectedText(0).isNullOrEmpty()) ic.deleteSurroundingText(1, 0) else ic.commitText("", 1)
    }

    override fun onEnter() {
        val info = currentInputEditorInfo
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION == 0 &&
            action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED)
            currentInputConnection?.performEditorAction(action)
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
    }

    override fun onSuggestion(w: String) {
        val ic = currentInputConnection ?: return
        val prefix = prefix()
        ic.deleteSurroundingText(prefix.length, 0)
        ic.commitText((if (prefix.firstOrNull()?.isUpperCase() == true) w.replaceFirstChar { it.uppercase() } else w) + " ", 1)
    }

    private fun prefix() = (currentInputConnection?.getTextBeforeCursor(24, 0)?.toString() ?: "").takeLastWhile { it.isLetter() }

    /** Bar saran hanya untuk field teks biasa; password, email, URL, angka, dll disembunyikan. */
    private fun wantsSuggestions(info: EditorInfo): Boolean {
        val type = info.inputType
        if (type and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) return false
        if (type and InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS != 0) return false
        return when (type and InputType.TYPE_MASK_VARIATION) {
            InputType.TYPE_TEXT_VARIATION_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
            InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
            InputType.TYPE_TEXT_VARIATION_URI -> false
            else -> true
        }
    }

    private fun updateSuggestions() {
        if (!::kb.isInitialized) return
        val p = prefix()
        kb.setSuggestions(
            if (p.isEmpty()) {
                // Belum mengetik: tampilkan kata pembuka supaya bar tidak kosong / tidak melompat
                val starters = listOf("saya", "yang", "terima kasih")
                if (kb.shiftState() > 0) starters.map { it.replaceFirstChar { c -> c.uppercase() } } else starters
            } else Native.suggest(p.lowercase()).toList()
        )
    }
}
