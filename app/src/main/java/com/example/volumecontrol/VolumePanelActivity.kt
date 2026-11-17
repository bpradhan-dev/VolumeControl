package com.example.volumecontrol

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VolumePanelActivity : AppCompatActivity() {

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to "Media",
        AudioManager.STREAM_VOICE_CALL to "Call",
        AudioManager.STREAM_RING to "Ringtone",
        AudioManager.STREAM_ALARM to "Alarm"
    )

    private val rowHolders = mutableListOf<Triple<Int, SeekBar, TextView>>()
    private lateinit var btnPanelMode: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume_panel)

        val container = findViewById<LinearLayout>(R.id.panelSlidersContainer)
        val inflater = LayoutInflater.from(this)

        for ((stream, label) in streams) {
            val row = inflater.inflate(R.layout.item_volume_row, container, false)
            val tvName = row.findViewById<TextView>(R.id.tvStreamName)
            val tvLevel = row.findViewById<TextView>(R.id.tvStreamLevel)
            val seekBar = row.findViewById<SeekBar>(R.id.seekBar)
            val btnMinus = row.findViewById<Button>(R.id.btnMinus)
            val btnPlus = row.findViewById<Button>(R.id.btnPlus)

            tvName.text = label
            seekBar.min = VolumeHelper.getMinVolume(this, stream)
            seekBar.max = VolumeHelper.getMaxVolume(this, stream)

            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        VolumeHelper.setVolume(this@VolumePanelActivity, stream, progress)
                        tvLevel.text = "$progress / ${sb?.max ?: 0}"
                        syncExternalViews()
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })

            btnMinus.setOnClickListener {
                VolumeHelper.stepVolume(this, stream, -1)
                refreshUi()
                syncExternalViews()
            }

            btnPlus.setOnClickListener {
                VolumeHelper.stepVolume(this, stream, +1)
                refreshUi()
                syncExternalViews()
            }

            rowHolders.add(Triple(stream, seekBar, tvLevel))
            container.addView(row)
        }

        btnPanelMode = findViewById(R.id.btnPanelMode)
        btnPanelMode.setOnClickListener {
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

        refreshUi()
    }

    private fun refreshUi() {
        for ((stream, seekBar, tvLevel) in rowHolders) {
            val vol = VolumeHelper.getVolume(this, stream)
            val max = VolumeHelper.getMaxVolume(this, stream)
            seekBar.progress = vol
            tvLevel.text = "$vol / $max"
        }
        btnPanelMode.text = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(this))
    }

    private fun syncExternalViews() {
        VolumeWidgetProvider.updateAllWidgets(this)
        VolumeNotificationHelper.refreshIfEnabled(this)
    }
}