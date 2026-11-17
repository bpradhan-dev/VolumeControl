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
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private val streams = listOf(
        AudioManager.STREAM_MUSIC to "Media",
        AudioManager.STREAM_VOICE_CALL to "Call",
        AudioManager.STREAM_RING to "Ringtone",
        AudioManager.STREAM_NOTIFICATION to "Notifications",
        AudioManager.STREAM_ALARM to "Alarm"
    )

    private val rowHolders = mutableListOf<Triple<Int, Slider, TextView>>()
    private lateinit var switchNotification: MaterialSwitch
    private lateinit var switchAutoRestore: MaterialSwitch
    private lateinit var cardNotifChoices: View
    private lateinit var btnDndAccess: Button
    private var isUpdatingUi = false

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                VolumeNotificationHelper.setEnabled(this, true)
                updateNotifOptionsVisibility(true)
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootScroll = findViewById<ScrollView>(R.id.mainRoot)
        val basePad = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(rootScroll) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(basePad, bars.top + basePad, basePad, bars.bottom + basePad)
            insets
        }

        val container = findViewById<LinearLayout>(R.id.slidersContainer)
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
                    val changed = VolumeHelper.setVolume(this@MainActivity, stream, target)
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

        findViewById<Button>(R.id.btnModeNormal).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_NORMAL)
            refreshUi()
            syncExternalViews()
        }
        findViewById<Button>(R.id.btnModeVibrate).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_VIBRATE)
            refreshUi()
            syncExternalViews()
        }
        findViewById<Button>(R.id.btnModeSilent).setOnClickListener { v ->
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            VolumeHelper.setRingerMode(this, AudioManager.RINGER_MODE_SILENT)
            refreshUi()
            syncExternalViews()
        }

        switchNotification = findViewById(R.id.switchNotification)
        switchAutoRestore = findViewById(R.id.switchAutoRestore)
        cardNotifChoices = findViewById(R.id.cardNotifChoices)

        val notifOn = VolumeNotificationHelper.isEnabled(this)
        switchNotification.isChecked = notifOn
        switchAutoRestore.isChecked = VolumeNotificationHelper.isAutoRestore(this)
        updateNotifOptionsVisibility(notifOn)

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    VolumeNotificationHelper.setEnabled(this, true)
                    updateNotifOptionsVisibility(true)
                }
            } else {
                VolumeNotificationHelper.setEnabled(this, false)
                updateNotifOptionsVisibility(false)
            }
        }

        switchAutoRestore.setOnCheckedChangeListener { _, isChecked ->
            VolumeNotificationHelper.setAutoRestore(this, isChecked)
        }

        bindNotifCheckbox(R.id.cbNotifMedia, VolumeNotificationHelper.KEY_SHOW_MEDIA, false)
        bindNotifCheckbox(R.id.cbNotifCall, VolumeNotificationHelper.KEY_SHOW_CALL, true)
        bindNotifCheckbox(R.id.cbNotifRing, VolumeNotificationHelper.KEY_SHOW_RING, true)
        bindNotifCheckbox(R.id.cbNotifNotif, VolumeNotificationHelper.KEY_SHOW_NOTIF, false)
        bindNotifCheckbox(R.id.cbNotifAlarm, VolumeNotificationHelper.KEY_SHOW_ALARM, false)
        bindNotifCheckbox(R.id.cbNotifMode, VolumeNotificationHelper.KEY_SHOW_MODE, false)

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

    private fun bindNotifCheckbox(viewId: Int, key: String, defaultVal: Boolean) {
        val cb = findViewById<MaterialCheckBox>(viewId)
        cb.isChecked = VolumeNotificationHelper.isItemVisible(this, key, defaultVal)
        cb.setOnCheckedChangeListener { _, isChecked ->
            VolumeNotificationHelper.setItemVisible(this, key, isChecked)
        }
    }

    private fun updateNotifOptionsVisibility(visible: Boolean) {
        val vis = if (visible) View.VISIBLE else View.GONE
        switchAutoRestore.visibility = vis
        cardNotifChoices.visibility = vis
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
        btnDndAccess.visibility = if (VolumeHelper.hasDndAccess(this)) View.GONE else View.VISIBLE
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