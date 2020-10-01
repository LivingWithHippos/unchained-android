package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedActivity

/**
 * A simple [UnchainedActivity] subclass.
 * Used to navigate from any fragment to the settings screen since the multiple backstack navigation makes it kind of complicated.
 */
class SettingsActivity : UnchainedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //todo: restore MainActivity fragment on back press
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