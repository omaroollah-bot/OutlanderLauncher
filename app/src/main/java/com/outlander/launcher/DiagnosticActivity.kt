package com.outlander.launcher

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Outil pour decouvrir comment l'autoradio signale l'ouverture des portes :
 * on ecoute des actions de broadcast (noms separes par des virgules) et on affiche
 * tout ce qui arrive, avec les extras. Les actions peuvent aussi etre mises, une par ligne,
 * dans <stockage>/Android/data/com.outlander.launcher/files/diag_actions.txt
 */
class DiagnosticActivity : Activity() {
    private val receivers = ArrayList<BroadcastReceiver>()
    private lateinit var logView: TextView
    private lateinit var input: EditText
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#101418"))
        root.setPadding(32, 32, 32, 32)

        input = EditText(this)
        input.hint = "action1, action2, ..."
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.GRAY)
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val listen = Button(this); listen.text = "Écouter"
        val clear = Button(this); clear.text = "Effacer"
        row.addView(listen); row.addView(clear)
        root.addView(row)

        logView = TextView(this)
        logView.setTextColor(Color.parseColor("#9CFF9C"))
        logView.textSize = 14f
        logView.text = "Ouvrez/fermez une porte : les messages reçus s'affichent ici.\n"
        val scroll = ScrollView(this)
        scroll.addView(logView)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)

        listen.setOnClickListener {
            val actions = input.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            startListening(actions)
        }
        clear.setOnClickListener { logView.text = "" }

        val f = File(getExternalFilesDir(null), "diag_actions.txt")
        if (f.exists()) startListening(f.readLines().map { it.trim() }.filter { it.isNotEmpty() })
    }

    private fun startListening(actions: List<String>) {
        for (a in actions) {
            val r = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    val sb = StringBuilder("${fmt.format(Date())}  ${intent.action}\n")
                    val ex = intent.extras
                    if (ex != null) {
                        @Suppress("DEPRECATION")
                        for (k in ex.keySet()) {
                            val v = ex.get(k)
                            sb.append("    $k = $v (${v?.javaClass?.simpleName})\n")
                        }
                    }
                    Log.i("CarDiag", sb.toString())
                    runOnUiThread { logView.append(sb.toString()) }
                }
            }
            val flt = IntentFilter(a)
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(r, flt, Context.RECEIVER_EXPORTED)
            else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(r, flt)
            }
            receivers.add(r)
            logView.append("-> écoute: $a\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        for (r in receivers) {
            try { unregisterReceiver(r) } catch (e: Exception) { }
        }
    }
}
