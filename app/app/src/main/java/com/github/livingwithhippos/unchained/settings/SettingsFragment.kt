package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.FEEDBACK_URL
import com.github.livingwithhippos.unchained.utilities.GPLV3_URL
import com.github.livingwithhippos.unchained.utilities.openExternalWebPage


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "feedback" -> openExternalWebPage(FEEDBACK_URL)
            "license" -> openExternalWebPage(GPLV3_URL)
            else -> return super.onPreferenceTreeClick(preference)
        }

        return true
    }
}