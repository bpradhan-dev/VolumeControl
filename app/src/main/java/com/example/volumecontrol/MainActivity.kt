package com.example.volumecontrol

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to "Media",
        AudioManager.STREAM_VOICE_CALL to "Call",
        AudioManager.STREAM_RING to "Ringtone",
        AudioManager.STREAM_NOTIFICATION to "Notifications",
        AudioManager.STREAM_ALARM to "Alarm"
    )

    private val rowHolders = mutableListOf<Triple<Int, SeekBar, TextView>>()
    private lateinit var switchNotification: MaterialSwitch
    private lateinit var btnDndAccess: Button

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                VolumeNotificationHelper.setEnabled(this, true)
            } else {
                switchNotification.isChecked = false
            }
        }

    private val volumeObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshUi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = findViewById<LinearLayout>(R.id.slidersContainer)
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
                        VolumeHelper.setVolume(this@MainActivity, stream, progress)
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

        findViewById<Button>(R.id.btnModeNormal).setOnClickListener {
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_NORMAL)
            refreshUi()
            syncExternalViews()
        }
        findViewById<Button>(R.id.btnModeVibrate).setOnClickListener {
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_VIBRATE)
            refreshUi()
            syncExternalViews()
        }
        findViewById<Button>(R.id.btnModeSilent).setOnClickListener {
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_SILENT)
            refreshUi()
            syncExternalViews()
        }

        switchNotification = findViewById(R.id.switchNotification)
        switchNotification.isChecked = VolumeNotificationHelper.isEnabled(this)
        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    VolumeNotificationHelper.setEnabled(this, true)
                }
            } else {
                VolumeNotificationHelper.setEnabled(this, false)
            }
        }

        btnDndAccess = findViewById(R.id.btnDndAccess)
        btnDndAccess.setOnClickListener {
            VolumeHelper.openDndSettings(this)
        }

        findViewById<Button>(R.id.btnOpenQuickPanel).setOnClickListener {
            startActivity(Intent(this, VolumePanelActivity::class.java))
        }

        findViewById<Button>(R.id.btnSystemUi).setOnClickListener {
            VolumeHelper.showSystemVolumePanel(this)
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
        for ((stream, seekBar, tvLevel) in rowHolders) {
            val vol = VolumeHelper.getVolume(this, stream)
            val max = VolumeHelper.getMaxVolume(this, stream)
            seekBar.progress = vol
            tvLevel.text = "$vol / $max"
        }
        val dndOk = VolumeHelper.hasDndAccess(this)
        btnDndAccess.visibility = if (dndOk) View.GONE else View.VISIBLE
    }

    private fun syncExternalViews() {
        VolumeWidgetProvider.updateAllWidgets(this)
        VolumeNotificationHelper.refreshIfEnabled(this)
    }
}