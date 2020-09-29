package com.github.livingwithhippos.unchained.base

import android.content.SharedPreferences
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.settings.SettingsFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
open class UnchainedActivity: AppCompatActivity() {

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        when (preferences.getString(SettingsFragment.KEY_THEME, "original")) {
            // this theme is the original one
            "original" -> setTheme(R.style.Theme_Unchained)
        }
    }
}