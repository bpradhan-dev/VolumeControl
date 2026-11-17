package com.example.volumecontrol

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import kotlin.math.roundToInt

object VolumeHelper {

    fun audioManager(context: Context): AudioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun hasDndAccess(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun openDndSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getVolume(context: Context, stream: Int): Int =
        audioManager(context).getStreamVolume(stream)

    fun getMaxVolume(context: Context, stream: Int): Int =
        audioManager(context).getStreamMaxVolume(stream)

    fun getMinVolume(context: Context, stream: Int): Int {
        return if (Build.VERSION.SDK_INT >= 28) {
            audioManager(context).getStreamMinVolume(stream)
        } else {
            0
        }
    }

    fun setVolume(context: Context, stream: Int, value: Int, showSystemUi: Boolean = false): Boolean {
        val am = audioManager(context)
        val min = getMinVolume(context, stream)
        val max = getMaxVolume(context, stream)
        val target = value.coerceIn(min, max)
        val before = am.getStreamVolume(stream)
        if (before == target && !showSystemUi) return false

        val flags = if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0
        try {
            am.setStreamVolume(stream, target, flags)
        } catch (_: SecurityException) {
            Toast.makeText(
                context,
                "Grant Do Not Disturb access in app to mute ringtone",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return am.getStreamVolume(stream) != before
    }

    fun stepVolume(context: Context, stream: Int, delta: Int, showSystemUi: Boolean = false): Boolean {
        if (delta == 0) return false
        val am = audioManager(context)
        val before = am.getStreamVolume(stream)
        val target = (before + delta).coerceIn(getMinVolume(context, stream), getMaxVolume(context, stream))
        if (before == target && !showSystemUi) return false

        setVolume(context, stream, target, showSystemUi)
        if (am.getStreamVolume(stream) != before) return true

        val dir = if (delta > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        try {
            am.adjustStreamVolume(stream, dir, if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0)
        } catch (_: SecurityException) {}

        if (am.getStreamVolume(stream) != before) return true

        if (stream == AudioManager.STREAM_MUSIC && !showSystemUi) {
            try {
                am.adjustStreamVolume(stream, dir, AudioManager.FLAG_SHOW_UI)
            } catch (_: SecurityException) {}
        }
        return am.getStreamVolume(stream) != before
    }

    fun setVolumeSegment(context: Context, stream: Int, segmentIndex: Int, totalSegments: Int = 5): Boolean {
        val min = getMinVolume(context, stream)
        val max = getMaxVolume(context, stream)
        val clampedIdx = segmentIndex.coerceIn(0, totalSegments - 1)
        val target = when (clampedIdx) {
            0 -> min
            totalSegments - 1 -> max
            else -> min + ((max - min) * (clampedIdx.toFloat() / (totalSegments - 1))).roundToInt()
        }
        return setVolume(context, stream, target)
    }

    fun filledSegments(context: Context, stream: Int, totalSegments: Int = 5): Int {
        val current = getVolume(context, stream)
        if (current <= 0) return 0
        val max = getMaxVolume(context, stream).coerceAtLeast(1)
        return ((current.toFloat() / max) * totalSegments).roundToInt().coerceIn(1, totalSegments)
    }

    fun getRingerMode(context: Context): Int = audioManager(context).ringerMode

    fun setRingerMode(context: Context, mode: Int) {
        if (mode == AudioManager.RINGER_MODE_SILENT && !hasDndAccess(context)) {
            Toast.makeText(
                context,
                "Please allow Do Not Disturb access for Silent mode",
                Toast.LENGTH_SHORT
            ).show()
            openDndSettings(context)
            return
        }
        try {
            audioManager(context).ringerMode = mode
        } catch (_: SecurityException) {
            openDndSettings(context)
        }
    }

    fun cycleRingerMode(context: Context) {
        val next = when (getRingerMode(context)) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE ->
                if (hasDndAccess(context)) AudioManager.RINGER_MODE_SILENT
                else AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        setRingerMode(context, next)
    }

    fun showSystemVolumePanel(context: Context) {
        audioManager(context).adjustVolume(AudioManager.ADJUST_SAME, AudioManager.FLAG_SHOW_UI)
    }

    fun ringerModeLabel(mode: Int): String = when (mode) {
        AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
        AudioManager.RINGER_MODE_SILENT -> "Silent"
        else -> "Normal"
    }
}