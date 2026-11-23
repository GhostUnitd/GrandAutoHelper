package com.grandauto.helper

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlin.math.*

class CaptureService : Service() {
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null
    private var display: VirtualDisplay? = null
    private var lastDirection = ""
    private var lastTapAt = 0L
    private var busy = false

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        val code = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mgr.getMediaProjection(code, data)
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val w = metrics.widthPixels
        val h = metrics.heightPixels
        reader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        display = projection?.createVirtualDisplay(
            "GrandAutoHelper", w, h, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, null
        )
        reader?.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                if (!busy) {
                    busy = true
                    val bmp = imageToBitmap(image)
                    if (bmp != null) processFrame(bmp)
                    busy = false
                }
            } finally { image.close() }
        }, Handler(Looper.getMainLooper()))
        return START_STICKY
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val padded = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        padded.copyPixelsFromBuffer(buffer)
        val cropped = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
        padded.recycle()
        return cropped
    }

    // Detects the white arrow glyph in the black prompt tile near the upper-middle area.
    // Template-free shape classifier: counts bright pixels in four directional sectors.
    private fun processFrame(b: Bitmap) {
        val w = b.width.toFloat()
        val h = b.height.toFloat()
        val left = (w * 0.465f).toInt().coerceAtLeast(0)
        val top = (h * 0.155f).toInt().coerceAtLeast(0)
        val right = (w * 0.535f).toInt().coerceAtMost(b.width)
        val bottom = (h * 0.26f).toInt().coerceAtMost(b.height)
        if (right <= left || bottom <= top) { b.recycle(); return }
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f
        var l = 0; var r = 0; var u = 0; var d = 0
        val x0 = (cx - (right-left)*0.32f).toInt().coerceAtLeast(0)
        val x1 = (cx + (right-left)*0.32f).toInt().coerceAtMost(b.width-1)
        val y0 = (cy - (bottom-top)*0.32f).toInt().coerceAtLeast(0)
        val y1 = (cy + (bottom-top)*0.32f).toInt().coerceAtMost(b.height-1)
        for (y in y0..y1 step 2) for (x in x0..x1 step 2) {
            val c = b.getPixel(x, y)
            val red = android.graphics.Color.red(c)
            val green = android.graphics.Color.green(c)
            val blue = android.graphics.Color.blue(c)
            if (red > 205 && green > 205 && blue > 205) {
                val dx = x - cx; val dy = y - cy
                if (abs(dx) > abs(dy) * 1.15f) { if (dx < 0) l++ else r++ }
                else { if (dy < 0) u++ else d++ }
            }
        }
        b.recycle()
        val scores = listOf("LEFT" to l, "RIGHT" to r, "UP" to u, "DOWN" to d).sortedByDescending { it.second }
        val best = scores.first()
        val second = scores.getOrNull(1)?.second ?: 0
        // Require a clear directional imbalance; avoids taps on non-game screens.
        if (best.second < 8 || best.second < second * 1.12) return
        val now = System.currentTimeMillis()
        val delay = getSharedPreferences("prefs", Context.MODE_PRIVATE).getInt("delay", 500).toLong()
        if (now - lastTapAt < delay) return
        // Do not repeat a tap for a static prompt; wait for the arrow to change.
        if (best.first == lastDirection) return
        lastDirection = best.first
        lastTapAt = now
        val tapX = when (best.first) {
            "LEFT" -> w * 0.305f
            "RIGHT" -> w * 0.405f
            "UP" -> w * 0.596f
            else -> w * 0.690f
        }
        val tapY = h * 0.56f
        TapAccessibilityService.instance?.tap(tapX, tapY)
    }

    private fun createNotification() {
        val channel = "gah_capture"
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(channel, "Screen analysis", NotificationManager.IMPORTANCE_LOW))
        }
        val notification = Notification.Builder(this, channel)
            .setContentTitle("Grand Auto Helper")
            .setContentText("Анализ экрана активен. Останови приложение, чтобы завершить.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(11, notification)
    }

    override fun onDestroy() {
        display?.release()
        reader?.close()
        projection?.stop()
        super.onDestroy()
    }
}
