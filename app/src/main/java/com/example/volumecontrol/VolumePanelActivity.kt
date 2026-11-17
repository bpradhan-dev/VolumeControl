package com.example.volumecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider

class VolumePanelActivity : AppCompatActivity() {

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to "Media",
        AudioManager.STREAM_VOICE_CALL to "Call",
        AudioManager.STREAM_RING to "Ringtone",
        AudioManager.STREAM_ALARM to "Alarm"
    )

    private val rowHolders = mutableListOf<Triple<Int, Slider, TextView>>()
    private lateinit var btnPanelMode: Button
    private var isUpdatingUi = false

    private val volumeObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume_panel)

        val container = findViewById<LinearLayout>(R.id.panelSlidersContainer)
        val inflater = LayoutInflater.from(this)

        for ((stream, label) in streams) {
            val row = inflater.inflate(R.layout.item_volume_row, container, false)
            val tvName = row.findViewById<TextView>(R.id.tvStreamName)
            val tvLevel = row.findViewById<TextView>(R.id.tvStreamLevel)
            val slider = row.findViewById<Slider>(R.id.slider)
            val btnMinus = row.findViewById<Button>(R.id.btnMinus)
            val btnPlus = row.findViewById<Button>(R.id.btnPlus)

            tvName.text = label
            setSliderBounds(
                slider,
                VolumeHelper.getMinVolume(this, stream),
                VolumeHelper.getMaxVolume(this, stream),
                VolumeHelper.getVolume(this, stream)
            )

            slider.addOnChangeListener { s, value, fromUser ->
                if (fromUser && !isUpdatingUi) {
                    val target = value.toInt()
                    val changed = VolumeHelper.setVolume(this@VolumePanelActivity, stream, target)
                    tvLevel.text = "$target / ${s.valueTo.toInt()}"
                    if (changed) {
                        s.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        syncExternalViews()
                    }
                }
            }

            btnMinus.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                if (VolumeHelper.stepVolume(this, stream, -1)) {
                    refreshUi()
                    syncExternalViews()
                }
            }

            btnPlus.setOnClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                if (VolumeHelper.stepVolume(this, stream, +1)) {
                    refreshUi()
                    syncExternalViews()
                }
            }

            rowHolders.add(Triple(stream, slider, tvLevel))
            container.addView(row)
        }

        btnPanelMode = findViewById(R.id.btnPanelMode)
        btnPanelMode.setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            VolumeHelper.cycleRingerMode(this)
            refreshUi()
            syncExternalViews()
        }

        findViewById<Button>(R.id.btnPanelOpenApp).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        findViewById<Button>(R.id.btnPanelClose).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        ContextCompat.registerReceiver(this, volumeObserver, filter, ContextCompat.RECEIVER_EXPORTED)
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(volumeObserver)
    }

    private fun refreshUi() {
        isUpdatingUi = true
        for ((stream, slider, tvLevel) in rowHolders) {
            val min = VolumeHelper.getMinVolume(this, stream)
            val max = VolumeHelper.getMaxVolume(this, stream)
            val vol = VolumeHelper.getVolume(this, stream)
            setSliderBounds(slider, min, max, vol)
            tvLevel.text = "$vol / $max"
        }
        isUpdatingUi = false
        btnPanelMode.text = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(this))
    }

    private fun setSliderBounds(slider: Slider, min: Int, max: Int, current: Int) {
        val safeMin = min.toFloat()
        val safeMax = maxOf(min + 1, max).toFloat()
        val safeVal = current.coerceIn(min, max).toFloat()
        slider.stepSize = 0f
        slider.valueFrom = minOf(slider.valueFrom, safeMin)
        slider.valueTo = maxOf(slider.valueTo, safeMax)
        slider.value = safeVal
        slider.valueFrom = safeMin
        slider.valueTo = safeMax
        slider.stepSize = 1f
    }

    private fun syncExternalViews() {
        VolumeWidgetProvider.updateAllWidgets(this)
        VolumeNotificationHelper.refreshIfEnabled(this)
    }
}