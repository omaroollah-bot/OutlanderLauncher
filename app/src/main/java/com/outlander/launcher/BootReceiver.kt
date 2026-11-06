package com.outlander.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Tentative de demarrage au boot (best effort : Android peut la bloquer). Le vrai mecanisme = launcher par defaut. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val i = Intent(context, MainActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        } catch (e: Exception) {
            // bloque par le systeme : sans importance
        }
    }
}
