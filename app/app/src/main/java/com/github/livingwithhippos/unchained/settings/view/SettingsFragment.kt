package com.github.livingwithhippos.unchained.settings.view

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.ThemingCallback.Companion.DAY_ONLY_THEMES
import com.github.livingwithhippos.unchained.settings.viewmodel.SettingsViewModel
import com.github.livingwithhippos.unchained.utilities.FEEDBACK_URL
import com.github.livingwithhippos.unchained.utilities.GPLV3_URL
import com.github.livingwithhippos.unchained.utilities.PLUGINS_URL
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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

    private val pickDirectoryLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) {
            if (it != null) {
                Timber.d("User has picked a folder $it")

                // permanent permissions
                val contentResolver = requireContext().contentResolver

                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                contentResolver.takePersistableUriPermission(it, takeFlags)

                viewModel.setDownloadFolder(it)
            } else {
                Timber.d("User has not picked a folder")
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val dayNightPreference = findPreference<ListPreference>(KEY_DAY_NIGHT)
        val themePreference = findPreference<ListPreference>(KEY_THEME)

        dayNightPreference?.setOnPreferenceChangeListener { _, newValue ->
            if (newValue != THEME_DAY && DAY_ONLY_THEMES.contains(themePreference?.value)) {
                context?.showToast(R.string.theme_day_support)
                false
            } else
                true
        }

        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            if (DAY_ONLY_THEMES.contains(newValue) && dayNightPreference?.entry != THEME_DAY) {
                setNightMode(THEME_DAY)
                // todo: this produces a flicker. If possible find another way to update only the dayNightPreference summary, or restart the app to apply it.
                // update the  dayNightPreference summary
                setPreferencesFromResource(R.xml.settings, rootKey)
            }
            true
        }

        findPreference<EditTextPreference>("filter_size_mb")?.setOnBindEditTextListener {
            it.keyListener = DigitsKeyListener.getInstance("0123456789")
        }

        setupKodi()

        setupVersion()

        findPreference<Preference>("download_folder_key")?.setOnPreferenceClickListener {
            pickDirectoryLauncher.launch(null)
            true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel.kodiLiveData.observe(viewLifecycleOwner) {
            when (it.getContentIfNotHandled()) {
                true -> {
                    context?.showToast(R.string.kodi_connection_successful)
                }
                false -> {
                    context?.showToast(R.string.kodi_connection_error)
                }
                null -> {
                }
            }
        }

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun setupKodi() {

        findPreference<Preference>("kodi_remote_control_info")?.setOnPreferenceClickListener {
            context?.openExternalWebPage("https://kodi.wiki/view/Settings/Services/Control") ?: false
        }
        findPreference<Preference>("kodi_list_editor")?.setOnPreferenceClickListener {
            openKodiManagementDialog()
            true
        }
        // todo: sistema per kodi
        val ipPreference = findPreference<EditTextPreference>("kodi_ip_address")
        val portPreference = findPreference<EditTextPreference>("kodi_port")

        // todo: aside from ips are domains accepted? remove this in that case
        ipPreference?.setOnBindEditTextListener {
            it.keyListener = DigitsKeyListener.getInstance("0123456789.")
        }
        portPreference?.setOnBindEditTextListener {
            it.keyListener = DigitsKeyListener.getInstance("0123456789")
        }

        portPreference?.setOnPreferenceChangeListener { _, newValue ->
            val portVal: Int? = newValue.toString().toIntOrNull()
            if (portVal != null && portVal > 0 && portVal <= 65535) {
                true
            } else {
                context?.showToast(R.string.port_range_error)
                false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun setupVersion() {
        val pi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context?.packageManager?.getPackageInfo(requireContext().packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            context?.packageManager?.getPackageInfo(requireContext().packageName, 0)
        }
        val version = pi?.versionName
        val versionPreference = findPreference<Preference>("app_version")
        versionPreference?.summary = version
    }

    private fun setNightMode(nightMode: String) {
        with(preferences.edit()) {
            putString(KEY_DAY_NIGHT, nightMode)
            apply()
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "feedback" -> context?.openExternalWebPage(FEEDBACK_URL)
            "license" -> context?.openExternalWebPage(GPLV3_URL)
            "credits" -> openCreditsDialog()
            "terms" -> openTermsDialog()
            "privacy" -> openPrivacyDialog()
            "update_regexps" -> {
                viewModel.updateRegexps()
                context?.showToast(R.string.updating_link_matcher)
            }
            "open_github_plugins" -> context?.openExternalWebPage(PLUGINS_URL)
            "test_kodi" -> testKodiConnection()
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

    private fun testKodiConnection() {
        val ipPreference = findPreference<EditTextPreference>("kodi_ip_address")
        val portPreference = findPreference<EditTextPreference>("kodi_port")
        val usernamePreference = findPreference<EditTextPreference>("kodi_username")
        val passwordPreference = findPreference<EditTextPreference>("kodi_password")

        val ip = ipPreference?.text
        val port = portPreference?.text?.toIntOrNull() ?: -1
        val username = usernamePreference?.text
        val password = passwordPreference?.text

        if (ip.isNullOrBlank() || port <= 0)
            context?.showToast(R.string.kodi_credentials_incomplete)
        else {
            viewModel.testKodi(ip, port, username, password)
        }
    }

    /**
     * This function opens a dialog to manage the kodi list
     */
    private fun openKodiManagementDialog() {
        val dialog = KodiManagementDialog()
        dialog.show(parentFragmentManager, "KodiManagementDialogFragment")
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
        const val KEY_APP_VERSION = "app_version_key"
        const val KEY_USE_DOH = "use_doh_key"
        const val THEME_AUTO = "auto"
        const val THEME_NIGHT = "night"
        const val THEME_DAY = "day"
    }
}
