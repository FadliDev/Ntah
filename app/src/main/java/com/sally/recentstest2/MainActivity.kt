package com.sally.recentstest2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.Parcelable
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

class MainActivity : AppCompatActivity() {

    companion object { const val DESC_RUNNER = "android.view.IRecentsAnimationRunner" }

    private lateinit var out: TextView
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var controller: Any? = null
    @Volatile private var finished = true
    @Volatile private var lastVariant = 'A'
    private var t0 = 0L

    private val permListener = Shizuku.OnRequestPermissionResultListener { _, r ->
        log("Izin Shizuku: " + if (r == PackageManager.PERMISSION_GRANTED) "GRANTED" else "DENIED")
    }

    // Binder buatan sendiri yang meniru IRecentsAnimationRunner (code 1 = canceled, 2 = start)
    private val runnerBinder = object : Binder() {
        init { attachInterface(null, DESC_RUNNER) }
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            try {
                when (code) {
                    1 -> {
                        log("CALLBACK onAnimationCanceled")
                        finished = true; controller = null
                        return true
                    }
                    2 -> {
                        data.enforceInterface(DESC_RUNNER)
                        onAnimationStart(data)
                        return true
                    }
                }
            } catch (t: Throwable) { log("[X] error di callback: $t") }
            return super.onTransact(code, data, reply, flags)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HiddenApiBypass.addHiddenApiExemptions("")

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 48, 24, 24) }
        val b1 = Button(this).apply { text = "1. Minta izin Shizuku" }
        val b2 = Button(this).apply { text = "2A. Tes: intent RecentsActivity (varian lama)" }
        val b2b = Button(this).apply { text = "2B. Tes: intent HOME + package launcher" }
        val b2c = Button(this).apply { text = "2C. Tes: intent HOME default (tanpa component)" }
        val b3 = Button(this).apply { text = "3. Tes 5 detik lagi (varian terakhir, pindah app)" }
        val b4 = Button(this).apply { text = "DARURAT: cancel recents animation" }
        out = TextView(this).apply { textSize = 12f; setTextIsSelectable(true) }
        listOf(b1, b2, b2b, b2c, b3, b4).forEach { root.addView(it) }
        root.addView(ScrollView(this).apply { addView(out) })
        setContentView(root)

        Shizuku.addRequestPermissionResultListener(permListener)

        b1.setOnClickListener {
            if (!Shizuku.pingBinder()) log("Shizuku belum jalan.")
            else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) log("Izin sudah ada.")
            else Shizuku.requestPermission(1)
        }
        b2.setOnClickListener { lastVariant = 'A'; startTest(0, 'A') }
        b2b.setOnClickListener { lastVariant = 'B'; startTest(0, 'B') }
        b2c.setOnClickListener { lastVariant = 'C'; startTest(0, 'C') }
        b3.setOnClickListener { startTest(5000, lastVariant) }
        b4.setOnClickListener {
            Thread {
                try {
                    val atm = atm()
                    val m = atm.javaClass.methods.first { it.name == "cancelRecentsAnimation" }
                    m.invoke(atm, true)
                    log("[OK] cancelRecentsAnimation dipanggil")
                } catch (e: Throwable) { log("[X] cancel: ${unwrap(e)}") }
            }.start()
        }
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permListener)
        super.onDestroy()
    }

    private fun log(s: String) = runOnUiThread { out.append(s + "\n\n") }

    private fun unwrap(e: Throwable): String {
        val c = if (e is InvocationTargetException) e.targetException else e
        return c.javaClass.simpleName + ": " + c.message
    }

    private fun atm(): Any {
        val b = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("activity_task"))
        return Class.forName("android.app.IActivityTaskManager\$Stub")
            .getMethod("asInterface", IBinder::class.java).invoke(null, b)!!
    }

    private fun startTest(delayMs: Long, variant: Char) {
        Thread {
            runOnUiThread { out.text = "" }
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                log("[X] Shizuku belum jalan / belum ada izin"); return@Thread
            }
            if (delayMs > 0) {
                log("Pindah ke app lain sekarang. Mulai ${delayMs / 1000} detik lagi...")
                Thread.sleep(delayMs)
            }
            try {
                val id = resources.getIdentifier("config_recentsComponentName", "string", "android")
                val comp = ComponentName.unflattenFromString(android.content.res.Resources.getSystem().getString(id))
                    ?: run { log("[X] recents component tidak ketemu"); return@Thread }
                val intent = when (variant) {
                    // A: persis seperti tes sebelumnya
                    'A' -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_DEFAULT)
                        .setComponent(comp).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // B: seperti mMyHomeIntent di OverviewComponentObserver (Launcher3 Quickstep)
                    'B' -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .setPackage(comp.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    // C: home default sistem
                    else -> Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                log("VARIAN $variant intent = $intent")
                t0 = System.currentTimeMillis()

                val ifaceCls = Class.forName(DESC_RUNNER)
                val proxy = Proxy.newProxyInstance(
                    MainActivity::class.java.classLoader, arrayOf(ifaceCls),
                    InvocationHandler { p, m, a ->
                        when (m.name) {
                            "asBinder" -> runnerBinder
                            "hashCode" -> System.identityHashCode(p)
                            "equals" -> p === a?.get(0)
                            "toString" -> "runnerProxy"
                            else -> null
                        }
                    })

                val atm = atm()
                val m = atm.javaClass.methods.first { it.name == "startRecentsActivity" }
                log("startRecentsActivity(${m.parameterTypes.joinToString { it.simpleName }}) ke $comp")
                val args = m.parameterTypes.map { t ->
                    when {
                        t == Intent::class.java -> intent
                        t.name.endsWith("IRecentsAnimationRunner") -> proxy
                        else -> null
                    }
                }.toTypedArray()
                m.invoke(atm, *args)
                log("startRecentsActivity kembali tanpa exception, menunggu callback...")
            } catch (e: Throwable) {
                log("[X] startRecentsActivity: ${unwrap(e)}")
            }
        }.start()
    }

    private fun onAnimationStart(data: Parcel) {
        log("[t+${System.currentTimeMillis() - t0}ms] onAnimationStart: parcel size=${data.dataSize()} pos=${data.dataPosition()}")
        val ctrlBinder = data.readStrongBinder()
        run {
            val pos = data.dataPosition()
            val n = data.readInt()
            data.setDataPosition(pos)
            log("array length mentah = $n (−1 = null), sisa parcel = ${data.dataAvail()} byte")
        }
        val rat = Class.forName("android.view.RemoteAnimationTarget")
        @Suppress("UNCHECKED_CAST")
        val creator = rat.getField("CREATOR").get(null) as Parcelable.Creator<Parcelable>
        val apps: Array<Parcelable> = data.createTypedArray(creator) ?: emptyArray()

        val ctrl = Class.forName("android.view.IRecentsAnimationController\$Stub")
            .getMethod("asInterface", IBinder::class.java).invoke(null, ctrlBinder)
        controller = ctrl
        finished = false

        val sb = StringBuilder("CALLBACK onAnimationStart: ${apps.size} target\n")
        if (apps.isEmpty()) sb.append("[!] 0 target: sistem tidak memberi leash. Tidak ada yang bisa dianimasikan.\n")
        for (t in apps) {
            val c = t.javaClass
            val pos = c.getField("position").get(t) as Point
            val bounds = c.getField("sourceContainerBounds").get(t) as Rect
            sb.append("  task=${c.getField("taskId").getInt(t)} mode=${c.getField("mode").getInt(t)} pos=${pos.x},${pos.y} bounds=${bounds.toShortString()}")
            runCatching { sb.append(" translucent=${c.getField("isTranslucent").getBoolean(t)}") }
            sb.append("\n")
        }
        sb.append("controller methods: " + Class.forName("android.view.IRecentsAnimationController").methods.map { it.name }.distinct().joinToString())
        log(sb.toString())

        main.postDelayed({ finishNow("watchdog 6 dtk") }, 6000)
        main.post { animate(apps) }
    }

    private fun animate(apps: Array<Parcelable>) {
        try {
            var targets = apps.filter { it.javaClass.getField("mode").getInt(it) == 1 }
            if (targets.isEmpty()) targets = apps.take(1)
            val leashes = targets.map {
                val p = it.javaClass.getField("position").get(it) as Point
                Triple(it.javaClass.getField("leash").get(it), p.x, p.y)
            }
            val txCls = Class.forName("android.view.SurfaceControl\$Transaction")
            val tx = txCls.getConstructor().newInstance()
            val mMatrix = txCls.methods.first { it.name == "setMatrix" && it.parameterCount == 5 }
            val mPos = txCls.methods.first { it.name == "setPosition" && it.parameterCount == 3 }
            val mAlpha = txCls.methods.first { it.name == "setAlpha" && it.parameterCount == 2 }
            val mApply = txCls.getMethod("apply")
            val dm = resources.displayMetrics
            val w = dm.widthPixels.toFloat(); val h = dm.heightPixels.toFloat()

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 380
                interpolator = DecelerateInterpolator()
                addUpdateListener { va ->
                    val t = va.animatedValue as Float
                    val s = 1f - 0.5f * t
                    try {
                        for ((leash, bx, by) in leashes) {
                            mMatrix.invoke(tx, leash, s, 0f, 0f, s)
                            mPos.invoke(tx, leash, bx + w * (1f - s) / 2f, by + h * (1f - s) / 2f - h * 0.12f * t)
                            mAlpha.invoke(tx, leash, 1f - 0.4f * t)
                        }
                        mApply.invoke(tx)
                    } catch (e: Throwable) { cancel(); log("[X] transaksi: ${unwrap(e)}"); finishNow("error") }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(a: Animator) { log("Animasi selesai, panggil finish()"); finishNow("animasi selesai") }
                })
                start()
            }
        } catch (e: Throwable) {
            log("[X] animate: ${unwrap(e)}")
            finishNow("error animate")
        }
    }

    private fun finishNow(why: String) {
        val c = controller ?: return
        if (finished) return
        finished = true
        Thread {
            try {
                val m = Class.forName("android.view.IRecentsAnimationController").methods.first { it.name == "finish" }
                val args = Array<Any?>(m.parameterCount) { true }
                log("[t+${System.currentTimeMillis() - t0}ms] memanggil finish(${m.parameterTypes.joinToString { it.simpleName }}) alasan: $why")
                val a = System.currentTimeMillis()
                m.invoke(c, *args)
                log("[OK] finish() kembali setelah ${System.currentTimeMillis() - a}ms")
            } catch (e: Throwable) { log("[X] finish: ${unwrap(e)}") }
            controller = null
        }.start()
    }
}
