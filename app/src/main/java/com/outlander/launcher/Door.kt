package com.outlander.launcher

import android.os.Handler
import android.os.Looper
import java.util.EnumSet
import java.util.concurrent.CopyOnWriteArrayList

/** Les ouvrants possibles. "key" sert aussi au nom des images : door_<key>.png */
enum class Door(val key: String, val label: String) {
    FL("fl", "Porte avant gauche"),
    FR("fr", "Porte avant droite"),
    RL("rl", "Porte arrière gauche"),
    RR("rr", "Porte arrière droite"),
    HOOD("hood", "Capot"),
    TAILGATE("tailgate", "Coffre");

    companion object {
        fun fromKey(k: String): Door? = values().firstOrNull { it.key.equals(k, ignoreCase = true) }
    }
}

/** Etat central des ouvrants. Toute source (CANbus, simulation) appelle set(). */
object DoorStateHub {
    interface Listener {
        fun onDoorsChanged(open: Set<Door>)
    }

    private val main = Handler(Looper.getMainLooper())
    private val state: MutableSet<Door> = EnumSet.noneOf(Door::class.java)
    private val listeners = CopyOnWriteArrayList<Listener>()

    val open: Set<Door> get() = HashSet(state)

    fun addListener(l: Listener) {
        listeners.add(l)
        l.onDoorsChanged(open)
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    fun set(door: Door, isOpen: Boolean) {
        main.post {
            val changed = if (isOpen) state.add(door) else state.remove(door)
            if (changed) {
                val snapshot = open
                listeners.forEach { it.onDoorsChanged(snapshot) }
            }
        }
    }
}
