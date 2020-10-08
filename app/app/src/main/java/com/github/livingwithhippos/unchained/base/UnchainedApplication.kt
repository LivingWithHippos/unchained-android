package com.github.livingwithhippos.unchained.base

import android.app.Application
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.settings.SettingsFragment
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Entry point for the Dagger-Hilt injection.
 * Deletes incomplete credentials from the db on start
 */
@HiltAndroidApp
class UnchainedApplication : Application() {
    @Inject
    lateinit var credentialsRepository: CredentialsRepository

    @Inject
    lateinit var preferences: SharedPreferences

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate() {
        super.onCreate()

        scope.launch {
            credentialsRepository.deleteIncompleteCredentials()
        }

        val nightModeArray = resources.getStringArray(R.array.night_mode_values)
        // enable or disable night mode according with the preferences
        when (preferences.getString(SettingsFragment.KEY_DAY_NIGHT, "auto")) {
            nightModeArray[0] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            nightModeArray[1] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            nightModeArray[2] -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}