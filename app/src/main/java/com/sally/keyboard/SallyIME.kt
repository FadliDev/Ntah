package com.sally.keyboard

import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo

class SallyIME : InputMethodService(), SallyKeyboardView.Listener {
    private lateinit var kb: SallyKeyboardView

    override fun onCreateInputView(): View {
        kb = SallyKeyboardView(ContextThemeWrapper(this, R.style.Theme_Keyboard), this)
        return kb
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val caps = info.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 ||
                info.inputType and InputType.TYPE_TEXT_FLAG_CAP_WORDS != 0
        kb.setShift(if (caps) 1 else 0)
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

    private fun updateSuggestions() {
        if (!::kb.isInitialized) return
        val p = prefix()
        kb.setSuggestions(if (p.isEmpty()) emptyList() else Native.suggest(p.lowercase()).toList())
    }
}
