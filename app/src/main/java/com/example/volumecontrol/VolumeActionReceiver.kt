package com.example.volumecontrol

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class VolumeActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STEP_VOLUME = "com.example.volumecontrol.ACTION_STEP_VOLUME"
        const val ACTION_SET_SEGMENT = "com.example.volumecontrol.ACTION_SET_SEGMENT"
        const val ACTION_CYCLE_MODE = "com.example.volumecontrol.ACTION_CYCLE_MODE"
        const val ACTION_RESTORE_NOTIFICATION = "com.example.volumecontrol.ACTION_RESTORE_NOTIFICATION"

        const val EXTRA_STREAM = "extra_stream"
        const val EXTRA_DELTA = "extra_delta"
        const val EXTRA_SEGMENT = "extra_segment"
    }

    override fun onReceive(context: Context, intent: Intent) {
        var changed = false
        when (intent.action) {
            ACTION_STEP_VOLUME -> {
                val stream = intent.getIntExtra(EXTRA_STREAM, AudioManager.STREAM_MUSIC)
                val delta = intent.getIntExtra(EXTRA_DELTA, 0)
                changed = VolumeHelper.stepVolume(context, stream, delta)
            }
            ACTION_SET_SEGMENT -> {
                val stream = intent.getIntExtra(EXTRA_STREAM, AudioManager.STREAM_MUSIC)
                val segment = intent.getIntExtra(EXTRA_SEGMENT, 0)
                changed = VolumeHelper.setVolumeSegment(context, stream, segment)
            }
            ACTION_CYCLE_MODE -> {
                VolumeHelper.cycleRingerMode(context)
                changed = true
            }
            ACTION_RESTORE_NOTIFICATION -> {
                if (VolumeNotificationHelper.isEnabled(context) &&
                    VolumeNotificationHelper.isAutoRestore(context)
                ) {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(VolumeNotificationHelper.NOTIF_ID)
                    VolumeNotificationHelper.show(context)
                }
                return
            }
        }

        if (changed) {
            VolumeWidgetProvider.updateAllWidgets(context)
            VolumeNotificationHelper.refreshIfEnabled(context)
        }
    }
}