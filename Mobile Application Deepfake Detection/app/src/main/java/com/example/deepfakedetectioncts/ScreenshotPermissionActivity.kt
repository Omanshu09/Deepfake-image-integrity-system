package com.example.deepfakedetectioncts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import com.example.deepfakedetectioncts.ui.theme.bubble.BubbleService

class ScreenshotPermissionActivity : Activity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            MediaProjectionHolder.resultCode = resultCode
            MediaProjectionHolder.data = data
            startService(Intent(this, BubbleService::class.java))
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 101
    }
}
