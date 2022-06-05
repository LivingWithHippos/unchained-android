package com.github.livingwithhippos.unchained.utilities

import android.app.Activity
import android.app.Application
import android.content.res.Configuration

/**
Empty object to avoid telemetry in the release version of the app, see the same file under debug for actual code
 */
object TelemetryManager {
    fun onStart(activity: Activity) {
    }

    fun onStop() {
    }

    fun onConfigurationChanged(newConfig: Configuration) {
    }

    fun onCreate(application: Application) {
    }
}