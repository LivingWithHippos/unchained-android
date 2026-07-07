package com.github.livingwithhippos.unchained.base

import android.app.Activity
import android.app.Application
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.livingwithhippos.unchained.R
import androidx.core.content.ContextCompat
import com.github.livingwithhippos.unchained.settings.view.CUSTOM_THEME_KEY
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_AUTO
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_DAY
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.THEME_NIGHT
import com.github.livingwithhippos.unchained.settings.view.ThemeItem
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.extension.applyThemeAwareSystemBarIconColors
import com.github.livingwithhippos.unchained.utilities.extension.getThemeList
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import java.util.WeakHashMap

class ThemingCallback(val preferences: SharedPreferences) : Application.ActivityLifecycleCallbacks {

    // signature of the theme each currently alive activity was (re)created with, so a change
    // made while another activity is on top can be detected and applied on resume, see #305.
    // this is not just the style resource id: the Custom theme keeps the same style id no
    // matter which seed color is active, since the color itself lives in a separate preference,
    // so the signature folds that color in too or a color-only change would go undetected
    private val appliedThemes = WeakHashMap<Activity, Int>()

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        val themeRes = currentThemeRes()
        val themesList = activity.applicationContext.getThemeList()
        val currentTheme: ThemeItem = themesList.find { it.themeID == themeRes } ?: themesList[1]
        setupNightMode(currentTheme.nightMode)
        if (activity is AppCompatActivity) {
            setCustomTheme(activity, themeRes)
            if (currentTheme.isDynamic) applyDynamicColors(activity, currentTheme)
            activity.applyThemeAwareSystemBarIconColors()
            appliedThemes[activity] = currentThemeSignature(themeRes, currentTheme)
        }
    }

    private fun applyDynamicColors(activity: Activity, theme: ThemeItem) {
        if (!DynamicColors.isDynamicColorAvailable()) return
        val options =
            if (theme.key == CUSTOM_THEME_KEY) {
                DynamicColorsOptions.Builder().setContentBasedSource(customSeedColor(activity)).build()
            } else {
                DynamicColorsOptions.Builder().build()
            }
        DynamicColors.applyToActivityIfAvailable(activity, options)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is AppCompatActivity) return
        val themeRes = currentThemeRes()
        val themesList = activity.applicationContext.getThemeList()
        val currentTheme: ThemeItem = themesList.find { it.themeID == themeRes } ?: themesList[1]
        val themeSignature = currentThemeSignature(themeRes, currentTheme)
        val appliedSignature = appliedThemes[activity]
        if (appliedSignature != null && appliedSignature != themeSignature) {
            // avoid re-triggering on the recreated instance's own resume
            appliedThemes.remove(activity)
            activity.recreate()
        }
    }

    private fun currentThemeRes(): Int =
        preferences.getInt(
            SettingsFragment.KEY_THEME_NEW,
            R.style.Theme_Unchained_Material3_Green_One,
        )

    private fun currentThemeSignature(themeRes: Int, theme: ThemeItem): Int =
        if (theme.key == CUSTOM_THEME_KEY) {
            // the style id alone can't tell two different custom colors apart, since both use
            // the same DynamicCustom style; fold the actual seed color into the signature too
            themeRes * 31 + preferences.getInt(PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY, 0)
        } else {
            themeRes
        }

    private fun customSeedColor(activity: Activity): Int {
        val defaultSeedColor = ContextCompat.getColor(activity, R.color.green_one_theme_primary)
        return preferences.getInt(PreferenceKeys.Ui.CUSTOM_THEME_SEED_COLOR_KEY, defaultSeedColor)
    }

    private fun currentThemeRes(): Int =
        preferences.getInt(
            SettingsFragment.KEY_THEME_NEW,
            R.style.Theme_Unchained_Material3_Green_One,
        )

    private fun setCustomTheme(activity: Activity, themeRes: Int) {
        activity.setTheme(themeRes)
        /*
        // todo: check if this can be avoided, android:navigationBarColor in xml is not working
        activity.window.statusBarColor = activity.getThemeColor(R.attr.customStatusBarColor)
        activity.window.navigationBarColor = activity.getThemeColor(R.attr.customNavigationBarColor)
         */
    }

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        appliedThemes.remove(activity)
    }

    /** Set the night mode depending on the user preferences and what the theme support */
    private fun setupNightMode(themeNightModeSupport: String) {
        when (themeNightModeSupport) {
            THEME_AUTO -> {
                when (preferences.getString(SettingsFragment.KEY_DAY_NIGHT, "auto")) {
                    THEME_AUTO ->
                        AppCompatDelegate.setDefaultNightMode(
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )
                    THEME_DAY ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    THEME_NIGHT ->
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            }
            THEME_DAY -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_NIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}
