package com.github.livingwithhippos.unchained.settings.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [AppCompatActivity] subclass.
 * Used to navigate from any fragment to the settings screen since the multiple backstack navigation makes it kind of complicated.
 */
@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        // todo: restore MainActivity fragment on back press
        setSupportActionBar(findViewById(R.id.emptyAppBar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, settingsFragment)
            .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
