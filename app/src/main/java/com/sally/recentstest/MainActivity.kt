package com.sally.recentstest

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationTargetException

class MainActivity : AppCompatActivity() {

    private lateinit var out: TextView

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, result ->
        log("Izin Shizuku: " + if (result == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HiddenApiBypass.addHiddenApiExemptions("")

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24) }
        val b1 = Button(this).apply { text = "1. Minta izin Shizuku" }
        val b2 = Button(this).apply { text = "2. Jalankan semua tes" }
        out = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        root.addView(b1); root.addView(b2)
        root.addView(ScrollView(this).apply { addView(out) })
        setContentView(root)

        Shizuku.addRequestPermissionResultListener(permListener)

        b1.setOnClickListener {
            if (!Shizuku.pingBinder()) { log("Shizuku belum jalan. Start dulu di app Shizuku."); return@setOnClickListener }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) log("Izin sudah ada.")
            else Shizuku.requestPermission(1)
        }
        b2.setOnClickListener { Thread { runTests() }.start() }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }

    private fun log(s: String) = runOnUiThread { out.append(s + "\n\n") }

    private fun runTests() {
        runOnUiThread { out.text = "" }

        // 0. Info dasar
        log("Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT}), ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")

        // 1. Status Shizuku
        if (!Shizuku.pingBinder()) { log("[X] Shizuku tidak jalan"); return }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) { log("[X] Belum ada izin Shizuku (tekan tombol 1)"); return }
        log("[OK] Shizuku jalan, uid = ${Shizuku.getUid()} (2000 = shell, 0 = root)")

        // 2. Izin yang dipegang paket shell
        val perms = listOf(
            "android.permission.MANAGE_ACTIVITY_STACKS",
            "android.permission.START_TASKS_FROM_RECENTS",
            "android.permission.CONTROL_REMOTE_APP_TRANSITION_ANIMATIONS",
            "android.permission.REMOVE_TASKS",
            "android.permission.STATUS_BAR_SERVICE",
            "android.permission.INJECT_EVENTS",
            "android.permission.READ_FRAME_BUFFER",
            "android.permission.SET_ANIMATION_SCALE"
        )
        val sb = StringBuilder("Izin paket com.android.shell:\n")
        for (p in perms) {
            val r = packageManager.checkPermission(p, "com.android.shell")
            sb.append(if (r == PackageManager.PERMISSION_GRANTED) "  [YA]  " else "  [TIDAK] ").append(p.removePrefix("android.permission.")).append("\n")
        }
        log(sb.toString().trimEnd())

        // 3. Komponen recents resmi
        try {
            val id = resources.getIdentifier("config_recentsComponentName", "string", "android")
            val v = if (id != 0) Resources_getSystemString(id) else "(resource tidak ketemu)"
            log("config_recentsComponentName = $v")
        } catch (e: Throwable) { log("config_recentsComponentName error: $e") }

        // 4. IActivityTaskManager lewat Shizuku
        val atm = try {
            val b = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity_task"))
            Class.forName("android.app.IActivityTaskManager\$Stub")
                .getMethod("asInterface", IBinder::class.java).invoke(null, b)!!
        } catch (e: Throwable) { log("[X] Gagal ambil IActivityTaskManager: $e"); return }
        log("[OK] IActivityTaskManager didapat via Shizuku")

        // 4a. getRecentTasks
        try {
            val m = atm.javaClass.methods.first { it.name == "getRecentTasks" }
            val slice = m.invoke(atm, 5, 2, 0)
            val list = slice!!.javaClass.getMethod("getList").invoke(slice) as List<*>
            val names = list.joinToString("\n") { t ->
                (t as android.app.ActivityManager.RecentTaskInfo).baseIntent.component?.flattenToShortString() ?: "?"
            }
            log("[OK] getRecentTasks: ${list.size} task\n$names")
        } catch (e: Throwable) { log("[X] getRecentTasks: ${unwrap(e)}") }

        // 4b. cancelRecentsAnimation (aman, cuma cek izin MANAGE_ACTIVITY_STACKS)
        try {
            val m = atm.javaClass.methods.first { it.name == "cancelRecentsAnimation" }
            m.invoke(atm, true)
            log("[OK] cancelRecentsAnimation: LOLOS cek izin (jalur recents animation kemungkinan terbuka)")
        } catch (e: Throwable) {
            val u = unwrap(e)
            log("[X] cancelRecentsAnimation: $u" + if (u.contains("SecurityException")) "\n=> ditolak sistem, jalur Shizuku kemungkinan mentok" else "")
        }

        log("Selesai. Screenshot hasil ini dan kirim ke gue.")
    }

    @Suppress("FunctionName")
    private fun Resources_getSystemString(id: Int): String = android.content.res.Resources.getSystem().getString(id)

    private fun unwrap(e: Throwable): String {
        val c = if (e is InvocationTargetException) e.targetException else e
        return c.javaClass.simpleName + ": " + c.message
    }
}
