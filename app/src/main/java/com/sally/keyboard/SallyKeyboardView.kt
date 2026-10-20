package com.sally.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import com.google.android.material.R as M

/** Keyboard QWERTY gaya Material 3: tombol tonal, pill saran, sudut membulat. */
class SallyKeyboardView(ctx: Context, private val l: Listener) : LinearLayout(ctx) {
    interface Listener { fun onText(t: String); fun onDelete(); fun onEnter(); fun onSuggestion(w: String) }

    private var shift = 0            // 0 off, 1 sekali, 2 caps lock
    private var symbols = false
    private val letters = arrayOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val syms = arrayOf("1234567890", "@#$%&-+()", "!\"'.:;/?")
    private val handler = Handler(Looper.getMainLooper())
    private val suggViews = List(3) { TextView(ctx) }
    private val keys = LinearLayout(ctx).apply { orientation = VERTICAL }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun c(attr: Int) = MaterialColors.getColor(this, attr)

    init {
        orientation = VERTICAL
        setBackgroundColor(c(M.attr.colorSurfaceContainer))
        setPadding(dp(4), dp(6), dp(4), dp(8))
        val bar = LinearLayout(context).apply { orientation = HORIZONTAL }
        suggViews.forEach { t ->
            t.gravity = Gravity.CENTER; t.textSize = 15f; t.maxLines = 1
            t.setTextColor(c(M.attr.colorOnSecondaryContainer))
            t.background = shape(c(M.attr.colorSecondaryContainer), 20)
            t.setOnClickListener { l.onSuggestion(t.text.toString()) }
            t.visibility = INVISIBLE
            bar.addView(t, LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), dp(2), dp(3), dp(4)) })
        }
        addView(bar); addView(keys)
        render()
    }

    fun setSuggestions(list: List<String>) =
        suggViews.forEachIndexed { i, t -> t.text = list.getOrNull(i) ?: ""; t.visibility = if (i < list.size) VISIBLE else INVISIBLE }

    fun setShift(s: Int) { if (shift != s) { shift = s; render() } }
    fun shiftState() = shift

    private fun shape(color: Int, r: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(r).toFloat() }
    private fun ripple(color: Int, r: Int) = RippleDrawable(
        ColorStateList.valueOf(ColorUtils.setAlphaComponent(c(M.attr.colorOnSurface), 40)), shape(color, r), null)

    // style: 0 biasa, 1 tonal, 2 primary
    private fun key(label: String?, icon: Int?, w: Float, style: Int, tint: Boolean = false, onClick: () -> Unit): View {
        val bg = when (style) { 1 -> c(M.attr.colorSecondaryContainer); 2 -> c(M.attr.colorPrimary); else -> c(M.attr.colorSurfaceContainerHighest) }
        val fg = when (style) { 1 -> c(M.attr.colorOnSecondaryContainer); 2 -> c(M.attr.colorOnPrimary); else -> c(M.attr.colorOnSurface) }
        val v: View = if (icon != null) ImageView(context).apply {
            setImageResource(icon); setColorFilter(if (tint) c(M.attr.colorOnPrimary) else fg); scaleType = ImageView.ScaleType.CENTER
        } else TextView(context).apply { text = label; setTextColor(fg); textSize = 20f; gravity = Gravity.CENTER }
        v.background = ripple(if (tint) c(M.attr.colorPrimary) else bg, 16)
        v.setOnClickListener { it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); onClick() }
        v.layoutParams = LayoutParams(0, dp(52), w).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }
        return v
    }
    private fun spacer(w: Float) = View(context).apply { layoutParams = LayoutParams(0, dp(52), w) }
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
        keys.addView(row(
            key(if (symbols) "ABC" else "?123", null, 1.5f, 1) { symbols = !symbols; render() },
            key(",", null, 1f, 0) { l.onText(",") },
            key(null, null, 5f, 0) { l.onText(" ") }.also { (it as TextView).text = "Sally" ; it.textSize = 14f; it.setTextColor(c(M.attr.colorOnSurfaceVariant)) },
            key(".", null, 1f, 0) { l.onText(".") },
            key(null, R.drawable.ic_enter, 1.5f, 2) { l.onEnter() }
        ))
    }
}
