package com.github.livingwithhippos.unchained.base

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.livingwithhippos.unchained.settings.SettingsFragment
import com.github.livingwithhippos.unchained.utilities.extension.setCustomTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
open class UnchainedActivity : AppCompatActivity() {

    @Inject
    lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferences.getString(SettingsFragment.KEY_THEME, "original")?.let{
            setCustomTheme(it)
        }
    }
}