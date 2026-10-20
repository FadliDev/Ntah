package com.sally.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as M

/**
 * Keyboard QWERTY gaya Material 3 yang adaptif:
 * - tinggi tombol & ukuran teks menyesuaikan layar / orientasi
 * - lebar dibatasi di layar lebar (tablet / foldable / landscape)
 * - padding bawah mengikuti navigation bar (gesture / 3-button)
 * - warna ikut Material You (lewat context dari SallyIME)
 * - bar saran bisa disembunyikan untuk field yang tidak butuh (password, angka, dll)
 */
class SallyKeyboardView(ctx: Context, private val l: Listener) : LinearLayout(ctx) {
    interface Listener { fun onText(t: String); fun onDelete(); fun onEnter(); fun onSuggestion(w: String) }

    private var shift = 0            // 0 off, 1 sekali, 2 caps lock
    private var symbols = false
    private val letters = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val syms = arrayOf("1234567890", "@#$%&-+()", "!\"'.:;/?")
    private val handler = Handler(Looper.getMainLooper())
    private val suggViews = List(3) { TextView(ctx) }
    private val bar = LinearLayout(ctx).apply { orientation = HORIZONTAL }
    private val keys = LinearLayout(ctx).apply { orientation = VERTICAL }

    private var keyH = 0             // px
    private var navInset = 0         // px
    private val maxContentW get() = dp(720)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun c(attr: Int) = MaterialColors.getColor(this, attr)

    private fun computeKeyH(): Int {
        val dm = resources.displayMetrics
        val land = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val raw = dm.heightPixels * (if (land) 0.118f else 0.062f)
        return raw.toInt().coerceIn(dp(40), dp(62))
    }
    private fun barH() = (keyH * 0.78f).toInt()

    init {
        orientation = VERTICAL
        setBackgroundColor(c(M.attr.colorSurfaceContainer))
        keyH = computeKeyH()
        suggViews.forEach { t ->
            t.gravity = Gravity.CENTER; t.textSize = 15f; t.maxLines = 1
            t.setPadding(dp(8), 0, dp(8), 0)
            t.setTextColor(c(M.attr.colorOnSecondaryContainer))
            t.background = shape(c(M.attr.colorSecondaryContainer), 20)
            t.setOnClickListener { l.onSuggestion(t.text.toString()) }
            t.visibility = INVISIBLE
            bar.addView(t, LayoutParams(0, barH(), 1f).apply { setMargins(dp(3), dp(2), dp(3), dp(4)) })
        }
        addView(bar); addView(keys)
        applyPadding()
        render()

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            if (nav != navInset) { navInset = nav; applyPadding() }
            insets
        }
    }

    private fun applyPadding() {
        val side = if (width > maxContentW) (width - maxContentW) / 2 else dp(4)
        setPadding(side, dp(6), side, maxOf(dp(8), navInset + dp(4)))
    }

    /** Hitung ulang ukuran bila layar/orientasi berubah. */
    private fun refreshMetrics() {
        val nh = computeKeyH()
        if (nh != keyH) {
            keyH = nh
            suggViews.forEach { (it.layoutParams as LayoutParams).height = barH(); it.requestLayout() }
            render()
        }
        applyPadding()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        if (w != ow) post { refreshMetrics() }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        post { refreshMetrics() }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    /** Tampilkan / sembunyikan bar saran (mis. disembunyikan untuk password & angka). */
    fun setSuggestionsEnabled(on: Boolean) { bar.visibility = if (on) VISIBLE else GONE }

    fun setSuggestions(list: List<String>) =
        suggViews.forEachIndexed { i, t -> t.text = list.getOrNull(i) ?: ""; t.visibility = if (i < list.size) VISIBLE else INVISIBLE }

    fun setShift(s: Int) { if (shift != s) { shift = s; render() } }
    fun shiftState() = shift

    private fun shape(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
    private fun ripple(color: Int, r: Int) = RippleDrawable(
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(c(M.attr.colorOnSurface), 40)), shape(color, r), null)

    // style: 0 biasa, 1 tonal, 2 primary. small = teks tombol fungsi (?123, Sally, dll)
    private fun key(label: String?, icon: Int?, w: Float, style: Int, tint: Boolean = false, small: Boolean = false, onClick: () -> Unit): View {
        val bg = when (style) { 1 -> c(M.attr.colorSecondaryContainer); 2 -> c(M.attr.colorPrimary); else -> c(M.attr.colorSurfaceContainerHighest) }
        val fg = when (style) { 1 -> c(M.attr.colorOnSecondaryContainer); 2 -> c(M.attr.colorOnPrimary); else -> c(M.attr.colorOnSurface) }
        val v: View = if (icon != null) ImageView(context).apply {
            setImageResource(icon); setColorFilter(if (tint) c(M.attr.colorOnPrimary) else fg); scaleType = ImageView.ScaleType.CENTER
        } else TextView(context).apply {
            text = label; setTextColor(fg); gravity = Gravity.CENTER; maxLines = 1; includeFontPadding = false
            setTextSize(TypedValue.COMPLEX_UNIT_PX, keyH * (if (small) 0.29f else 0.38f))
        }
        v.background = ripple(if (tint) c(M.attr.colorPrimary) else bg, 16)
        v.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() }
        v.layoutParams = LayoutParams(0, keyH, w).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        return v
    }
    private fun spacer(w: Float) = View(context).apply { layoutParams = LayoutParams(0, keyH, w) }
    private fun row(vararg v: View) = LinearLayout(context).apply { orientation = HORIZONTAL; v.forEach { addView(it) } }

    private fun render() {
        keys.removeAllViews()
        val src = if (symbols) syms else letters
        fun chars(s: String) = s.map { ch ->
            val t = if (!symbols && shift > 0) ch.uppercaseChar().toString() else ch.toString()
            key(t, null, 1f, 0) { l.onText(t) }
        }
        keys.addView(row(*chars(src[0]).toTypedArray()))
        keys.addView(row(*(listOf(spacer(0.5f)) + chars(src[1]) + listOf(spacer(0.5f))).toTypedArray()))
        val shiftKey = if (symbols) spacer(1.5f) else key(null, R.drawable.ic_shift, 1.5f, 1, tint = shift > 0) {
            shift = when (shift) { 0 -> 1; 1 -> 2; else -> 0 }; render()
        }
        val del = key(null, R.drawable.ic_backspace, 1.5f, 1) {}
        del.setOnClickListener(null)
        del.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true; v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); l.onDelete()
                    handler.postDelayed(object : Runnable { override fun run() { l.onDelete(); handler.postDelayed(this, 45) } }, 380)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { v.isPressed = false; handler.removeCallbacksAndMessages(null) }
            }; true
        }
        keys.addView(row(*(listOf(shiftKey) + chars(src[2]) + listOf(del)).toTypedArray()))
        val space = key("Sally", null, 5f, 0, small = true) { l.onText(" ") }
        (space as TextView).setTextColor(c(M.attr.colorOnSurfaceVariant))
        keys.addView(row(
            key(if (symbols) "ABC" else "?123", null, 1.5f, 1, small = true) { symbols = !symbols; render() },
            key(",", null, 1f, 0) { l.onText(",") },
            space,
            key(".", null, 1f, 0) { l.onText(".") },
            key(null, R.drawable.ic_enter, 1.5f, 2) { l.onEnter() }
        ))
    }
}
