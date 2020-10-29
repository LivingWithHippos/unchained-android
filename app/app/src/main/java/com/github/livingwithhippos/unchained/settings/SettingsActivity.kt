package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedActivity
import com.github.livingwithhippos.unchained.utilities.extension.getThemeColor
import com.github.livingwithhippos.unchained.utilities.extension.setCustomTheme
import com.github.livingwithhippos.unchained.utilities.extension.setNavigationBarColor

/**
 * A simple [UnchainedActivity] subclass.
 * Used to navigate from any fragment to the settings screen since the multiple backstack navigation makes it kind of complicated.
 */
class SettingsActivity : UnchainedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setCustomTheme(preferences.getString(SettingsFragment.KEY_THEME, "original")!!)
        setContentView(R.layout.activity_settings)

        //todo: restore MainActivity fragment on back press
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