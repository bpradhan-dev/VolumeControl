package com.example.volumecontrol

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.widget.RemoteViews

class VolumeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VolumeWidgetProvider::class.java))
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }

        private fun updateSingleWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val mVol = VolumeHelper.getVolume(context, AudioManager.STREAM_MUSIC)
            val mMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_MUSIC)
            val cVol = VolumeHelper.getVolume(context, AudioManager.STREAM_VOICE_CALL)
            val cMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_VOICE_CALL)
            val rVol = VolumeHelper.getVolume(context, AudioManager.STREAM_RING)
            val rMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_RING)
            val mode = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(context))

            val views = RemoteViews(context.packageName, R.layout.widget_volume).apply {
                setTextViewText(R.id.wTvMedia, "Media: $mVol / $mMax")
                setTextViewText(R.id.wTvCall, "Call: $cVol / $cMax")
                setTextViewText(R.id.wTvRing, "Ring: $rVol / $rMax")
                setTextViewText(R.id.wBtnMode, mode)

                setOnClickPendingIntent(R.id.wBtnMediaDown, stepIntent(context, 11, AudioManager.STREAM_MUSIC, -1))
                setOnClickPendingIntent(R.id.wBtnMediaUp, stepIntent(context, 12, AudioManager.STREAM_MUSIC, +1))
                setOnClickPendingIntent(R.id.wBtnCallDown, stepIntent(context, 13, AudioManager.STREAM_VOICE_CALL, -1))
                setOnClickPendingIntent(R.id.wBtnCallUp, stepIntent(context, 14, AudioManager.STREAM_VOICE_CALL, +1))
                setOnClickPendingIntent(R.id.wBtnRingDown, stepIntent(context, 15, AudioManager.STREAM_RING, -1))
                setOnClickPendingIntent(R.id.wBtnRingUp, stepIntent(context, 16, AudioManager.STREAM_RING, +1))
                setOnClickPendingIntent(R.id.wBtnMode, modeIntent(context, 17))
                setOnClickPendingIntent(R.id.wBtnPanel, panelIntent(context, 18))
            }

            manager.updateAppWidget(widgetId, views)
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

        private fun panelIntent(context: Context, reqCode: Int): PendingIntent {
            val intent = Intent(context, VolumePanelActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return PendingIntent.getActivity(
                context, reqCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}