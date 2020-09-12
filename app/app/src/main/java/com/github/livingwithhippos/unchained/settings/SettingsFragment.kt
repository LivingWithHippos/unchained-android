package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R


class SettingsFragment: PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }
}