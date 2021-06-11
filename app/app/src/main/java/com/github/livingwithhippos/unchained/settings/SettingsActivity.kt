package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.setNavigationBarColor
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

        // no bottom app bar, change the color of the navigation bar from the primary one
        val surfaceColor = getThemeColor(R.attr.colorSurface)
        setNavigationBarColor(surfaceColor)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
