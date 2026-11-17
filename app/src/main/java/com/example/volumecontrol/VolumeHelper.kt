package com.example.volumecontrol

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.widget.Toast

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
        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            audioManager(context).getStreamMinVolume(stream)
        } else {
            0
        }
    }

    fun setVolume(context: Context, stream: Int, value: Int, showSystemUi: Boolean = false) {
        val am = audioManager(context)
        val min = getMinVolume(context, stream)
        val max = getMaxVolume(context, stream)
        val target = value.coerceIn(min, max)
        val flags = if (showSystemUi) AudioManager.FLAG_SHOW_UI else 0

        try {
            am.setStreamVolume(stream, target, flags)
        } catch (_: SecurityException) {
            Toast.makeText(
                context,
                "Grant Do Not Disturb access in app to mute ringtone",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun stepVolume(context: Context, stream: Int, delta: Int, showSystemUi: Boolean = false) {
        val current = getVolume(context, stream)
        setVolume(context, stream, current + delta, showSystemUi)
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