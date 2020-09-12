package com.github.livingwithhippos.unchained.start.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedFragment
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint


/**
 * A simple [Fragment] subclass.
 */
@AndroidEntryPoint
class StartFragment : UnchainedFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // check our credentials and decide to navigate to the user fragment or the authentication one.
        checkCredentialsStatus(activityViewModel)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    private fun checkCredentialsStatus(viewModel: MainActivityViewModel) {
        viewModel.fetchFirstWorkingCredentials()
        viewModel.workingCredentialsLiveData.observe(this, Observer {
            // navigate to user fragment
            if (it?.accessToken != null) {
                val action =
                    StartFragmentDirections.actionStartFragmentToUserProfileFragment(it.accessToken)
                findNavController().navigate(action)
            }
            // no complete credentials: navigate to authentication fragment
            else {
                val action = StartFragmentDirections.actionStartFragmentToAuthenticationFragment()
                findNavController().navigate(action)
            }
        })
    }
}