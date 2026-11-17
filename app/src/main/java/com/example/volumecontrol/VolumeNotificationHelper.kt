package com.example.volumecontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

object VolumeNotificationHelper {

    private const val PREFS = "volume_prefs"
    private const val KEY_ENABLED = "notif_enabled"
    private const val CHANNEL_ID = "volume_controls_channel"
    private const val NOTIF_ID = 1001

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) show(context) else hide(context)
    }

    fun refreshIfEnabled(context: Context) {
        if (isEnabled(context)) show(context)
    }

    fun show(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Quick Volume Controls",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)

        val mediaVol = VolumeHelper.getVolume(context, AudioManager.STREAM_MUSIC)
        val mediaMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_MUSIC)
        val callVol = VolumeHelper.getVolume(context, AudioManager.STREAM_VOICE_CALL)
        val callMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_VOICE_CALL)
        val modeText = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(context))

        val views = RemoteViews(context.packageName, R.layout.notification_volume).apply {
            setTextViewText(R.id.nTvStatus, "Media $mediaVol/$mediaMax  |  Call $callVol/$callMax  |  $modeText")
            setOnClickPendingIntent(R.id.nBtnMediaDown, stepIntent(context, 1, AudioManager.STREAM_MUSIC, -1))
            setOnClickPendingIntent(R.id.nBtnMediaUp, stepIntent(context, 2, AudioManager.STREAM_MUSIC, +1))
            setOnClickPendingIntent(R.id.nBtnCallDown, stepIntent(context, 3, AudioManager.STREAM_VOICE_CALL, -1))
            setOnClickPendingIntent(R.id.nBtnCallUp, stepIntent(context, 4, AudioManager.STREAM_VOICE_CALL, +1))
            setOnClickPendingIntent(R.id.nBtnMode, modeIntent(context, 5))
        }

        val openPanelIntent = PendingIntent.getActivity(
            context,
            99,
            Intent(context, VolumePanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_volume)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPanelIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(views)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    fun hide(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(NOTIF_ID)
    }

    private fun stepIntent(context: Context, reqCode: Int, stream: Int, delta: Int): PendingIntent {
        val intent = Intent(context, VolumeActionReceiver::class.java).apply {
            action = VolumeActionReceiver.ACTION_STEP_VOLUME
            putExtra(VolumeActionReceiver.EXTRA_STREAM, stream)
            putExtra(VolumeActionReceiver.EXTRA_DELTA, delta)
        }
        return PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun modeIntent(context: Context, reqCode: Int): PendingIntent {
        val intent = Intent(context, VolumeActionReceiver::class.java).apply {
            action = VolumeActionReceiver.ACTION_CYCLE_MODE
        }
        return PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}