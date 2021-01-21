package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [UnchainedFragment] subclass.
 * The starting fragment of the app. It navigates the user to either the authentication process or the profile fragment, depending on the saved credentials status.
 */
@AndroidEntryPoint
class StartFragment : UnchainedFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check our credentials and decide to navigate to the user fragment or the authentication one.
        activityViewModel.userLiveData.observe(this, {
            // navigate to user fragment
            if (it != null) {
                if (it.premium > 0)
                    activityViewModel.setAuthenticated()
                else
                    activityViewModel.setAuthenticatedNoPremium()

                val action =
                    StartFragmentDirections.actionStartFragmentToUserProfileFragment()
                findNavController().navigate(action)
            }
            // no complete credentials: navigate to authentication fragment
            else {
                //todo: check if null could be because of missing network connectivity
                activityViewModel.setUnauthenticated()
                val action = StartFragmentDirections.actionStartFragmentToAuthenticationFragment()
                findNavController().navigate(action)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }
}