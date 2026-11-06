package com.outlander.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Lit l'etat des ouvrants envoye par l'autoradio/CANbus sous forme de broadcasts Android.
 * Les actions et les noms d'extras se reglent dans door_config.json (sans recompiler) :
 *   - fichier editable : <stockage>/Android/data/com.outlander.launcher/files/door_config.json
 *   - sinon le fichier par defaut dans assets/
 */
class BroadcastDoorSource(private val ctx: Context) {
    private val receivers = ArrayList<BroadcastReceiver>()

    fun start() {
        try {
            val cfg = loadConfig()
            val arr = cfg.optJSONArray("mappings") ?: return
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                if (!m.optBoolean("enabled", true)) continue
                val action = m.optString("action")
                if (action.isEmpty()) continue
                val r = object : BroadcastReceiver() {
                    override fun onReceive(c: Context, intent: Intent) {
                        try {
                            apply(m, intent)
                        } catch (e: Exception) {
                            Log.w(TAG, "mapping error", e)
                        }
                    }
                }
                val f = IntentFilter(action)
                if (Build.VERSION.SDK_INT >= 33) {
                    ctx.registerReceiver(r, f, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    ctx.registerReceiver(r, f)
                }
                receivers.add(r)
                Log.i(TAG, "listening for $action")
            }
        } catch (e: Exception) {
            Log.w(TAG, "config error", e)
        }
    }

    fun stop() {
        for (r in receivers) {
            try {
                ctx.unregisterReceiver(r)
            } catch (e: Exception) {
                // deja retire
            }
        }
        receivers.clear()
    }

    private fun loadConfig(): JSONObject {
        val ext = File(ctx.getExternalFilesDir(null), "door_config.json")
        val text = if (ext.exists()) ext.readText()
        else ctx.assets.open("door_config.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    @Suppress("DEPRECATION")
    private fun apply(m: JSONObject, intent: Intent) {
        val extras = intent.extras ?: return
        when (m.optString("mode", "booleans")) {
            "booleans" -> {
                val map = m.getJSONObject("extras")
                val keys = map.keys()
                while (keys.hasNext()) {
                    val extraName = keys.next()
                    val door = Door.fromKey(map.getString(extraName)) ?: continue
                    if (extras.containsKey(extraName)) {
                        DoorStateHub.set(door, truthy(extras.get(extraName)))
                    }
                }
            }
            "bitmask" -> {
                val v = (extras.get(m.getString("extra")) as? Number)?.toLong() ?: return
                val bits = m.getJSONObject("bits")
                val keys = bits.keys()
                while (keys.hasNext()) {
                    val bitIndex = keys.next()
                    val door = Door.fromKey(bits.getString(bitIndex)) ?: continue
                    val isSet = ((v shr bitIndex.toInt()) and 1L) == 1L
                    DoorStateHub.set(door, isSet)
                }
            }
        }
    }

    private fun truthy(v: Any?): Boolean = when (v) {
        is Boolean -> v
        is Number -> v.toInt() != 0
        is String -> v == "1" || v.equals("true", true) || v.equals("open", true) || v.equals("on", true)
        else -> false
    }

    companion object {
        private const val TAG = "DoorSource"
    }
}
