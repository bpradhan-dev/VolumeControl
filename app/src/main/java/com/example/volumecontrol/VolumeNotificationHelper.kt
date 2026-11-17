package com.example.volumecontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

object VolumeNotificationHelper {

    private const val PREFS = "volume_prefs"
    private const val KEY_ENABLED = "notif_enabled"
    private const val KEY_AUTO_RESTORE = "notif_auto_restore"
    private const val CHANNEL_ID = "volume_controls_channel_v2"
    const val NOTIF_ID = 1001

    const val KEY_SHOW_MEDIA = "notif_show_media"
    const val KEY_SHOW_CALL = "notif_show_call"
    const val KEY_SHOW_RING = "notif_show_ring"
    const val KEY_SHOW_NOTIF = "notif_show_notif"
    const val KEY_SHOW_ALARM = "notif_show_alarm"
    const val KEY_SHOW_MODE = "notif_show_mode"

    private data class StreamRow(
        val key: String,
        val defaultOn: Boolean,
        val stream: Int,
        val label: String,
        val rowId: Int,
        val tvId: Int,
        val pbId: Int,
        val downId: Int,
        val upId: Int,
        val reqBase: Int
    )

    private val streamRows = listOf(
        StreamRow(KEY_SHOW_MEDIA, false, AudioManager.STREAM_MUSIC, "Media", R.id.nRowMedia, R.id.nTvMedia, R.id.nPbMedia, R.id.nBtnMediaDown, R.id.nBtnMediaUp, 10),
        StreamRow(KEY_SHOW_CALL, true, AudioManager.STREAM_VOICE_CALL, "Call", R.id.nRowCall, R.id.nTvCall, R.id.nPbCall, R.id.nBtnCallDown, R.id.nBtnCallUp, 20),
        StreamRow(KEY_SHOW_RING, true, AudioManager.STREAM_RING, "Ring", R.id.nRowRing, R.id.nTvRing, R.id.nPbRing, R.id.nBtnRingDown, R.id.nBtnRingUp, 30),
        StreamRow(KEY_SHOW_NOTIF, false, AudioManager.STREAM_NOTIFICATION, "Notif", R.id.nRowNotif, R.id.nTvNotif, R.id.nPbNotif, R.id.nBtnNotifDown, R.id.nBtnNotifUp, 40),
        StreamRow(KEY_SHOW_ALARM, false, AudioManager.STREAM_ALARM, "Alarm", R.id.nRowAlarm, R.id.nTvAlarm, R.id.nPbAlarm, R.id.nBtnAlarmDown, R.id.nBtnAlarmUp, 50)
    )

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) show(context) else hide(context)
    }

    fun isAutoRestore(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_AUTO_RESTORE, true)

    fun setAutoRestore(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_RESTORE, enabled).apply()
    }

    fun isItemVisible(context: Context, key: String, defaultVal: Boolean = false): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, defaultVal)

    fun setItemVisible(context: Context, key: String, visible: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key, visible).apply()
        refreshIfEnabled(context)
    }

    fun refreshIfEnabled(context: Context) {
        if (isEnabled(context)) show(context)
    }

    fun show(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quick Volume Controls",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }

        val openPanelIntent = PendingIntent.getActivity(
            context,
            299,
            Intent(context, VolumePanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val selected = streamRows.filter { isItemVisible(context, it.key, it.defaultOn) }
        val bigViews = RemoteViews(context.packageName, R.layout.notification_volume)

        for (row in streamRows) {
            val visible = isItemVisible(context, row.key, row.defaultOn)
            bigViews.setViewVisibility(row.rowId, if (visible) View.VISIBLE else View.GONE)
            if (visible) {
                val vol = VolumeHelper.getVolume(context, row.stream)
                val max = VolumeHelper.getMaxVolume(context, row.stream)
                bigViews.setTextViewText(row.tvId, "${row.label} $vol/$max")
                bigViews.setProgressBar(row.pbId, max, vol, false)
                bigViews.setOnClickPendingIntent(row.downId, stepIntent(context, row.reqBase, row.stream, -1))
                bigViews.setOnClickPendingIntent(row.upId, stepIntent(context, row.reqBase + 1, row.stream, +1))
            }
        }

        val showMode = isItemVisible(context, KEY_SHOW_MODE, false)
        bigViews.setViewVisibility(R.id.nBtnMode, if (showMode) View.VISIBLE else View.GONE)
        if (showMode) {
            val modeText = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(context))
            bigViews.setTextViewText(R.id.nBtnMode, "Mode: $modeText")
            bigViews.setOnClickPendingIntent(R.id.nBtnMode, modeIntent(context, 90))
        }
        bigViews.setOnClickPendingIntent(R.id.nBtnAll, openPanelIntent)

        // Compact view shows up to the first 2 selected streams cleanly without clipping
        val compactViews = RemoteViews(context.packageName, R.layout.notification_volume_compact)
        val firstTwo = if (selected.isNotEmpty()) selected.take(2) else listOf(streamRows[1])
        bindCompactSlot(context, compactViews, firstTwo.getOrNull(0), R.id.ncRow0, R.id.ncLabel0, R.id.ncProg0, R.id.ncMinus0, R.id.ncPlus0, 100)
        bindCompactSlot(context, compactViews, firstTwo.getOrNull(1), R.id.ncRow1, R.id.ncLabel1, R.id.ncProg1, R.id.ncMinus1, R.id.ncPlus1, 110)

        val restoreIntent = PendingIntent.getBroadcast(
            context,
            290,
            Intent(context, VolumeActionReceiver::class.java).apply {
                action = VolumeActionReceiver.ACTION_RESTORE_NOTIFICATION
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_volume)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setWhen(0L)
            .setShowWhen(false)
            .setContentIntent(openPanelIntent)
            .setDeleteIntent(restoreIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactViews)
            .setCustomBigContentView(bigViews)
            .build()

        nm.notify(NOTIF_ID, notification)
    }

    private fun bindCompactSlot(
        context: Context,
        views: RemoteViews,
        row: StreamRow?,
        rowId: Int,
        labelId: Int,
        progId: Int,
        minusId: Int,
        plusId: Int,
        reqBase: Int
    ) {
        if (row == null) {
            views.setViewVisibility(rowId, View.GONE)
            return
        }
        views.setViewVisibility(rowId, View.VISIBLE)
        val vol = VolumeHelper.getVolume(context, row.stream)
        val max = VolumeHelper.getMaxVolume(context, row.stream)
        views.setTextViewText(labelId, "${row.label} $vol/$max")
        views.setProgressBar(progId, max, vol, false)
        views.setOnClickPendingIntent(minusId, stepIntent(context, reqBase, row.stream, -1))
        views.setOnClickPendingIntent(plusId, stepIntent(context, reqBase + 1, row.stream, +1))
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