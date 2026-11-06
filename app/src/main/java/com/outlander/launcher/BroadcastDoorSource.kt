package com.outlander.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

class BroadcastDoorSource(
    private val ctx: Context,
    private var listener: ((Door, Boolean) -> Unit)? = null
) {
    private var isRegistered = false

    fun setListener(cb: (Door, Boolean) -> Unit) {
        listener = cb
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val extras = intent.extras ?: return

            // Décodage standard CANbus RP5-MT-002 / Baogood / XYAUTO / TS
            for (key in extras.keySet()) {
                val k = key.lowercase()
                val v = extras.get(key)
                val isOpen = when (v) {
                    is Boolean -> v
                    is Int -> v != 0
                    is Byte -> v.toInt() != 0
                    is String -> v == "1" || v.equals("true", ignoreCase = true) || v.equals("open", ignoreCase = true)
                    else -> false
                }

                when {
                    k.contains("fl") || k.contains("front_left") || k.contains("left_front") -> listener?.invoke(Door.FRONT_LEFT, isOpen)
                    k.contains("fr") || k.contains("front_right") || k.contains("right_front") -> listener?.invoke(Door.FRONT_RIGHT, isOpen)
                    k.contains("rl") || k.contains("rear_left") || k.contains("left_rear") -> listener?.invoke(Door.REAR_LEFT, isOpen)
                    k.contains("rr") || k.contains("rear_right") || k.contains("right_rear") -> listener?.invoke(Door.REAR_RIGHT, isOpen)
                    k.contains("hood") || k.contains("bonnet") -> listener?.invoke(Door.HOOD, isOpen)
                    k.contains("trunk") || k.contains("boot") || k.contains("tailgate") || k.contains("rear") -> listener?.invoke(Door.TRUNK, isOpen)
                }
            }
        }
    }

    fun start() {
        if (isRegistered) return
        val filter = IntentFilter().apply {
            addAction("com.outlander.launcher.DOOR_STATE")
            addAction("com.ts.can.DOOR_INFO")
            addAction("com.xyauto.broadcast.CANBUS_DOOR")
            addAction("com.microntek.canbus.display")
            addAction("autochips.intent.action.CANBUS_DOOR")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            ctx.registerReceiver(receiver, filter)
        }
        isRegistered = true
    }

    fun stop() {
        if (!isRegistered) return
        try {
            ctx.unregisterReceiver(receiver)
        } catch (_: Exception) {
        }
        isRegistered = false
    }
}
