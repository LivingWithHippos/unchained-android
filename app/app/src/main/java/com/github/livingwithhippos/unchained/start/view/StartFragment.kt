package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.AuthenticationStatus
import com.github.livingwithhippos.unchained.data.model.UserAction
import com.github.livingwithhippos.unchained.databinding.FragmentStartBinding
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass.
 * The starting fragment of the app. It navigates the user to either the authentication process or the profile fragment, depending on the saved credentials status.
 */
@AndroidEntryPoint
class StartFragment : UnchainedFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentStartBinding.inflate(inflater, container, false)

        activityViewModel.newAuthenticationState.observe(
            viewLifecycleOwner,
            {
                when (it.peekContent()) {
                    is AuthenticationStatus.Authenticated -> {
                        try {
                            val action =
                                StartFragmentDirections.actionStartFragmentToUserProfileFragment()
                            findNavController().navigate(action)
                        } catch (e: IllegalArgumentException) {
                            Timber.e("This could have been a crash")
                            // todo: fix this and remove this bypass
                        }
                    }
                    is AuthenticationStatus.AuthenticatedNoPremium -> {
                        try {
                            val action =
                                StartFragmentDirections.actionStartFragmentToUserProfileFragment()
                            findNavController().navigate(action)
                        } catch (e: IllegalArgumentException) {
                            Timber.e("This could have been a crash")
                            // todo: fix this and remove this bypass
                        }
                    }
                    is AuthenticationStatus.RefreshToken -> {
                        activityViewModel.refreshToken()
                    }
                    is AuthenticationStatus.Unauthenticated -> {
                        val action =
                            StartFragmentDirections.actionStartFragmentToAuthenticationFragment()
                        findNavController().navigate(action)
                    }
                    is AuthenticationStatus.NeedUserAction -> {

                        binding.loadingCircle.visibility = View.INVISIBLE
                        binding.bRetry.visibility = View.VISIBLE

                        val actionNeeded =
                            (it.peekContent() as AuthenticationStatus.NeedUserAction).actionNeeded
                        binding.tvErrorMessage.text = when (actionNeeded) {
                            UserAction.PERMISSION_DENIED -> getString(R.string.permission_denied)
                            UserAction.TFA_NEEDED -> getString(R.string.tfa_needed)
                            UserAction.TFA_PENDING -> getString(R.string.tfa_pending)
                            UserAction.IP_NOT_ALLOWED -> getString(R.string.ip_Address_not_allowed)
                            UserAction.UNKNOWN -> getString(R.string.generic_login_error)
                            UserAction.NETWORK_ERROR -> getString(R.string.network_error)
                            UserAction.RETRY_LATER -> getString(R.string.retry_later)
                        }
                    }
                }
            }
        )

        binding.bRetry.setOnClickListener {
            activityViewModel.setupAuthenticationStatus()
            binding.loadingCircle.visibility = View.VISIBLE
            binding.bRetry.visibility = View.INVISIBLE
        }

        // Inflate the layout for this fragment
        return binding.root
    }
}
