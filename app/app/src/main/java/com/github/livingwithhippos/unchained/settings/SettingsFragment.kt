package com.github.livingwithhippos.unchained.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.utilities.FEEDBACK_URL
import com.github.livingwithhippos.unchained.utilities.GPLV3_URL
import com.github.livingwithhippos.unchained.utilities.PLUGINS_URL
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A simple [PreferenceFragmentCompat] subclass.
 * Manages the interactions with the items in the preferences menu
 */
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var preferences: SharedPreferences

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val dayNightPreference = findPreference<ListPreference>(KEY_DAY_NIGHT)
        val themePreference = findPreference<ListPreference>(KEY_THEME)

        dayNightPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue != THEME_DAY && themePreference?.value == "tropical_sunset") {
                context?.showToast(R.string.theme_day_support)
                false
            } else
                true
        }

        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue == "tropical_sunset" && dayNightPreference?.entry != THEME_DAY) {
                setNightMode(THEME_DAY)
                // todo: this produces a flicker. If possible find another way to update only the dayNightPreference summary, or restart the app to apply it.
                // update the  dayNightPreference summary
                setPreferencesFromResource(R.xml.settings, rootKey)
            }
            true
        }
    }

    private fun setNightMode(nightMode: String) {
        with(preferences.edit()) {
            putString(KEY_DAY_NIGHT, nightMode)
            apply()
        }

    }

    override fun onPreferenceTreeClick(preference: Preference?): Boolean {
        when (preference?.key) {
            "feedback" -> openExternalWebPage(FEEDBACK_URL)
            "license" -> openExternalWebPage(GPLV3_URL)
            "credits" -> openCreditsDialog()
            "terms" -> openTermsDialog()
            "privacy" -> openPrivacyDialog()
            "update_regexps" -> {
                viewModel.updateRegexps()
                context?.showToast(R.string.updating_link_matcher)
            }
            "open_github_plugins" -> openExternalWebPage(PLUGINS_URL)
            "delete_external_plugins" -> {
                val removedPlugins = viewModel.removeExternalPlugins(requireContext())
                if (removedPlugins >= 0)
                    context?.showToast(getString(R.string.plugin_removed, removedPlugins))
                else
                    context?.showToast(getString(R.string.error))
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
        const val KEY_TORRENT_NOTIFICATIONS = "notification_torrent_key"
        const val KEY_REFERRAL_ASKED = "referral_asked_key"
        const val KEY_REFERRAL_USE = "use_referral_key"
        const val THEME_AUTO = "auto"
        const val THEME_NIGHT = "night"
        const val THEME_DAY = "day"
        const val THEME_ORIGINAL = "original"
        const val THEME_TROPICAL = "tropical_sunset"
    }
}