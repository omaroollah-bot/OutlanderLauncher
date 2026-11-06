package com.outlander.launcher

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class DoorPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val openDoors = mutableSetOf<Door>()

    private val baseBitmap: Bitmap? = loadBitmap(R.drawable.car_top_base)
    private val flBitmap: Bitmap? = loadBitmap(R.drawable.door_fl)
    private val frBitmap: Bitmap? = loadBitmap(R.drawable.door_fr)
    private val rlBitmap: Bitmap? = loadBitmap(R.drawable.door_rl)
    private val rrBitmap: Bitmap? = loadBitmap(R.drawable.door_rr)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    // Halo rouge néon sous les ouvrants ouverts
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3FF2D55")
        style = Paint.Style.FILL
        maskFilter = BlurMaskFilter(42f, BlurMaskFilter.Blur.NORMAL)
    }

    // Vignette radiale pour fondre les bords du bitume dans le fond studio #070A10
    private val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dstRect = RectF()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    private fun loadBitmap(resId: Int): Bitmap? {
        return try {
            BitmapFactory.decodeResource(resources, resId)
        } catch (e: Exception) {
            null
        }
    }

    fun setDoorState(door: Door, isOpen: Boolean) {
        val changed = if (isOpen) openDoors.add(door) else openDoors.remove(door)
        if (changed) invalidate()
    }

    fun setAllDoors(states: Map<Door, Boolean>) {
        openDoors.clear()
        states.forEach { (door, open) -> if (open) openDoors.add(door) }
        invalidate()
    }

    fun closeAll() {
        openDoors.clear()
        invalidate()
    }

    fun hasAnyOpen(): Boolean = openDoors.isNotEmpty()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            vignettePaint.shader = RadialGradient(
                w / 2f, h / 2f,
                Math.max(w, h) * 0.58f,
                intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, Color.parseColor("#070A10")),
                floatArrayOf(0f, 0.62f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val base = baseBitmap ?: return

        // Calcul du rectangle centré (Center-Inside)
        val viewRatio = width.toFloat() / height.toFloat()
        val imgRatio = base.width.toFloat() / base.height.toFloat()
        val drawW: Float
        val drawH: Float
        if (imgRatio > viewRatio) {
            drawW = width.toFloat()
            drawH = width / imgRatio
        } else {
            drawH = height.toFloat()
            drawW = height * imgRatio
        }
        val left = (width - drawW) / 2f
        val top = (height - drawH) / 2f
        dstRect.set(left, top, left + drawW, top + drawH)

        // 1. Image de base (voiture fermée vue de haut)
        canvas.drawBitmap(base, null, dstRect, paint)

        // 2. Halos lumineux rouges + Calques des ouvrants ouverts
        if (openDoors.contains(Door.FRONT_LEFT)) {
            canvas.drawOval(
                left + drawW * 0.30f, top + drawH * 0.62f,
                left + drawW * 0.55f, top + drawH * 0.96f, glowPaint
            )
            flBitmap?.let { canvas.drawBitmap(it, null, dstRect, paint) }
        }

        if (openDoors.contains(Door.FRONT_RIGHT)) {
            canvas.drawOval(
                left + drawW * 0.30f, top + drawH * 0.04f,
                left + drawW * 0.55f, top + drawH * 0.38f, glowPaint
            )
            frBitmap?.let { canvas.drawBitmap(it, null, dstRect, paint) }
        }

        if (openDoors.contains(Door.REAR_LEFT)) {
            canvas.drawOval(
                left + drawW * 0.52f, top + drawH * 0.62f,
                left + drawW * 0.76f, top + drawH * 0.96f, glowPaint
            )
            rlBitmap?.let { canvas.drawBitmap(it, null, dstRect, paint) }
        }

        if (openDoors.contains(Door.REAR_RIGHT)) {
            canvas.drawOval(
                left + drawW * 0.52f, top + drawH * 0.04f,
                left + drawW * 0.76f, top + drawH * 0.38f, glowPaint
            )
            rrBitmap?.let { canvas.drawBitmap(it, null, dstRect, paint) }
        }

        if (openDoors.contains(Door.HOOD)) {
            canvas.drawOval(
                left + drawW * 0.06f, top + drawH * 0.22f,
                left + drawW * 0.32f, top + drawH * 0.78f, glowPaint
            )
        }

        if (openDoors.contains(Door.TRUNK)) {
            canvas.drawOval(
                left + drawW * 0.75f, top + drawH * 0.20f,
                left + drawW * 0.96f, top + drawH * 0.80f, glowPaint
            )
        }

        // 3. Vignette radiale tout autour pour unifier avec le fond #070A10
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
    }
}