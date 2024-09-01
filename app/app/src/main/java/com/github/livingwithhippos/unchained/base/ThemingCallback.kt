package com.github.livingwithhippos.unchained.base

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_AUTO
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_DAY
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_NIGHT
import com.github.livingwithhippos.unchained.settings.view.ThemeItem
import com.github.livingwithhippos.unchained.utilities.extension.getThemeList

class ThemingCallback(val preferences: SharedPreferences) : Application.ActivityLifecycleCallbacks {

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val themeRes =
            preferences.getInt(
                SettingsFragment.KEY_THEME_NEW, R.style.Theme_Unchained_Material3_Green_One)
        val themesList = activity.applicationContext.getThemeList()
        val currentTheme: ThemeItem = themesList.find { it.themeID == themeRes } ?: themesList[1]
        setupNightMode(currentTheme.nightMode)
        if (activity is AppCompatActivity) setCustomTheme(activity, themeRes)
    }

    private fun setCustomTheme(activity: Activity, themeRes: Int) {
        activity.setTheme(themeRes)
        /*
        // todo: check if this can be avoided, android:navigationBarColor in xml is not working
        activity.window.statusBarColor = activity.getThemeColor(R.attr.customStatusBarColor)
        activity.window.navigationBarColor = activity.getThemeColor(R.attr.customNavigationBarColor)
         */
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

    /**
     * Set the night mode depending on the user preferences and what the theme support
     */
    private fun setupNightMode(themeNightModeSupport: String) {
        when (themeNightModeSupport) {
            THEME_AUTO -> {
                when (preferences.getString(SettingsFragment.KEY_DAY_NIGHT, "auto")) {
                    THEME_AUTO ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    THEME_DAY -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            THEME_DAY -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

    }
}
