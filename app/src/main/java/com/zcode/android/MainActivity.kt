package com.zcode.android

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val btn = Button(ctx).apply { text = "运行 ZCode (zcode-linux-arm64)" }
        logView = TextView(ctx).apply { textIsSelectable = true }
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(btn)
            addView(ScrollView(ctx).apply { addView(logView) })
        }
        setContentView(layout)
        btn.setOnClickListener { runZCode() }
        log("ZCode Android harness. assets 里应含 zcode-linux-arm64")
    }

    private fun log(s: String) {
        handler.post { logView.append(s + "\n") }
    }

    private fun runZCode() {
        Thread {
            try {
                val assetName = "zcode-linux-arm64"
                val bin = File(filesDir, "zcode")
                if (!bin.exists()) {
                    val names = assets.list("")?.toList() ?: emptyList()
                    log("assets 内容: $names")
                    assets.open(assetName).use { inp ->
                        FileOutputStream(bin).use { out -> inp.copyTo(out) }
                    }
                }
                bin.setExecutable(true, false)
                bin.setReadable(true, false)
                log("二进制: ${bin.absolutePath} size=${bin.length()} executable=${bin.canExecute()}")
                val p = ProcessBuilder(bin.absolutePath, "--version")
                    .redirectErrorStream(true)
                    .directory(filesDir)
                    .start()
                val out = p.inputStream.bufferedReader().readText()
                val code = p.waitFor()
                log("exitCode=$code")
                log("---- output ----")
                log(out.ifEmpty { "(empty)" })
            } catch (e: Exception) {
                log("EXCEPTION ${e.javaClass.name}: ${e.message}")
                e.cause?.let { log("  cause: ${it.javaClass.name}: ${it.message}") }
            }
        }.start()
    }
}
