package com.outlander.launcher

import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var carTextureView: TextureView
    private lateinit var doorPanelView: DoorPanelView
    private lateinit var tvCanbusBanner: TextView
    private lateinit var tvScrubHint: TextView
    private lateinit var tvClock: TextView

    private lateinit var pillButtons: Map<Door, Button>
    private val doorStates = mutableMapOf<Door, Boolean>().apply {
        Door.values().forEach { put(it, false) }
    }

    private var mediaPlayer: MediaPlayer? = null
    private var lastTouchX = 0f
    private var doorSource: BroadcastDoorSource? = null

    private val clockHandler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            tvClock.text = sdf.format(Date())
            clockHandler.postDelayed(this, 10_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        hideSystemBars()

        carTextureView = findViewById(R.id.carTextureView)
        doorPanelView = findViewById(R.id.doorPanelView)
        tvCanbusBanner = findViewById(R.id.tvCanbusBanner)
        tvScrubHint = findViewById(R.id.tvScrubHint)
        tvClock = findViewById(R.id.tvClock)

        pillButtons = mapOf(
            Door.FRONT_LEFT to findViewById(R.id.pillFl),
            Door.FRONT_RIGHT to findViewById(R.id.pillFr),
            Door.REAR_LEFT to findViewById(R.id.pillRl),
            Door.REAR_RIGHT to findViewById(R.id.pillRr),
            Door.HOOD to findViewById(R.id.pillHood),
            Door.TRUNK to findViewById(R.id.pillTrunk)
        )

        // Boutons de test / contrôle rapide des 6 ouvrants
        pillButtons.forEach { (door, btn) ->
            btn.setOnClickListener {
                val newState = !(doorStates[door] ?: false)
                doorStates[door] = newState
                updateCockpitUI()
            }
        }

        findViewById<View>(R.id.btnCloseAll).setOnClickListener {
            Door.values().forEach { doorStates[it] = false }
            updateCockpitUI()
        }

        // Configuration du Video Scrubbing 360° au doigt
        carTextureView.surfaceTextureListener = this
        findViewById<View>(R.id.stageCard).setOnTouchListener { _, event ->
            if (doorStates.values.any { it }) return@setOnTouchListener false
            val mp = mediaPlayer ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.x
                    if (mp.isPlaying) mp.pause()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - lastTouchX
                    lastTouchX = event.x
                    val duration = mp.duration.coerceAtLeast(1)
                    val deltaMs = (dx * 12f).toInt()
                    var targetMs = (mp.currentPosition + deltaMs) % duration
                    if (targetMs < 0) targetMs += duration
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        mp.seekTo(targetMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                    } else {
                        mp.seekTo(targetMs)
                    }
                    true
                }
                else -> false
            }
        }

        // Raccourcis Dock CarPlay Gauche (ZhangXun B9320B / Zlink / Maps / Radio)
        findViewById<View>(R.id.btnNavCarplay).setOnClickListener {
            launchFirstAvailable(
                "com.zhangxun.cp",
                "com.zjinnova.zlink",
                "net.easyconn"
            )
        }
        findViewById<View>(R.id.btnNavMaps).setOnClickListener {
            launchFirstAvailable("com.google.android.apps.maps")
        }
        findViewById<View>(R.id.btnNavRadio).setOnClickListener {
            launchFirstAvailable("com.ts.MainUI", "com.microntek.radio")
        }
        findViewById<View>(R.id.btnNavPhone).setOnClickListener {
            launchFirstAvailable("com.android.dialer", "com.ts.bt")
        }
        findViewById<View>(R.id.btnOpenDiag).setOnClickListener {
            startActivity(Intent(this, DiagnosticActivity::class.java))
        }

        // Écouteur CANbus temps réel (BroadcastDoorSource créé par Claude)
        doorSource = BroadcastDoorSource(this) { updatedDoor, isOpen ->
            runOnUiThread {
                doorStates[updatedDoor] = isOpen
                updateCockpitUI()
            }
        }
    }

    private fun updateCockpitUI() {
        doorPanelView.setAllDoors(doorStates)
        val openCount = doorStates.values.count { it }

        // Mise à jour des 6 tuiles à droite
        val labels = mapOf(
            Door.FRONT_LEFT to "AV. G",
            Door.FRONT_RIGHT to "AV. D",
            Door.REAR_LEFT to "AR. G",
            Door.REAR_RIGHT to "AR. D",
            Door.HOOD to "Capot",
            Door.TRUNK to "Coffre"
        )

        pillButtons.forEach { (door, btn) ->
            val isOpen = doorStates[door] == true
            val title = labels[door] ?: door.name
            btn.text = "$title\n${if (isOpen) "OUVERT" else "Fermé"}"
            btn.setBackgroundColor(
                if (isOpen) Color.parseColor("#4DFF2D55") else Color.parseColor("#14FFFFFF")
            )
            btn.setTextColor(
                if (isOpen) Color.parseColor("#FF6482") else Color.WHITE
            )
        }

        // Transition fluide entre Vidéo 360° (fermé) et Vue de haut (ouvert)
        if (openCount == 0) {
            doorPanelView.animate().alpha(0f).setDuration(220).start()
            carTextureView.animate().alpha(1f).setDuration(220).start()
            tvCanbusBanner.text = "● OUTLANDER SPORT SE • FERMÉ"
            tvCanbusBanner.setTextColor(Color.parseColor("#30D158"))
            tvScrubHint.text = "👆 Glisser pour tourner à 360°"
        } else {
            doorPanelView.animate().alpha(1f).setDuration(220).start()
            carTextureView.animate().alpha(0.15f).setDuration(220).start()
            tvCanbusBanner.text = "⚠️ ALERTE : $openCount OUVRANT(S) OUVERT(S)"
            tvCanbusBanner.setTextColor(Color.parseColor("#FF2D55"))
            tvScrubHint.text = "👁️ Vue Zénithale CANbus Active (RP5-MT-002)"
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        try {
            val videoUri = Uri.parse("android.resource://$packageName/${R.raw.car_360}")
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, videoUri)
                setSurface(Surface(surface))
                isLooping = true
                setVolume(0f, 0f)
                prepareAsync()
                setOnPreparedListener { mp ->
                    mp.start()
                    mp.pause()
                    mp.seekTo(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        mediaPlayer?.release()
        mediaPlayer = null
        return true
    }
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun launchFirstAvailable(vararg packages: String) {
        for (pkg in packages) {
            val intent = packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                startActivity(intent)
                return
            }
        }
    }

    private fun hideSystemBars() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        clockHandler.post(clockRunnable)
        doorSource?.start()
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
        doorSource?.stop()
    }
}