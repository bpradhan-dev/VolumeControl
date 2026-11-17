package com.example.volumecontrol

import android.app.Application
import com.google.android.material.color.DynamicColors

class VolumeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}