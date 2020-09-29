package com.github.livingwithhippos.unchained.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A simple [AppCompatActivity] subclass.
 * Used to navigate from any fragment to the settings screen since the multiple backstack navigation makes it kinda complicated.
 */
class SettingsActivity : UnchainedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, settingsFragment)
            .commit()
    }
}