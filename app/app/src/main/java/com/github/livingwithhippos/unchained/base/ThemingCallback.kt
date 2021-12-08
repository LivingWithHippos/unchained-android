package com.github.livingwithhippos.unchained.base

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment

class ThemingCallback(val preferences: SharedPreferences) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        preferences.getString(SettingsFragment.KEY_THEME, "original")?.let {
            setupNightMode(activity.resources, it)
            if (activity is AppCompatActivity)
                setCustomTheme(activity, it)
        }
    }

    private fun setCustomTheme(activity: Activity, theme: String) {
        val themeID = when (theme) {
            "original" -> R.style.Theme_Unchained
            "tropical_sunset" -> R.style.Theme_TropicalSunset
            "black_n_white" -> R.style.Theme_BlackAndWhite
            "waves_01" -> R.style.Theme_Wave01
            else -> R.style.Theme_Unchained
        }
        activity.setTheme(themeID)
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    private fun setupNightMode(resources: Resources, theme: String) {
        // get night mode values
        val nightModeArray = resources.getStringArray(R.array.night_mode_values)
        val nightMode = preferences.getString(SettingsFragment.KEY_DAY_NIGHT, "auto")
        // get current theme and check if it's has a night mode
        if (theme in DAY_ONLY_THEMES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            // update the night mode if not the day one
            if (nightMode != "day")
                with(preferences.edit()) {
                    putString(SettingsFragment.KEY_DAY_NIGHT, "day")
                    apply()
                }
        } else {
            // enable or disable night mode according with the preferences
            when (nightMode) {
                nightModeArray[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                nightModeArray[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                nightModeArray[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    companion object {
        val DAY_ONLY_THEMES = arrayOf("tropical_sunset","waves_01")
    }
}
