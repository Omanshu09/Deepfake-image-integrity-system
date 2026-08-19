package com.example.deepfakedetectioncts.ui.theme.bubble

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.ImageReader
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.example.deepfakedetectioncts.MediaProjectionHolder
import com.example.deepfakedetectioncts.R
import com.example.deepfakedetectioncts.core.DeepfakeModelEngine
import com.example.deepfakedetectioncts.core.ModelResultOverlay
import com.example.deepfakedetectioncts.ui.theme.selection.SelectionView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

enum class BubbleState { PEEKING, EXPANDED }

class BubbleService : Service() {

    companion object { var isRunning = false }

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleView: ImageView
    private lateinit var params: WindowManager.LayoutParams
    private var bubbleState = BubbleState.EXPANDED
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val modelEngine = DeepfakeModelEngine()
    private lateinit var resultOverlay: ModelResultOverlay

    private val fullSize = 150
    private val handleWidth = 40
    private val handleHeight = 180
    private val peekAmount = 30

    override fun onCreate() {
        super.onCreate()
        if (isRunning) { stopSelf(); return }
        isRunning = true
        startAsForegroundService()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        resultOverlay = ModelResultOverlay(this)
        showBubble()
    }

    override fun onDestroy() {
        if (::bubbleView.isInitialized) windowManager.removeView(bubbleView)
        isRunning = false
        super.onDestroy()
    }

    private fun startAsForegroundService() {
        val id = "bubble_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(id, "Bubble Service", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
        val n = NotificationCompat.Builder(this, id)
            .setContentTitle("Deepfake Detection Active")
            .setContentText("Tap the bubble to select area")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
        startForeground(1, n)
    }

    @Suppress("DEPRECATION")
    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        bubbleView = ImageView(this)
        bubbleView.setImageResource(R.drawable.app_logo)

        params = WindowManager.LayoutParams(
            fullSize, fullSize, overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH, PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 400
        }

        bubbleView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_OUTSIDE -> {
                        if (bubbleState == BubbleState.EXPANDED) {
                            snapToEdgeAndPeek()
                        }
                        return true
                    }
                    MotionEvent.ACTION_DOWN -> {
                        isDragging = false
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            if (bubbleState == BubbleState.PEEKING) {
                                expandBubble()
                            } else {
                                showSelectionOverlay()
                            }
                        } else {
                            snapToEdgeAndPeek()
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDragging = true
                            if (bubbleState == BubbleState.PEEKING) expandBubble(false)
                        }
                        if (isDragging) {
                            params.x = initialX + dx.toInt()
                            params.y = initialY + dy.toInt()
                            windowManager.updateViewLayout(bubbleView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(bubbleView, params)
        snapToEdgeAndPeek()
    }

    private fun snapToEdgeAndPeek() {
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (params.x + params.width / 2 < screenWidth / 2) -fullSize + peekAmount else screenWidth - peekAmount

        val animator = ValueAnimator.ofInt(params.x, targetX)
        animator.addUpdateListener { animation ->
            params.x = animation.animatedValue as Int
            windowManager.updateViewLayout(bubbleView, params)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                bubbleView.setImageResource(R.drawable.handle_background)
                params.width = handleWidth
                params.height = handleHeight
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(bubbleView, params)
                bubbleState = BubbleState.PEEKING
            }
        })
        animator.duration = 200
        animator.start()
    }

    private fun expandBubble(animate: Boolean = true) {
        val screenWidth = resources.displayMetrics.widthPixels
        val isOnLeft = params.x < screenWidth / 2
        val targetX = if (isOnLeft) 0 else screenWidth - fullSize

        bubbleView.setImageResource(R.drawable.app_logo)
        params.width = fullSize
        params.height = fullSize
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        windowManager.updateViewLayout(bubbleView, params)

        if (animate) {
            val animator = ValueAnimator.ofInt(params.x, targetX)
            animator.addUpdateListener { animation ->
                params.x = animation.animatedValue as Int
                windowManager.updateViewLayout(bubbleView, params)
            }
            animator.duration = 200
            animator.start()
        } else {
            params.x = targetX
            windowManager.updateViewLayout(bubbleView, params)
        }
        bubbleState = BubbleState.EXPANDED
    }

    private fun showSelectionOverlay() {
        bubbleView.visibility = View.GONE
        val selectionView = SelectionView(this)

        selectionView.onSelectionCancelled = {
            windowManager.removeView(selectionView)
            bubbleView.visibility = View.VISIBLE
            snapToEdgeAndPeek()
        }
        selectionView.onSelectionComplete = { rect ->
            windowManager.removeView(selectionView)
            Handler(Looper.getMainLooper()).postDelayed({
                val full = captureScreen() ?: return@postDelayed
                val bitmapToProcess = if (rect.width() <= 0 || rect.height() <= 0) {
                    full
                } else {
                    val safe = Rect(rect.left.coerceAtLeast(0), rect.top.coerceAtLeast(0), rect.right.coerceAtMost(full.width), rect.bottom.coerceAtMost(full.height))
                    Bitmap.createBitmap(full, safe.left, safe.top, safe.width(), safe.height())
                }
                
                processSelection(bitmapToProcess)
                
                bubbleView.visibility = View.VISIBLE
                snapToEdgeAndPeek()
            }, 100)
        }
        windowManager.addView(selectionView, WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ))
    }

    private fun processSelection(bitmap: Bitmap) {
        val sharedPrefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val shouldExplain = sharedPrefs.getBoolean("explanation_enabled", false)
        
        resultOverlay.show("Analyzing content...")
        serviceScope.launch {
            val analysisResult = withContext(Dispatchers.IO) {
                modelEngine.analyze(bitmap, shouldExplain)
            }
            resultOverlay.show(analysisResult.resultText)
            
            // Save for history with the analysis result and explanation
            withContext(Dispatchers.IO) {
                saveBitmapAndResult(bitmap, analysisResult.resultText, analysisResult.explanation)
            }
        }
    }

    private fun captureScreen(): Bitmap? {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val data = MediaProjectionHolder.data ?: return null
        val projection = mgr.getMediaProjection(MediaProjectionHolder.resultCode, data) ?: return null
        val metrics = resources.displayMetrics
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        val reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        projection.createVirtualDisplay("screen", w, h, metrics.densityDpi, 0, reader.surface, null, null)
        Thread.sleep(100)
        val image = reader.acquireLatestImage() ?: return null
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * w
        val bitmap = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
        buffer.rewind()
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        reader.close()
        projection.stop()
        return Bitmap.createBitmap(bitmap, 0, 0, w, h)
    }

    private fun saveBitmapAndResult(bitmap: Bitmap, result: String, explanation: String?) {
        try {
            val dir = getDir("screenshots", Context.MODE_PRIVATE)
            if (!dir.exists()) {
                dir.mkdir()
            }
            val baseName = "processed_${System.currentTimeMillis()}"
            val imageFile = File(dir, "$baseName.png")
            FileOutputStream(imageFile).use { fOut ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)
            }
            
            // Save the result text
            val resultFile = File(dir, "$baseName.txt")
            resultFile.writeText(result)
            
            // Save explanation if available
            if (explanation != null) {
                val explanationFile = File(dir, "$baseName.explanation")
                explanationFile.writeText(explanation)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
