package com.github.livingwithhippos.unchained.user.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.databinding.FragmentUserProfileBinding
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.user.viewmodel.UserProfileViewModel
import com.github.livingwithhippos.unchained.utilities.openExternalWebPage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


const val REFERRAL_LINK = "http://real-debrid.com/?id=78841"
const val PREMIUM_LINK = "https://real-debrid.com/premium"

/**
 * A simple [UnchainedFragment] subclass.
 */
@AndroidEntryPoint
class UserProfileFragment : UnchainedFragment() {

    private val viewModel: UserProfileViewModel by viewModels()

    val args: UserProfileFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.saveToken(args.token)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val userBinding = FragmentUserProfileBinding.inflate(inflater, container, false)

        viewModel.fetchUserInfo()

        viewModel.userLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                userBinding.user = it
                lifecycleScope.launch {
                    userBinding.privateToken = activityViewModel.isTokenPrivate()
                }
            }
        })

        userBinding.bPremium.setOnClickListener {
            //todo: ask user and either load the referral link
            // or the premium page, add to settings fragment
            openExternalWebPage(REFERRAL_LINK)
        }

        activityViewModel.authenticationState.observe(viewLifecycleOwner, Observer {
            // todo: getContentIfNotHandled() works only if none of the other observers has called it already
            // it's possible to use peek with findNavController().currentDestination to avoid launching the navigate(action) twice (it crsahes)
            // val destination = findNavController().currentDestination
            // val destinationId = findNavController().currentDestination?.id
            when (it.peekContent()) {
                // back to authentication fragment
                MainActivityViewModel.AuthenticationState.UNAUTHENTICATED -> {
                    //todo: empty backstack
                    val action = UserProfileFragmentDirections.actionUserToAuthentication()
                    findNavController().navigate(action)
                }
                //do nothing for now, add other states later
                else -> {
                }
            }
        })

        userBinding.bLogout.setOnClickListener {
            activityViewModel.logout()
        }

        return userBinding.root
    }
}