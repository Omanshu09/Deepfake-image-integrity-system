package com.example.deepfakedetectioncts.ui.theme.bubble

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.example.deepfakedetectioncts.R
import kotlin.math.abs

// A stable BubbleView implementation that relies on a standard Android View and OnTouchListener.
// This avoids the complexities and potential crashes of mixing Compose gesture detectors with WindowManager.
@SuppressLint("ViewConstructor")
class BubbleView(context: Context) : View(context) {

    var onBubbleClick: () -> Unit = {}
    var onMove: (Float, Float) -> Unit = { _, _ -> }
    var onRelease: (Float) -> Unit = {}

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    init {
        // Set a simple circular background drawable
        setBackgroundResource(R.drawable.bubble_background)
        // Set up the touch listener
        setOnTouchListener(getTouchListener())
    }

    private fun getTouchListener() = OnTouchListener { _, event ->
        val layoutParams = layoutParams as WindowManager.LayoutParams
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = false
                initialX = layoutParams.x
                initialY = layoutParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                true
            }
            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                }
                onRelease(event.rawX)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                // Only start dragging if the user has moved more than a few pixels
                if (abs(dx) > 10 || abs(dy) > 10) {
                    isDragging = true
                    onMove(dx, dy)
                }
                true
            }
            else -> false
        }
    }

    override fun performClick(): Boolean {
        onBubbleClick()
        return super.performClick()
    }
}
