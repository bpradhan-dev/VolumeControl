package com.example.volumecontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class VolumeActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STEP_VOLUME = "com.example.volumecontrol.ACTION_STEP_VOLUME"
        const val ACTION_CYCLE_MODE = "com.example.volumecontrol.ACTION_CYCLE_MODE"
        const val ACTION_SHOW_SYSTEM_UI = "com.example.volumecontrol.ACTION_SHOW_SYSTEM_UI"

        const val EXTRA_STREAM = "extra_stream"
        const val EXTRA_DELTA = "extra_delta"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_STEP_VOLUME -> {
                val stream = intent.getIntExtra(EXTRA_STREAM, AudioManager.STREAM_MUSIC)
                val delta = intent.getIntExtra(EXTRA_DELTA, 0)
                VolumeHelper.stepVolume(context, stream, delta, showSystemUi = false)
            }
            ACTION_CYCLE_MODE -> {
                VolumeHelper.cycleRingerMode(context)
            }
            ACTION_SHOW_SYSTEM_UI -> {
                VolumeHelper.showSystemVolumePanel(context)
            }
        }

        VolumeWidgetProvider.updateAllWidgets(context)
        VolumeNotificationHelper.refreshIfEnabled(context)
    }
}