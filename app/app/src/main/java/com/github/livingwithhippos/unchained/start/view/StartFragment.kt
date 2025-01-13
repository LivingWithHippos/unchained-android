package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.data.model.UserAction
import com.github.livingwithhippos.unchained.databinding.FragmentStartBinding
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationEvent
import com.github.livingwithhippos.unchained.statemachine.authentication.FSMAuthenticationState
import timber.log.Timber

/**
 * A simple [UnchainedFragment] subclass. The starting fragment of the app. It navigates the user to
 * either the authentication process or the profile fragment, depending on the saved credentials
 * status.
 */
class StartFragment : UnchainedFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentStartBinding.inflate(inflater, container, false)

        activityViewModel.fsmAuthenticationState.observe(viewLifecycleOwner) {
            if (it != null) {
                when (it.peekContent()) {
                    FSMAuthenticationState.StartNewLogin -> {
                        val action =
                            StartFragmentDirections.actionStartFragmentToAuthenticationFragment()
                        findNavController().navigate(action)
                    }
                    FSMAuthenticationState.AuthenticatedOpenToken -> {
                        val action =
                            StartFragmentDirections.actionStartFragmentToUserProfileFragment()
                        findNavController().navigate(action)
                        activityViewModel.goToStartUpScreen()
                    }
                    FSMAuthenticationState.AuthenticatedPrivateToken -> {
                        val action =
                            StartFragmentDirections.actionStartFragmentToUserProfileFragment()
                        findNavController().navigate(action)
                        activityViewModel.goToStartUpScreen()
                    }
                    is FSMAuthenticationState.WaitingUserAction -> {
                        // todo: show action needed

                        binding.loadingCircle.visibility = View.INVISIBLE
                        binding.buttonsLayout.visibility = View.VISIBLE

                        val actionNeeded =
                            (it.peekContent() as FSMAuthenticationState.WaitingUserAction).action
                        binding.tvErrorMessage.text =
                            when (actionNeeded) {
                                UserAction.PERMISSION_DENIED ->
                                    getString(R.string.permission_denied)
                                UserAction.TFA_NEEDED -> getString(R.string.tfa_needed)
                                UserAction.TFA_PENDING -> getString(R.string.tfa_pending)
                                UserAction.IP_NOT_ALLOWED ->
                                    getString(R.string.ip_Address_not_allowed)
                                UserAction.UNKNOWN -> getString(R.string.generic_login_error)
                                UserAction.NETWORK_ERROR -> getString(R.string.network_error)
                                UserAction.RETRY_LATER -> getString(R.string.retry_later)
                                null -> getString(R.string.generic_login_error)
                            }
                    }
                    else -> {
                        // ignore other statuses
                        Timber.d("AuthMachine State: ${it.peekContent()}")
                    }
                }
            }
        }

        binding.bRetry.setOnClickListener {
            activityViewModel.transitionAuthenticationMachine(
                FSMAuthenticationEvent.OnUserActionRetry
            )
            binding.loadingCircle.visibility = View.VISIBLE
            binding.buttonsLayout.visibility = View.INVISIBLE
        }

        binding.bReset.setOnClickListener {
            activityViewModel.transitionAuthenticationMachine(
                FSMAuthenticationEvent.OnUserActionReset
            )
            binding.loadingCircle.visibility = View.VISIBLE
            binding.buttonsLayout.visibility = View.INVISIBLE
        }

        return binding.root
    }
}
