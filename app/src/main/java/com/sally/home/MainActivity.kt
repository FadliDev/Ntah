package com.sally.home

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import com.sally.home.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private var all: List<AppInfo> = emptyList()
    private var drawerOpen = false
    private val prefs by lazy { getSharedPreferences("home", MODE_PRIVATE) }
    private val dock = AppAdapter(false, { launch(it) }, { a, v -> menu(a, v) })
    private val grid = AppAdapter(true, { launch(it) }, { a, v -> menu(a, v) })

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = loadApps()
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        ViewCompat.setOnApplyWindowInsetsListener(b.root) { _, i ->
            val bars = i.getInsets(WindowInsetsCompat.Type.systemBars())
            b.home.setPadding(0, bars.top, 0, bars.bottom)
            b.drawer.setPadding(0, bars.top, 0, bars.bottom)
            i
        }
        b.dock.layoutManager = GridLayoutManager(this, 4)
        b.dock.adapter = dock
        b.apps.layoutManager = GridLayoutManager(this, 4)
        b.apps.adapter = grid

        b.root.doOnLayout { b.drawer.translationY = b.root.height.toFloat(); b.drawer.visibility = View.VISIBLE }

        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (vy < -800) { openDrawer(); return true }; return false
            }
            override fun onLongPress(e: MotionEvent) {
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), getString(R.string.wallpaper)))
            }
        })
        b.home.setOnTouchListener { _, e -> gd.onTouchEvent(e); true }
        b.fab.setOnClickListener { openDrawer() }

        b.search.doOnTextChanged { _, _, _, _ -> refresh() }
        b.search.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) { current().firstOrNull()?.let { launch(it) }; true } else false
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (drawerOpen) closeDrawer() }
        })
        registerReceiver(pkgReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addDataScheme("package")
        })
        loadApps()
    }

    override fun onDestroy() { unregisterReceiver(pkgReceiver); super.onDestroy() }
    override fun onNewIntent(i: Intent) { super.onNewIntent(i); if (drawerOpen) closeDrawer() }

    private fun loadApps() = Thread {
        val pm = packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(q, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName,
                ComponentName(it.activityInfo.packageName, it.activityInfo.name), it.loadIcon(pm)) }
            .filter { it.pkg != packageName }
            .sortedBy { it.label.lowercase() }
        runOnUiThread { all = list; refresh() }
    }.start()

    private fun pins() = prefs.getString("pins", "")!!.split(",").filter { it.isNotEmpty() }
    private fun current(): List<AppInfo> {
        val q = b.search.text.toString().trim()
        if (q.isEmpty()) return all
        return all.map { it to Native.score(q, it.label) }.filter { it.second > 0 }
            .sortedByDescending { it.second }.map { it.first }
    }
    private fun refresh() {
        grid.submit(current())
        dock.submit(pins().mapNotNull { p -> all.firstOrNull { it.pkg == p } })
    }

    private fun launch(a: AppInfo) {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(a.cn).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        if (drawerOpen) closeDrawer()
    }

    private fun menu(a: AppInfo, v: View) {
        val pinned = a.pkg in pins()
        PopupMenu(this, v).apply {
            menu.add(0, 1, 0, getString(if (pinned) R.string.unpin else R.string.pin))
            menu.add(0, 2, 1, getString(R.string.app_info))
            menu.add(0, 3, 2, getString(R.string.uninstall))
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> {
                        val l = pins().toMutableList()
                        if (pinned) l.remove(a.pkg) else if (l.size < 4) l.add(a.pkg)
                        prefs.edit().putString("pins", l.joinToString(",")).apply(); refresh()
                    }
                    2 -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${a.pkg}")))
                    3 -> startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${a.pkg}")))
                }; true
            }
            show()
        }
    }

    private fun openDrawer() {
        drawerOpen = true
        b.drawer.animate().translationY(0f).setDuration(380)
            .setInterpolator(android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f)).start()
    }
    private fun closeDrawer() {
        drawerOpen = false
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(b.search.windowToken, 0)
        b.search.setText(""); b.search.clearFocus(); b.apps.scrollToPosition(0)
        b.drawer.animate().translationY(b.root.height.toFloat()).setDuration(280).start()
    }
}
