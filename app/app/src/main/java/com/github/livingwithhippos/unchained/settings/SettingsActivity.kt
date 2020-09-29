package com.github.livingwithhippos.unchained.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A simple [AppCompatActivity] subclass.
 * Used to navigate from any fragment to the settings screen since the multiple backstack navigation makes it kinda complicated.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        when (preferences.getInt(SettingsFragment.KEY_THEME, 0)) {
            // this theme is the original one
            0 -> setTheme(R.style.Theme_Unchained)
        }

        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, settingsFragment)
            .commit()
    }
}