package com.github.livingwithhippos.unchained.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.FEEDBACK_URL
import com.github.livingwithhippos.unchained.utilities.GPLV3_URL
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import javax.inject.Inject

/**
 * A simple [PreferenceFragmentCompat] subclass.
 * Manages the interactions with the items in the preferences menu
 */
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferences: SharedPreferences

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
            "privacy" -> {
                openPrivacyDialog()
            }
            else -> return super.onPreferenceTreeClick(preference)
        }

        return true
    }

    private fun openCreditsDialog() {
        val dialog = HtmlDialogFragment(R.string.credits_title, R.string.credits_text)
        dialog.show(parentFragmentManager, "CreditsDialogFragment")
    }

    private fun openTermsDialog() {
        val dialog = HtmlDialogFragment(R.string.terms_title, R.string.terms_text)
        dialog.show(parentFragmentManager, "TermsDialogFragment")
    }

    private fun openPrivacyDialog() {
        val dialog = HtmlDialogFragment(R.string.privacy_policy_title, R.string.privacy_text)
        dialog.show(parentFragmentManager, "TermsDialogFragment")
    }

    companion object {
        // these must match the ones used in [xml/settings.xml]
        const val KEY_DAY_NIGHT = "day_night_theme"
        const val KEY_THEME = "current_theme"
        const val KEY_THEME_AUTO = 0
        const val KEY_THEME_NIGHT = 1
        const val KEY_THEME_DAY = 2
    }
}