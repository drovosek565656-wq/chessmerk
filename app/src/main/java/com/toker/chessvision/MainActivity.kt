package com.toker.chessvision

import android.app.Activity
import android.content.Intent
import android.content.Context
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var projectionManager: MediaProjectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_OVERLAY)
        } else {
            requestCapture()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQ_OVERLAY -> requestCapture()
            REQ_CAPTURE -> if (resultCode == Activity.RESULT_OK && data != null) {
                val svc = Intent(this, CaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startForegroundService(svc)
                startService(Intent(this, OverlayService::class.java))
                finish()
            }
        }
    }

    private fun requestCapture() {
        startActivityForResult(projectionManager.createScreenCaptureIntent(), REQ_CAPTURE)
    }

    companion object {
        const val REQ_OVERLAY = 1001
        const val REQ_CAPTURE = 1002
    }
}
