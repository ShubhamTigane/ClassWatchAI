package com.example.classwatchai.ui.adapters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
class OverlayView  @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : View(ctx, attrs) {

    private val pts = mutableListOf<Pair<Float, Float>>()        // green dots
    private val labs = mutableListOf<String>()                 // matching labels
    private val handPath = Path()                                // red poly‑line
    private var faceBox: RectF? = null

    /* ■ 2. add a Text‑paint */
    private val textPaint = Paint().apply {
        color       = 0xFFFFFFFF.toInt()
        textSize    = 26f
        isAntiAlias = true
       // style = Paint.Style.FILL
    }
    private val textBgPaint = Paint().apply {
        color = 0xFF000000.toInt() // black shadow
        textSize = 26f
        isAntiAlias = true
        style = Paint.Style.FILL
    }


    /* ■ 3. extend the API to accept labels */
    fun showLandmarks(
        points : List<Pair<Float, Float>>,
        labels : List<String>               = emptyList(),
        handPoints : List<Pair<Float, Float>> = emptyList(),
        boundingBox: RectF? = null
    ) {
        pts .apply { clear(); addAll(points ) }
        labs.apply { clear(); addAll(labels) }
        faceBox = boundingBox

        handPath.reset()
        if (handPoints.isNotEmpty()) {
            handPath.moveTo(handPoints.first().first, handPoints.first().second)
            for (p in handPoints.drop(1)) handPath.lineTo(p.first, p.second)
        }
        postInvalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        // dots + labels
        pts.forEachIndexed { idx, (x, y) ->
            c.drawCircle(x, y, 7f, dotPaint)
            labs.getOrNull(idx)?.let { lbl ->
               // c.drawText(lbl, x + 8f, y - 8f, textPaint)     // little offset
                c.drawText(lbl, x + 9f, y - 7f, textBgPaint) // draw background first
                c.drawText(lbl, x + 8f, y - 8f, textPaint)   // draw actual text
            }
        }
        faceBox?.let {
            c.drawRect(it, boxPaint)
        }

        // hand poly‑line (future use)
        c.drawPath(handPath, linePaint)
    }
    private val dotPaint  = Paint().apply {
        style = Paint.Style.FILL
        strokeWidth = 8f
        isAntiAlias = true
        color = 0xFF00FF00.toInt()   // green
    }
    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        color = 0xFFFF0000.toInt()   // red
    }
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        color = 0xFF00FFFF.toInt()   // cyan box
    }

}



//    /** Called from Detector thread (post to main‑thread!) */
//    fun showLandmarks(facePoints: List<Pair<Float, Float>>, handPoints: List<Pair<Float, Float>>) {
//        pts.clear()
//        pts.addAll(facePoints)
//
//        handPath.reset()
//        if (handPoints.isNotEmpty()) {
//            handPath.moveTo(handPoints.first().first, handPoints.first().second)
//            for (p in handPoints.drop(1)) handPath.lineTo(p.first, p.second)
//        }
//        postInvalidate()           // refresh on UI thread
//    }
//
//    override fun onDraw(c: Canvas) {
//        super.onDraw(c)
//        pts.forEach { (x, y) -> c.drawCircle(x, y, 7f, dotPaint) }
//        c.drawPath(handPath, linePaint)
//    }
//}