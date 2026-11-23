package com.grandauto.helper

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.*
import android.view.Gravity

class MainActivity : Activity() {
    private val requestCapture = 701
    private lateinit var status: TextView
    private lateinit var delay: SeekBar
    private lateinit var delayText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val title = TextView(this).apply {
            text = "Grand Auto Helper"
            textSize = 25f
        }
        status = TextView(this).apply {
            text = "Сначала включи службу специальных возможностей."
            textSize = 16f
            setPadding(0, 20, 0, 20)
        }
        val access = Button(this).apply {
            text = "1. Включить службу управления"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }
        delayText = TextView(this).apply { text = "Интервал: 500 мс"; textSize = 17f }
        delay = SeekBar(this).apply {
            max = 900
            progress = 200
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                    val ms = p + 300
                    delayText.text = "Интервал: $ms мс"
                    getSharedPreferences("prefs", MODE_PRIVATE).edit().putInt("delay", ms).apply()
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        val start = Button(this).apply {
            text = "2. Запросить захват экрана и запустить"
            setOnClickListener {
                if (!TapAccessibilityService.isReady) {
                    status.text = "Сначала включи службу специальных возможностей."
                    return@setOnClickListener
                }
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), requestCapture)
            }
        }
        val stop = Button(this).apply {
            text = "Остановить"
            setOnClickListener { stopService(Intent(this@MainActivity, CaptureService::class.java)); status.text = "Остановлено." }
        }
        val note = TextView(this).apply {
            text = "Важно: сначала проверь распознавание на тестовом экране. Координаты рассчитаны по присланному скриншоту 16:9; на другом масштабе интерфейса может потребоваться калибровка."
            textSize = 14f
            setPadding(0, 22, 0, 0)
        }
        listOf(title, status, access, delayText, delay, start, stop, note).forEach { layout.addView(it) }
        setContentView(layout)
        delay.progress = getSharedPreferences("prefs", MODE_PRIVATE).getInt("delay", 500) - 300
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCapture && resultCode == RESULT_OK && data != null) {
            val i = Intent(this, CaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            startForegroundService(i)
            status.text = "Захват экрана запущен. Открой мини-игру."
        }
    }
}
