package com.github.livingwithhippos.unchained.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.FEEDBACK_URL
import com.github.livingwithhippos.unchained.utilities.GPLV3_URL
import com.github.livingwithhippos.unchained.utilities.openExternalWebPage
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "feedback" -> openExternalWebPage(FEEDBACK_URL)
            "license" -> openExternalWebPage(GPLV3_URL)
            "credits" -> {
                openCreditsDialog()
            }
            "terms" -> {
                openTermsDialog()
            }
            else -> return super.onPreferenceTreeClick(preference)
        }

        return true
    }

    private fun openCreditsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.credits_title))
            .setMessage(resources.getString(R.string.credits_text))
            .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }

    private fun openTermsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(resources.getString(R.string.terms_title))
            .setMessage(resources.getString(R.string.terms_text))
            .setNeutralButton(resources.getString(R.string.close)) { dialog, _ ->
                dialog.cancel()
            }
            .show()
    }
}