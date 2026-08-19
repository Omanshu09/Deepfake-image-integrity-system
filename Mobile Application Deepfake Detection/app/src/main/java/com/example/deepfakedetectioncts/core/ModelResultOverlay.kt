package com.example.deepfakedetectioncts.core

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.deepfakedetectioncts.R

class ModelResultOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var resultView: View? = null

    @SuppressLint("InflateParams")
    fun show(message: String) {
        if (resultView != null) dismiss()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 100
        }

        // Determine background color based on message content
        val bgColor = when {
            message.contains("Deepfake", ignoreCase = true) -> Color.parseColor("#E53935") // Red
            message.contains("Synthetic", ignoreCase = true) || message.contains("AI Generated", ignoreCase = true) -> Color.parseColor("#2196F3") // Blue
            message.contains("Authentic", ignoreCase = true) -> Color.parseColor("#3DDC84") // Green
            else -> Color.parseColor("#CC000000") // Default Dark
        }

        val view = TextView(context).apply {
            text = message
            setTextColor(if (bgColor == Color.parseColor("#3DDC84")) Color.BLACK else Color.WHITE)
            setBackgroundColor(bgColor)
            setPadding(40, 60, 40, 60)
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            elevation = 10f
        }

        resultView = view
        windowManager.addView(view, params)

        // Auto-dismiss after 5 seconds
        view.postDelayed({ dismiss() }, 5000)
    }

    fun dismiss() {
        resultView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
            resultView = null
        }
    }
}
