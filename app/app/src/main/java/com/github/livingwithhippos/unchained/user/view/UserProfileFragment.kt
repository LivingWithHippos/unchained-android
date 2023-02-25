package com.github.livingwithhippos.unchained.user.view

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentUserProfileBinding
import com.github.livingwithhippos.unchained.settings.view.SettingsActivity
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.KEY_REFERRAL_ASKED
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment.Companion.KEY_REFERRAL_USE
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import com.github.livingwithhippos.unchained.user.viewmodel.UserProfileViewModel
import com.github.livingwithhippos.unchained.utilities.ACCOUNT_LINK
import com.github.livingwithhippos.unchained.utilities.REFERRAL_LINK
import com.github.livingwithhippos.unchained.utilities.extension.openExternalWebPage
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/** A simple [UnchainedFragment] subclass. Shows a user profile details. */
@AndroidEntryPoint
class UserProfileFragment : UnchainedFragment() {

    private val viewModel: UserProfileViewModel by viewModels()

    @Inject lateinit var preferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val userBinding = FragmentUserProfileBinding.inflate(inflater, container, false)

        viewModel.fetchUserInfo()

        viewModel.userLiveData.observe(viewLifecycleOwner) {
            if (it != null) {
                userBinding.user = it
                lifecycleScope.launch {
                    userBinding.privateToken = activityViewModel.isTokenPrivate()
                }
            }
        }

        userBinding.bAccount.setOnClickListener {
            // if we never asked, show a dialog
            if (!preferences.getBoolean(KEY_REFERRAL_ASKED, false)) {
                // set asked as true
                with(preferences.edit()) {
                    putBoolean(KEY_REFERRAL_ASKED, true)
                    apply()
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.referral))
                    .setMessage(getString(R.string.referral_proposal))
                    .setNegativeButton(getString(R.string.decline)) { _, _ ->
                        with(preferences.edit()) {
                            putBoolean(KEY_REFERRAL_USE, false)
                            apply()
                        }
                        context?.openExternalWebPage(ACCOUNT_LINK)
                    }
                    .setPositiveButton(getString(R.string.accept)) { _, _ ->
                        with(preferences.edit()) {
                            putBoolean(KEY_REFERRAL_USE, true)
                            apply()
                        }
                        context?.openExternalWebPage(REFERRAL_LINK)
                    }
                    .show()
            } else {
                if (preferences.getBoolean(KEY_REFERRAL_USE, false))
                    context?.openExternalWebPage(REFERRAL_LINK)
                else context?.openExternalWebPage(ACCOUNT_LINK)
            }
        }

        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.peekContent()) {
                    is FSMAuthenticationState.WaitingUserAction -> {
                        // an error occurred, check it and eventually go back to the start fragment
                        val action = UserProfileFragmentDirections.actionUserToStartFragment()
                        findNavController().navigate(action)
                    }
                    FSMAuthenticationState.StartNewLogin -> {
                        // the user reset the login, go to the auth fragment
                        val action =
                            UserProfileFragmentDirections.actionUserToAuthenticationFragment()
                        findNavController().navigate(action)
                    }
                    FSMAuthenticationState.AuthenticatedOpenToken,
                    FSMAuthenticationState.AuthenticatedPrivateToken,
                    FSMAuthenticationState.RefreshingOpenToken -> {
                        // managed by activity
                    }
                    FSMAuthenticationState.CheckCredentials -> {
                        // shouldn't matter
                    }
                    FSMAuthenticationState.Start,
                    FSMAuthenticationState.WaitingToken,
                    FSMAuthenticationState.WaitingUserConfirmation -> {
                        // shouldn't happen
                    }
                }
            }
        }

        userBinding.bSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            activityViewModel.requireNotificationPermissions()
        }

        return userBinding.root
    }
}
