package com.github.livingwithhippos.unchained.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.github.livingwithhippos.unchained.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        val settingsFragment = SettingsFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, settingsFragment)
            .commit()
    }
}