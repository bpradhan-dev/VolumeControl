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
        private val mediaSegIds = intArrayOf(
            R.id.wMediaSeg0, R.id.wMediaSeg1, R.id.wMediaSeg2, R.id.wMediaSeg3, R.id.wMediaSeg4
        )
        private val callSegIds = intArrayOf(
            R.id.wCallSeg0, R.id.wCallSeg1, R.id.wCallSeg2, R.id.wCallSeg3, R.id.wCallSeg4
        )
        private val ringSegIds = intArrayOf(
            R.id.wRingSeg0, R.id.wRingSeg1, R.id.wRingSeg2, R.id.wRingSeg3, R.id.wRingSeg4
        )

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, VolumeWidgetProvider::class.java))
            if (ids.isEmpty()) return
            for (id in ids) {
                updateSingleWidget(context, manager, id)
            }
        }

        private fun updateSingleWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val mVol = VolumeHelper.getVolume(context, AudioManager.STREAM_MUSIC)
            val mMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_MUSIC)
            val mLit = VolumeHelper.filledSegments(context, AudioManager.STREAM_MUSIC)

            val cVol = VolumeHelper.getVolume(context, AudioManager.STREAM_VOICE_CALL)
            val cMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_VOICE_CALL)
            val cLit = VolumeHelper.filledSegments(context, AudioManager.STREAM_VOICE_CALL)

            val rVol = VolumeHelper.getVolume(context, AudioManager.STREAM_RING)
            val rMax = VolumeHelper.getMaxVolume(context, AudioManager.STREAM_RING)
            val rLit = VolumeHelper.filledSegments(context, AudioManager.STREAM_RING)

            val mode = VolumeHelper.ringerModeLabel(VolumeHelper.getRingerMode(context))

            val views = RemoteViews(context.packageName, R.layout.widget_volume).apply {
                setTextViewText(R.id.wTvMedia, "Media $mVol/$mMax")
                setTextViewText(R.id.wTvCall, "Call $cVol/$cMax")
                setTextViewText(R.id.wTvRing, "Ring $rVol/$rMax")
                setTextViewText(R.id.wBtnMode, mode)

                bindSegments(this, context, mediaSegIds, 300, AudioManager.STREAM_MUSIC, mLit)
                bindSegments(this, context, callSegIds, 310, AudioManager.STREAM_VOICE_CALL, cLit)
                bindSegments(this, context, ringSegIds, 320, AudioManager.STREAM_RING, rLit)

                setOnClickPendingIntent(R.id.wBtnMode, modeIntent(context, 390))
                setOnClickPendingIntent(R.id.wBtnPanel, panelIntent(context, 391))
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun bindSegments(
            views: RemoteViews,
            context: Context,
            segIds: IntArray,
            baseReqCode: Int,
            stream: Int,
            litCount: Int
        ) {
            for (i in 0 until 5) {
                views.setImageViewResource(
                    segIds[i],
                    if (i < litCount) R.drawable.seg_active else R.drawable.seg_inactive
                )
                views.setOnClickPendingIntent(
                    segIds[i],
                    segmentIntent(context, baseReqCode + i, stream, i)
                )
            }
        }

        private fun segmentIntent(context: Context, reqCode: Int, stream: Int, segment: Int): PendingIntent {
            val intent = Intent(context, VolumeActionReceiver::class.java).apply {
                action = VolumeActionReceiver.ACTION_SET_SEGMENT
                putExtra(VolumeActionReceiver.EXTRA_STREAM, stream)
                putExtra(VolumeActionReceiver.EXTRA_SEGMENT, segment)
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