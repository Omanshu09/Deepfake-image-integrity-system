package com.example.deepfakedetectioncts.ui.theme.selection

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import kotlin.math.max
import kotlin.math.min

class SelectionView(context: Context) : FrameLayout(context) {

    var onSelectionComplete: ((Rect) -> Unit)? = null
    var onSelectionCancelled: (() -> Unit)? = null
    private var isProcessing = false

    init {
        isFocusableInTouchMode = true
        requestFocus()

        val selectionDrawingView = SelectionDrawingView(context)
        addView(selectionDrawingView)

        val cancelButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            setOnClickListener { 
                if (!isProcessing) onSelectionCancelled?.invoke() 
            }
        }
        val params = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = android.view.Gravity.TOP or android.view.Gravity.END
        params.topMargin = 60
        params.rightMargin = 60
        addView(cancelButton, params)

        selectionDrawingView.onSelectionComplete = { 
            if (!isProcessing) {
                isProcessing = true
                onSelectionComplete?.invoke(it) 
            }
        }
        selectionDrawingView.onSelectionCancelled = { 
            if (!isProcessing) onSelectionCancelled?.invoke() 
        }

        setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (!isProcessing) onSelectionCancelled?.invoke()
                true
            } else {
                false
            }
        }
    }

    private class SelectionDrawingView(context: Context) : View(context) {

        var onSelectionComplete: ((Rect) -> Unit)? = null
        var onSelectionCancelled: (() -> Unit)? = null

        private val path = Path()
        private val permanentStrokePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 16f
            isAntiAlias = true
        }

        private val glowPaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 60f
            maskFilter = android.graphics.BlurMaskFilter(50f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }

        private var startX = 0f
        private var startY = 0f

        private var minX = 0f
        private var minY = 0f
        private var maxX = 0f
        private var maxY = 0f

        private var hasDrawn = false
        private var isInputLocked = false
        private val glowingPoints = mutableListOf<Pair<PointF, Long>>()
        private val handler = Handler(Looper.getMainLooper())

        private val cornerArcPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 20f
            isAntiAlias = true
        }

        private var selectionRect: RectF? = null
        private var showCorners = false

        init {
            val flashPaint = Paint().apply { color = Color.WHITE; alpha = 150 }
            ValueAnimator.ofInt(150, 0).apply {
                duration = 300
                addUpdateListener { animation ->
                    flashPaint.alpha = animation.animatedValue as Int
                    invalidate()
                }
                start()
            }

            handler.post(object : Runnable {
                override fun run() {
                    val currentTime = System.currentTimeMillis()
                    glowingPoints.removeAll { currentTime - it.second > 200 }
                    invalidate()
                    handler.postDelayed(this, 16)
                }
            })
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            glowPaint.shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(Color.MAGENTA, Color.BLUE, Color.YELLOW),
                null,
                Shader.TileMode.CLAMP
            )
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (isInputLocked) return true
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    hasDrawn = false
                    path.reset()
                    startX = x
                    startY = y
                    minX = x
                    minY = y
                    maxX = x
                    maxY = y
                    path.moveTo(x, y)
                    glowingPoints.add(PointF(x, y) to System.currentTimeMillis())
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    hasDrawn = true
                    path.lineTo(x, y)
                    minX = min(minX, x)
                    minY = min(minY, y)
                    maxX = max(maxX, x)
                    maxY = max(maxY, y)
                    glowingPoints.add(PointF(x, y) to System.currentTimeMillis())
                }
                MotionEvent.ACTION_UP -> {
                    if (!hasDrawn) {
                        onSelectionCancelled?.invoke()
                        return true
                    }
                    isInputLocked = true
                    selectionRect = RectF(minX, minY, maxX, maxY)
                    showCorners = true
                    invalidate()

                    handler.postDelayed({
                        onSelectionComplete?.invoke(Rect(minX.toInt(), minY.toInt(), maxX.toInt(), maxY.toInt()))
                    }, 600)
                }
            }
            invalidate()
            return true
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.drawColor(Color.parseColor("#99000000"))

            for (point in glowingPoints) {
                canvas.drawPoint(point.first.x, point.first.y, glowPaint)
            }

            canvas.drawPath(path, permanentStrokePaint)

            if (showCorners) {
                selectionRect?.let {
                    val arcSize = 160f
                    canvas.drawArc(it.left, it.top, it.left + arcSize, it.top + arcSize, 180f, 90f, false, cornerArcPaint)
                    canvas.drawArc(it.right - arcSize, it.top, it.right, it.top + arcSize, 270f, 90f, false, cornerArcPaint)
                    canvas.drawArc(it.left, it.bottom - arcSize, it.left + arcSize, it.bottom, 90f, 90f, false, cornerArcPaint)
                    canvas.drawArc(it.right - arcSize, it.bottom - arcSize, it.right, it.bottom, 0f, 90f, false, cornerArcPaint)
                }
            }
        }
    }
}
